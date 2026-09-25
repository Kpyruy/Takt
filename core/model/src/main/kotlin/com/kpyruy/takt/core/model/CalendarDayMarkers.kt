package com.kpyruy.takt.core.model

/** Independent month indicators: one date can have all three kinds of activity. */
data class CalendarDayMarkers(
    val lessons: Boolean = false,
    val work: Boolean = false,
    val exams: Boolean = false,
) {
    companion object {
        fun from(
            events: List<ResolvedScheduleEvent>,
            tasks: List<StudyTask>,
            assessments: List<GradeItem>,
        ) = CalendarDayMarkers(
            lessons = events.any { it.status != ScheduleEventStatus.CANCELLED },
            work = tasks.isNotEmpty() || assessments.any { it.type != GradeItemType.EXAM },
            exams = assessments.any { it.type == GradeItemType.EXAM },
        )
    }
}
