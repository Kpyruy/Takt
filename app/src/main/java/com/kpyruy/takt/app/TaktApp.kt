package com.kpyruy.takt.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.BackupRepository
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.ui.components.TaktBottomNavigation
import com.kpyruy.takt.core.ui.components.TaktNavItem
import com.kpyruy.takt.feature.calendar.CalendarScreen
import com.kpyruy.takt.feature.home.HomeScreen
import com.kpyruy.takt.feature.settings.SettingsScreen
import com.kpyruy.takt.feature.studyplan.StudyPlanScreen
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import com.kpyruy.takt.feature.subjects.SubjectsScreen

private enum class Destination(val route: String, val label: String) {
    HOME("home", "Головна"),
    CALENDAR("calendar", "Календар"),
    SUBJECTS("subjects", "Предмети"),
    PLAN("plan", "План"),
}

private const val SETTINGS_ROUTE = "settings"
private const val SUBJECT_ROUTE = "subject/{courseId}"

@Composable
fun TaktApp(
    repository: StudyPlanRepository,
    scheduleRepository: ScheduleRepository,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    settingsRepository: AppSettingsRepository,
    backupRepository: BackupRepository,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showRootNavigation = Destination.entries.any { it.route == currentRoute }

    fun openCourse(courseId: String) {
        navController.navigate("subject/$courseId")
    }

    val navItems = Destination.entries.map { destination ->
        val icon = when (destination) {
            Destination.HOME -> Icons.Default.Home
            Destination.CALENDAR -> Icons.Default.CalendarMonth
            Destination.SUBJECTS -> Icons.Default.MenuBook
            Destination.PLAN -> Icons.Default.School
        }
        TaktNavItem(
            label = destination.label,
            icon = icon,
            selected = currentRoute == destination.route,
            onClick = {
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }

    Scaffold(
        bottomBar = {
            if (showRootNavigation) {
                TaktBottomNavigation(items = navItems)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.HOME.route) {
                HomeScreen(
                    repository = repository,
                    scheduleRepository = scheduleRepository,
                    gradeRepository = gradeRepository,
                    studyContentRepository = studyContentRepository,
                    settingsRepository = settingsRepository,
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                )
            }
            composable(Destination.CALENDAR.route) {
                CalendarScreen(
                    scheduleRepository = scheduleRepository,
                    studyContentRepository = studyContentRepository,
                    studyPlanRepository = repository,
                    settingsRepository = settingsRepository,
                )
            }
            composable(Destination.SUBJECTS.route) {
                SubjectsScreen(
                    repository = repository,
                    onCourseClick = ::openCourse,
                )
            }
            composable(Destination.PLAN.route) {
                StudyPlanScreen(
                    repository = repository,
                    onCourseClick = ::openCourse,
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    backupRepository = backupRepository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = SUBJECT_ROUTE,
                arguments = listOf(navArgument("courseId") { type = NavType.StringType }),
            ) { entry ->
                val courseId = entry.arguments?.getString("courseId").orEmpty()
                SubjectDetailScreen(
                    repository = repository,
                    gradeRepository = gradeRepository,
                    studyContentRepository = studyContentRepository,
                    courseId = courseId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
