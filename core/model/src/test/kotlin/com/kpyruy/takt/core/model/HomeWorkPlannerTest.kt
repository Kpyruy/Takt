package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class HomeWorkPlannerTest {
    private val monday = LocalDate.of(2026, 9, 21)
    private val rules = DayOfWeek.entries.filter { it.value <= 5 }.map { day ->
        ScheduleRule("$day", "course", day.name, day, LocalTime.of(9, 0),
            LocalTime.of(10, 0), ScheduleRecurrence.WEEKLY)
    }

    @Test fun nextSevenClassDaysStartTomorrowAndSkipDaysWithoutLessons() {
        val expected = listOf(22, 23, 24, 25, 28, 29, 30).map { monday.withDayOfMonth(it) }
        assertEquals(expected, days(monday))
        assertEquals(listOf(25, 28, 29, 30, 1, 2, 5), days(monday.plusDays(3)).map { it.dayOfMonth })
        assertEquals(listOf(28, 29, 30, 1, 2, 5, 6), days(monday.plusDays(5)).map { it.dayOfMonth })
    }

    @Test fun homeTasksUnifyOrdinaryAndGradedWorkAndCountDatedEntries() {
        val tuesday = monday.plusDays(1)
        val task = StudyTask("task", "course", "Homework", null, tuesday, false)
        val test = grade("test", GradeItemType.TEST, tuesday)
        val exam = grade("exam", GradeItemType.EXAM, tuesday)
        val entries = HomeWorkPlanner.visible(listOf(task), listOf(test, exam), monday,
            days(monday), HomeWorkFilter())
        assertEquals(2, entries.size)
        assertTrue(entries.any { it is HomeWorkEntry.Task })
        assertTrue(entries.any { it is HomeWorkEntry.Graded })
    }

    @Test fun subjectTypeAndCustomDateFiltersApplyAcrossSources() {
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        val entries = HomeWorkPlanner.visible(
            listOf(StudyTask("task", "course", "Task", null, tuesday, false)),
            listOf(grade("test", GradeItemType.TEST, tuesday),
                grade("lab", GradeItemType.LAB, wednesday).copy(courseId = "other")),
            monday, days(monday), HomeWorkFilter(
                period = HomeWorkPeriod.CUSTOM, courseId = "course",
                types = setOf(GradeItemType.TEST), fromDate = tuesday, toDate = tuesday,
            ),
        )
        assertEquals(1, entries.size)
        assertEquals("test", (entries.single() as HomeWorkEntry.Graded).value.id)
    }

    @Test fun noDateTaskRemainsVisibleAndCompletedWorkCanBeIncluded() {
        val task = StudyTask("task", "course", "No date", null, null, false)
        val completed = grade("done", GradeItemType.TEST, monday.plusDays(1)).copy(completed = true)
        val default = HomeWorkPlanner.visible(listOf(task), listOf(completed), monday,
            days(monday), HomeWorkFilter())
        assertEquals(1, default.size)
        assertTrue(default.single() is HomeWorkEntry.Task)
        val all = HomeWorkPlanner.visible(listOf(task), listOf(completed), monday,
            days(monday), HomeWorkFilter(includeCompleted = true))
        assertEquals(2, all.size)
        assertFalse(all.isEmpty())
    }

    @Test fun workDueTodayIsVisibleEvenThoughFutureClassDaysBeginTomorrow() {
        val dueToday = grade("today", GradeItemType.TEST, monday)
        val entries = HomeWorkPlanner.visible(emptyList(), listOf(dueToday), monday,
            days(monday), HomeWorkFilter())
        assertEquals("today", (entries.single() as HomeWorkEntry.Graded).value.id)
    }

    private fun days(today: LocalDate) = HomeWorkPlanner.nextClassDays(
        today, rules, emptyList(), emptyList(), AppSettings(),
    )

    private fun grade(id: String, type: GradeItemType, due: LocalDate) = GradeItem(
        id, "course", id, type, 0.0, 10.0, dueDate = due, completed = false,
    )
}
