package com.kpyruy.takt.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.data.PlanningSnapshot
import com.kpyruy.takt.feature.home.HomeScreen
import kotlinx.coroutines.flow.MutableStateFlow
import com.kpyruy.takt.core.ui.components.WorkRequirementIcons
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.calendar.CalendarEmptyDay
import com.kpyruy.takt.feature.home.HomeTaskRow
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompactWorkCardsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun taskKeepsItsTitleCodeDateAndPointsWhileRequirementIconsExplainTheirMeaning() {
        var completionChanges = 0
        val task = StudyTask(
            id = "compact-task", courseId = "physics", title = "Prepare the laboratory report",
            description = null, dueDate = LocalDate.now(), completed = true,
            maxPoints = 10.0, earnedPoints = 2.0, requiredForExam = true, minimumPointsForExam = 5.0,
        )
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                TaktI18n.use(AppLanguage.ENGLISH)
                TaktTheme(AppSettings()) {
                    HomeTaskRow(task, "FYZI_6B") { completionChanges++ }
                }
            }
        }
        compose.onNodeWithText(task.title).assertIsDisplayed()
        compose.onNodeWithText("FYZI_6B · Today").assertIsDisplayed()
        compose.onNodeWithText("2 / 10 pts").assertIsDisplayed()
        // Descriptions remain available to accessibility services without taking up a text line.
        compose.onAllNodesWithText("Required for exam").assertCountEquals(0)
        compose.onNodeWithTag("home-task-compact-task-required", useUnmergedTree = true)
            .assertHasClickAction().performClick()
        compose.onNodeWithTag("home-task-compact-task-required-description")
            .assertTextEquals("Complete this work to qualify for the exam.").assertIsDisplayed()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag("home-task-compact-task-minimum", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("home-task-compact-task-minimum-description")
            .assertTextEquals("Minimum score for exam admission: 5 pts").assertIsDisplayed()
        captureStudiesScreenshot("task-requirement-popup.png")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag("home-task-compact-task-warning", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("home-task-compact-task-warning-description").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, completionChanges) }
    }

    @Test fun homeUsesCourseCodesAndAssessmentDisclosuresDoNotOpenTheAssessment() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        var assessmentOpens = 0
        val course = Course("compact-course", "PY_6B", "A long full course title that should stay out of work cards",
            credits = 6, semester = 1, status = CourseStatus.ENROLLED)
        val today = LocalDate.now()
        val assessment = GradeItem("compact-assessment", course.id, "Admission quiz", GradeItemType.TEST,
            earnedPoints = 0.0, maxPoints = 10.0, completed = false, dueDate = today,
            contributesToGrade = false, requiredForExam = true, minimumPointsForExam = 5.0)
        val task = StudyTask("compact-code", course.id, "Course code task", null, today, false)
        val plan = MutableStateFlow<PlanningSnapshot?>(PlanningSnapshot(
            courses = listOf(course), absences = emptyList(), rules = emptyList(),
            oneOffEvents = emptyList(), exceptions = emptyList(), tasks = listOf(task),
            assessments = listOf(assessment), settings = AppSettings()))
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                TaktI18n.use(AppLanguage.ENGLISH)
                TaktTheme(AppSettings()) {
                    HomeScreen(plan, data.studyContentRepository, data.settingsRepository, {}, {},
                        onOpenCourse = {}, onOpenAssessment = { assessmentOpens++ }, onEventLongClick = {})
                }
            }
        }
        compose.onNodeWithTag("home-task-compact-code").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("PY_6B · Today").assertIsDisplayed()
        compose.onNodeWithTag("home-assessment-compact-assessment").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("PY_6B").assertIsDisplayed()
        captureStudiesScreenshot("compact-work-home.png")
        compose.onNodeWithText(course.title).assertDoesNotExist()
        compose.onNodeWithTag("home-assessment-compact-assessment-excluded", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("home-assessment-compact-assessment-excluded-description")
            .assertTextEquals("This work does not count toward the final grade.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, assessmentOpens) }
    }

    @Test fun explanationsWrapAtLargeFontInLightAndDarkThemes() {
        for (mode in listOf(AppThemeMode.LIGHT, AppThemeMode.DARK)) {
            compose.activityRule.scenario.onActivity { activity ->
                activity.setContent {
                    TaktI18n.use(AppLanguage.SLOVAK)
                    TaktTheme(AppSettings(themeMode = mode)) {
                        val density = LocalDensity.current
                        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                            Column(Modifier.width(240.dp)) {
                                WorkRequirementIcons(requiredForExam = true, minimumPointsForExam = 12.5,
                                    admissionWarning = true, contributesToGrade = false, tagPrefix = "large")
                            }
                        }
                    }
                }
            }
            compose.onNodeWithTag("large-excluded").assertHasClickAction().performClick()
            compose.onNodeWithTag("large-excluded-description").assertIsDisplayed()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
            compose.waitForIdle()
            compose.onNodeWithTag("large-minimum").performClick()
            val text = compose.onNodeWithTag("large-minimum-description")
            text.assertTextEquals("Minimálny počet bodov na pripustenie ku skúške: 12.5").assertIsDisplayed()
            // A long description at 2× font must occupy multiple lines in the anchored popup.
            captureStudiesScreenshot("task-popup-large-${mode.name}.png")
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            text.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("Full explanation wraps", layouts.single().lineCount > 1)
            assertTrue("Explanation must not be truncated", !layouts.single().hasVisualOverflow)
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
            compose.waitForIdle()
        }
    }

    @Test fun ordinaryWorkHasNoEmptyRequirementTargetsAndEmptyDayHasNoVerboseAddHint() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                TaktI18n.use(AppLanguage.ENGLISH)
                TaktTheme(AppSettings()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        WorkRequirementIcons(requiredForExam = false, minimumPointsForExam = null,
                            admissionWarning = false, tagPrefix = "ordinary")
                        CalendarEmptyDay(LocalDate.now())
                    }
                }
            }
        }
        compose.onNodeWithTag("ordinary-required").assertDoesNotExist()
        compose.onNodeWithTag("ordinary-minimum").assertDoesNotExist()
        compose.onNodeWithTag("ordinary-warning").assertDoesNotExist()
        compose.onNodeWithTag("ordinary-excluded").assertDoesNotExist()
        compose.onNodeWithTag("calendar-empty-day").assertIsDisplayed()
        captureStudiesScreenshot("calendar-empty-day.png")
        compose.onNodeWithText("There are no classes on this day").assertIsDisplayed()
        compose.onNodeWithText("Add a class or one-off event with the button at the bottom right").assertDoesNotExist()
        // The finite entrance settles and lets the Compose rule reach idle.
        compose.waitForIdle()
    }
}
