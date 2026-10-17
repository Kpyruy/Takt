package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TeachingPersistenceTest {
    private val course = Course("uis:123:441254", "TPAR_6B", "Hardware", 5, 3, CourseStatus.ENROLLED,
        syllabusUrl = "https://is.stuba.sk/auth/katalog/syllabus.pl?predmet=441254")
    private val teacher = CourseTeacher("71659", "Martin", listOf("instructor"))
    private fun repository(store: TeachingStore, load: suspend (Course, Boolean) -> List<CourseTeacher> = { _, _ -> listOf(teacher) }) =
        CachedCourseTeachingRepository(load, { id, _ -> TeacherProfile(id, "Martin", consultationHours = "Monday 10:00") }, store = store)

    @Test fun restartRetainsTeamAndConsultationsAndRefreshFailurePreservesBoth() = runBlocking {
        val file = File.createTempFile("teaching", ".json").apply { delete() }
        try {
            val first = repository(TeachingStore(file))
            first.teachers(course); first.profile(teacher, AppLanguage.SLOVAK)
            val store = TeachingStore(file)
            val restarted = repository(store) { _, _ -> error("offline") }
            assertEquals(listOf(teacher), restarted.savedTeachers(course.copy(id = "another-id")))
            assertEquals("Monday 10:00", restarted.savedProfile(teacher, AppLanguage.SLOVAK)?.consultationHours)
            assertTrue(runCatching { restarted.teachers(course, true) }.isFailure)
            assertEquals(listOf(teacher), restarted.savedTeachers(course))
            assertEquals(store.snapshot.value, BackupPayloadCodec.decode(BackupPayloadCodec.encode(BackupPayload(teaching = store.snapshot.value))).teaching)
        } finally { file.delete() }
    }
    @Test fun savedTeamIsReadableWhileRefreshIsPendingAndUpdatedWhenFinished() = runBlocking {
        val store = TeachingStore()
        repository(store).teachers(course)
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val refreshed = teacher.copy(name = "Updated")
        val next = repository(store) { _, _ -> started.complete(Unit); release.await(); listOf(refreshed) }
        val job = async { next.teachers(course, true) }
        started.await()
        assertEquals(listOf(teacher), next.savedTeachers(course))
        release.complete(Unit); job.await()
        assertEquals(listOf(refreshed), next.savedTeachers(course))
    }
    @Test fun restoreInvalidatesOldRequestAndLegacyBackupDoesNotEraseSavedContacts() = runBlocking {
        val store = TeachingStore()
        repository(store).teachers(course)
        val saved = store.snapshot.value
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val next = repository(store) { _, _ -> started.complete(Unit); release.await(); listOf(teacher.copy(name = "Stale")) }
        val job = async { runCatching { next.teachers(course, true) } }
        started.await(); store.replace(saved); release.complete(Unit)
        assertTrue(job.await().isFailure)
        assertEquals(listOf(teacher), next.savedTeachers(course))
        val local = BackupPayload(teaching = saved)
        assertEquals(saved, BackupMerger.merge(local, BackupPayload(version = 9), setOf(BackupSection.TEACHERS)).teaching)
        next.clear(); assertNull(next.savedTeachers(course))
        assertTrue(TeachingStore().snapshot.value.teams.isEmpty())
    }
    @Test fun selectiveImportMatchesCodesAcrossCourseIdChangesAndPreservesUnselectedContacts() {
        val team = BackupTeachingTeam("TPAR_6B", listOf(BackupTeacher("71659", "Updated")))
        val details = BackupTeacherProfile("71659", "SLOVAK", "Updated", consultationHours = "Tuesday 12:00")
        val localCourse = BackupCourse("local-id", "TPAR_6B", "Hardware", 5, 3, "enrolled", "COMPULSORY")
        val local = BackupPayload(courses = listOf(localCourse), teaching = BackupTeaching())
        val incoming = local.copy(courses = listOf(localCourse.copy(id = "other-device")),
            teaching = BackupTeaching(listOf(team, BackupTeachingTeam("UNKNOWN", emptyList())), listOf(details)))
        assertEquals(local.teaching, BackupMerger.merge(local, incoming, setOf(BackupSection.APPEARANCE)).teaching)
        val merged = BackupMerger.merge(local, incoming, setOf(BackupSection.TEACHERS))
        assertEquals(listOf(localCourse), merged.courses)
        assertEquals(BackupTeaching(listOf(team), listOf(details)), merged.teaching)
        assertTrue(BackupSection.TEACHERS in BackupMerger.changedSections(local, incoming))
        assertFalse(BackupSection.TEACHERS in BackupMerger.changedSections(local, local.copy(teaching = null)))
    }
    @Test fun invalidTeacherIdsAreRejectedBeforeRestoreAndCorruptLocalFileFallsBack() {
        val snapshot = BackupTeaching(listOf(BackupTeachingTeam("TPAR_6B", listOf(BackupTeacher("../outside", "Invalid")))))
        assertTrue(runCatching { BackupPayloadCodec.decode(BackupPayloadCodec.encode(BackupPayload(teaching = snapshot))) }.isFailure)
        val file = File.createTempFile("bad-teaching", ".json")
        try { file.writeText("broken"); assertEquals(BackupTeaching(), TeachingStore(file).snapshot.value) }
        finally { file.delete() }
    }

}
