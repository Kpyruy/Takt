package com.kpyruy.takt.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BackupPayloadCodecTest {
    @Test
    fun backupPayload_roundTripsAllV2SectionsThroughJson() {
        val payload = BackupPayload(
            version = 2,
            courses = listOf(
                BackupCourse(
                    id = "FYZI_6B",
                    code = "FYZI_6B",
                    title = "Fyzika",
                    credits = 5,
                    semester = 3,
                    status = "enrolled",
                    requirementType = "COMPULSORY",
                    syllabusUrl = null,
                    gradingType = "EXAM_LETTER",
                    passFailResult = null,
                )
            ),
            scheduleRules = listOf(
                BackupScheduleRule(
                    id = "physics-friday",
                    courseId = "FYZI_6B",
                    title = "Fyzika",
                    dayOfWeek = 5,
                    startMinute = 480,
                    endMinute = 590,
                    recurrence = "WEEKLY",
                    room = "T-aula",
                )
            ),
            oneOffEvents = listOf(
                BackupOneOffEvent(
                    id = "block-1",
                    courseId = null,
                    title = "Bloková akcia",
                    dateEpochDay = 20721L,
                    startMinute = 480,
                    endMinute = 770,
                    room = "T-aula",
                    type = "BLOCK_ACTION",
                )
            ),
            scheduleExceptions = listOf(
                BackupScheduleException(
                    id = "move-1",
                    ruleId = "physics-friday",
                    dateEpochDay = 20721L,
                    type = "MOVED",
                    replacementDateEpochDay = 20722L,
                    replacementStartMinute = 720,
                    replacementEndMinute = 830,
                    replacementRoom = "T-068",
                )
            ),
            gradeItems = listOf(
                BackupGradeItem(
                    id = "grade-1",
                    courseId = "FYZI_6B",
                    title = "Test 1",
                    type = "TEST",
                    earnedPoints = 18.0,
                    maxPoints = 20.0,
                    recordedAtEpochMillis = 123456789L,
                    dueDateEpochDay = 20726L,
                    completed = true,
                    requiredForExam = true,
                )
            ),
            gradeScales = listOf(
                BackupGradeScale(
                    courseId = "FYZI_6B",
                    aMin = 92.0,
                    bMin = 83.0,
                    cMin = 74.0,
                    dMin = 65.0,
                    eMin = 56.0,
                )
            ),
            gradeOverrides = listOf(
                BackupGradeOverride(
                    courseId = "FYZI_6B",
                    grade = "B",
                )
            ),
            studyTasks = listOf(
                BackupStudyTask(
                    id = "task-1",
                    courseId = "FYZI_6B",
                    title = "Lab report",
                    description = "Finish graphs",
                    dueDateEpochDay = 20726L,
                    completed = false,
                    requiredForExam = true,
                )
            ),
            courseNotes = listOf(
                BackupCourseNote(
                    id = "note-1",
                    courseId = "FYZI_6B",
                    title = "Formula",
                    content = "Remember this.",
                    updatedAtEpochMillis = 987654321L,
                )
            ),
            examInfo = listOf(
                BackupExamInfo(
                    courseId = "FYZI_6B",
                    gradeItemId = "exam-grade",
                    dateEpochDay = 20838L,
                    startMinute = 570,
                    endMinute = 660,
                    room = "T-068",
                    attemptNumber = 2,
                    maxAttempts = 3,
                    notes = "Bring calculator",
                )
            ),
            examMaterials = listOf(
                BackupExamMaterial(
                    id = "material-1",
                    courseId = "FYZI_6B",
                    title = "Vzorce",
                    uri = "content://example/formulas",
                )
            ),
            settings = BackupSettings(
                cancellationStyle = "HIDDEN",
                showHiddenLessons = true,
                parityOverride = "ODD",
                cardAppearance = "TONAL_FILLED",
                themeFamily = "WARM",
                themeMode = "DARK",
                weekLayout = "COMPACT_LIST",
            ),
        )

        val restored = BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload))

        assertEquals(payload, restored)
    }

    @Test
    fun v1BackupStillDecodesWithSafeAcademicDefaults() {
        val raw = """
            {
              "version": 1,
              "courses": [{
                "id":"c1",
                "code":"C1",
                "title":"Course",
                "credits":5,
                "semester":3,
                "status":"enrolled",
                "requirementType":"COMPULSORY"
              }],
              "gradeItems": [{
                "id":"g1",
                "courseId":"c1",
                "title":"Old result",
                "type":"TEST",
                "earnedPoints":8.0,
                "maxPoints":10.0,
                "recordedAtEpochMillis":1
              }],
              "studyTasks": [{
                "id":"t1",
                "courseId":"c1",
                "title":"Old task",
                "completed":false
              }],
              "settings": {
                "cancellationStyle":"STRIKETHROUGH",
                "showHiddenLessons":false,
                "parityOverride":"AUTO"
              }
            }
        """.trimIndent()

        val restored = BackupPayloadCodec.decode(raw)

        assertEquals(1, restored.version)
        assertEquals("CONTINUOUS_LETTER", restored.courses.single().gradingType)
        assertNull(restored.courses.single().passFailResult)
        assertNull(restored.gradeItems.single().dueDateEpochDay)
        assertEquals(true, restored.gradeItems.single().completed)
        assertFalse(restored.gradeItems.single().requiredForExam)
        assertFalse(restored.studyTasks.single().requiredForExam)
        assertEquals(emptyList<BackupExamInfo>(), restored.examInfo)
        assertEquals(emptyList<BackupExamMaterial>(), restored.examMaterials)
        assertEquals("ELEVATED", restored.settings.cardAppearance)
        assertEquals("BLUE", restored.settings.themeFamily)
        assertEquals("SYSTEM", restored.settings.themeMode)
        assertEquals("TIMETABLE", restored.settings.weekLayout)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyBackup_isRejectedBeforeItCanReplaceLocalData() {
        BackupPayloadCodec.decode("""{"version":2}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedBackupVersion_isRejected() {
        BackupPayloadCodec.decode("""{"version":999}""")
    }
}
