package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeWorkPlannerTest {
    private val monday = LocalDate.of(2026, 9, 21)
    @Test fun defaultIsFourteenCalendarDaysIncludingToday() {
        assertEquals(HomeWorkPeriod.FOURTEEN_DAYS, HomeWorkFilter().period)
        val inside = grade("inside", GradeItemType.TEST, monday.plusDays(13))
        val outside = grade("outside", GradeItemType.TEST, monday.plusDays(14))
        val entries = HomeWorkPlanner.visible(emptyList(), listOf(inside, outside), monday, HomeWorkFilter())
        assertEquals(listOf("inside"), entries.map { (it as HomeWorkEntry.Graded).value.id })
    }

    @Test fun sevenDaysAreCalendarDaysIncludingWeekend() {
        val saturday = grade("saturday", GradeItemType.TEST, monday.plusDays(5))
        val sunday = grade("sunday", GradeItemType.LAB, monday.plusDays(6))
        val nextMonday = grade("next-monday", GradeItemType.TEST, monday.plusDays(7))
        val entries = HomeWorkPlanner.visible(emptyList(), listOf(saturday, sunday, nextMonday),
            monday, HomeWorkFilter(period = HomeWorkPeriod.SEVEN_DAYS))
        assertEquals(listOf("saturday", "sunday"), entries.map { (it as HomeWorkEntry.Graded).value.id })
    }

    @Test fun homeTasksUnifyOrdinaryAndGradedWorkAndCountDatedEntries() {
        val tuesday = monday.plusDays(1)
        val task = StudyTask("task", "course", "Homework", null, tuesday, false)
        val test = grade("test", GradeItemType.TEST, tuesday)
        val exam = grade("exam", GradeItemType.EXAM, tuesday)
        val entries = HomeWorkPlanner.visible(listOf(task), listOf(test, exam), monday, HomeWorkFilter())
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
            monday, HomeWorkFilter(
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
        val default = HomeWorkPlanner.visible(listOf(task), listOf(completed), monday, HomeWorkFilter())
        assertEquals(1, default.size)
        assertTrue(default.single() is HomeWorkEntry.Task)
        val all = HomeWorkPlanner.visible(listOf(task), listOf(completed), monday,
            HomeWorkFilter(includeCompleted = true))
        assertEquals(2, all.size)
        assertFalse(all.isEmpty())
    }

    @Test fun workDueTodayIsVisible() {
        val dueToday = grade("today", GradeItemType.TEST, monday)
        val entries = HomeWorkPlanner.visible(emptyList(), listOf(dueToday), monday, HomeWorkFilter())
        assertEquals("today", (entries.single() as HomeWorkEntry.Graded).value.id)
    }

    @Test fun testFilterIncludesLegacyMidterms() {
        val entries = HomeWorkPlanner.visible(emptyList(),
            listOf(grade("legacy", GradeItemType.MIDTERM, monday)), monday,
            HomeWorkFilter(types = setOf(GradeItemType.TEST)))
        assertEquals("legacy", (entries.single() as HomeWorkEntry.Graded).value.id)
    }

    private fun grade(id: String, type: GradeItemType, due: LocalDate) = GradeItem(
        id, "course", id, type, 0.0, 10.0, dueDate = due, completed = false,
    )
}
