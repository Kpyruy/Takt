package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.GradeItemEntity
import com.kpyruy.takt.core.database.StudyTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMappingsTest {
    @Test
    fun gradeItem_roundTripsThroughBackupDto() {
        val entity = GradeItemEntity(
            id = "g1",
            courseId = "FYZI_6B",
            title = "Test",
            type = "TEST",
            earnedPoints = 18.0,
            maxPoints = 20.0,
            recordedAtEpochMillis = 123L,
        )

        assertEquals(entity, entity.toBackup().toEntity())
    }

    @Test
    fun studyTask_roundTripsThroughBackupDto() {
        val entity = StudyTaskEntity(
            id = "t1",
            courseId = "ZAST_6B",
            title = "Homework",
            description = "Page 4",
            dueDateEpochDay = 20730L,
            completed = false,
        )

        assertEquals(entity, entity.toBackup().toEntity())
    }
}
