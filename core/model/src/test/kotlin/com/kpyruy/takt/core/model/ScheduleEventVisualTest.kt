package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleEventVisualTest {
    private val now = LocalDateTime.of(2026, 9, 26, 12, 0)

    private fun event(date: LocalDate, endTime: LocalTime, absent: Boolean = false) =
        ResolvedScheduleEvent(
            id = "lesson", courseId = "course", title = "Lecture", date = date,
            startTime = LocalTime.of(8, 0), endTime = endTime, room = null,
            status = ScheduleEventStatus.NORMAL, isAbsent = absent,
        )

    @Test fun pastDayAndEndedClassTodayAreMuted() {
        assertTrue(event(now.toLocalDate().minusDays(1), LocalTime.of(18, 0)).isVisuallyMuted(now))
        assertTrue(event(now.toLocalDate(), LocalTime.NOON).isVisuallyMuted(now))
    }

    @Test fun upcomingClassesStayVisibleUnlessAbsent() {
        assertFalse(event(now.toLocalDate(), LocalTime.of(13, 0)).isVisuallyMuted(now))
        assertFalse(event(now.toLocalDate().plusDays(1), LocalTime.of(9, 0)).isVisuallyMuted(now))
        assertTrue(event(now.toLocalDate(), LocalTime.of(13, 0), absent = true).isVisuallyMuted(now))
    }
}
