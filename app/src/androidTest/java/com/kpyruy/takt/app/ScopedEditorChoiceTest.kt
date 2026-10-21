package com.kpyruy.takt.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.RoomScheduleRepository
import com.kpyruy.takt.core.data.SharedPreferencesAppSettingsRepository
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.calendar.AddLessonForm
import com.kpyruy.takt.feature.subjects.AddGradeItemForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** New interactions only. Authored for root's native run; does not alter existing contracts. */
class ScopedEditorChoiceTest {
    @get:Rule val compose = createComposeRule()
    @Before fun setLanguage() = TaktI18n.use(AppLanguage.UKRAINIAN)
    @After fun restoreLanguage() = TaktI18n.use(AppLanguage.UKRAINIAN)
    private val date = LocalDate.of(2026, 11, 19)
    private val course = Course("course", "CODE", "Course title", 5, 2, CourseStatus.ENROLLED)

    @Test fun lessonLinkHidesCustomTitleAndUnlinkRestoresItsUnsavedValue() {
        var saved: ScheduleRule? = null
        compose.setContent {
            TaktTheme {
                AddLessonForm(DayOfWeek.THURSDAY, courses = listOf(course), initialTitle = "Custom draft",
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("lesson-custom-title").performTextReplacement("Kept custom draft")
        compose.onNodeWithTag("course-link-trigger").performScrollTo().performClick()
        compose.onNodeWithText(course.title).performClick()
        compose.onNodeWithTag("lesson-custom-title").assertDoesNotExist()
        compose.onNodeWithTag("lesson-type-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("takt-choice-menu").assertIsDisplayed()
        compose.onNodeWithTag("lesson-type-LAB").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-type-trigger").assertTextContains(t(LessonType.LAB.label))
        compose.onNodeWithText(t("Save")).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(course.id, saved?.courseId)
            assertEquals(course.title, saved?.title)
            assertEquals(LessonType.LAB, saved?.lessonType)
        }
        compose.onNodeWithTag("course-link-trigger").performScrollTo().performClick()
        compose.onNodeWithText(t("Without binding")).performClick()
        compose.onNodeWithTag("lesson-custom-title").assertTextContains("Kept custom draft")
    }

    @Test fun recurrencePopupConvertsToExactlyOneSaveTargetAndKeepsEditingId() {
        val initial = ScheduleRule("stable", null, "Lab", DayOfWeek.THURSDAY,
            LocalTime.of(8, 0), LocalTime.of(9, 50), ScheduleRecurrence.ODD_WEEKS,
            room = "207", lessonType = LessonType.LAB)
        val once = mutableListOf<OneOffScheduleEvent>()
        val recurring = mutableListOf<ScheduleRule>()
        compose.setContent {
            TaktTheme {
                AddLessonForm(initial.dayOfWeek, initialRule = initial, initialDate = date,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    onSaveOneOff = { once.add(it) }, onSave = { recurring.add(it) })
            }
        }
        compose.onNodeWithTag("lesson-repeat-once").assertDoesNotExist()
        compose.onNodeWithTag("lesson-repeat-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-repeat-ODD_WEEKS").assertIsSelected()
        compose.onNodeWithTag("lesson-repeat-once").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-once-date").assertExists()
        compose.onNodeWithText(t("The recurring lesson will be replaced with a single lesson.")).assertDoesNotExist()
        compose.onNodeWithText(t("Update")).performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(recurring.isEmpty())
            assertEquals(initial.id, once.single().id)
            assertEquals(date, once.single().date)
            assertEquals(initial.room, once.single().room)
            assertEquals(initial.lessonType, once.single().lessonType)
        }
        compose.onNodeWithTag("lesson-repeat-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-repeat-EVEN_WEEKS").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-once-date").assertDoesNotExist()
        compose.onNodeWithText(t("Update")).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, once.size)
            assertEquals(initial.id, recurring.single().id)
            assertEquals(ScheduleRecurrence.EVEN_WEEKS, recurring.single().recurrence)
        }
    }

