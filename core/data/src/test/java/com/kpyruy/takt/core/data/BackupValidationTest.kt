package com.kpyruy.takt.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupValidationTest {
    private val course = BackupCourse("c", "C", "Course", 5, 1, "enrolled", "COMPULSORY")
    private val rule = BackupScheduleRule("r", "c", "Lesson", 1, 480, 540, "WEEKLY")
    private val event = BackupOneOffEvent("e", "c", "Event", 20700, startMinute = 600, endMinute = 660, type = "EXTRA")
    private val grade = BackupGradeItem("g", "c", "Exam", "EXAM", 8.0, 10.0, 0, lessonId = "r")
    private val task = BackupStudyTask("t", "c", "Task", completed = false)
    private val note = BackupCourseNote("n", "c", "Note", "", 0)
    private val scale = BackupGradeScale("c", 92.0, 83.0, 74.0, 65.0, 56.0)
    private val exception = BackupScheduleException("x", "r", 20700, "MOVED", 20701, 660, 720)
    private val exam = BackupExamInfo("c", "g", 20700, 600, 660)
    private val material = BackupExamMaterial("m", "c", "Material", "content://documents/1")
    private val valid get() = BackupPayload(
        courses = listOf(course), scheduleRules = listOf(rule), oneOffEvents = listOf(event),
        scheduleExceptions = listOf(exception), lessonAbsences = listOf(BackupLessonAbsence("r", 20700, false)),
        gradeItems = listOf(grade), gradeScales = listOf(scale), gradeOverrides = listOf(BackupGradeOverride("c", "B")),
        studyTasks = listOf(task), courseNotes = listOf(note), examInfo = listOf(exam), examMaterials = listOf(material),
    )

    private fun rejects(section: String, payload: BackupPayload) {
        try {
            BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload))
            fail("Expected invalid $section to be rejected")
        } catch (error: IllegalArgumentException) {
            assertTrue("Missing section in error: ${error.message}", error.message.orEmpty().contains(section))
        }
    }

    @Test fun rejectsInvalidCourseEnumsAndRanges() {
        listOf(course.copy(id = " "), course.copy(code = ""), course.copy(title = ""),
            course.copy(status = "ENROLLED"), course.copy(requirementType = "unknown"),
            course.copy(gradingType = "unknown"), course.copy(passFailResult = "unknown"),
            course.copy(officialGrade = "unknown"), course.copy(credits = -1),
            course.copy(semester = 0), course.copy(fulfilledOnEpochDay = Long.MAX_VALUE),
        ).forEach { rejects("courses", valid.copy(courses = listOf(it))) }
    }

    @Test fun rejectsInvalidScheduleEnumsDatesAndTimes() {
        listOf(rule.copy(dayOfWeek = 9), rule.copy(startMinute = -1), rule.copy(endMinute = 1440),
            rule.copy(endMinute = 480), rule.copy(recurrence = "unknown"), rule.copy(lessonType = "unknown"),
            rule.copy(id = ""), rule.copy(title = ""),
        ).forEach { rejects("scheduleRules", valid.copy(scheduleRules = listOf(it))) }
        listOf(event.copy(dateEpochDay = Long.MIN_VALUE), event.copy(startMinute = 1440),
            event.copy(endMinute = 600), event.copy(type = "unknown"), event.copy(lessonType = "unknown"),
        ).forEach { rejects("oneOffEvents", valid.copy(oneOffEvents = listOf(it))) }
    }

    @Test fun rejectsInvalidPointsScalesAndExamAttempts() {
        listOf(grade.copy(type = "unknown"), grade.copy(maxPoints = 0.0), grade.copy(earnedPoints = -1.0),
            grade.copy(minimumPointsForExam = 11.0),
            grade.copy(durationMinutes = 0), grade.copy(dueDateEpochDay = Long.MAX_VALUE),
        ).forEach { rejects("gradeItems", valid.copy(gradeItems = listOf(it))) }
        listOf(task.copy(maxPoints = 0.0), task.copy(earnedPoints = 1.0), task.copy(minimumPointsForExam = 1.0),
            task.copy(maxPoints = 10.0, earnedPoints = 11.0), task.copy(dueDateEpochDay = Long.MAX_VALUE),
        ).forEach { rejects("studyTasks", valid.copy(studyTasks = listOf(it))) }
        listOf(scale.copy(aMin = 101.0), scale.copy(bMin = 92.0), scale.copy(eMin = 0.0))
            .forEach { rejects("gradeScales", valid.copy(gradeScales = listOf(it))) }
        rejects("gradeOverrides", valid.copy(gradeOverrides = listOf(BackupGradeOverride("c", "unknown"))))
        listOf(exam.copy(attemptNumber = 0), exam.copy(maxAttempts = 4), exam.copy(attemptNumber = 2, maxAttempts = 1),
            exam.copy(dateEpochDay = Long.MAX_VALUE), exam.copy(startMinute = -1), exam.copy(endMinute = 600),
        ).forEach { rejects("examInfo", valid.copy(examInfo = listOf(it))) }
    }

    @Test fun rejectsDuplicatePrimaryKeysInEveryStoredSection() {
        val p = valid
        listOf(
            "courses" to p.copy(courses = p.courses + course),
            "scheduleRules" to p.copy(scheduleRules = p.scheduleRules + rule),
            "oneOffEvents" to p.copy(oneOffEvents = p.oneOffEvents + event),
            "scheduleExceptions" to p.copy(scheduleExceptions = p.scheduleExceptions + exception),
            "lessonAbsences" to p.copy(lessonAbsences = p.lessonAbsences + p.lessonAbsences.single()),
            "gradeItems" to p.copy(gradeItems = p.gradeItems + grade),
            "gradeScales" to p.copy(gradeScales = p.gradeScales + scale),
            "gradeOverrides" to p.copy(gradeOverrides = p.gradeOverrides + p.gradeOverrides.single()),
            "studyTasks" to p.copy(studyTasks = p.studyTasks + task),
            "courseNotes" to p.copy(courseNotes = p.courseNotes + note),
            "examInfo" to p.copy(examInfo = p.examInfo + exam),
            "examMaterials" to p.copy(examMaterials = p.examMaterials + material),
        ).forEach { (section, payload) -> rejects(section, payload) }
        rejects("scheduleExceptions", p.copy(scheduleExceptions = listOf(exception, exception.copy(id = "other"))))
    }

    @Test fun rejectsDanglingAndCrossCourseLinks() {
        listOf(
            "scheduleRules" to valid.copy(scheduleRules = listOf(rule.copy(courseId = "missing"))),
            "oneOffEvents" to valid.copy(oneOffEvents = listOf(event.copy(courseId = "missing"))),
            "scheduleExceptions" to valid.copy(scheduleExceptions = listOf(exception.copy(ruleId = ""))),
            "lessonAbsences" to valid.copy(lessonAbsences = listOf(BackupLessonAbsence("missing", 20700, false))),
            "gradeItems" to valid.copy(gradeItems = listOf(grade.copy(courseId = "missing"))),
            "gradeScales" to valid.copy(gradeScales = listOf(scale.copy(courseId = "missing"))),
            "gradeOverrides" to valid.copy(gradeOverrides = listOf(BackupGradeOverride("missing", "B"))),
            "studyTasks" to valid.copy(studyTasks = listOf(task.copy(courseId = "missing"))),
            "courseNotes" to valid.copy(courseNotes = listOf(note.copy(courseId = "missing"))),
            "examInfo" to valid.copy(examInfo = listOf(exam.copy(gradeItemId = ""))),
            "examMaterials" to valid.copy(examMaterials = listOf(material.copy(courseId = "missing"))),
        ).forEach { (section, payload) -> rejects(section, payload) }
        val twoCourses = valid.copy(courses = listOf(course, course.copy(id = "other", code = "OTHER")))
        rejects("gradeItems", twoCourses.copy(scheduleRules = listOf(rule.copy(courseId = "other"))))
        rejects("examInfo", twoCourses.copy(examInfo = listOf(exam.copy(courseId = "other"))))
    }

    @Test fun rejectsInvalidMovedOccurrenceMetadata() {
        listOf(exception.copy(type = "unknown"), exception.copy(dateEpochDay = Long.MAX_VALUE),
            exception.copy(replacementDateEpochDay = Long.MAX_VALUE), exception.copy(replacementStartMinute = -1),
            exception.copy(replacementEndMinute = 660), exception.copy(replacementStartMinute = 550, replacementEndMinute = null),
            exception.copy(type = "CANCELLED"),
        ).forEach { rejects("scheduleExceptions", valid.copy(scheduleExceptions = listOf(it))) }
    }

    @Test fun rejectsMalformedSettingsInsteadOfFallingBackOrDroppingRecords() {
        listOf(
            BackupSettings(cancellationStyle = "unknown"), BackupSettings(parityOverride = "unknown"),
            BackupSettings(cardAppearance = "unknown"), BackupSettings(themeFamily = "unknown"),
            BackupSettings(themeMode = "unknown"), BackupSettings(weekLayout = "unknown"),
            BackupSettings(language = "unknown"), BackupSettings(courseNameLanguage = "unknown"),
            BackupSettings(ukrainianCourseNameFallback = "FOLLOW_APP"), BackupSettings(uisProgressFrequency = "unknown"),
            BackupSettings(uisSubjectFrequency = "unknown"), BackupSettings(uisPeriodFrequency = "unknown"),
            BackupSettings(uisTimetableFrequency = "DAILY"), BackupSettings(currentSemester = 0),
            BackupSettings(semesterPeriods = listOf(BackupSemesterPeriod(1, "invalid", "2026-12-01"))),
            BackupSettings(semesterPeriods = listOf(BackupSemesterPeriod(1, "2026-12-02", "2026-12-01"))),
            BackupSettings(semesterPeriods = listOf(BackupSemesterPeriod(1, "2026-12-01"))),
            BackupSettings(semesterPeriods = listOf(BackupSemesterPeriod(1), BackupSemesterPeriod(1))),
            BackupSettings(homeWorkFilter = BackupHomeWorkFilter(period = "unknown")),
            BackupSettings(homeWorkFilter = BackupHomeWorkFilter(types = listOf("unknown"))),
            BackupSettings(homeWorkFilter = BackupHomeWorkFilter(fromDate = "invalid")),
            BackupSettings(homeWorkFilter = BackupHomeWorkFilter(fromDate = "2026-12-02", toDate = "2026-12-01")),
            BackupSettings(customThemes = listOf(BackupCustomColorTheme("x", "Theme", "broken"))),
            BackupSettings(selectedCustomThemeId = "missing"),
            BackupSettings(reminders = BackupReminderSettings(dateOnlyTime = "25:00")),
            BackupSettings(reminders = BackupReminderSettings(dateOnlyTime = "09:00:01")),
            BackupSettings(reminders = BackupReminderSettings(rules = listOf(BackupReminderRule("unknown")))),
            BackupSettings(reminders = BackupReminderSettings(rules = listOf(BackupReminderRule("TASK", offsetsMinutes = listOf(0))))),
            BackupSettings(reminders = BackupReminderSettings(rules = listOf(BackupReminderRule("TASK"), BackupReminderRule("TASK")))),
        ).forEach { rejects("settings", valid.copy(settings = it)) }
    }

    @Test fun rejectsInvalidMetadataAndOptionalReferences() {
        listOf(
            "courseIdAliases" to valid.copy(courseIdAliases = listOf(BackupCourseIdAlias("old", "missing"))),
            "courseIdAliases" to valid.copy(courseIdAliases = listOf(BackupCourseIdAlias("c", "c"))),
            "courseIdAliases" to valid.copy(courseIdAliases = listOf(BackupCourseIdAlias("old", "c"), BackupCourseIdAlias("old", "c"))),
            "courseImportDecisions" to valid.copy(courseImportDecisions = listOf("c")),
            "lessonAbsences" to valid.copy(lessonAbsences = listOf(BackupLessonAbsence("r", Long.MAX_VALUE, false))),
            "lessonAbsences" to valid.copy(lessonAbsences = listOf(BackupLessonAbsence("e", 20701, true))),
            "courseNotes" to valid.copy(courseNotes = listOf(note.copy(attachments = listOf(BackupNoteAttachment("file", "", "image/png"))))),
            "courseNotes" to valid.copy(courseNotes = listOf(note.copy(attachments = listOf(BackupNoteAttachment("link", "http://[", "application/vnd.takt.web-link"))))),
            "examMaterials" to valid.copy(examMaterials = listOf(material.copy(uri = ""))),
            "teaching" to valid.copy(teaching = BackupTeaching(listOf(BackupTeachingTeam("MISSING", emptyList())))),
            "teaching" to valid.copy(teaching = BackupTeaching(profiles = listOf(BackupTeacherProfile("1", "invalid", "Teacher")))),
            "settings" to valid.copy(settings = BackupSettings(homeWorkFilter = BackupHomeWorkFilter(courseId = ""))),
            "settings" to valid.copy(settings = BackupSettings(homeWorkFilter = BackupHomeWorkFilter(courseIds = listOf("c", "c")))),
            "settings" to valid.copy(settings = BackupSettings(semesterPeriods = listOf(BackupSemesterPeriod(1, assessmentMode = "invalid")))),
            "settings" to valid.copy(settings = BackupSettings(customThemes = listOf(BackupCustomColorTheme("x", "Theme", "#112233"), BackupCustomColorTheme("x", "Theme 2", "#112233")))),
            "settings" to valid.copy(settings = BackupSettings(reminders = BackupReminderSettings(rules = listOf(BackupReminderRule("TASK", offsetsMinutes = listOf(1, 1)))))),
            "settings" to valid.copy(settings = BackupSettings(reminders = BackupReminderSettings(rules = listOf(BackupReminderRule("TEST"), BackupReminderRule("MIDTERM"))))),
            "settings" to valid.copy(settings = BackupSettings(wallpaper = BackupWallpaper.fromBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0)))),
        ).forEach { (section, payload) -> rejects(section, payload) }
    }

    @Test fun acceptsUnlinkedTeacherProfileCacheAndValidAppearanceMetadata() {
        val payload = valid.copy(
            teaching = BackupTeaching(profiles = listOf(BackupTeacherProfile("123", "ENGLISH", "Teacher"))),
            settings = BackupSettings(customThemes = listOf(BackupCustomColorTheme("theme", "Theme", "#112233")),
                selectedCustomThemeId = "theme", homeWorkFilter = BackupHomeWorkFilter(courseIds = listOf("c")),
                reminders = BackupReminderSettings(rules = listOf(BackupReminderRule("TASK", offsetsMinutes = listOf(60, 30))))),
        )
        assertEquals(payload, BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload)))
    }

    @Test fun legacyVersionsAndDocumentedLegacyEnumsRemainReadableWithoutRewriting() {
        for (version in 1..BackupPayload.CURRENT_VERSION) {
            val payload = valid.copy(version = version, gradeItems = listOf(grade.copy(type = "MIDTERM")),
                settings = BackupSettings(homeWorkFilter = BackupHomeWorkFilter(period = "SEVEN_CLASS_DAYS"),
                    reminders = BackupReminderSettings(rules = listOf(BackupReminderRule("MIDTERM")))))
            assertEquals(payload, BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload)))
        }
    }

    @Test fun preservesInertReferencesLeftByNormalDeletionAndPartialImports() {
        val payload = valid.copy(scheduleExceptions = listOf(exception.copy(ruleId = "deleted-rule")),
            examInfo = listOf(exam.copy(gradeItemId = "deleted-grade")),
            settings = BackupSettings(homeWorkFilter = BackupHomeWorkFilter(courseIds = listOf("deleted-course"))))
        assertEquals(payload, BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload)))
    }

    @Test fun acceptsBonusPointsLargeCourseValuesAndHistoricalLessonLinks() {
        val payload = valid.copy(courses = listOf(course.copy(credits = 61, semester = 31)),
            gradeItems = listOf(grade.copy(earnedPoints = 11.0, lessonId = "deleted-lesson")))
        assertEquals(payload, BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload)))
    }

    @Test fun absentOptionalLinksAndPartialExamTimesRemainSupported() {
        val payload = valid.copy(gradeItems = listOf(grade.copy(lessonId = null)),
            scheduleRules = listOf(rule.copy(courseId = null)), oneOffEvents = listOf(event.copy(courseId = null)),
            scheduleExceptions = listOf(exception.copy(replacementDateEpochDay = null, replacementStartMinute = null, replacementEndMinute = null)),
            examInfo = listOf(exam.copy(gradeItemId = null, endMinute = null)),
        )
        assertEquals(payload, BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload)))
    }
}
