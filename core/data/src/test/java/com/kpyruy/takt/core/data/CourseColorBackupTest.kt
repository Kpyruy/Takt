package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import org.junit.Assert.*
import org.junit.Test

class CourseColorBackupTest {
    private val course = BackupCourse("c", "C", "Course", 5, 1, "enrolled", "COMPULSORY", colorKey = "TEAL")

    @Test fun customColorSurvivesDatabaseAndJsonBackupRoundTrip() {
        val entity = course.toEntity()
        assertEquals("TEAL", entity.colorKey)
        assertEquals(entity, BackupPayloadCodec.decode(BackupPayloadCodec.encode(BackupPayload(courses = listOf(entity.toBackup())))).courses.single().toEntity())
    }

    @Test fun legacyBackupDefaultsToAutomaticColor() {
        val json = """{"version":11,"courses":[{"id":"c","code":"C","title":"Course","credits":5,"semester":1,"status":"enrolled","requirementType":"COMPULSORY"}]}"""
        assertNull(BackupPayloadCodec.decode(json).courses.single().colorKey)
    }

    @Test fun unrecognizedColorIsRejectedBeforeRestore() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupPayloadCodec.decode(BackupPayloadCodec.encode(BackupPayload(courses = listOf(course.copy(colorKey = "unbounded")))))
        }
    }

    @Test fun colorUsesSelectiveSubjectAppearanceChoice() {
        val local = BackupPayload(courses = listOf(course))
        val incoming = BackupPayload(courses = listOf(course.copy(colorKey = "ROSE")))
        assertEquals("TEAL", BackupMerger.merge(local, incoming, setOf(BackupSection.SUBJECTS)).courses.single().colorKey)
        assertEquals("ROSE", BackupMerger.merge(local, incoming, setOf(BackupSection.ICONS)).courses.single().colorKey)
        assertEquals(setOf(BackupSection.ICONS), BackupMerger.changedSections(local, incoming))
    }

    @Test fun studentHubCourseCopiesRetainColor() {
        val snapshot = StudentHubSnapshot(studies = listOf(StudentStudy("101", "Programme", active = true,
            courses = listOf(Course("uis:101:10", "C", "Course", 5, 1, CourseStatus.ENROLLED, colorKey = "TEAL")))))
        assertEquals(snapshot, StudentHubCodec.decode(StudentHubCodec.encode(snapshot)))
    }
}
