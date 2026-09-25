package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class HomeLessonVisualTest {
    private val today = LocalDate.of(2026, 9, 25)
    private val now = LocalTime.of(10, 30)
    private val lesson = ResolvedScheduleEvent(
        id = "lesson", courseId = "course", title = "Lesson", date = today,
        startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0),
        room = null, status = ScheduleEventStatus.NORMAL,
    )

    @Test fun missedOngoingLessonIsMutedAndLosesActiveHighlightImmediately() {
        assertEquals(HomeLessonVisual(active = true, muted = false),
            HomeLessonVisual.forEvent(lesson, today, today, now, "lesson"))
        assertEquals(HomeLessonVisual(active = false, muted = true),
            HomeLessonVisual.forEvent(lesson.copy(isAbsent = true), today, today, now, "lesson"))
    }

    @Test fun finishedAndCancelledLessonsRemainMuted() {
        assertEquals(HomeLessonVisual(active = false, muted = true),
            HomeLessonVisual.forEvent(lesson, today, today, LocalTime.NOON, null))
        assertEquals(HomeLessonVisual(active = false, muted = true),
            HomeLessonVisual.forEvent(lesson.copy(status = ScheduleEventStatus.CANCELLED),
                today, today, now, "lesson"))
    }
}
