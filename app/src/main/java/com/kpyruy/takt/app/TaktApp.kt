package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.i18n.t

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.kpyruy.takt.core.ui.motion.TaktMotion
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.feature.calendar.ScheduleEventEditor
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.CompositionLocalProvider
import com.kpyruy.takt.core.ui.components.LocalCourseIconKeys
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
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
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.data.DocumentSyncStatus
import com.kpyruy.takt.core.data.ExamRepository
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.ui.components.TaktAddFab
import com.kpyruy.takt.core.ui.components.TaktBottomNavigation
import com.kpyruy.takt.core.ui.components.TaktNavItem
import com.kpyruy.takt.core.ui.components.TaktIcons
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics
import com.kpyruy.takt.feature.calendar.CalendarScreen
import com.kpyruy.takt.feature.home.HomeScreen
import com.kpyruy.takt.feature.settings.SettingsScreen
import com.kpyruy.takt.feature.studyplan.StudyPlanScreen
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import com.kpyruy.takt.feature.subjects.SubjectsScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private enum class Destination(val route: String, private val ukrainianLabel: String) {
    HOME("home", "Сьогодні"),
    CALENDAR("calendar", "Календар"),
    SUBJECTS("subjects", "Предмети"),
    PLAN("plan", "Прогрес"),

    ;
    val label: String get() = t(ukrainianLabel)
}