    @Test fun compactGradeEditSavesExistingHiddenDetailsWithoutResettingThem() {
        val initial = GradeItem("existing", course.id, "Quiz", GradeItemType.TEST, 0.0, 20.0,
            dueDate = date, completed = false, requiredForExam = true, minimumPointsForExam = 10.0,
            lessonId = "lesson", durationMinutes = 35, contributesToGrade = false, awaitingResult = true)
        var saved: GradeItem? = null
        compose.setContent {
            TaktTheme {
                AddGradeItemForm(course.id, initialItem = initial,
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("grade-explicit-date").assertDoesNotExist()
        compose.onNodeWithTag("grade-minimum").assertDoesNotExist()
        compose.onNodeWithTag("grade-maximum").performTextReplacement("25")
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(initial.copy(maxPoints = 25.0), saved) }
    }

    @Test fun assessmentChoiceAndDetailsSurviveCollapseAndSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        var saved: GradeItem? = null
        restoration.setContent {
            TaktTheme {
                AddGradeItemForm(course.id, initialTitle = "Draft", initialDueDate = date,
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("grade-type-LAB").assertDoesNotExist()
        compose.onNodeWithTag("grade-type-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("takt-choice-menu").assertIsDisplayed()
        compose.onNodeWithTag("grade-type-LAB").performScrollTo().performClick()
        compose.onNodeWithTag("grade-type-trigger").assertTextContains(t("Laboratory"))
        compose.onNodeWithTag("grade-type-trigger").performClick()
        compose.onNodeWithTag("grade-type-TEST").performScrollTo().performClick()
        compose.onNodeWithTag("grade-maximum").performTextReplacement("20")
        openDetails()
        tab("WHEN")
        compose.onNodeWithTag("grade-duration").performScrollTo().performTextReplacement("45")
        tab("RESULT")
        compose.onNodeWithTag("grade-result-AWAITING").performClick()
        tab("RULES")
        compose.onNodeWithTag("grade-required").performClick()
        compose.onNodeWithTag("grade-minimum").performScrollTo().performTextReplacement("6")
        compose.onNodeWithTag("grade-contributes").performClick()
        doneDetails()
        compose.onNodeWithTag("grade-minimum").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("grade-details").assertDoesNotExist()
        compose.onNodeWithTag("grade-explicit-date").assertDoesNotExist()
        compose.onNodeWithTag("grade-maximum").assertTextContains("20")
        compose.runOnIdle { assertNull(saved) }
        openDetails()
        tab("WHEN")
        compose.onNodeWithTag("grade-duration").assertTextContains("45")
        tab("RESULT")
        compose.onNodeWithTag("grade-result-AWAITING").assertIsSelected()
        tab("RULES")
        compose.onNodeWithTag("grade-minimum").assertTextContains("6")
        // The open sheet and selected tab are restored alongside the independent draft fields.
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("grade-details").assertExists()
        compose.onNodeWithTag("grade-details-tab-RULES").assertIsSelected()
        compose.onNodeWithTag("grade-minimum").assertExists()
        compose.onNodeWithTag("grade-required").assertIsOn()
        compose.onNodeWithTag("grade-contributes").assertIsOff()
        doneDetails()
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(date, saved?.dueDate)
            assertEquals(45, saved?.durationMinutes)
            assertEquals(6.0, saved?.minimumPointsForExam)
            assertTrue(saved!!.requiredForExam)
            assertTrue(saved!!.isAwaitingResult)
            assertFalse(saved!!.contributesToGrade)
        }
    }

    @Test fun shortMinimumAndMaximumStayCompactAtNarrowSlovakWidth() {
        compose.setContent {
            TaktI18n.use(AppLanguage.SLOVAK)
            TaktTheme(AppSettings(language = AppLanguage.SLOVAK)) {
                Box(Modifier.width(320.dp).padding(20.dp)) {
                    AddGradeItemForm(course.id, initialRequiredForExam = true,
                        modifier = Modifier.verticalScroll(rememberScrollState()), onSave = {})
                }
            }
        }
        compose.onNodeWithTag("grade-maximum").assertHeightIsAtLeast(48.dp)
        assertTrue(compose.onNodeWithTag("grade-maximum").getUnclippedBoundsInRoot().run { bottom - top } <= 80.dp)
        compose.onNodeWithTag("grade-maximum").performScrollTo().performTextReplacement("20")
        openDetails()
        tab("RULES")
        compose.onNodeWithTag("grade-minimum").performScrollTo().assertTextContains(t("Minimum"))
        assertTrue(compose.onNodeWithTag("grade-minimum").getUnclippedBoundsInRoot().run { bottom - top } <= 80.dp)
    }

    @Test fun knownResultKeepsEarnedPointsWhenDetailsAreCollapsed() {
        var saved: GradeItem? = null
        compose.setContent {
            TaktTheme {
                AddGradeItemForm(course.id, initialTitle = "Marked quiz",
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("grade-maximum").performTextReplacement("20")
        openDetails()
        tab("RESULT")
        compose.onNodeWithTag("grade-result-AWAITING").performClick()
        compose.onNodeWithTag("grade-result-KNOWN").performClick()
        compose.onNodeWithTag("grade-result-AWAITING").assertIsNotSelected()
        compose.onNodeWithTag("grade-result-KNOWN").assertIsSelected()
        compose.onNodeWithTag("grade-earned").performScrollTo().performTextReplacement("7")
        doneDetails()
        compose.onNodeWithTag("grade-earned").assertDoesNotExist()
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(7.0, saved!!.earnedPoints, 0.0)
            assertTrue(saved!!.completed)
            assertFalse(saved!!.awaitingResult)
        }
    }

    @Test fun ambiguousTeachingWeekNeedsPopupChoiceAndHiddenValidationReopensDetails() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TaktDatabase::class.java).build()
        val schedule = RoomScheduleRepository(database)
        val start = LocalDate.of(2026, 9, 14)
        val target = start.plusWeeks(6).plusDays(3)
        val settings = object : AppSettingsRepository by SharedPreferencesAppSettingsRepository(context) {
            override val settings = MutableStateFlow(AppSettings(semesterPeriods = mapOf(2 to
                SemesterPeriod(studyStart = start, studyEnd = start.plusWeeks(12).plusDays(4)))))
        }
        var saved: GradeItem? = null
        var visible by mutableStateOf(true)
        try {
            runBlocking {
                database.courseDao().insertAll(listOf(CourseEntity(course.id, course.code, course.title,
                    5, 2, "enrolled", "COMPULSORY", null)))
                schedule.upsertRules(listOf(
                    ScheduleRule("lecture", course.id, course.title, DayOfWeek.MONDAY,
                        LocalTime.of(8, 0), LocalTime.of(9, 50), ScheduleRecurrence.WEEKLY),
                    ScheduleRule("seminar", course.id, course.title, DayOfWeek.THURSDAY,
                        LocalTime.of(13, 0), LocalTime.of(14, 50), ScheduleRecurrence.WEEKLY,
                        lessonType = LessonType.SEMINAR),
                ))
            }
            compose.setContent {
                TaktTheme {
                    if (visible) AddGradeItemForm(course.id, academicSemester = 2, scheduleRepository = schedule,
                        settingsRepository = settings, initialTitle = "Quiz",
                        modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
                }
            }
            compose.onNodeWithTag("grade-maximum").performTextReplacement("20")
            openDetails()
            tab("WHEN")
            compose.onNodeWithTag("grade-date-mode-SCHEDULE").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-teaching-week").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("grade-teaching-week").performScrollTo().performClick()
            compose.onNodeWithTag("grade-week-7").performScrollTo().performClick()
            val occurrenceTag = "grade-week-occurrence-seminar-${target.toEpochDay()}"
            compose.onNodeWithTag(occurrenceTag).assertIsDisplayed()
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            // If native Back only dismissed the IME, close the occurrence menu next.
            if (compose.onAllNodesWithTag(occurrenceTag).fetchSemanticsNodes().isNotEmpty()) {
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag(occurrenceTag).fetchSemanticsNodes().isEmpty() }
            doneDetails()
            compose.onNodeWithTag("grade-save").performScrollTo().performClick()
            compose.runOnIdle { assertNull(saved) }
            compose.onNodeWithTag("grade-details-tab-WHEN").assertIsSelected()
            compose.onNodeWithTag("grade-details-error").assertTextContains(t("Choose a lesson this week or enter a date."))
            compose.onNodeWithTag("grade-week-lesson-trigger").performScrollTo().performClick()
            compose.onNodeWithTag("takt-choice-menu").assertIsDisplayed()
            compose.onNodeWithTag(occurrenceTag).performScrollTo().performClick()
            compose.onNodeWithTag(occurrenceTag).assertDoesNotExist()
            doneDetails()
            compose.onNodeWithTag("grade-save").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(target, saved?.dueDate)
                assertEquals("seminar", saved?.lessonId)
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
    private fun doneDetails() {
        compose.onNodeWithTag("grade-details-done").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
    }
}
