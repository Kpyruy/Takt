package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime

data class ExamInfo(
    val courseId: String,
    val gradeItemId: String?,
    val date: LocalDate?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val room: String?,
    val attemptNumber: Int = 1,
    val maxAttempts: Int = 3,
    val notes: String = "",
) {
    init {
        require(attemptNumber >= 1)
        require(maxAttempts in 1..3)
        require(attemptNumber <= maxAttempts)
        require(endTime == null || startTime == null || endTime > startTime)
    }
}

data class ExamMaterial(
    val id: String,
    val courseId: String,
    val title: String,
    val uri: String,
)

data class ExamEligibility(
    val requiredCount: Int,
    val completedCount: Int,
) {
    val eligible: Boolean
        get() = completedCount >= requiredCount
}

object ExamEligibilityCalculator {
    fun calculate(
        tasks: List<StudyTask>,
        assessments: List<GradeItem>,
    ): ExamEligibility {
        val requiredTasks = tasks.filter { it.requiredForExam }
        val requiredAssessments = assessments.filter { it.requiredForExam }
        val requiredCount = requiredTasks.size + requiredAssessments.size
        val completedCount =
            requiredTasks.count { it.meetsAdmissionRequirement } +
                requiredAssessments.count { it.meetsAdmissionRequirement }

        return ExamEligibility(
            requiredCount = requiredCount,
            completedCount = completedCount,
        )
    }
}
