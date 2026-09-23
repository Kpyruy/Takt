package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.LessonAbsenceEntity
import org.junit.Assert.*
import org.junit.Test

class LessonAbsenceBackupTest {
    private val course = BackupCourse("c", "C", "Course", 5, 1, "enrolled", "COMPULSORY")
    @Test fun absencesRoundTripThroughJsonAndEntity() {
        val entity = LessonAbsenceEntity("r", 20719, false)
        val payload = BackupPayload(courses = listOf(course), lessonAbsences = listOf(entity.toBackup()))
        assertEquals(entity, BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload)).lessonAbsences.single().toEntity())
    }
    @Test fun oldBackupHasNoAbsences() {
        val payload = BackupPayload(courses = listOf(course))
        assertTrue(BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload)).lessonAbsences.isEmpty())
    }
}
