package com.kpyruy.takt.feature.home

import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventStatus
import java.time.LocalDate
import java.time.LocalTime

internal data class HomeLessonVisual(val active: Boolean, val muted: Boolean) {
    companion object {
        fun forEvent(
            event: ResolvedScheduleEvent,
            selectedDate: LocalDate,
            today: LocalDate,
            now: LocalTime,
            nextEventId: String?,
        ): HomeLessonVisual {
            val finished = selectedDate < today || (selectedDate == today && event.endTime < now)
            val cancelled = event.status == ScheduleEventStatus.CANCELLED
            return HomeLessonVisual(
                active = !event.isAbsent && !cancelled && event.id == nextEventId,
                muted = finished || cancelled || event.isAbsent,
            )
        }
    }
}
