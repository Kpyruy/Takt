package com.kpyruy.takt.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.database.BackupRestoreDecision
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class BackupAtomicRestoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun payload(id: String) = BackupPayload(courses = listOf(BackupCourse(id, id.uppercase(), id, 5, 1, "enrolled", "COMPULSORY")))
    @Test fun failedTeachingWriteKeepsOriginalDatabaseSettingsAndExport() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, TaktDatabase::class.java).build()
        val blockedParent = File.createTempFile("blocked-teaching-parent", ".tmp", context.cacheDir)
        val prefsName = "restore-failure-${System.nanoTime()}"
        try {
            val settings = SharedPreferencesAppSettingsRepository(context, prefsName)
            val teaching = TeachingStore(File(blockedParent, "teaching.json"))
            val backup = RoomBackupRepository(db, settings, teachingStore = teaching)
            backup.importJson(BackupPayloadCodec.encode(payload("old")))
            val before = backup.exportJson()
            val initialSettings = settings.settings.value
            val next = payload("new").copy(settings = BackupSettings(language = "SLOVAK"), teaching = BackupTeaching(
                listOf(BackupTeachingTeam("NEW", listOf(BackupTeacher("123", "Synthetic teacher"))))))
            assertTrue(runCatching { backup.importJson(BackupPayloadCodec.encode(next)) }.isFailure)
            assertEquals(listOf("old"), db.courseDao().getAllSnapshot().map { it.id })
            assertEquals(initialSettings, settings.settings.value)
            assertEquals(before, backup.exportJson())
        } finally { db.close(); blockedParent.delete(); context.getSharedPreferences(prefsName, 0).edit().clear().commit() }
    }
    private fun team(code: String) = BackupTeaching(listOf(BackupTeachingTeam(code,
        listOf(BackupTeacher("123", "Teacher $code")))))

    private suspend fun withFixture(test: suspend (TaktDatabase, SharedPreferencesAppSettingsRepository, TeachingStore, BackupRestoreJournal, File) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(context, TaktDatabase::class.java).build()
        val root = File(context.cacheDir, "atomic-${UUID.randomUUID()}").apply { mkdirs() }
        val prefsName = root.name
        try { test(db, SharedPreferencesAppSettingsRepository(context, prefsName), TeachingStore(File(root, "teaching.json")),
            BackupRestoreJournal(File(root, "restore.json")), root) }
        finally { db.close(); root.deleteRecursively(); context.getSharedPreferences(prefsName, 0).edit().clear().commit() }
    }

    private class FailingSettings(private val real: SharedPreferencesAppSettingsRepository,
        private val cancel: Boolean = false) : AppSettingsRepository by real, BackupSettingsParticipant by real {
        override fun beginBackupRestore(): BackupSettingsLease {
            val lease = real.beginBackupRestore()
            return object : BackupSettingsLease by lease {
                override fun persist(settings: AppSettings) {
                    lease.persist(settings) // Fail after the durable preference mutation.
                    if (settings.language == AppLanguage.SLOVAK) {
                        if (cancel) throw CancellationException("Injected cancellation")
                        error("Injected late settings failure")
                    }
                }
            }
        }
    }

    @Test fun settingsFailureAfterTeachingAndPreferencesWriteRollsBackEverything() = runBlocking {
        verifyLateSettingsFailure(false)
    }
    @Test fun cancellationAfterExternalWritesRollsBackInNonCancellableContext() = runBlocking {
        verifyLateSettingsFailure(true)
    }
    private suspend fun verifyLateSettingsFailure(cancel: Boolean) = withFixture { db, settings, teaching, journal, root ->
        val backup = RoomBackupRepository(db, FailingSettings(settings, cancel), teachingStore = teaching, journal = journal)
        backup.importJson(BackupPayloadCodec.encode(payload("old").copy(teaching = team("OLD"))))
        val before = backup.exportJson()
        val fileBefore = File(root, "teaching.json").readBytes()
        val next = payload("new").copy(teaching = team("NEW"), settings = BackupSettings(language = "SLOVAK"))
        val failure = runCatching { backup.importJson(BackupPayloadCodec.encode(next)) }.exceptionOrNull()
        assertNotNull(failure)
        if (cancel) assertTrue(failure is CancellationException)
        assertEquals(before, backup.exportJson())
        assertArrayEquals(fileBefore, File(root, "teaching.json").readBytes())
        assertEquals(AppLanguage.ENGLISH, settings.settings.value.language)
        assertEquals(BackupRestoreStatus.READY, backup.restoreStatus.value)
        assertFalse(journal.exists())
    }

    @Test fun roomInsertFailureAfterExternalWritesRestoresOldState() = runBlocking {
        withFixture { db, settings, teaching, journal, root ->
            val backup = RoomBackupRepository(db, settings, teachingStore = teaching, journal = journal)
            backup.importJson(BackupPayloadCodec.encode(payload("old").copy(teaching = team("OLD"))))
            val before = backup.exportJson()
            val fileBefore = File(root, "teaching.json").readBytes()
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_restore BEFORE INSERT ON courses WHEN NEW.id = 'new' BEGIN SELECT RAISE(ABORT, 'injected'); END")
            val next = payload("new").copy(teaching = team("NEW"), settings = BackupSettings(language = "SLOVAK"))
            assertTrue(runCatching { backup.importJson(BackupPayloadCodec.encode(next)) }.isFailure)
            assertEquals(before, backup.exportJson())
            assertArrayEquals(fileBefore, File(root, "teaching.json").readBytes())
            assertEquals(BackupRestoreStatus.READY, backup.restoreStatus.value)
        }
    }

    @Test fun malformedPayloadNeverChangesDatabaseSettingsOrTeachingFile() = runBlocking {
        withFixture { db, settings, teaching, journal, root ->
            val backup = RoomBackupRepository(db, settings, teachingStore = teaching, journal = journal)
            backup.importJson(BackupPayloadCodec.encode(payload("old").copy(teaching = team("OLD"))))
            val before = backup.exportJson()
            val bytes = File(root, "teaching.json").readBytes()
            val invalid = payload("new").copy(scheduleRules = listOf(BackupScheduleRule("r", "new", "Lesson", 9, 480, 540, "WEEKLY")))
            assertTrue(runCatching { backup.importJson(BackupPayloadCodec.encode(invalid)) }.isFailure)
            assertEquals(before, backup.exportJson())
            assertArrayEquals(bytes, File(root, "teaching.json").readBytes())
            assertFalse(journal.exists())
        }
    }

    @Test fun freshStoresRecoverUndecidedExternalWritesBeforePublishingData() = runBlocking {
        withFixture { db, settings, teaching, journal, root ->
            val backup = RoomBackupRepository(db, settings, teachingStore = teaching, journal = journal)
            backup.importJson(BackupPayloadCodec.encode(payload("old").copy(teaching = team("OLD"))))
            val before = backup.exportJson()
            val oldSettings = BackupPayloadCodec.decode(before).settings
            val undo = BackupRestoreUndo(UUID.randomUUID().toString(), oldSettings, teaching = team("OLD"), teachingFileWasPresent = true)
            journal.write(undo)
            settings.beginBackupRestore().apply {
                persist(settings.settings.value.copy(language = AppLanguage.SLOVAK)); releaseWithoutPublishing()
            }
            teaching.beginBackupRestore().apply { persist(team("NEW")); releaseWithoutPublishing() }
            val restartedSettings = SharedPreferencesAppSettingsRepository(context, root.name)
            val restartedTeaching = TeachingStore(File(root, "teaching.json"))
            val restarted = RoomBackupRepository(db, restartedSettings, teachingStore = restartedTeaching, journal = journal)
            restarted.recoverPendingRestore()
            assertEquals(before, restarted.exportJson())
            assertEquals(team("OLD"), restartedTeaching.snapshot.value)
            assertEquals(AppLanguage.ENGLISH, restartedSettings.settings.value.language)
            assertFalse(journal.exists())
        }
    }

    @Test fun resolvedJournalsNeverOverwriteLaterUserEdits() = runBlocking {
        for (committed in listOf(false, true)) withFixture { db, settings, teaching, journal, root ->
            val backup = RoomBackupRepository(db, settings, teachingStore = teaching, journal = journal)
            backup.importJson(BackupPayloadCodec.encode(payload("old").copy(teaching = team("OLD"))))
            val undo = BackupRestoreUndo(UUID.randomUUID().toString(), BackupPayloadCodec.decode(backup.exportJson()).settings,
                teaching = team("OLD"), teachingFileWasPresent = true)
            journal.write(undo)
            db.backupRestoreDecisionDao().put(BackupRestoreDecision(undo.id, committed))
            settings.setLanguage(AppLanguage.ENGLISH)
            teaching.replace(team("LATER"))
            val restartedSettings = SharedPreferencesAppSettingsRepository(context, root.name)
            val restartedTeaching = TeachingStore(File(root, "teaching.json"))
            val restarted = RoomBackupRepository(db, restartedSettings, teachingStore = restartedTeaching, journal = journal)
            restarted.recoverPendingRestore()
            assertEquals(AppLanguage.ENGLISH, restartedSettings.settings.value.language)
            assertEquals(team("LATER"), restartedTeaching.snapshot.value)
            assertFalse(journal.exists())
        }
    }

    @Test fun selectiveImportKeepsUnselectedSectionsAndCanRepeatWithoutChanges() = runBlocking {
        withFixture { db, settings, teaching, journal, _ ->
            val backup = RoomBackupRepository(db, settings, teachingStore = teaching, journal = journal)
            backup.importJson(BackupPayloadCodec.encode(payload("old")))
            val before = backup.exportJson()
            val incoming = payload("new").copy(settings = BackupSettings(language = "SLOVAK"))
            backup.importSelectedJson(BackupPayloadCodec.encode(incoming), before, setOf(BackupSection.LANGUAGE))
            assertEquals(listOf("old"), db.courseDao().getAllSnapshot().map { it.id })
            assertEquals(AppLanguage.SLOVAK, settings.settings.value.language)
            backup.importSelectedJson(backup.exportJson(), backup.exportJson(), setOf(BackupSection.LANGUAGE))
        }
    }

    @Test fun schema18MigratesWithoutChangingExistingCourses() = runBlocking {
        val name = "restore-migration-${UUID.randomUUID()}.db"
        val schema = org.json.JSONObject(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.kpyruy.takt.core.database.TaktDatabase/18.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) old.execSQL(setup.getString(index))
            old.execSQL("INSERT INTO courses(id,code,title,credits,semester,status,requirementType) VALUES('old','OLD','Old',5,1,'enrolled','COMPULSORY')")
            old.version = 18
        }
        val db = Room.databaseBuilder(context, TaktDatabase::class.java, name).addMigrations(TaktDatabase.MIGRATION_18_19, TaktDatabase.MIGRATION_19_20).build()
        try {
            assertEquals(listOf("old"), db.courseDao().getAllSnapshot().map { it.id })
            val marker = BackupRestoreDecision(UUID.randomUUID().toString(), true)
            db.backupRestoreDecisionDao().put(marker)
            assertEquals(marker, db.backupRestoreDecisionDao().find(marker.id))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    private fun wallpaper(color: Int): BackupWallpaper {
        val bitmap = android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)
        bitmap.recycle()
        return BackupWallpaper.fromBytes(output.toByteArray())
    }
    private fun wallpaperStore(root: File) = WallpaperStore(object : android.content.ContextWrapper(context) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    })

    @Test fun failedRestorePreservesOriginalPhotoBytesAndReference() = runBlocking {
        withFixture { db, settings, teaching, journal, root ->
            val photos = wallpaperStore(root)
            val backup = RoomBackupRepository(db, settings, photos, teaching, journal)
            val old = payload("old").copy(settings = BackupSettings(wallpaperRecorded = true, wallpaper = wallpaper(android.graphics.Color.RED)))
            backup.importJson(BackupPayloadCodec.encode(old))
            val before = backup.exportJson()
            val reference = settings.settings.value.customWallpaper!!
            val bytes = File(root, "wallpapers/$reference").readBytes()
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_photo_restore BEFORE INSERT ON courses WHEN NEW.id='new' BEGIN SELECT RAISE(ABORT, 'injected'); END")
            val next = payload("new").copy(settings = old.settings.copy(wallpaper = wallpaper(android.graphics.Color.BLUE)))
            assertTrue(runCatching { backup.importJson(BackupPayloadCodec.encode(next)) }.isFailure)
            assertEquals(before, backup.exportJson())
            assertEquals(reference, settings.settings.value.customWallpaper)
            assertArrayEquals(bytes, File(root, "wallpapers/$reference").readBytes())
            assertEquals(listOf(reference), File(root, "wallpapers").list()!!.toList())
        }
    }

    @Test fun repeatedSuccessfulRestoresKeepPhotoStorageBounded() = runBlocking {
        withFixture { db, settings, teaching, journal, root ->
            val backup = RoomBackupRepository(db, settings, wallpaperStore(root), teaching, journal)
            repeat(5) { index ->
                backup.importJson(BackupPayloadCodec.encode(payload("old").copy(settings = BackupSettings(
                    wallpaperRecorded = true, wallpaper = wallpaper(android.graphics.Color.rgb(index * 40, 0, 0))))))
                assertTrue(File(root, "wallpapers").listFiles()!!.size <= 2)
                assertTrue(File(root, "wallpapers/${settings.settings.value.customWallpaper}").isFile)
            }
        }
    }

    @Test fun concurrentPickerCannotPrunePreparedRestoreImage() = runBlocking {
        val root = File(context.cacheDir, "photo-lease-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val photos = wallpaperStore(root)
            val photo = wallpaper(android.graphics.Color.RED)
            val old = photos.restoreWallpaper(photo, null)
            val prepared = photos.restoreWallpaper(photo, old, preserveExisting = true)
            val source = File(root, "picker.jpg").apply { writeBytes(photo.decodedBytes()) }
            photos.importImage(android.net.Uri.fromFile(source), old)
            assertTrue(File(root, "wallpapers/$prepared").isFile)
            assertTrue(File(root, "wallpapers/$old").isFile)
            photos.finishRestore(prepared, old, prepared)
            assertTrue(File(root, "wallpapers").listFiles()!!.size <= 2)
        } finally { root.deleteRecursively() }
    }

    @Test fun failedRollbackBlocksWritersUntilDurableRecoverySucceeds() = runBlocking {
        withFixture { db, real, teaching, journal, _ ->
            RoomBackupRepository(db, real, teachingStore = teaching, journal = journal)
                .importJson(BackupPayloadCodec.encode(payload("old").copy(teaching = team("OLD"))))
            var failRollback = true
            val settings = object : AppSettingsRepository by real, BackupSettingsParticipant by real {
                override fun beginBackupRestore(): BackupSettingsLease {
                    val lease = real.beginBackupRestore()
                    return object : BackupSettingsLease by lease {
                        override fun persist(settings: AppSettings) {
                            if (settings.language == AppLanguage.ENGLISH && failRollback) error("Disk still unavailable")
                            lease.persist(settings)
                            if (settings.language == AppLanguage.SLOVAK) error("Injected failure after new settings write")
                        }
                    }
                }
            }
            val backup = RoomBackupRepository(db, settings, teachingStore = teaching, journal = journal)
            val failure = runCatching { backup.importJson(BackupPayloadCodec.encode(payload("new").copy(
                teaching = team("NEW"), settings = BackupSettings(language = "SLOVAK")))) }.exceptionOrNull()
            assertTrue(failure is BackupRecoveryRequired)
            assertEquals(BackupRestoreStatus.RECOVERY_REQUIRED, backup.restoreStatus.value)
            assertEquals(listOf("old"), db.courseDao().getAllSnapshot().map { it.id })
            assertTrue(runCatching { real.setLanguage(AppLanguage.ENGLISH) }.isFailure)
            assertTrue(journal.exists())
            failRollback = false
            backup.recoverPendingRestore()
            assertEquals(AppLanguage.ENGLISH, real.settings.value.language)
            assertEquals(team("OLD"), teaching.snapshot.value)
            assertEquals(BackupRestoreStatus.READY, backup.restoreStatus.value)
            assertFalse(journal.exists())
        }
    }

    @Test fun failedJournalPreparationLeavesOriginalDataReadable() = runBlocking {
        withFixture { db, settings, teaching, journal, root ->
            val original = RoomBackupRepository(db, settings, teachingStore = teaching, journal = journal)
            original.importJson(BackupPayloadCodec.encode(payload("old")))
            val before = original.exportJson()
            val blocker = File(root, "blocked-parent").apply { writeText("file") }
            val failing = RoomBackupRepository(db, settings, teachingStore = teaching,
                journal = BackupRestoreJournal(File(blocker, "undo.json")))
            assertTrue(runCatching { failing.importJson(BackupPayloadCodec.encode(payload("new"))) }.isFailure)
            assertEquals(before, original.exportJson())
            assertEquals(BackupRestoreStatus.READY, failing.restoreStatus.value)
        }
    }

}
