package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class GenericEventMappingTest {
    @Test fun eventRoundTripsByNameThroughDatabaseAndBackupWithoutCourse() {
        val event = OneOffScheduleEvent("chess", null, "Chess tournament", LocalDate.of(2026, 11, 19),
            LocalTime.of(16, 0), LocalTime.of(18, 0), "Club", OneOffScheduleEventType.EXTRA, LessonType.EVENT)
        val entity = event.toEntity()
        assertEquals("EVENT", entity.lessonType)
        assertEquals(event, entity.toDomain())
        val json = BackupPayloadCodec.encode(BackupPayload(oneOffEvents = listOf(entity.toBackup())))
        assertEquals(event, BackupPayloadCodec.decode(json).oneOffEvents.single().toEntity().toDomain())
        assertEquals(LessonType.UNSPECIFIED, entity.copy(lessonType = "unknown").toDomain().lessonType)
    }

    @Test fun recurringEventRoundTripsWithoutChangingItsRecurrence() {
        val rule = ScheduleRule("club", null, "Chess club", LocalDate.of(2026, 11, 19).dayOfWeek,
            LocalTime.of(16, 0), LocalTime.of(18, 0), ScheduleRecurrence.ODD_WEEKS, lessonType = LessonType.EVENT)
        assertEquals(rule, rule.toEntity().toBackup().toEntity().toDomain())
    }
}
