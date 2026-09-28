package com.kpyruy.takt.core.model

import java.time.LocalDate

enum class HomeWorkPeriod { SEVEN_DAYS, FOURTEEN_DAYS, ALL, CUSTOM }

sealed interface HomeWorkEntry {
    val courseId: String
    val dueDate: LocalDate?
    val completed: Boolean
    val requiredForExam: Boolean
    val meetsAdmissionRequirement: Boolean
    val type: GradeItemType?

    data class Task(val value: StudyTask) : HomeWorkEntry {
        override val courseId get() = value.courseId
        override val dueDate get() = value.dueDate
        override val completed get() = value.completed
        override val requiredForExam get() = value.requiredForExam
        override val meetsAdmissionRequirement get() = value.meetsAdmissionRequirement
        override val type: GradeItemType? get() = null
    }

    data class Graded(val value: GradeItem) : HomeWorkEntry {
        override val courseId get() = value.courseId
        override val dueDate get() = value.dueDate
        override val completed get() = value.completed
        override val requiredForExam get() = value.requiredForExam
        override val meetsAdmissionRequirement get() = value.meetsAdmissionRequirement
        override val type get() = value.type
    }
}

data class HomeWorkFilter(
    val period: HomeWorkPeriod = HomeWorkPeriod.FOURTEEN_DAYS,
    val courseId: String? = null,
    /** Empty means every type; null is an ordinary task. */
    val types: Set<GradeItemType?> = emptySet(),
    val includeCompleted: Boolean = false,
    val includeUndated: Boolean = true,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
)

object HomeWorkPlanner {
    fun overdue(
        tasks: List<StudyTask>,
        assessments: List<GradeItem>,
        today: LocalDate,
        filter: HomeWorkFilter,
    ): List<HomeWorkEntry> = entries(tasks, assessments)
        .filter { entry ->
            entry.dueDate?.isBefore(today) == true &&
                (!entry.completed || (entry.requiredForExam && !entry.meetsAdmissionRequirement)) &&
                matchesCourseAndType(entry, filter)
        }.sortedWith(entryOrder)

    fun visible(
        tasks: List<StudyTask>,
        assessments: List<GradeItem>,
        today: LocalDate,
        filter: HomeWorkFilter,
    ): List<HomeWorkEntry> {
        return entries(tasks, assessments).filter { entry ->
            (filter.includeCompleted || !entry.completed ||
                (entry.requiredForExam && !entry.meetsAdmissionRequirement)) &&
                matchesCourseAndType(entry, filter) &&
                when (val date = entry.dueDate) {
                    null -> filter.includeUndated
                    else -> when (filter.period) {
                        HomeWorkPeriod.SEVEN_DAYS -> date >= today && date < today.plusDays(7)
                        HomeWorkPeriod.FOURTEEN_DAYS -> date >= today && date < today.plusDays(14)
                        HomeWorkPeriod.ALL -> true
                        HomeWorkPeriod.CUSTOM ->
                            (filter.fromDate == null || date >= filter.fromDate) &&
                                (filter.toDate == null || date <= filter.toDate)
                    }
                }
        }.sortedWith(entryOrder)
    }

    private fun entries(tasks: List<StudyTask>, assessments: List<GradeItem>) = buildList {
        CourseWork.tasksNotRepresentedByAssessments(tasks, assessments)
            .forEach { add(HomeWorkEntry.Task(it)) }
        CourseWork.actionable(assessments).forEach { add(HomeWorkEntry.Graded(it)) }
    }

    private fun matchesCourseAndType(entry: HomeWorkEntry, filter: HomeWorkFilter) =
        (filter.courseId == null || entry.courseId == filter.courseId) &&
            (filter.types.isEmpty() || entry.type in filter.types ||
                (entry.type == GradeItemType.MIDTERM && GradeItemType.TEST in filter.types) ||
                (entry.type == GradeItemType.TEST && GradeItemType.MIDTERM in filter.types))

    private val entryOrder = compareBy<HomeWorkEntry> { it.dueDate ?: LocalDate.MAX }
        .thenBy { if (it is HomeWorkEntry.Task) it.value.title else (it as HomeWorkEntry.Graded).value.title }
}
