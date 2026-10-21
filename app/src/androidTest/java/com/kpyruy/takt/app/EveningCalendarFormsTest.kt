package com.kpyruy.takt.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.RoomGradeRepository
import com.kpyruy.takt.core.data.RoomScheduleRepository
import com.kpyruy.takt.core.data.SharedPreferencesAppSettingsRepository
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.calendar.AddLessonForm
import com.kpyruy.takt.feature.calendar.WeekTimetable
import com.kpyruy.takt.feature.subjects.AddGradeItemForm
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Authored for the root's emulator run; no device/Gradle invocation is performed by the worker. */
class EveningCalendarFormsTest {
    @get:Rule val compose = createComposeRule()
    @After fun restoreLanguage() = TaktI18n.use(AppLanguage.UKRAINIAN)

    @Test fun selectedTimetableKeepsTemporalGridWithWideOverlapLanesAndEventActions() {
        val day = LocalDate.of(2026, 10, 12)
        val events = listOf(
            ResolvedScheduleEvent("a", null, "Long readable laboratory name", day,
                LocalTime.of(8, 0), LocalTime.of(10, 0), "207", ScheduleEventStatus.NORMAL,
                lessonType = LessonType.LAB),
            ResolvedScheduleEvent("b", null, "Overlapping seminar", day,
                LocalTime.of(9, 0), LocalTime.of(11, 0), "302", ScheduleEventStatus.NORMAL),
        )
        var opened: String? = null
        var longPressed: String? = null
        compose.setContent {
            TaktTheme {
                Box(Modifier.width(320.dp).height(600.dp)) {
                    WeekTimetable((0L..4L).map(day::plusDays), day.minusDays(1).atStartOfDay(),
                        eventsForDate = { if (it == day) events else emptyList() },
                        onEventClick = { opened = it.id }, onEventLongClick = { longPressed = it.id })
                }
            }
        }
        compose.onNodeWithTag("week-timetable").assertIsDisplayed()
        compose.onNodeWithTag("week-list-day-${day.toEpochDay()}").assertDoesNotExist()
        val first = compose.onNodeWithTag("week-timetable-event-a")
        val second = compose.onNodeWithTag("week-timetable-event-b")
        val a = first.getUnclippedBoundsInRoot()
        val b = second.getUnclippedBoundsInRoot()
        assertTrue((a.right - a.left) >= 178.dp)
        assertTrue(b.left >= a.right)
        assertTrue(b.top > a.top && b.top < a.bottom)
        first.performClick()
        compose.runOnIdle { assertEquals("a", opened) }
        first.performTouchInput { longClick() }
        compose.runOnIdle { assertEquals("a", longPressed) }
    }

