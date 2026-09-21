package com.kpyruy.takt.core.model

import java.time.LocalDate

data class StudyTask(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String?,
    val dueDate: LocalDate?,
    val completed: Boolean,
)

data class CourseNote(
    val id: String,
    val courseId: String,
    val title: String,
    val content: String,
    val updatedAtEpochMillis: Long,
)

object StudyTaskPlanner {
    fun upcoming(
        tasks: List<StudyTask>,
        fromDate: LocalDate,
        limit: Int,
    ): List<StudyTask> {
        require(limit >= 0)
        return tasks
            .asSequence()
            .filter { !it.completed }
            .filter { it.dueDate != null && !it.dueDate.isBefore(fromDate) }
            .sortedWith(compareBy<StudyTask> { it.dueDate }.thenBy { it.title })
            .take(limit)
            .toList()
    }
}
