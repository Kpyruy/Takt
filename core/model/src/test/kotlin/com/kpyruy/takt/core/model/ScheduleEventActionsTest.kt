package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleEventActionsTest {
    private val base = ResolvedScheduleEvent(
        id = "x",
        courseId = null,
        title = "Event",
        date = LocalDate.of(2026, 9, 25),
        startTime = LocalTime.of(8, 0),
        endTime = LocalTime.of(9, 0),
        room = null,
        status = ScheduleEventStatus.NORMAL,
    )

    @Test
    fun recurringNormalEvent_canManageOccurrenceAndRule() {
        val actions = ScheduleEventActions.forEvent(base, isRecurringRule = true)

        assertTrue(actions.canCancelOccurrence)
        assertTrue(actions.canMoveOccurrence)
        assertTrue(actions.canEditRecurringRule)
        assertTrue(actions.canDeleteRecurringRule)
        assertFalse(actions.canEditOneOff)
    }

    @Test
    fun oneOffEvent_canOnlyEditOrDeleteOneOff() {
        val actions = ScheduleEventActions.forEvent(
            base.copy(status = ScheduleEventStatus.ONE_OFF),
            isRecurringRule = false,
        )

        assertTrue(actions.canEditOneOff)
        assertTrue(actions.canDeleteOneOff)
        assertFalse(actions.canCancelOccurrence)
        assertFalse(actions.canMoveOccurrence)
        assertFalse(actions.canEditRecurringRule)
    }
}
