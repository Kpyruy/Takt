package com.kpyruy.takt.app

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Opt-in two-process test. The host force-stops only the emulator preview at READY_TO_KILL. */
class BackupRestoreProcessDeathTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "restore-process-death.db"
    private val preferencesName = "restore-process-death"
    private fun payload(id: String) = BackupPayload(courses = listOf(
        BackupCourse(id, id.uppercase(), id, 5, 1, "enrolled", "COMPULSORY")),
        teaching = BackupTeaching(listOf(BackupTeachingTeam(id.uppercase(), listOf(BackupTeacher("123", id))))))
    private fun enabled(phase: String) {
        assumeTrue(context.packageName.endsWith(".glass"))
        assumeTrue(InstrumentationRegistry.getArguments().getString("restoreProcessDeath") == phase)
    }
    @Test fun seedThenPauseBeforeRoomCommit() = runBlocking<Unit> {
        enabled("seed")
        context.deleteDatabase(databaseName)
        context.getSharedPreferences(preferencesName, 0).edit().clear().commit()
        val root = File(context.filesDir, "restore-process-test").apply { deleteRecursively(); mkdirs() }
        val db = Room.databaseBuilder(context, TaktDatabase::class.java, databaseName).build()
        val real = SharedPreferencesAppSettingsRepository(context, preferencesName)
        val teaching = TeachingStore(File(root, "teaching.json"))
        val settings = object : AppSettingsRepository by real, BackupSettingsParticipant by real {
            override fun beginBackupRestore(): BackupSettingsLease {
                val lease = real.beginBackupRestore()
                return object : BackupSettingsLease by lease {
                    override fun persist(settings: AppSettings) {
                        lease.persist(settings)
                        if (settings.language == AppLanguage.SLOVAK) {
                            Log.i("TaktRestoreTest", "READY_TO_KILL")
                            Thread.sleep(120_000) // Host kills before this transaction can commit.
                            error("Host did not stop process")
                        }
                    }
                }
            }
        }
        val backup = RoomBackupRepository(db, settings, teachingStore = teaching,
            journal = BackupRestoreJournal(File(root, "undo.json")))
        backup.importJson(BackupPayloadCodec.encode(payload("old")))
        backup.importJson(BackupPayloadCodec.encode(payload("new").copy(settings = BackupSettings(language = "SLOVAK"))))
        fail("Expected host process termination")
    }
    @Test fun newProcessRecoversDurableStoresAndRolledBackDatabase() = runBlocking<Unit> {
        enabled("recover")
        val root = File(context.filesDir, "restore-process-test")
        val db = Room.databaseBuilder(context, TaktDatabase::class.java, databaseName).build()
        try {
            val settings = SharedPreferencesAppSettingsRepository(context, preferencesName)
            val teaching = TeachingStore(File(root, "teaching.json"))
            val journal = BackupRestoreJournal(File(root, "undo.json"))
            assertEquals(AppLanguage.SLOVAK, settings.settings.value.language)
            assertEquals("NEW", teaching.snapshot.value.teams.single().courseCode)
            assertTrue(journal.exists())
            assertEquals(listOf("old"), db.courseDao().getAllSnapshot().map { it.id })
            val backup = RoomBackupRepository(db, settings, teachingStore = teaching, journal = journal)
            backup.recoverPendingRestore()
            assertEquals(AppLanguage.ENGLISH, settings.settings.value.language)
            assertEquals("OLD", teaching.snapshot.value.teams.single().courseCode)
            assertEquals(listOf("old"), db.courseDao().getAllSnapshot().map { it.id })
            assertEquals(BackupRestoreStatus.READY, backup.restoreStatus.value)
            assertFalse(journal.exists())
            BackupPayloadCodec.decode(backup.exportJson())
        } finally {
            db.close(); context.deleteDatabase(databaseName); root.deleteRecursively()
            context.getSharedPreferences(preferencesName, 0).edit().clear().commit()
        }
    }
}
