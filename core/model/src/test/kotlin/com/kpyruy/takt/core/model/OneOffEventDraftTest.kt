package com.kpyruy.takt.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneOffEventDraftTest {
    @Test
    fun validDraft_buildsConcreteOneOffEvent() {
        val draft = OneOffEventDraft(
            title = "Extra lab",
            date = "2026-09-30",
            startTime = "14:00",
            endTime = "15:30",
            room = "T-068",
            blockAction = false,
        )

        val result = draft.toEvent(
            id = "event-1",
            courseId = "FYZI_6B",
        )

        assertTrue(result.isSuccess)
        val event = result.getOrThrow()
        assertEquals(LocalDate.of(2026, 9, 30), event.date)
        assertEquals(OneOffScheduleEventType.EXTRA, event.type)
        assertEquals("T-068", event.room)
    }

    @Test
    fun invalidEndTime_isRejected() {
        val draft = OneOffEventDraft(
            title = "Invalid",
            date = "2026-09-30",
            startTime = "15:00",
            endTime = "14:00",
            room = "",
            blockAction = true,
        )

        assertTrue(draft.toEvent("x", null).isFailure)
    }
}
