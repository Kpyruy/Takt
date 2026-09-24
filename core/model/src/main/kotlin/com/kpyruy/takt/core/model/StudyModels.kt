package com.kpyruy.takt.core.model

import java.time.LocalDate

data class StudyTask(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String?,
    val dueDate: LocalDate?,
    val completed: Boolean,
    val requiredForExam: Boolean = false,
    val earnedPoints: Double? = null,
    val maxPoints: Double? = null,
    val minimumPointsForExam: Double? = null,
) {
    init {
        require(maxPoints == null || (maxPoints.isFinite() && maxPoints > 0.0))
        require(earnedPoints == null || (earnedPoints.isFinite() && earnedPoints >= 0.0 && maxPoints != null && earnedPoints <= maxPoints))
        require(minimumPointsForExam == null ||
            (minimumPointsForExam.isFinite() && minimumPointsForExam >= 0.0 && maxPoints != null && minimumPointsForExam <= maxPoints))
    }

    val meetsAdmissionRequirement: Boolean
        get() = completed && (minimumPointsForExam == null || (earnedPoints ?: 0.0) >= minimumPointsForExam)
}

/** Scored tasks contribute to the semester total while remaining editable as tasks. */
fun StudyTask.asScoredGradeItem(): GradeItem? = maxPoints?.let { maximum ->
    val recordedPoints = earnedPoints.takeIf { completed }
    GradeItem(
        id = "task:$id",
        courseId = courseId,
        title = title,
        type = GradeItemType.HOMEWORK,
        earnedPoints = recordedPoints ?: 0.0,
        maxPoints = maximum,
        dueDate = dueDate,
        completed = recordedPoints != null,
    )
}

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
            .filter { task -> task.dueDate?.let { !it.isBefore(fromDate) } == true }
            .sortedWith(compareBy<StudyTask> { it.dueDate }.thenBy { it.title })
            .take(limit)
            .toList()
    }
}
