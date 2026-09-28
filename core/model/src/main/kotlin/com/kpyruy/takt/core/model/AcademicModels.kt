package com.kpyruy.takt.core.model

enum class CourseStatus(val storageValue: String) {
    FULFILLED("fulfilled"),
    ENROLLED("enrolled"),
    PLANNED("planned"),
    NOT_ENROLLED("not_enrolled"),
    NOT_NEEDED("not_needed");

    companion object {
        fun fromStorage(value: String): CourseStatus = entries.first { it.storageValue == value }
    }
}

enum class CourseRequirementType { COMPULSORY, SEMI_COMPULSORY, ELECTIVE }

enum class SemesterSeason { WINTER, SUMMER }

enum class CourseGradingType {
    EXAM_LETTER,
    CONTINUOUS_LETTER,
    PASS_FAIL,
}

enum class PassFailResult {
    PASSED,
    FAILED,
}

data class Course(
    val id: String,
    val code: String,
    val title: String,
    val credits: Int,
    val semester: Int,
    val status: CourseStatus,
    val requirementType: CourseRequirementType = CourseRequirementType.COMPULSORY,
    val syllabusUrl: String? = null,
    val gradingType: CourseGradingType = CourseGradingType.CONTINUOUS_LETTER,
    val passFailResult: PassFailResult? = null,
    val iconKey: String? = null,
    val titleEn: String? = null,
    val titleSk: String? = null,
    val officialGrade: GradeLetter? = null,
    val fulfilledOn: java.time.LocalDate? = null,
)

fun List<Course>.activeCourseChoices(): List<Course> = filter { it.status == CourseStatus.ENROLLED }

enum class WeekParity {
    EVEN,
    ODD;

    companion object {
        fun fromIsoWeek(weekNumber: Int): WeekParity = if (weekNumber % 2 == 0) EVEN else ODD
    }
}

enum class GradeLetter { A, B, C, D, E, FX }

data class GradeBand(val grade: GradeLetter, val minimumPercentage: Double)

data class GradeScale(val bands: List<GradeBand>) {
    init {
        require(bands.isNotEmpty())
        require(bands.zipWithNext().all { (first, second) -> first.minimumPercentage > second.minimumPercentage })
    }

    fun gradeFor(percentage: Double): GradeLetter {
        val normalized = percentage.coerceIn(0.0, 100.0)
        return bands.firstOrNull { normalized >= it.minimumPercentage }?.grade ?: GradeLetter.FX
    }

    companion object {
        fun default(): GradeScale = GradeScale(
            listOf(
                GradeBand(GradeLetter.A, 92.0),
                GradeBand(GradeLetter.B, 83.0),
                GradeBand(GradeLetter.C, 74.0),
                GradeBand(GradeLetter.D, 65.0),
                GradeBand(GradeLetter.E, 56.0),
                GradeBand(GradeLetter.FX, 0.0),
            )
        )
    }
}
