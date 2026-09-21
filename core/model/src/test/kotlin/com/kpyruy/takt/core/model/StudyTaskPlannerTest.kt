package com.kpyruy.takt.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyTaskPlannerTest {
    private val today = LocalDate.of(2026, 9, 21)

    @Test
    fun upcoming_includesTodayAndFutureOpenTasksSortedByDeadline() {
        val tasks = listOf(
            StudyTask("future", "FYZI_6B", "Lab report", "Finish report", today.plusDays(3), false),
            StudyTask("today", "ZAST_6B", "Statistics homework", null, today, false),
            StudyTask("past", "TPAR_6B", "Old task", null, today.minusDays(1), false),
            StudyTask("done", "FYZI_6B", "Done task", null, today.plusDays(1), true),
            StudyTask("note-like", "FYZI_6B", "No deadline", null, null, false),
        )

        val result = StudyTaskPlanner.upcoming(tasks, fromDate = today, limit = 5)

        assertEquals(listOf("today", "future"), result.map { it.id })
    }

    @Test
    fun upcoming_respectsLimit() {
        val tasks = (1..5).map {
            StudyTask(
                id = "task-$it",
                courseId = "FYZI_6B",
                title = "Task $it",
                description = null,
                dueDate = today.plusDays(it.toLong()),
                completed = false,
            )
        }

        assertEquals(3, StudyTaskPlanner.upcoming(tasks, today, limit = 3).size)
    }
}
