package com.kpyruy.takt.app

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.kpyruy.takt.core.data.ExamRepository
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.ui.components.TaktAddFab
import com.kpyruy.takt.core.ui.components.TaktBottomNavigation
import com.kpyruy.takt.core.ui.components.TaktNavItem
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics
import com.kpyruy.takt.feature.calendar.CalendarScreen
import com.kpyruy.takt.feature.home.HomeQuickAction
import com.kpyruy.takt.feature.home.HomeScreen
import com.kpyruy.takt.feature.settings.SettingsScreen
import com.kpyruy.takt.feature.studyplan.StudyPlanScreen
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import com.kpyruy.takt.feature.subjects.SubjectsScreen
import kotlinx.coroutines.launch

private enum class Destination(val route: String, val label: String) {
    HOME("home", "Головна"),
    CALENDAR("calendar", "Календар"),
    SUBJECTS("subjects", "Предмети"),
    PLAN("plan", "План"),
}

private const val SETTINGS_ROUTE = "settings"
private const val SUBJECT_ROUTE = "subject/{courseId}"
private const val CREATE_ROUTE = "create/{type}?courseId={courseId}"

@Composable
fun TaktApp(
    repository: StudyPlanRepository,
    scheduleRepository: ScheduleRepository,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    examRepository: ExamRepository,
    settingsRepository: AppSettingsRepository,
    backupRepository: BackupRepository,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showRootNavigation = Destination.entries.any { it.route == currentRoute }
    val courses by repository.observeCourses().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val haptics = rememberTaktHaptics()

    var showGlobalAdd by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var courseSelectionFor by remember { mutableStateOf<CreateItemType?>(null) }
    var courseSelectionDraft by remember { mutableStateOf<CreateItemDraft?>(null) }
    var pendingCreateDraft by remember { mutableStateOf<CreateItemDraft?>(null) }

    fun openCourse(courseId: String) {
        navController.navigate("subject/$courseId")
    }

    fun startCreate(
        type: CreateItemType,
        courseId: String? = null,
        draft: CreateItemDraft? = null,
    ) {
        showGlobalAdd = false
        showQuickAdd = false
        if (type.requiresCourse && courseId == null) {
            courseSelectionFor = type
            courseSelectionDraft = draft
            return
        }

        pendingCreateDraft = draft?.copy(courseId = courseId)
        val route = buildString {
            append("create/")
            append(type.name)
            if (courseId != null) {
                append("?courseId=")
                append(Uri.encode(courseId))
            }
        }
        navController.navigate(route)
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
                TaktBottomNavigation(
                    items = navItems,
                    centerContent = {
                        TaktAddFab(onClick = { showGlobalAdd = true })
                    },
                )
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
                    onOpenCourses = { navController.navigate(Destination.SUBJECTS.route) },
                    onQuickAction = { action ->
                        startCreate(
                            when (action) {
                                HomeQuickAction.LESSON -> CreateItemType.CLASS
                                HomeQuickAction.TASK -> CreateItemType.TASK
                                HomeQuickAction.EXAM -> CreateItemType.EXAM
                                HomeQuickAction.NOTE -> CreateItemType.NOTE
                            }
                        )
                    },
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
                    gradeRepository = gradeRepository,
                    studyContentRepository = studyContentRepository,
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
                    examRepository = examRepository,
                    courseId = courseId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = CREATE_ROUTE,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("courseId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val type = runCatching {
                    CreateItemType.valueOf(entry.arguments?.getString("type").orEmpty())
                }.getOrNull()
                val courseId = entry.arguments?.getString("courseId")

                if (type != null) {
                    CreateItemScreen(
                        type = type,
                        courseId = courseId,
                        draft = pendingCreateDraft?.takeIf { draft ->
                            draft.type == type &&
                                (draft.courseId == null || draft.courseId == courseId)
                        },
                        scheduleRepository = scheduleRepository,
                        gradeRepository = gradeRepository,
                        studyContentRepository = studyContentRepository,
                        onBack = {
                            pendingCreateDraft = null
                            navController.popBackStack()
                        },
                        onSaved = {
                            pendingCreateDraft = null
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }

    if (showGlobalAdd) {
        GlobalAddSheet(
            onDismiss = { showGlobalAdd = false },
            onCreate = { type -> startCreate(type) },
            onQuickAdd = {
                showGlobalAdd = false
                showQuickAdd = true
            },
        )
    }

    if (showQuickAdd) {
        QuickAddSheet(
            courses = courses,
            onDismiss = { showQuickAdd = false },
            onSaveTask = {
                scope.launch {
                    studyContentRepository.upsertTask(it)
                    haptics.confirm()
                    showQuickAdd = false
                }
            },
            onSaveLesson = {
                scope.launch {
                    scheduleRepository.upsertRule(it)
                    haptics.confirm()
                    showQuickAdd = false
                }
            },
            onSaveNote = {
                scope.launch {
                    studyContentRepository.upsertNote(it)
                    haptics.confirm()
                    showQuickAdd = false
                }
            },
            onOpenFull = { draft ->
                startCreate(draft.type, draft.courseId, draft)
            },
        )
    }

    courseSelectionFor?.let { type ->
        SelectCourseSheet(
            courses = courses,
            onDismiss = { courseSelectionFor = null },
            onSelected = { course ->
                val draft = courseSelectionDraft
                courseSelectionFor = null
                courseSelectionDraft = null
                startCreate(type, course.id, draft)
            },
        )
    }

}
