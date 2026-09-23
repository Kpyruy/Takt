package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class LessonMetadataMappingTest {
    @Test fun recurringAndOneOffMetadataRoundTripThroughDatabaseAndBackup() {
        val rule = ScheduleRule("r", "physics", "Physics", DayOfWeek.MONDAY, LocalTime.of(9,0), LocalTime.of(10,0), ScheduleRecurrence.WEEKLY, lessonType = LessonType.SEMINAR)
        val event = OneOffScheduleEvent("e", "chemistry", "Chemistry", LocalDate.of(2026,9,23), LocalTime.NOON, LocalTime.of(14,0), type = OneOffScheduleEventType.EXTRA, lessonType = LessonType.LAB)
        assertEquals(rule, rule.toEntity().toBackup().toEntity().toDomain())
        assertEquals(event, event.toEntity().toBackup().toEntity().toDomain())
    }

    @Test fun previousBackupWithoutMetadataStillLoads() {
        val old = """{"version":2,"courses":[{"id":"c","code":"C","title":"Course","credits":5,"semester":5,"status":"enrolled","requirementType":"COMPULSORY"}],"scheduleRules":[{"id":"r","title":"Course","dayOfWeek":1,"startMinute":540,"endMinute":600,"recurrence":"WEEKLY"}]}"""
        val backup = BackupPayloadCodec.decode(old)
        assertNull(backup.courses.single().iconKey)
        assertEquals("enrolled", backup.courses.single().status)
        assertEquals(LessonType.UNSPECIFIED, backup.scheduleRules.single().toEntity().toDomain().lessonType)
    }
}
