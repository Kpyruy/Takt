package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.database.ExamInfoEntity
import com.kpyruy.takt.core.database.ExamMaterialEntity
import com.kpyruy.takt.core.database.GradeItemEntity
import com.kpyruy.takt.core.database.StudyTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMappingsTest {
    @Test
    fun course_roundTripsAcademicFieldsThroughBackupDto() {
        val entity = CourseEntity(
            id = "c1",
            code = "C1",
            title = "Course",
            credits = 5,
            semester = 3,
            status = "enrolled",
            requirementType = "COMPULSORY",
            syllabusUrl = null,
            gradingType = "PASS_FAIL",
            passFailResult = "PASSED",
        )

        assertEquals(entity, entity.toBackup().toEntity())
    }

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
            dueDateEpochDay = 20730L,
            completed = false,
            requiredForExam = true,
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
            requiredForExam = true,
        )

        assertEquals(entity, entity.toBackup().toEntity())
    }

    @Test
    fun examInfo_roundTripsThroughBackupDto() {
        val entity = ExamInfoEntity(
            courseId = "FYZI_6B",
            gradeItemId = "exam-1",
            dateEpochDay = 20838L,
            startMinute = 570,
            endMinute = 660,
            room = "T-068",
            attemptNumber = 2,
            maxAttempts = 3,
            notes = "Bring calculator",
        )

        assertEquals(entity, entity.toBackup().toEntity())
    }

    @Test
    fun examMaterial_roundTripsThroughBackupDto() {
        val entity = ExamMaterialEntity(
            id = "m1",
            courseId = "FYZI_6B",
            title = "Vzorce",
            uri = "content://example/formulas",
        )

        assertEquals(entity, entity.toBackup().toEntity())
    }
}
