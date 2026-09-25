package com.kpyruy.takt.core.model

import java.time.LocalDate

enum class HomeWorkPeriod { SEVEN_CLASS_DAYS, FOURTEEN_DAYS, ALL, CUSTOM }

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
    val period: HomeWorkPeriod = HomeWorkPeriod.SEVEN_CLASS_DAYS,
    val courseId: String? = null,
    /** Empty means every type; null is an ordinary task. */
    val types: Set<GradeItemType?> = emptySet(),
    val includeCompleted: Boolean = false,
    val includeUndated: Boolean = true,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
)

object HomeWorkPlanner {
    fun nextClassDays(
        today: LocalDate,
        rules: List<ScheduleRule>,
        exceptions: List<ScheduleException>,
        oneOffEvents: List<OneOffScheduleEvent>,
        settings: AppSettings,
        count: Int = 7,
    ): List<LocalDate> {
        require(count > 0)
        val days = (1L..60L).asSequence().map(today::plusDays).filter { date ->
            settings.filterScheduleEvents(ScheduleResolver.eventsForDate(
                rules, exceptions, oneOffEvents, date, settings.effectiveParity(date),
            )).any { it.status != ScheduleEventStatus.CANCELLED }
        }.take(count).toList()
        // A fresh plan has no lessons yet; its tasks must still be discoverable.
        return days.ifEmpty { (1L..count.toLong()).map(today::plusDays) }
    }

    fun visible(
        tasks: List<StudyTask>,
        assessments: List<GradeItem>,
        today: LocalDate,
        classDays: List<LocalDate>,
        filter: HomeWorkFilter,
    ): List<HomeWorkEntry> {
        val dates = classDays.toSet()
        return buildList {
            tasks.forEach { add(HomeWorkEntry.Task(it)) }
            CourseWork.actionable(assessments).forEach { add(HomeWorkEntry.Graded(it)) }
        }.filter { entry ->
            (filter.includeCompleted || !entry.completed ||
                (entry.requiredForExam && !entry.meetsAdmissionRequirement)) &&
                (filter.courseId == null || entry.courseId == filter.courseId) &&
                (filter.types.isEmpty() || entry.type in filter.types) &&
                when (val date = entry.dueDate) {
                    null -> filter.includeUndated
                    else -> when (filter.period) {
                        HomeWorkPeriod.SEVEN_CLASS_DAYS -> date == today || date in dates
                        HomeWorkPeriod.FOURTEEN_DAYS -> date >= today && date <= today.plusDays(14)
                        HomeWorkPeriod.ALL -> true
                        HomeWorkPeriod.CUSTOM ->
                            (filter.fromDate == null || date >= filter.fromDate) &&
                                (filter.toDate == null || date <= filter.toDate)
                    }
                }
        }.sortedWith(compareBy<HomeWorkEntry> { it.dueDate ?: LocalDate.MAX }
            .thenBy { if (it is HomeWorkEntry.Task) it.value.title else (it as HomeWorkEntry.Graded).value.title })
    }
}
