package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.ScheduleException
import com.kpyruy.takt.core.model.ScheduleExceptionType
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleExceptionMappingTest {
    @Test
    fun movedException_roundTripsThroughEntity() {
        val model = ScheduleException(
            id = "move-1",
            ruleId = "physics",
            date = LocalDate.of(2026, 9, 25),
            type = ScheduleExceptionType.MOVED,
            replacementDate = LocalDate.of(2026, 9, 26),
            replacementStartTime = LocalTime.of(12, 0),
            replacementEndTime = LocalTime.of(13, 50),
            replacementRoom = "T-068",
        )

        val restored = model.toEntity().toDomain()

        assertEquals(model, restored)
    }

    @Test
    fun cancelledException_roundTripsWithoutReplacementFields() {
        val model = ScheduleException(
            id = "cancel-1",
            ruleId = "stats",
            date = LocalDate.of(2026, 9, 28),
            type = ScheduleExceptionType.CANCELLED,
        )

        assertEquals(model, model.toEntity().toDomain())
    }
}
