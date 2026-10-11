package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in current UI captures; only run on disposable review emulator. */
class UiReviewCaptureTest {
    @get:Rule val compose = createComposeRule()
    @Test fun captureCurrentExamTasksAndThemes() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("uiReview") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val data = (context.applicationContext as TaktApplication).dataContainer
        val date = LocalDate.of(2026, 10, 11)
        data.database.clearAllTables()
        runBlocking {
            data.database.courseDao().insertAll(listOf(CourseEntity("review", "FYZI_6B", "Фізика", 5, 3,
                CourseStatus.ENROLLED.storageValue, "COMPULSORY", null, gradingType = "EXAM_LETTER")))
            data.studyContentRepository.upsertTask(StudyTask("ordinary", "review", "Повторити формули хвиль", null, date.plusDays(3), false))
            data.gradeRepository.upsertItem(GradeItem("quiz", "review", "Тест: механічні коливання", GradeItemType.TEST, 14.0, 20.0,
                dueDate = date.plusDays(2), completed = true, requiredForExam = true, minimumPointsForExam = 10.0, durationMinutes = 45))
            data.gradeRepository.upsertItem(GradeItem("project", "review", "Звіт про лабораторну роботу з вимірювання прискорення", GradeItemType.LAB, 20.0, 20.0,
                dueDate = date.minusDays(2), completed = true))
            data.gradeRepository.upsertItem(GradeItem("overdue", "review", "Здати протокол експерименту", GradeItemType.LAB, 0.0, 10.0,
                dueDate = date.minusDays(1), completed = false, requiredForExam = true, minimumPointsForExam = 5.0))
            data.gradeRepository.upsertItem(GradeItem("exam", "review", "Підсумковий екзамен", GradeItemType.EXAM, 0.0, 50.0,
                dueDate = LocalDate.of(2026, 12, 18), completed = false))
            data.examRepository.upsertExamInfo(ExamInfo("review", "exam", LocalDate.of(2026, 12, 18), LocalTime.of(9,0), LocalTime.of(11,0), "B-214"))
            data.settingsRepository.setLanguage(AppLanguage.UKRAINIAN)
        }
        val approved = InstrumentationRegistry.getArguments().getString("approvedUiReview") == "true"
        var capture by mutableStateOf("exam")
        compose.setContent {
            TaktI18n.use(AppLanguage.UKRAINIAN)
            val mode = when { capture.contains("dark") -> AppThemeMode.DARK; else -> AppThemeMode.LIGHT }
            val family = if (capture.contains("warm")) ThemeFamily.WARM else ThemeFamily.BLUE
            TaktTheme(AppSettings(themeMode = mode, themeFamily = family, language = AppLanguage.UKRAINIAN)) {
                val scheme = MaterialTheme.colorScheme
                val types = MaterialTheme.typography
                val activity = checkNotNull(LocalActivity.current)
                val view = LocalView.current
                SideEffect {
                    androidx.core.view.WindowCompat.getInsetsController(activity.window, view).apply {
                        isAppearanceLightStatusBars = !capture.contains("dark")
                        isAppearanceLightNavigationBars = !capture.contains("dark")
                    }
                }
                MaterialTheme(
                    colorScheme = if (capture.contains("readable")) scheme.copy(onSurfaceVariant = Color(0xFF58677C), primary = Color(0xFFAE4724)) else scheme,
                    typography = if (capture.contains("readable")) types.copy(bodySmall = types.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp), labelSmall = types.labelSmall.copy(fontSize = 12.sp, lineHeight = 18.sp), labelMedium = types.labelMedium.copy(fontSize = 13.sp, lineHeight = 19.sp)) else types,
                ) { key(capture) {
                    Box(Modifier.fillMaxSize().background(scheme.background).statusBarsPadding().navigationBarsPadding()) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                        SubjectDetailScreen(data.studyPlanRepository, data.gradeRepository, data.scheduleRepository,
                            data.settingsRepository, data.studyContentRepository, data.examRepository, data.documentStore,
                            courseId = "review", onBack = {}, initialTab = if (capture.contains("exam")) "exam" else "tasks")
                        }
                    }
                }
            }
            }
        }
        val screens = if (approved) listOf("tasks", "tasks-warm", "tasks-dark", "exam", "exam-unreachable")
            else listOf("exam", "tasks", "tasks-warm", "tasks-warm-readable", "tasks-dark", "exam-warm", "exam-warm-readable")
        screens.forEach { name ->
            if (name == "exam-unreachable") runBlocking {
                data.gradeRepository.upsertItem(GradeItem("overdue", "review", "Здати протокол експерименту", GradeItemType.LAB, 0.0, 10.0,
                    dueDate = date.minusDays(1), completed = true, requiredForExam = true, minimumPointsForExam = 5.0))
            }
            compose.runOnIdle { capture = name }
            compose.waitForIdle()
            // Wait for lifecycle-backed Room flows to supply the content.
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText( if (name.contains("exam")) "Цільова оцінка" else "Тест: механічні коливання").fetchSemanticsNodes().isNotEmpty()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            // Semantics can settle before SurfaceFlinger presents the new frame.
            android.os.SystemClock.sleep(350)
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            context.openFileOutput(if (approved) "approved-$name.png" else "review-before-$name.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
