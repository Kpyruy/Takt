package com.kpyruy.takt.core.data

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.runBlocking

class BackupMergerTest {
    private val local = BackupPayload(
        courses = listOf(BackupCourse("local", "PHY", "Local Physics", 5, 1, "enrolled", "COMPULSORY")),
        scheduleRules = listOf(BackupScheduleRule("local-rule", "local", "Local lesson", 1, 480, 590, "WEEKLY")),
        gradeItems = listOf(BackupGradeItem("grade-local", "local", "Quiz", "TEST", 7.0, 10.0, 1)),
    )
    private val file = BackupPayload(
        courses = listOf(BackupCourse("file", "PHY", "File Physics", 6, 1, "fulfilled", "COMPULSORY")),
        scheduleRules = listOf(BackupScheduleRule("file-rule", "file", "File lesson", 2, 600, 710, "WEEKLY")),
        gradeItems = listOf(BackupGradeItem("grade-file", "file", "Exam", "EXAM", 80.0, 100.0, 2)),
    )

    @Test fun scheduleOnlyKeepsLocalSubjectsAndProgressAndMapsCourseId() {
        val merged = BackupMerger.merge(local, file, setOf(BackupSection.SCHEDULE))
        assertEquals(local.courses, merged.courses)
        assertEquals(local.gradeItems, merged.gradeItems)
        assertEquals("file-rule", merged.scheduleRules.single().id)
        assertEquals("local", merged.scheduleRules.single().courseId)
    }

    @Test fun progressOnlyKeepsSubjectMetadataAndSchedule() {
        val merged = BackupMerger.merge(local, file, setOf(BackupSection.PROGRESS))
        assertEquals("Local Physics", merged.courses.single().title)
        assertEquals("fulfilled", merged.courses.single().status)
        assertEquals(local.scheduleRules, merged.scheduleRules)
        assertEquals("local", merged.gradeItems.single().courseId)
    }

    @Test fun subjectsOnlyDoesNotRestoreFileResultOverEmptyLocalResult() {
        val phone = local.copy(courses = local.courses.map { it.copy(passFailResult = null) })
        val backup = file.copy(courses = file.courses.map { it.copy(passFailResult = "PASSED") })
        val merged = BackupMerger.merge(phone, backup, setOf(BackupSection.SUBJECTS))
        assertEquals(null, merged.courses.single().passFailResult)
    }

    @Test fun iconsCanComeFromFileWithoutChangingSubjectMetadata() {
        val phone = local.copy(courses = local.courses.map { it.copy(iconKey = "School") })
        val backup = file.copy(courses = file.courses.map { it.copy(iconKey = "Science") })
        val merged = BackupMerger.merge(phone, backup, setOf(BackupSection.ICONS))
        assertEquals("Science", merged.courses.single().iconKey)
        assertEquals("Local Physics", merged.courses.single().title)
        assertEquals(phone.gradeItems, merged.gradeItems)
    }

    @Test fun subjectImportKeepsPhoneIconUnlessIconsAreSelected() {
        val phone = local.copy(courses = local.courses.map { it.copy(iconKey = "School") })
        val backup = file.copy(courses = file.courses.map { it.copy(iconKey = "Science") })
        assertEquals("School", BackupMerger.merge(phone, backup, setOf(BackupSection.SUBJECTS)).courses.single().iconKey)
        assertEquals("Science", BackupMerger.merge(phone, backup,
            setOf(BackupSection.SUBJECTS, BackupSection.ICONS)).courses.single().iconKey)
    }

    @Test fun newSubjectIconAlsoRequiresIconSelection() {
        val backup = file.copy(courses = file.courses +
            BackupCourse("new", "CHEM", "Chemistry", 4, 1, "enrolled", "COMPULSORY", iconKey = "Science"))
        val subjects = BackupMerger.merge(local, backup, setOf(BackupSection.SUBJECTS))
        assertEquals(null, subjects.courses.first { it.code == "CHEM" }.iconKey)
        val withIcons = BackupMerger.merge(local, backup, setOf(BackupSection.SUBJECTS, BackupSection.ICONS))
        assertEquals("Science", withIcons.courses.first { it.code == "CHEM" }.iconKey)
    }

    @Test fun datesCanComeFromFileWithoutChangingOtherSettings() {
        val phone = local.copy(settings = BackupSettings(language = "ENGLISH",
            semesterPeriods = listOf(BackupSemesterPeriod(1, "2026-09-21", "2026-12-12"))))
        val backup = file.copy(settings = BackupSettings(language = "SLOVAK",
            semesterPeriods = listOf(BackupSemesterPeriod(1, "2026-09-22", "2026-12-13"))))
        val merged = BackupMerger.merge(phone, backup, setOf(BackupSection.PERIODS))
        assertEquals("ENGLISH", merged.settings.language)
        assertEquals("2026-09-22", merged.settings.semesterPeriods.single().studyStart)
        assertEquals(phone.courses, merged.courses)
    }

    @Test fun refusesSelectiveRestoreWhenPhoneChangedAfterPreview() = runBlocking {
        val reviewed = BackupPayloadCodec.encode(local)
        val changed = BackupPayloadCodec.encode(local.copy(courses = local.courses.map { it.copy(title = "Edited") }))
        var imported = false
        val repository = object : BackupRepository {
            override suspend fun exportJson() = changed
            override suspend fun importJson(raw: String) { imported = true }
        }
        val result = runCatching {
            repository.importSelectedJson(BackupPayloadCodec.encode(file), reviewed, setOf(BackupSection.SCHEDULE))
        }
        assertEquals(false, imported)
        assertEquals(true, result.isFailure)
    }

    @Test fun retryAfterDataAppliedDoesNotImportTwice() = runBlocking {
        val alreadyMerged = BackupMerger.merge(local, file, setOf(BackupSection.SCHEDULE))
        var importCount = 0
        val repository = object : BackupRepository {
            override suspend fun exportJson() = BackupPayloadCodec.encode(alreadyMerged)
            override suspend fun importJson(raw: String) { importCount++ }
        }
        repository.importSelectedJson(BackupPayloadCodec.encode(file), BackupPayloadCodec.encode(local),
            setOf(BackupSection.SCHEDULE))
        assertEquals(0, importCount)
    }
}
