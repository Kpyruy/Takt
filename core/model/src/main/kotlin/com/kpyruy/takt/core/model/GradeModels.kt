package com.kpyruy.takt.core.model

import java.time.LocalDate

enum class GradeItemType(val label: String) {
    TEST("Тест"),
    MIDTERM("Модуль / проміжний тест"),
    LAB("Лабораторна"),
    SEMINAR("Семінар"),
    HOMEWORK("Домашня робота"),
    PROJECT("Проєкт"),
    ORAL("Усна відповідь"),
    EXAM("Екзамен"),
    OTHER("Інше"),
}

data class GradeItem(
    val id: String,
    val courseId: String,
    val title: String,
    val type: GradeItemType,
    val earnedPoints: Double,
    val maxPoints: Double,
    val recordedAtEpochMillis: Long = 0L,
    val dueDate: LocalDate? = null,
    val completed: Boolean = true,
    val requiredForExam: Boolean = false,
    val minimumPointsForExam: Double? = null,
    val lessonId: String? = null,
    val durationMinutes: Int? = null,
) {
    init {
        require(maxPoints > 0.0) { "maxPoints must be positive" }
        require(earnedPoints >= 0.0) { "earnedPoints cannot be negative" }
        require(minimumPointsForExam == null ||
            (minimumPointsForExam.isFinite() && minimumPointsForExam >= 0.0 && minimumPointsForExam <= maxPoints)) {
            "minimumPointsForExam must be between 0 and maxPoints"
        }
        require(durationMinutes == null || durationMinutes > 0) { "durationMinutes must be positive" }
    }

    val meetsAdmissionRequirement: Boolean
        get() = completed && (minimumPointsForExam == null || earnedPoints >= minimumPointsForExam)
}

data class GradeSummary(
    val earnedPoints: Double,
    val maxPoints: Double,
    val percentage: Double,
    val letter: GradeLetter?,
) {
    companion object {
        fun calculate(
            items: List<GradeItem>,
            scale: GradeScale,
            manualLetter: GradeLetter? = null,
        ): GradeSummary {
            val earned = items.sumOf { it.earnedPoints }
            val max = items.sumOf { it.maxPoints }
            if (max <= 0.0) {
                return GradeSummary(
                    earnedPoints = 0.0,
                    maxPoints = 0.0,
                    percentage = 0.0,
                    letter = manualLetter,
                )
            }

            val percentage = (earned / max * 100.0).coerceIn(0.0, 100.0)
            return GradeSummary(
                earnedPoints = earned,
                maxPoints = max,
                percentage = percentage,
                letter = manualLetter ?: scale.gradeFor(percentage),
            )
        }
    }
}

data class GradeProjection(
    val securedPoints: Double,
    val totalPoints: Double,
    val maximumPossiblePoints: Double,
    val examRemainingPoints: Double,
    val minimumPossibleLetter: GradeLetter?,
    val maximumPossibleLetter: GradeLetter?,
    val examPointsNeeded: Map<GradeLetter, Double?>,
) {
    companion object {
        fun calculate(
            items: List<GradeItem>,
            scale: GradeScale,
        ): GradeProjection {
            if (items.isEmpty()) {
                return GradeProjection(
                    securedPoints = 0.0,
                    totalPoints = 0.0,
                    maximumPossiblePoints = 0.0,
                    examRemainingPoints = 0.0,
                    minimumPossibleLetter = null,
                    maximumPossibleLetter = null,
                    examPointsNeeded = GradeLetter.entries.associateWith { null },
                )
            }

            val total = items.sumOf { it.maxPoints }
            val secured = items.filter { it.completed }.sumOf { it.earnedPoints }
            val unfinished = items.filterNot { it.completed }
            val maximum = secured + unfinished.sumOf { it.maxPoints }
            val exam = unfinished.firstOrNull { it.type == GradeItemType.EXAM }
            val examRemaining = exam?.maxPoints ?: 0.0

            fun letter(points: Double): GradeLetter =
                scale.gradeFor((points / total * 100.0).coerceIn(0.0, 100.0))

            val pendingNonExamMax = unfinished
                .filterNot { it.type == GradeItemType.EXAM }
                .sumOf { it.maxPoints }
            val pointsBeforeExamPotential = secured + pendingNonExamMax

            val examNeeded = GradeLetter.entries.associateWith { grade ->
                if (grade == GradeLetter.FX || exam == null) {
                    null
                } else {
                    val threshold = scale.bands
                        .first { it.grade == grade }
                        .minimumPercentage
                    val thresholdPoints = total * threshold / 100.0
                    val needed = (thresholdPoints - pointsBeforeExamPotential).coerceAtLeast(0.0)
                    needed.takeIf { it <= exam.maxPoints }
                }
            }

            return GradeProjection(
                securedPoints = secured,
                totalPoints = total,
                maximumPossiblePoints = maximum,
                examRemainingPoints = examRemaining,
                minimumPossibleLetter = letter(secured),
                maximumPossibleLetter = letter(maximum),
                examPointsNeeded = examNeeded,
            )
        }
    }
}

object GradeBook {
    fun recent(
        items: List<GradeItem>,
        limit: Int,
    ): List<GradeItem> {
        require(limit >= 0)
        return items
            .sortedWith(
                compareByDescending<GradeItem> { it.recordedAtEpochMillis }
                    .thenBy { it.title }
            )
            .take(limit)
    }
}