    @Test fun longSlovakMinimumLabelHasSameFieldHeightAndPreservesAssessmentState() {
        val initial = GradeItem("existing", "course", "Quiz", GradeItemType.TEST, 0.0, 20.0,
            dueDate = LocalDate.of(2026, 11, 19), requiredForExam = true, minimumPointsForExam = 10.0,
            completed = false, contributesToGrade = false, awaitingResult = true, durationMinutes = 35)
        var saved: GradeItem? = null
        compose.setContent {
            TaktI18n.use(AppLanguage.SLOVAK)
            TaktTheme(AppSettings(language = AppLanguage.SLOVAK)) {
                Box(Modifier.width(320.dp).fillMaxHeight().padding(horizontal = 20.dp)) {
                    AddGradeItemForm("course", initialItem = initial,
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), onSave = { saved = it })
                }
            }
        }
        compose.onNodeWithTag("grade-maximum").performScrollTo()
        val maximum = compose.onNodeWithTag("grade-maximum").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("grade-details-tab-RULES").performClick()
        compose.onNodeWithTag("grade-minimum").performScrollTo()
        val minimum = compose.onNodeWithTag("grade-minimum").getUnclippedBoundsInRoot()
        assertTrue(maximum.bottom - maximum.top <= 80.dp)
        assertTrue(minimum.bottom - minimum.top <= 80.dp)
        doneDetails()
        compose.onNodeWithTag("grade-save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(initial.dueDate, saved?.dueDate)
            assertEquals(initial.minimumPointsForExam, saved?.minimumPointsForExam)
            assertEquals(initial.durationMinutes, saved?.durationMinutes)
            assertTrue(saved!!.requiredForExam)
            assertTrue(saved!!.isAwaitingResult)
            assertFalse(saved!!.contributesToGrade)
            assertFalse(saved!!.completed)
        }
    }

    @Test fun oneOffLessonEditKeepsIdArbitraryDateAndTypeWithoutSavingRecurringRule() {
        val initial = OneOffScheduleEvent("stable-id", null, "Lab", LocalDate.of(2026, 11, 19),
            LocalTime.of(8, 0), LocalTime.of(9, 50), "207", OneOffScheduleEventType.BLOCK_ACTION, LessonType.LAB)
        var saved: OneOffScheduleEvent? = null
        var savedRule: ScheduleRule? = null
        compose.setContent {
            TaktI18n.use(AppLanguage.SLOVAK)
            TaktTheme(AppSettings(language = AppLanguage.SLOVAK)) {
                Box(Modifier.width(320.dp).fillMaxHeight().padding(horizontal = 20.dp)) {
                    AddLessonForm(initialDay = initial.date.dayOfWeek, initialOneOff = initial,
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        onSaveOneOff = { saved = it }, onSave = { savedRule = it })
                }
            }
        }
        compose.onNodeWithTag("lesson-type-trigger").performScrollTo().performClick()
        LessonType.entries.forEach { type ->
            compose.onNodeWithTag("lesson-type-${type.name}").performScrollTo().assertExists()
        }
        compose.onNodeWithTag("lesson-type-${initial.lessonType.name}").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-repeat-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-repeat-once").performScrollTo().assertIsSelected().performClick()
        compose.onNodeWithTag("lesson-once-date").assertExists()
        compose.onNodeWithText(t("Update")).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(initial, saved)
            assertNull(savedRule)
        }
    }

    @Test fun teachingWeekRequiresOccurrenceChoiceAndPersistsItsDerivedDate() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TaktDatabase::class.java).build()
        val schedule = RoomScheduleRepository(database)
        val start = LocalDate.of(2026, 9, 14)
        val target = start.plusWeeks(6).plusDays(3)
        // Only settings reads are used by the form. The supplied flow is isolated; no preferences are written.
        val settings = object : AppSettingsRepository by SharedPreferencesAppSettingsRepository(context) {
            override val settings = MutableStateFlow(AppSettings(semesterPeriods = mapOf(2 to SemesterPeriod(
                studyStart = start, studyEnd = start.plusWeeks(12).plusDays(4)))))
        }
        val initial = GradeItem("week-quiz", "course", "Quiz", GradeItemType.TEST, 0.0, 20.0,
            completed = false, requiredForExam = true, minimumPointsForExam = 10.0, durationMinutes = 35,
            contributesToGrade = false, awaitingResult = true)
        var saved: GradeItem? = null
        var visible by mutableStateOf(true)
        try {
            runBlocking {
                database.courseDao().insertAll(listOf(CourseEntity("course", "CODE", "Course", 5, 2,
                    "enrolled", "COMPULSORY", null)))
                schedule.upsertRules(listOf(
                    ScheduleRule("lecture", "course", "Course", java.time.DayOfWeek.MONDAY,
                        LocalTime.of(8, 0), LocalTime.of(9, 50), ScheduleRecurrence.WEEKLY),
                    ScheduleRule("seminar", "course", "Course", java.time.DayOfWeek.THURSDAY,
                        LocalTime.of(13, 0), LocalTime.of(14, 50), ScheduleRecurrence.WEEKLY,
                        lessonType = LessonType.SEMINAR),
                ))
            }
            compose.setContent {
                TaktI18n.use(AppLanguage.UKRAINIAN)
                TaktTheme {
                    if (visible) AddGradeItemForm("course", academicSemester = 2,
                        scheduleRepository = schedule, settingsRepository = settings, initialItem = initial,
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        onSave = { saved = it })
                }
            }
            compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
            compose.onNodeWithTag("grade-details-tab-WHEN").performClick()
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
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("takt-choice-menu").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("grade-date-mode-SCHEDULE").assertIsSelected()
            compose.onNodeWithTag("grade-teaching-week").assertTextContains("7", substring = true)
            compose.onNodeWithTag("grade-explicit-date").assertDoesNotExist()
            doneDetails()
            compose.onNodeWithTag("grade-save").performScrollTo().performClick()
            compose.runOnIdle { assertNull("Two occurrences must not silently select the first lesson", saved) }
            compose.onNodeWithTag("grade-details-tab-WHEN").assertIsSelected()
            compose.onNodeWithTag("grade-details-error").assertTextContains(t("Choose a lesson this week or enter a date."))
            compose.onNodeWithTag("grade-week-lesson-trigger").performScrollTo().performClick()
            compose.onNodeWithTag("grade-week-occurrence-seminar-${target.toEpochDay()}").performScrollTo().performClick()
            doneDetails()
            compose.onNodeWithTag("grade-save").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(target, saved?.dueDate)
                assertEquals("seminar", saved?.lessonId)
                assertEquals(35, saved?.durationMinutes)
                assertTrue(saved!!.isAwaitingResult)
                assertTrue(saved!!.requiredForExam)
                assertFalse(saved!!.contributesToGrade)
            }
            runBlocking {
                val grades = RoomGradeRepository(database)
                grades.upsertItem(saved!!)
                val persisted = grades.observeItems("course").first().single()
                assertEquals(target, persisted.dueDate)
                assertEquals("seminar", persisted.lessonId)
                assertTrue(persisted.isAwaitingResult)
            }
        } finally {
            compose.runOnIdle { visible = false }
            compose.waitForIdle()
            database.close()
        }
    }

    private fun doneDetails() {
        compose.onNodeWithTag("grade-details-done").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
    }
}
