package com.kpyruy.takt.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.subjects.AddGradeItemForm
import com.kpyruy.takt.feature.subjects.GradeItemRow
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.feature.settings.SettingsScreen
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NewFeaturesUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun excludedExamCanStillBeLinkedToItsDateAndAttempt() {
        val data = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TaktApplication).dataContainer
        val courseId = "excluded-exam-review"
        runBlocking {
            data.database.courseDao().insertAll(listOf(CourseEntity(courseId, "EXAM_REVIEW", "Предмет", 5, 3,
                "enrolled", "COMPULSORY", null, gradingType = "EXAM_LETTER")))
            data.gradeRepository.upsertItem(GradeItem("excluded-exam", courseId, "Екзамен без балів", GradeItemType.EXAM,
                0.0, 100.0, completed = false, contributesToGrade = false))
        }
        try {
            compose.setContent {
                TaktI18n.use(AppLanguage.UKRAINIAN)
                TaktTheme {
                    SubjectDetailScreen(data.studyPlanRepository, data.gradeRepository, data.scheduleRepository,
                        data.settingsRepository, data.studyContentRepository, data.examRepository, data.documentStore,
                        courseId = courseId, onBack = {}, initialTab = "exam")
                }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Додати дату й аудиторію").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Додати дату й аудиторію").performClick()
            compose.onNodeWithText("Екзамен без балів").performScrollTo().assertExists()
            compose.onNodeWithText("Екзамен без балів").performClick()
            compose.onNodeWithText("Зберегти").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                runBlocking { data.database.examDao().getExamInfoSnapshot().any { it.courseId == courseId && it.gradeItemId == "excluded-exam" } }
            }
        } finally {
            runBlocking {
                data.gradeRepository.deleteItem("excluded-exam")
                data.database.examDao().deleteExamInfo(courseId)
                data.database.courseDao().deleteById(courseId)
            }
        }
    }
    @Test fun admissionOnlyWaitingTestKeepsMinimumAndUnknownResultOnSave() {
        var saved: GradeItem? = null
        compose.setContent {
            TaktI18n.use(AppLanguage.UKRAINIAN)
            TaktTheme { AddGradeItemForm("course", modifier=Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), onSave={saved=it}) }
        }
        compose.onNode(hasText("Назва") and hasSetTextAction()).performTextInput("Допусковий тест")
        compose.onNodeWithTag("grade-maximum").performScrollTo().performTextInput("20")
        compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("grade-details-tab-RESULT").performClick()
        compose.onNodeWithTag("grade-result-AWAITING").performClick()
        compose.onNodeWithTag("grade-details-tab-RULES").performClick()
        compose.onNodeWithTag("grade-contributes").performClick()
        compose.onNodeWithTag("grade-required").performClick()
        compose.onNodeWithTag("grade-minimum").performScrollTo().performTextInput("10")
        compose.onNodeWithTag("grade-details-done").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()
        compose.runOnIdle {
            assertNotNull(saved);assertFalse(saved!!.contributesToGrade);assertTrue(saved!!.isAwaitingResult)
            assertTrue(saved!!.requiredForExam);assertEquals(10.0,saved!!.minimumPointsForExam!!,0.0)
            assertFalse(saved!!.completed);assertEquals(0.0,saved!!.earnedPoints,0.0);assertFalse(saved!!.meetsAdmissionRequirement)
        }
    }
    @Test fun pastWrittenTestWaitsForGradeInsteadOfBeingOverdue() {
        val grade=GradeItem("wait","FYZI_6B","Написаний тест",GradeItemType.TEST,0.0,20.0,
            completed=false,dueDate=LocalDate.now().minusDays(2),requiredForExam=true,minimumPointsForExam=10.0,
            contributesToGrade=false,awaitingResult=true)
        compose.setContent { TaktI18n.use(AppLanguage.UKRAINIAN);TaktTheme { GradeItemRow(grade,{},{}) } }
        compose.onNodeWithText("Очікую оцінку").assertExists()
        compose.onNodeWithText("Не входить у підсумок").assertExists()
        compose.onNodeWithText("Мінімум 10 б.").assertExists()
        compose.onAllNodes(hasText("Прострочено",substring=true)).assertCountEquals(0)
        compose.onAllNodes(hasText("0 / 20",substring=true)).assertCountEquals(0)
    }
    @Test fun updatesPageAllowsManualCheckEvenWithAutomaticChecksDisabled() {
        val data=(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TaktApplication).dataContainer
        val prefs=object:AppReleasePreferenceStore {
            var value=AppReleasePreferences()
            override fun read()=value
            override fun write(value:AppReleasePreferences){this.value=value}
        }
        var requests=0
        val checker=AppReleaseRepository("0.2.0",prefs,{ requests++;AppRelease("v0.3.0",
            "https://github.com/Kpyruy/Takt/releases/tag/v0.3.0","https://github.com/Kpyruy/Takt/releases/download/v0.3.0/Takt.apk") })
        compose.setContent { TaktI18n.use(AppLanguage.UKRAINIAN);TaktTheme {
            SettingsScreen(data.settingsRepository,data.backupRepository,data.documentStore,data.studyPlanRepository,
                data.universityAccountRepository,authenticateDevice={_,callback->callback(DeviceAuthenticationResult.CANCELLED)},
                appReleaseRepository=checker,onBack={})
        } }
        compose.onNodeWithText("Оновлення додатка").performScrollTo().performClick()
        compose.onNodeWithText("Takt 0.2.0").assertExists()
        compose.onNode(androidx.compose.ui.test.SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,
            androidx.compose.ui.semantics.Role.Switch)).performScrollTo().performClick()
        compose.runOnIdle { assertFalse(checker.state.value.automaticChecks) }
        compose.onNodeWithText("Перевірити зараз").performScrollTo().performClick()
        compose.waitUntil(5_000) { checker.state.value.availableUpdate!=null }
        compose.onNodeWithText("v0.3.0").assertExists()
        compose.onNodeWithText("Відкрити завантаження APK").assertExists()
        compose.runOnIdle { assertEquals(1,requests) }
    }
}