private const val SETTINGS_ROUTE = "settings"
private const val SUBJECT_ROUTE = "subject/{courseId}?tab={tab}"
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
    documentStore: TaktDocumentStore,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showRootNavigation = Destination.entries.any { it.route == currentRoute }
    val courses by remember(repository) { repository.observeCourses() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val courseIconKeys = remember(courses) { courses.associate { it.id to it.iconKey } }
    val scope = rememberCoroutineScope()
    val haptics = rememberTaktHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    val documentStatus by documentStore.status.collectAsStateWithLifecycle()
    val unmigratedMaterials by documentStore.unmigratedMaterials.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            runCatching { documentStore.connect(uri) }
                .onSuccess { status ->
                    if (status == DocumentSyncStatus.CONFLICT) navController.navigate(SETTINGS_ROUTE)
                }
                .onFailure { error ->
                    snackbarHostState.showSnackbar(t("Не вдалося підключити Documents: ${error.message}"))
                }
        }
    }

    LaunchedEffect(documentStore, documentStatus) {
        delay(1500)
        if (!documentStore.isConnected) {
            val result = snackbarHostState.showSnackbar(
                message = t("Підключіть Documents/Takt для автозбереження й відновлення даних"),
                actionLabel = t("Підключити"),
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) folderPicker.launch(null)
        }
    }
    LaunchedEffect(documentStatus) {
        if (documentStatus == DocumentSyncStatus.CONFLICT || documentStatus == DocumentSyncStatus.ERROR) {
            val result = snackbarHostState.showSnackbar(
                message = if (documentStatus == DocumentSyncStatus.CONFLICT) {
                    t("Копія в Documents/Takt відрізняється від даних на телефоні")
                } else t("Не вдалося синхронізувати Documents/Takt"),
                actionLabel = t("Перевірити"),
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) navController.navigate(SETTINGS_ROUTE)
        }
    }
    LaunchedEffect(unmigratedMaterials) {
        if (unmigratedMaterials > 0) {
            val result = snackbarHostState.showSnackbar(
                message = t("Не вдалося скопіювати $unmigratedMaterials старих матеріалів"),
                actionLabel = t("Деталі"),
                duration = SnackbarDuration.Indefinite,
            )
            if (result == SnackbarResult.ActionPerformed) navController.navigate(SETTINGS_ROUTE)
        }
    }

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

    var actionEvent by remember { mutableStateOf<ResolvedScheduleEvent?>(null) }

    fun navigateRoot(destination: Destination) {
        if (destination.route == currentRoute) return
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navItems = Destination.entries.map { destination ->
        val icon = when (destination) {
            Destination.HOME -> TaktIcons.Home
            Destination.CALENDAR -> TaktIcons.Calendar
            Destination.SUBJECTS -> TaktIcons.Book
            Destination.PLAN -> TaktIcons.Progress
        }
        TaktNavItem(
            label = destination.label,
            icon = icon,
            selected = currentRoute == destination.route,
            onClick = {
                navigateRoot(destination)
            },
        )
    }

    CompositionLocalProvider(LocalCourseIconKeys provides courseIconKeys) {
    actionEvent?.let { event ->
        ScheduleEventEditor(event, scheduleRepository, courses, onDismiss = { actionEvent = null })
    }
    Scaffold(
        snackbarHost = { if (currentRoute != SETTINGS_ROUTE) SnackbarHost(snackbarHostState) },
        containerColor = if (currentRoute == Destination.HOME.route || currentRoute == Destination.PLAN.route)
            MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showRootNavigation) {
                TaktBottomNavigation(
                    items = navItems,
                )
            }
        },
        floatingActionButton = {
            if (showRootNavigation) TaktAddFab { showGlobalAdd = true }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
            enterTransition = {
                val from = Destination.entries.indexOfFirst { it.route == initialState.destination.route }
                val to = Destination.entries.indexOfFirst { it.route == targetState.destination.route }
                if (from >= 0 && to >= 0) slideInHorizontally(TaktMotion.menu()) { if (to > from) it else -it }
                else fadeIn(TaktMotion.fast())
            },
            exitTransition = {
                val from = Destination.entries.indexOfFirst { it.route == initialState.destination.route }
                val to = Destination.entries.indexOfFirst { it.route == targetState.destination.route }
                if (from >= 0 && to >= 0) slideOutHorizontally(TaktMotion.menu()) { if (to > from) -it else it }
                else fadeOut(TaktMotion.fast())
            },
            modifier = Modifier.padding(padding).testTag("root-content"),
        ) {
            composable(Destination.HOME.route) {
                HomeScreen(
                    repository = repository,
                    scheduleRepository = scheduleRepository,
                    gradeRepository = gradeRepository,
                    studyContentRepository = studyContentRepository,
                    settingsRepository = settingsRepository,
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                    onAddCourse = { startCreate(CreateItemType.COURSE) },
                    onOpenCourse = ::openCourse,
                    onOpenAssessment = { item ->
                        val tab = if (item.type == GradeItemType.EXAM) "grades" else "tasks"
                        navController.navigate("subject/${Uri.encode(item.courseId)}?tab=$tab")
                    },
                    onEventLongClick = { actionEvent = it },
                )
            }
            composable(Destination.CALENDAR.route) {
                CalendarScreen(
                    scheduleRepository = scheduleRepository,
                    studyContentRepository = studyContentRepository,
                    gradeRepository = gradeRepository,
                    studyPlanRepository = repository,
                    onOpenCourse = ::openCourse,
                    onOpenAssessment = { item ->
                        val tab = if (item.type == GradeItemType.EXAM) "grades" else "tasks"
                        navController.navigate("subject/${Uri.encode(item.courseId)}?tab=$tab")
                    },
                    onEventLongClick = { actionEvent = it },
                    settingsRepository = settingsRepository,
                )
            }
            composable(Destination.SUBJECTS.route) {
                SubjectsScreen(
                    repository = repository,
                    gradeRepository = gradeRepository,
                    studyContentRepository = studyContentRepository,
                    onCourseClick = ::openCourse,
                    onAddCourse = { startCreate(CreateItemType.COURSE) },
                )
            }
            composable(Destination.PLAN.route) {
                StudyPlanScreen(
                    repository = repository,
                    settingsRepository = settingsRepository,
                    onCourseClick = ::openCourse,
                    onAddCourse = { startCreate(CreateItemType.COURSE) },
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    backupRepository = backupRepository,
                    documentStore = documentStore,
                    studyPlanRepository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = SUBJECT_ROUTE,
                arguments = listOf(navArgument("courseId") { type = NavType.StringType }, navArgument("tab") { type = NavType.StringType; defaultValue = "overview" }),
            ) { entry ->
                val courseId = entry.arguments?.getString("courseId").orEmpty()
                SubjectDetailScreen(
                    repository = repository,
                    gradeRepository = gradeRepository,
                    scheduleRepository = scheduleRepository,
                    settingsRepository = settingsRepository,
                    studyContentRepository = studyContentRepository,
                    examRepository = examRepository,
                    documentStore = documentStore,
                    courseId = courseId,
                    initialTab = entry.arguments?.getString("tab").orEmpty().takeIf { it in setOf("tasks", "grades") } ?: "overview",
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
                        courses = courses,                        type = type,
                        studyPlanRepository = repository,
                        courseId = courseId,
                        draft = pendingCreateDraft?.takeIf { draft ->
                            draft.type == type &&
                                (draft.courseId == null || draft.courseId == courseId)
                        },
                        scheduleRepository = scheduleRepository,
                        settingsRepository = settingsRepository,
                        gradeRepository = gradeRepository,
                        studyContentRepository = studyContentRepository,
                        documentStore = documentStore,
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
