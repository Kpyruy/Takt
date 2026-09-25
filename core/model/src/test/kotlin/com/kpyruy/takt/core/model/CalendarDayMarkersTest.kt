package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CalendarDayMarkersTest {
    private val day = LocalDate.of(2026, 9, 25)
    private val lesson = ResolvedScheduleEvent("lesson", "course", "Lecture", day,
        LocalTime.of(9, 0), LocalTime.of(10, 0), null, ScheduleEventStatus.NORMAL)
    private val task = StudyTask("task", "course", "Lab report", null, day, false)
    private fun grade(type: GradeItemType) = GradeItem(type.name, "course", type.label,
        type, 0.0, 10.0, dueDate = day, completed = false)

    @Test fun separateDotsAppearForLessonWorkAndExamOnSameDate() {
        assertEquals(CalendarDayMarkers(true, true, true), CalendarDayMarkers.from(
            listOf(lesson), listOf(task), listOf(grade(GradeItemType.TEST), grade(GradeItemType.EXAM))))
    }

    @Test fun cancelledLessonDoesNotShowLessonDot() {
        assertEquals(CalendarDayMarkers(work = true), CalendarDayMarkers.from(
            listOf(lesson.copy(status = ScheduleEventStatus.CANCELLED)),
            emptyList(), listOf(grade(GradeItemType.LAB))))
    }
}
