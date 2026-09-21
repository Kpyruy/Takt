package com.kpyruy.takt.core.model

enum class GradeItemType {
    TEST,
    LAB,
    HOMEWORK,
    EXAM,
    OTHER,
}

data class GradeItem(
    val id: String,
    val courseId: String,
    val title: String,
    val type: GradeItemType,
    val earnedPoints: Double,
    val maxPoints: Double,
    val recordedAtEpochMillis: Long = 0L,
) {
    init {
        require(maxPoints > 0.0) { "maxPoints must be positive" }
        require(earnedPoints >= 0.0) { "earnedPoints cannot be negative" }
    }
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
        ): GradeSummary {
            val earned = items.sumOf { it.earnedPoints }
            val max = items.sumOf { it.maxPoints }
            if (max <= 0.0) {
                return GradeSummary(
                    earnedPoints = 0.0,
                    maxPoints = 0.0,
                    percentage = 0.0,
                    letter = null,
                )
            }

            val percentage = (earned / max * 100.0).coerceIn(0.0, 100.0)
            return GradeSummary(
                earnedPoints = earned,
                maxPoints = max,
                percentage = percentage,
                letter = scale.gradeFor(percentage),
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
