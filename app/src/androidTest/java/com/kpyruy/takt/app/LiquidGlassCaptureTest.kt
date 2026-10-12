package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.calendar.CalendarScreen
import com.kpyruy.takt.feature.calendar.ScheduleEventEditor
import com.kpyruy.takt.feature.home.HomeScreen
import com.kpyruy.takt.feature.settings.SettingsScreen
import com.kpyruy.takt.feature.studyplan.StudyPlanScreen
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/** Rendering harness: real screens and repository state, no network or production demo data. */
class LiquidGlassCaptureTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val data get() = (context.applicationContext as TaktApplication).dataContainer
    private var screen by mutableStateOf("home-refined")
    private var modal by mutableStateOf<String?>(null)
    private var actionEvent by mutableStateOf<ResolvedScheduleEvent?>(null)
    private val roots = listOf("home-refined", "calendar-day", "subjects", "progress")

    private fun start() {
        runBlocking { data.seedLiquidGlassReview() }
        compose.setContent {
            val saved by data.settingsRepository.settings.collectAsStateWithLifecycle()
            val dark = screen.endsWith("-dark")
            TaktI18n.use(AppLanguage.UKRAINIAN)
            TaktTheme(saved.copy(themeMode = if (dark) AppThemeMode.DARK else AppThemeMode.LIGHT)) {
                val activity = checkNotNull(LocalActivity.current)
                val view = LocalView.current
                SideEffect {
                    androidx.core.view.WindowCompat.getInsetsController(activity.window, view).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                val snapshot by data.planningSnapshot.collectAsStateWithLifecycle()
                val courses = snapshot?.courses.orEmpty()
                CompositionLocalProvider(LocalCourseIconKeys provides courses.associate { it.id to it.iconKey }) {
                    TaktGlassBackground {
                        val base = if (screen == "home-dark") "home-refined" else screen
                        val root = base in roots
                        Scaffold(
                            containerColor = Color.Transparent,
                            bottomBar = {
                                if (root) TaktBottomNavigation(listOf(
                                    TaktNavItem("Сьогодні", TaktIcons.Home, base == "home-refined") { screen = "home-refined" },
                                    TaktNavItem("Календар", TaktIcons.Calendar, base == "calendar-day") { screen = "calendar-day" },
                                    TaktNavItem("Предмети", TaktIcons.Book, base == "subjects") { screen = "subjects" },
                                    TaktNavItem("Прогрес", TaktIcons.Progress, base == "progress") { screen = "progress" },
                                ))
                            },
                            floatingActionButton = { if (root) TaktAddFab { modal = "add-menu" } },
                        ) { padding ->
                            Box(Modifier.fillMaxSize().padding(padding)) {
                                key(screen) {
                                    when {
                                        base == "home-refined" -> HomeScreen(data.planningSnapshot, data.studyContentRepository,
                                            data.settingsRepository, onOpenSettings = { screen = "settings" },
                                            onAddCourse = { screen = "create-course" }, onOpenCourse = { screen = "subject-overview" },
                                            onOpenAssessment = { screen = "subject-tasks" }, onEventLongClick = { actionEvent = it })
                                        base == "calendar-day" -> CalendarScreen(data.planningSnapshot, data.studyContentRepository,
                                            data.settingsRepository, onOpenCourse = { screen = "subject-overview" },
                                            onOpenAssessment = { screen = "subject-tasks" }, onEventLongClick = { actionEvent = it })
                                        base == "subjects" -> com.kpyruy.takt.feature.subjects.SubjectsScreen(data.studyPlanRepository,
                                            data.gradeRepository, data.studyContentRepository, onCourseClick = { screen = "subject-overview" },
                                            onAddCourse = { screen = "create-course" })
                                        base == "progress" -> StudyPlanScreen(data.studyPlanRepository, data.gradeRepository,
                                            data.settingsRepository, data.universityAccountRepository,
                                            onCourseClick = { screen = "subject-overview" }, onAddCourse = { screen = "create-course" })
                                        screen.startsWith("subject-") -> SubjectDetailScreen(data.studyPlanRepository, data.gradeRepository,
                                            data.scheduleRepository, data.settingsRepository, data.studyContentRepository, data.examRepository,
                                            data.documentStore, courseId = "glass-physics", onBack = { screen = "subjects" },
                                            initialTab = screen.removePrefix("subject-").removeSuffix("-dark"))
                                        base == "settings" -> SettingsScreen(data.settingsRepository, data.backupRepository,
                                            data.documentStore, data.studyPlanRepository, data.universityAccountRepository,
                                            authenticateDevice = { _, callback -> callback(DeviceAuthenticationResult.CANCELLED) },
                                            appReleaseRepository = showcaseReleases(), onBack = { screen = "home-refined" })
                                        screen.startsWith("create-") -> CreateItemScreen(type = createType(screen),
                                            courseId = if (createType(screen).requiresCourse) "glass-physics" else null,
                                            draft = null, courses = courses, studyPlanRepository = data.studyPlanRepository,
                                            scheduleRepository = data.scheduleRepository, settingsRepository = data.settingsRepository,
                                            gradeRepository = data.gradeRepository, studyContentRepository = data.studyContentRepository,
                                            documentStore = data.documentStore, onBack = { screen = "home-refined" }, onSaved = {})
                                        base == "onboarding" -> TaktWelcomeScreen({}, {})
                                        base == "lock" -> DeviceLockScreen(null) {}
                                    }
                                }
                            }
                        }
                        when (modal) {
                            "add-menu" -> GlobalAddSheet(onDismiss = { modal = null },
                                onCreate = { screen = "create-${it.name.lowercase()}"; modal = null },
                                onQuickAdd = { modal = "quick-add" })
                            "course-select" -> SelectCourseSheet(courses, onDismiss = { modal = null }, onSelected = { modal = null })
                            "quick-add" -> QuickAddSheet(courses, onDismiss = { modal = null }, onSaveTask = {}, onSaveLesson = {},
                                onSaveNote = {}, onOpenFull = { modal = null })
                        }
                        actionEvent?.let { ScheduleEventEditor(it, data.scheduleRepository, courses) { actionEvent = null } }
                    }
                }
            }
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Твій день").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun captureMainAndSubjectScreens() {
        start()
        val requested = instrumentation.arguments.getString("glassScreens")?.split(",")
        val names = requested ?: listOf("home-refined", "calendar-day", "subjects", "progress", "subject-overview",
            "subject-tasks", "subject-exam", "subject-notes", "home-dark", "subject-tasks-dark")
        for (name in names) {
            switch(name)
            capture(name)
        }
    }

    @Test fun captureFormsAndRootSheets() {
        start()
        for (name in listOf("onboarding", "lock", "create-test", "create-task", "create-note", "create-course",
            "create-lesson", "create-exam", "create-event", "create-reminder")) {
            switch(name)
            capture(name)
        }
        switch("home-refined")
        for (name in listOf("add-menu", "quick-add", "course-select")) {
            compose.runOnIdle { modal = name }
            capture(name)
            compose.runOnIdle { modal = null }
            compose.waitForIdle()
        }
    }

    @Test fun captureCalendarSettingsAndSubjectDialogs() {
        start()
        switch("calendar-day")
        compose.onNode(hasText("Тиждень") and hasClickAction()).performClick()
        capture("calendar-week")
        compose.onNode(hasText("Місяць") and hasClickAction()).performClick()
        capture("calendar-month")
        for ((name, label) in listOf("settings" to null, "appearance" to "Вигляд", "language" to "Мова",
            "calendar-settings" to "Календар і розклад", "task-settings" to "Задачі", "periods" to "Періоди навчання",
            "uis-account" to "Університетська система", "uis-sync" to "Автооновлення UIS", "updates" to "Оновлення додатка")) {
            switch("settings")
            label?.let { compose.onNode(hasText(it) and hasClickAction()).performScrollTo().performClick() }
            capture(name)
        }
        switch("subject-overview")
        compose.onNodeWithContentDescription("Налаштування предмета").performClick()
        capture("course-settings")
    }

    private fun switch(name: String) {
        compose.runOnIdle { modal = null; actionEvent = null; screen = "transition" }
        compose.waitForIdle()
        compose.runOnIdle { screen = name }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        // A semantics frame can settle before SurfaceFlinger presents a new dialog window.
        android.os.SystemClock.sleep(1200)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Screenshot unavailable: $name" }
        context.openFileOutput("glass-$name.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun createType(name: String) = when (name) {
        "create-lesson", "create-class" -> CreateItemType.CLASS
        else -> CreateItemType.valueOf(name.removePrefix("create-").uppercase())
    }

    private fun showcaseReleases(): AppReleaseRepository {
        val store = object : AppReleasePreferenceStore {
            var value = AppReleasePreferences(latest = AppRelease("v0.2.0", "https://github.com/Kpyruy/Takt/releases/tag/v0.2.0"),
                lastSuccessfulCheck = System.currentTimeMillis())
            override fun read() = value
            override fun write(value: AppReleasePreferences) { this.value = value }
        }
        return AppReleaseRepository("0.2.0", store, { checkNotNull(store.value.latest) })
    }
}
