package com.kpyruy.takt.app

import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.RoomScheduleRepository
import com.kpyruy.takt.core.data.SharedPreferencesAppSettingsRepository
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.model.SemesterPeriod
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.subjects.AddGradeItemForm
import com.kpyruy.takt.feature.subjects.AddGradeItemSheet
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Catches the old inline cascade, lost modal drafts, and saving invalid hidden categories. */
class DetailsPanelTest {
    @get:Rule val compose = createComposeRule()
    @Before fun language() = TaktI18n.use(AppLanguage.UKRAINIAN)
    @After fun restoreLanguage() = TaktI18n.use(AppLanguage.UKRAINIAN)
    private val date = LocalDate.of(2026, 11, 19)

    @Test fun detailsOpensSeparatelyWithoutResizingOuterFormOrSaving() {
        var parentBounds = Rect.Zero
        var saves = 0
        compose.setContent {
            TaktTheme {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    AddGradeItemForm("course", initialTitle = "Quiz",
                        modifier = Modifier.fillMaxWidth().onGloballyPositioned { parentBounds = it.boundsInRoot() },
                        onSave = { saves++ })
                }
            }
        }
        compose.waitForIdle()
        val before = parentBounds
        openDetails()
        compose.onNodeWithTag("grade-details").assertIsDisplayed()
        compose.onNodeWithTag("grade-date-mode-DATE").assertIsSelected()
        compose.onNodeWithTag("grade-result-PLANNED").assertDoesNotExist()
        compose.onNodeWithTag("grade-required").assertDoesNotExist()
        compose.runOnIdle { assertEquals(before, parentBounds); assertEquals(0, saves) }
        tab("RESULT")
        compose.onNodeWithTag("grade-result-PLANNED").assertIsSelected()
        compose.onNodeWithTag("grade-explicit-date").assertDoesNotExist()
        tab("RULES")
        compose.onNodeWithTag("grade-required").assertExists()
        compose.onNodeWithTag("grade-result-PLANNED").assertDoesNotExist()
        done()
        compose.runOnIdle { assertEquals(before, parentBounds); assertEquals(0, saves) }
    }

    @Test fun categoryModeBackAndRestorationKeepDraftUntilOuterSave() {
        val restoration = StateRestorationTester(compose)
        var saved: GradeItem? = null
        restoration.setContent {
            TaktTheme {
                AddGradeItemForm("course", initialTitle = "Draft", initialDueDate = date,
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("grade-maximum").performTextReplacement("20")
        openDetails()
        compose.onNodeWithTag("grade-duration").performScrollTo().performTextReplacement("45")
        compose.onNodeWithTag("grade-date-mode-SCHEDULE").performClick()
        compose.onNodeWithTag("grade-explicit-date").assertDoesNotExist()
        compose.onNodeWithTag("grade-date-mode-DATE").performClick()
        compose.onNodeWithTag("grade-explicit-date").assertTextContains("19.11.2026")
        tab("RESULT")
        compose.onNodeWithTag("grade-result-AWAITING").performClick()
        tab("RULES")
        compose.onNodeWithTag("grade-contributes").performClick()
        compose.onNodeWithTag("grade-required").performClick()
        compose.onNodeWithTag("grade-minimum").performScrollTo().performTextReplacement("6")
        // Close IME first, then dismiss the actual details sheet with native Back.
        nativeBack()
        if (compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isNotEmpty()) nativeBack()
        waitClosed()
        compose.runOnIdle { assertNull(saved) }
        openDetails()
        compose.onNodeWithTag("grade-minimum").assertTextContains("6")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("grade-details-tab-RULES").assertIsSelected()
        compose.onNodeWithTag("grade-required").assertIsOn()
        compose.onNodeWithTag("grade-contributes").assertIsOff()
        tab("RESULT")
        compose.onNodeWithTag("grade-result-AWAITING").assertIsSelected()
        tab("WHEN")
        compose.onNodeWithTag("grade-duration").assertTextContains("45")
        compose.onNodeWithTag("grade-explicit-date").assertTextContains("19.11.2026")
        done()
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(date, saved?.dueDate)
            assertEquals(45, saved?.durationMinutes)
            assertEquals(6.0, saved?.minimumPointsForExam)
            assertTrue(saved!!.requiredForExam)
            assertFalse(saved!!.contributesToGrade)
            assertFalse(saved!!.completed)
            assertTrue(saved!!.awaitingResult)
        }
    }

    @Test fun resultChoiceIsExclusiveAndKnownPointsStayWithItsStatus() {
        var saved: GradeItem? = null
        compose.setContent {
            TaktTheme {
                AddGradeItemForm("course", initialTitle = "Result",
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("grade-maximum").performTextReplacement("20")
        openDetails(); tab("RESULT")
        compose.onNodeWithTag("grade-result-KNOWN").performClick()
        compose.onNodeWithTag("grade-earned").performScrollTo().performTextReplacement("7")
        val known = compose.onNodeWithTag("grade-result-KNOWN").getUnclippedBoundsInRoot()
        val earned = compose.onNodeWithTag("grade-earned").getUnclippedBoundsInRoot()
        assertTrue("Known points must be immediately after their result choice", earned.top - known.bottom <= 24.dp)
        compose.onNodeWithTag("grade-result-AWAITING").performClick()
        compose.onNodeWithTag("grade-earned").assertDoesNotExist()
        compose.onNodeWithTag("grade-result-KNOWN").assertIsNotSelected()
        compose.onNodeWithTag("grade-result-PLANNED").performClick()
        compose.onNodeWithTag("grade-result-AWAITING").assertIsNotSelected()
        compose.onNodeWithTag("grade-result-KNOWN").performClick()
        compose.onNodeWithTag("grade-earned").assertTextContains("7")
        done()
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(7.0, saved?.earnedPoints)
            assertTrue(saved!!.completed)
            assertFalse(saved!!.awaitingResult)
        }
    }

    @Test fun hiddenInvalidFieldsReopenTheirCategoryWhileMaximumStaysOutside() {
        var saved: GradeItem? = null
        val initial = GradeItem("existing", "course", "Quiz", GradeItemType.TEST, 7.0, 20.0,
            requiredForExam = true, minimumPointsForExam = 6.0, durationMinutes = 45)
        compose.setContent {
            TaktTheme {
                AddGradeItemForm("course", initialItem = initial,
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        openDetails(); tab("RESULT")
        compose.onNodeWithTag("grade-earned").performScrollTo().performTextReplacement("21")
        done(); save()
        compose.onNodeWithTag("grade-details-tab-RESULT").assertIsSelected()
        compose.onNodeWithTag("grade-details-error").assertTextContains(t("The obtained points cannot exceed the maximum."))
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithTag("grade-earned").performScrollTo().performTextReplacement("7")
        tab("RULES")
        compose.onNodeWithTag("grade-minimum").performScrollTo().performTextReplacement("25")
        done(); save()
        compose.onNodeWithTag("grade-details-tab-RULES").assertIsSelected()
        compose.onNodeWithTag("grade-details-error").assertTextContains(t("The minimum must be from 0 to the maximum of points."))
        compose.onNodeWithTag("grade-minimum").performScrollTo().performTextReplacement("6")
        tab("WHEN")
        compose.onNodeWithTag("grade-duration").performScrollTo().performTextReplacement("0")
        done(); save()
        compose.onNodeWithTag("grade-details-tab-WHEN").assertIsSelected()
        compose.onNodeWithTag("grade-details-error").assertTextContains(t("The duration of the test must be a positive number of minutes."))
        compose.onNodeWithTag("grade-duration").performScrollTo().performTextReplacement("45")
        done()
        compose.onNodeWithTag("grade-maximum").performTextReplacement("0")
        save()
        compose.onNodeWithTag("grade-details").assertDoesNotExist()
        compose.onNodeWithTag("grade-form-error").assertTextContains(t("Maximum must be greater than 0."))
        compose.runOnIdle { assertNull(saved) }
    }

    @Test fun nestedEditorKeepsOuterSheetAndExistingIndependentFlags() {
        val initial = GradeItem("existing", "course", "Quiz", GradeItemType.TEST, 0.0, 20.0,
            dueDate = date, completed = false, requiredForExam = true, minimumPointsForExam = 6.0,
            lessonId = "lesson", durationMinutes = 35, contributesToGrade = false, awaitingResult = true)
        var saved: GradeItem? = null
        var dismisses = 0
        compose.setContent {
            TaktTheme {
                AddGradeItemSheet("course", initialItem = initial, onDismiss = { dismisses++ }, onSave = { saved = it })
            }
        }
        openDetails(); tab("RESULT")
        compose.onNodeWithTag("grade-result-AWAITING").assertIsSelected()
        nativeBack(); waitClosed()
        compose.onNodeWithTag("grade-details-trigger").assertExists()
        compose.runOnIdle { assertEquals(0, dismisses); assertNull(saved) }
        compose.onNodeWithTag("grade-maximum").performTextReplacement("25")
        save()
        compose.runOnIdle { assertEquals(initial.copy(maxPoints = 25.0), saved) }
    }

    @Test fun changingDateModeDoesNotDiscardUnresolvedWeekOrChosenOccurrence() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TaktDatabase::class.java).build()
        val schedule = RoomScheduleRepository(database)
        val start = LocalDate.of(2026, 9, 14)
        val target = LocalDate.of(2026, 10, 29)
        val settings = object : AppSettingsRepository by SharedPreferencesAppSettingsRepository(context) {
            override val settings = MutableStateFlow(AppSettings(semesterPeriods = mapOf(2 to
                SemesterPeriod(studyStart = start, studyEnd = LocalDate.of(2026, 12, 11)))))
        }
        var saved: GradeItem? = null
        var visible by mutableStateOf(true)
        try {
            runBlocking {
                database.courseDao().insertAll(listOf(CourseEntity("course", "CODE", "Course", 5, 2,
                    "enrolled", "COMPULSORY", null)))
                schedule.upsertRules(listOf(
                    ScheduleRule("lecture", "course", "Course", DayOfWeek.MONDAY,
                        LocalTime.of(8, 0), LocalTime.of(9, 50), ScheduleRecurrence.WEEKLY),
                    ScheduleRule("seminar", "course", "Course", DayOfWeek.THURSDAY,
                        LocalTime.of(13, 0), LocalTime.of(14, 50), ScheduleRecurrence.WEEKLY,
                        lessonType = LessonType.SEMINAR),
                ))
            }
            compose.setContent {
                TaktTheme {
                    if (visible) AddGradeItemForm("course", academicSemester = 2, scheduleRepository = schedule,
                        settingsRepository = settings, initialTitle = "Scheduled quiz",
                        modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
                }
            }
            compose.onNodeWithTag("grade-maximum").performTextReplacement("20")
            openDetails()
            compose.onNodeWithTag("grade-date-mode-SCHEDULE").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-teaching-week").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("grade-teaching-week").performScrollTo().performClick()
            compose.onNodeWithTag("grade-week-7").performScrollTo().performClick()
            val occurrence = "grade-week-occurrence-seminar-${target.toEpochDay()}"
            compose.onNodeWithTag(occurrence).assertExists()
            nativeBack()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag(occurrence).fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("grade-date-mode-DATE").performClick()
            compose.onNodeWithTag("grade-teaching-week").assertDoesNotExist()
            compose.onNodeWithTag("grade-date-mode-SCHEDULE").performClick()
            compose.onNodeWithTag("grade-teaching-week").assertTextContains("7", substring = true)
            done(); save()
            compose.onNodeWithTag("grade-details-tab-WHEN").assertIsSelected()
            compose.onNodeWithTag("grade-date-mode-SCHEDULE").assertIsSelected()
            compose.runOnIdle { assertNull(saved) }
            compose.onNodeWithTag("grade-week-lesson-trigger").performScrollTo().performClick()
            compose.onNodeWithTag(occurrence).performScrollTo().performClick()
            compose.onNodeWithTag("grade-date-mode-DATE").performClick()
            compose.onNodeWithTag("grade-explicit-date").assertTextContains("29.10.2026")
            compose.onNodeWithTag("grade-date-mode-SCHEDULE").performClick()
            done(); save()
            compose.runOnIdle {
                assertEquals(target, saved?.dueDate)
                assertEquals("seminar", saved?.lessonId)
                assertFalse(saved!!.completed)
            }
        } finally {
            compose.runOnIdle { visible = false }
            compose.waitForIdle()
            database.close()
        }
    }

    private fun openDetails() {
        compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
    private fun tab(name: String) = compose.onNodeWithTag("grade-details-tab-$name").performClick()
    private fun done() {
        compose.onNodeWithTag("grade-details-done").performClick()
        waitClosed()
    }
    private fun save() = compose.onNodeWithTag("grade-save").performScrollTo().performClick()
    private fun waitClosed() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
    }
    private fun nativeBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }
}
