package com.kpyruy.takt.core.model

object CourseWork {
    /** The home task feed omits exams, which are presented separately there. */
    fun actionable(items: List<GradeItem>): List<GradeItem> =
        items.filterNot { it.type == GradeItemType.EXAM }

    fun hasTestOnLesson(
        event: ResolvedScheduleEvent,
        dayEvents: List<ResolvedScheduleEvent>,
        items: List<GradeItem>,
    ): Boolean {
        val courseId = event.courseId ?: return false
        if (event.status == ScheduleEventStatus.CANCELLED) return false
        val firstLessonId = dayEvents.asSequence()
            .filter { it.courseId == courseId && it.status != ScheduleEventStatus.CANCELLED }
            .minWithOrNull(compareBy<ResolvedScheduleEvent> { it.startTime }.thenBy { it.id })?.id
        return items.any { item ->
            item.courseId == courseId && item.dueDate == event.date &&
                (item.type == GradeItemType.TEST || item.type == GradeItemType.MIDTERM) &&
                (item.lessonId ?: firstLessonId) == event.id
        }
    }
}
