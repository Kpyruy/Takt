package com.kpyruy.takt.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.subjects.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ApprovedSubjectUiTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)
    private val overdue = "Прострочено · " + yesterday.format(DateTimeFormatter.ofPattern("dd.MM"))

    @Test fun unfinishedGradedWorkShowsOverdueAndKeepsItsTagsAndResult() {
        compose.setContent {
            TaktI18n.use(AppLanguage.UKRAINIAN)
            TaktTheme { GradeItemRow(GradeItem("late", "FYZI_6B", "Протокол", GradeItemType.TEST, 0.0, 10.0,
                dueDate = yesterday, completed = false, requiredForExam = true, minimumPointsForExam = 5.0,
                durationMinutes = 45), {}, {}) }
        }
        compose.onNodeWithText(overdue).assertExists()
        compose.onNodeWithText("45 хв").assertExists()
        compose.onNodeWithText("Для допуску").assertExists()
        compose.onNodeWithText("Мінімум 5 б.").assertExists()
        compose.onNodeWithText("Макс. 10 б.").assertExists()
    }
    @Test fun completedGradedWorkIsNotOverdue() {
        compose.setContent {
            TaktI18n.use(AppLanguage.UKRAINIAN)
            TaktTheme { GradeItemRow(GradeItem("done", "FYZI_6B", "Тест", GradeItemType.TEST, 7.0, 10.0,
                dueDate = yesterday, completed = true), {}, {}) }
        }
        compose.onNodeWithText(overdue).assertDoesNotExist()
        compose.onNodeWithText("7 / 10 б.").assertExists()
    }
    @Test fun workDueTodayIsNotOverdue() {
        compose.setContent {
            TaktI18n.use(AppLanguage.UKRAINIAN)
            TaktTheme { GradeItemRow(GradeItem("today", "FYZI_6B", "Тест", GradeItemType.TEST, 0.0, 10.0,
                dueDate = today, completed = false), {}, {}) }
        }
        compose.onAllNodes(hasText("Прострочено", substring = true)).assertCountEquals(0)
    }
    @Test fun ordinaryTaskKeepsCompletionControlAndAdmissionWhileShowingOverdue() {
        compose.setContent {
            TaktI18n.use(AppLanguage.UKRAINIAN)
            TaktTheme { StudyTaskRow(StudyTask("late-task", "FYZI_6B", "Звіт", null, yesterday, false,
                requiredForExam = true), {}, {}, {}) }
        }
        compose.onNodeWithText(overdue).assertExists()
        compose.onNodeWithContentDescription("Виконано: Звіт").assertExists()
        compose.onNode(hasText("потрібно для допуску", substring = true)).assertExists()
    }
    @Test fun subjectTasksShowCompactAdmissionWithActualCompletedCount() {
        showSubject("tasks", failedWork = false)
        compose.onNodeWithText("1 / 2").assertExists()
        compose.onNodeWithText("Допуск до екзамену").assertExists()
        compose.onNodeWithText("Залишилась 1 робота").assertDoesNotExist()
    }
    @Test fun unreachableTargetIsExplicitWhileOriginalExamInformationStaysVisible() {
        showSubject("exam", failedWork = true)
        compose.onNodeWithText("A поки недосяжна").assertExists()
        compose.onNodeWithText("Досяжний максимум").assertExists()
        compose.onNodeWithText("84 / 100").assertExists()
        compose.onNodeWithText("За семестр").assertExists()
        compose.onNodeWithText("Доступно на екзамені").assertExists()
        compose.onNodeWithText("Як досягти оцінки").assertDoesNotExist()
    }
    private fun showSubject(tab: String, failedWork: Boolean) {
        val data = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TaktApplication).dataContainer
        data.database.clearAllTables()
        runBlocking {
            data.database.courseDao().insertAll(listOf(CourseEntity("approved", "FYZI_6B", "Фізика", 5, 3,
                "enrolled", "COMPULSORY", null, gradingType = "EXAM_LETTER")))
            data.gradeRepository.upsertItem(GradeItem("quiz", "approved", "Тест", GradeItemType.TEST, 14.0, 20.0,
                completed = true, requiredForExam = true, minimumPointsForExam = 10.0))
            data.gradeRepository.upsertItem(GradeItem("report", "approved", "Звіт", GradeItemType.LAB, 20.0, 20.0))
            data.gradeRepository.upsertItem(GradeItem("late", "approved", "Протокол", GradeItemType.LAB, 0.0, 10.0,
                dueDate = yesterday, completed = failedWork, requiredForExam = true, minimumPointsForExam = 5.0))
            data.gradeRepository.upsertItem(GradeItem("exam", "approved", "Екзамен", GradeItemType.EXAM, 0.0, 50.0,
                completed = false))
            data.examRepository.upsertExamInfo(ExamInfo("approved", "exam", today.plusMonths(2), LocalTime.of(9,0), LocalTime.of(11,0), "B-214"))
        }
        compose.setContent {
            TaktI18n.use(AppLanguage.UKRAINIAN)
            TaktTheme { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                SubjectDetailScreen(data.studyPlanRepository, data.gradeRepository, data.scheduleRepository,
                    data.settingsRepository, data.studyContentRepository, data.examRepository, data.documentStore,
                    courseId = "approved", onBack = {}, initialTab = tab)
            } }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(if (tab=="tasks") "Протокол" else "Цільова оцінка").fetchSemanticsNodes().isNotEmpty() }
    }
}
