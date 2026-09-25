package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.database.ExamInfoEntity
import com.kpyruy.takt.core.database.ExamMaterialEntity
import com.kpyruy.takt.core.database.GradeItemEntity
import com.kpyruy.takt.core.database.StudyTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMappingsTest {
    @Test fun homeWorkFilterSurvivesBackupMapping() {
        val settings = com.kpyruy.takt.core.model.AppSettings(
            homeWorkFilter = com.kpyruy.takt.core.model.HomeWorkFilter(
                period = com.kpyruy.takt.core.model.HomeWorkPeriod.SEVEN_DAYS,
                courseId = "TPAR_6B",
                types = setOf(com.kpyruy.takt.core.model.GradeItemType.TEST, null),
                fromDate = java.time.LocalDate.of(2026, 10, 1),
            ),
        )
        assertEquals(settings, settings.toBackup().toModel())
    }

    @Test
    fun noteAttachments_surviveBackupMapping() {
        val entity = com.kpyruy.takt.core.database.CourseNoteEntity(
            id = "note", courseId = "DIVR_6B", title = "Materials", content = "",
            updatedAtEpochMillis = 42,
            attachmentsJson = NoteAttachmentCodec.encode(
                listOf(com.kpyruy.takt.core.model.NoteAttachment("diagram.png", "takt:///DIVR_6B/file.png", "image/png"))
            ),
        )
        assertEquals(entity, entity.toBackup().toEntity())
    }

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
            iconKey = "Science",
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
            minimumPointsForExam = 12.0,
            lessonId = "physics-friday-rule",
            durationMinutes = 45,
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
            earnedPoints = 4.0,
            maxPoints = 10.0,
            minimumPointsForExam = 5.0,
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
