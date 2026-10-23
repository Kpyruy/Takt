package com.kpyruy.takt.core.data

import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.database.BackupRestoreDecision
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class RoomBackupRepository(
    private val database: TaktDatabase,
    private val settingsRepository: AppSettingsRepository,
    private val wallpaperStore: WallpaperStore? = null,
    private val teachingStore: TeachingStore? = null,
    journal: BackupRestoreJournal? = null,
) : BackupRepository {
    private val participant get() = settingsRepository as? BackupSettingsParticipant
        ?: error("Settings repository does not support atomic backup restore")
    private val journal = journal ?: (settingsRepository as? BackupSettingsParticipant)?.let { store ->
        val name = store.restoreNamespace + ":" + (database.openHelper.databaseName ?: "memory-${System.identityHashCode(database)}")
        val key = MessageDigest.getInstance("SHA-256").digest(name.toByteArray()).take(12).joinToString("") { "%02x".format(it.toInt() and 255) }
        BackupRestoreJournal(File(store.restoreJournalDirectory, "$key.undo.json"))
    }
    private val runtime = synchronized(runtimes) {
        runtimes.getOrPut(database) { RestoreRuntime(Mutex(), MutableStateFlow(if (this.journal?.exists() == true)
            BackupRestoreStatus.RECOVERY_REQUIRED else BackupRestoreStatus.READY)) }
    }
    private val mutex get() = runtime.mutex
    private val mutableRestoreStatus get() = runtime.status
    val restoreStatus: StateFlow<BackupRestoreStatus> = mutableRestoreStatus.asStateFlow()
    override val changes: Flow<Unit> get() = merge(teachingStore?.snapshot?.map { Unit } ?: emptyFlow(), database.studentHubDao().observe().map { Unit })

    suspend fun <T> withStableData(read: suspend () -> T): T = withContext(Dispatchers.IO) { mutex.withLock {
        recoverPendingLocked(); read()
    } }

    suspend fun recoverPendingRestore() = withContext(Dispatchers.IO) { mutex.withLock { recoverPendingLocked() } }

    override suspend fun exportJson(): String = withContext(Dispatchers.IO) { mutex.withLock {
        recoverPendingLocked()
        BackupPayloadCodec.encode(exportPayload())
    } }

    private suspend fun exportPayload(): BackupPayload {
        val settings = settingsRepository.settings.value
        val photo = wallpaperStore?.exportWallpaper(settings.customWallpaper)
        return database.withTransaction {
            val courses = database.courseDao().getAllSnapshot().map { it.toBackup() }
            BackupPayload(
                lessonAbsences = database.scheduleDao().getAbsencesSnapshot().map { it.toBackup() },
                courses = courses,
                courseIdAliases = database.courseDao().getAliasesSnapshot().map { BackupCourseIdAlias(it.oldId, it.courseId) },
                courseImportDecisions = database.courseDao().getImportDecisionsSnapshot().map { it.courseId },
                scheduleRules = database.scheduleDao().getRulesSnapshot().map { it.toBackup() },
                oneOffEvents = database.scheduleDao().getOneOffEventsSnapshot().map { it.toBackup() },
                scheduleExceptions = database.scheduleDao().getExceptionsSnapshot().map { it.toBackup() },
                gradeItems = database.gradeDao().getItemsSnapshot().map { it.toBackup() },
                gradeScales = database.gradeDao().getScalesSnapshot().map { it.toBackup() },
                gradeOverrides = database.gradeDao().getOverridesSnapshot().map { it.toBackup() },
                studyTasks = database.studyContentDao().getTasksSnapshot().map { it.toBackup() },
                courseNotes = database.studyContentDao().getNotesSnapshot().map { it.toBackup() },
                examInfo = database.examDao().getExamInfoSnapshot().map { it.toBackup() },
                examMaterials = database.examDao().getMaterialsSnapshot().map { it.toBackup() },
                studentHub = database.studentHubDao().read()?.let { StudentHubCodec.json.decodeFromString<BackupStudentHub>(it.payloadJson).validated() },
                teaching = teachingStore?.snapshot?.value?.forCourses(courses.map { it.code }),
                settings = settings.toBackup().copy(wallpaperRecorded = wallpaperStore != null &&
                    (settings.customWallpaper == null || photo != null), wallpaper = photo),
            )
        }
    }

    override suspend fun importJson(raw: String) = withContext(Dispatchers.IO) {
        val payload = BackupPayloadCodec.decode(raw)
        mutex.withLock { recoverPendingLocked(); importLocked(payload) }
    }

    override suspend fun importSelectedJson(raw: String, expectedLocalRaw: String, fromBackup: Set<BackupSection>) = withContext(Dispatchers.IO) {
        require(fromBackup.isNotEmpty()) { "Select at least one backup section" }
        val expected = BackupPayloadCodec.decode(expectedLocalRaw).copy(version = 0)
        val incoming = BackupPayloadCodec.decode(raw)
        val merged = BackupMerger.merge(expected, incoming, fromBackup).validated()
        mutex.withLock {
            recoverPendingLocked()
            val current = exportPayload().copy(version = 0)
            if (current == merged.copy(version = 0)) return@withLock
            check(current == expected) { "Local data changed; review the backup again" }
            importLocked(merged, expected)
        }
    }

    private suspend fun importLocked(payload: BackupPayload, expected: BackupPayload? = null) {
        val log = requireNotNull(journal) { "Restore journal is unavailable" }
        check(!log.exists()) { "Previous restore cleanup must finish before another import" }
        mutableRestoreStatus.value = BackupRestoreStatus.RESTORING
        var settings: BackupSettingsLease? = null
        var teaching: TeachingStore.TeachingRestoreLease? = null
        var stagedPhoto: String? = null
        var undo: BackupRestoreUndo? = null
        var completed = false
        var journalPrepared = false
        try {
            settings = participant.beginBackupRestore()
            teaching = teachingStore?.beginBackupRestore()
            val old = settings.before
            stagedPhoto = if (payload.settings.wallpaperRecorded && wallpaperStore != null)
                payload.settings.wallpaper?.let { wallpaperStore.restoreWallpaper(it, old.customWallpaper, preserveExisting = true) }
                else null
            val targetPhoto = if (payload.settings.wallpaperRecorded && wallpaperStore != null) stagedPhoto else old.customWallpaper
            val targetSettings = payload.settings.toModel(old.uisCurrentSemester, targetPhoto)
            val record = BackupRestoreUndo(UUID.randomUUID().toString(), old.toBackup(), old.uisCurrentSemester,
                old.customWallpaper, teaching?.before, teaching?.fileWasPresent ?: false, stagedPhoto)
            undo = record
            database.withTransaction {
                if (expected != null) check(exportPayload().copy(version = 0) == expected) { "Local data changed; review the backup again" }
                log.write(record) // Must be durable before the first external-store mutation.
                journalPrepared = true
                payload.teaching?.let { teaching?.persist(it) }
                settings.persist(targetSettings)
                currentCoroutineContext().ensureActive()
                replaceDatabase(payload)
                database.backupRestoreDecisionDao().clear()
                database.backupRestoreDecisionDao().put(BackupRestoreDecision(record.id, true))
            } // Imported rows and the authoritative decision become durable together.
            withContext(NonCancellable) {
                settings.publishAndRelease(); teaching?.publishAndRelease()
                mutableRestoreStatus.value = BackupRestoreStatus.READY
                cleanupResolved(record)
            }
            completed = true
        } catch (failure: Throwable) {
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    val record = undo
                    val decision = record?.let { database.backupRestoreDecisionDao().find(it.id) }
                    if (decision?.committed == true) {
                        // Commit may have completed just before cancellation. Never undo a decided transaction.
                        teaching?.reloadPersisted()
                        settings?.publishAndRelease(); teaching?.publishAndRelease()
                    } else {
                        if (record != null && journalPrepared) {
                            settings?.persist(record.settings.toModel(record.uisCurrentSemester, record.wallpaperReference))
                            record.teaching?.let { teaching?.undo(it, record.teachingFileWasPresent) }
                            database.withTransaction {
                                database.backupRestoreDecisionDao().clear()
                                database.backupRestoreDecisionDao().put(BackupRestoreDecision(record.id, false))
                            }
                        }
                        // A failed intent write never changed external stores; discard its incomplete record.
                        if (!journalPrepared && log.exists()) log.clear()
                        settings?.publishAndRelease(); teaching?.publishAndRelease()
                        wallpaperStore?.discardPrepared(stagedPhoto)
                    }
                    mutableRestoreStatus.value = BackupRestoreStatus.READY
                    if (record != null) cleanupResolved(record)
                    completed = true
                } catch (recovery: Throwable) {
                    recovery.addSuppressed(failure)
                    throw BackupRecoveryRequired(recovery)
                }
            }
            throw failure
        } finally {
            if (!completed) {
                settings?.releaseWithoutPublishing(); teaching?.releaseWithoutPublishing()
                mutableRestoreStatus.value = BackupRestoreStatus.RECOVERY_REQUIRED
            }
        }
    }

    private suspend fun recoverPendingLocked() {
        val log = journal ?: return
        val record = try { log.read() } catch (failure: Exception) {
            mutableRestoreStatus.value = BackupRestoreStatus.RECOVERY_REQUIRED
            throw BackupRecoveryRequired(failure)
        }
        if (record == null) {
            if (mutableRestoreStatus.value == BackupRestoreStatus.RECOVERY_REQUIRED) {
                val settings = participant.beginBackupRestore()
                val teaching = teachingStore?.beginBackupRestore()
                try {
                    teaching?.reloadPersisted(); settings.publishAndRelease(); teaching?.publishAndRelease()
                    mutableRestoreStatus.value = BackupRestoreStatus.READY
                } catch (failure: Throwable) {
                    settings.releaseWithoutPublishing(); teaching?.releaseWithoutPublishing()
                    throw BackupRecoveryRequired(failure)
                }
            }
            return
        }
        mutableRestoreStatus.value = BackupRestoreStatus.RESTORING
        var settings: BackupSettingsLease? = null
        var teaching: TeachingStore.TeachingRestoreLease? = null
        var completed = false
        try {
            settings = participant.beginBackupRestore(); teaching = teachingStore?.beginBackupRestore()
            val decision = database.backupRestoreDecisionDao().find(record.id)
            if (decision == null) {
                settings.persist(record.settings.toModel(record.uisCurrentSemester, record.wallpaperReference))
                record.teaching?.let { teaching?.undo(it, record.teachingFileWasPresent) }
                database.withTransaction {
                    database.backupRestoreDecisionDao().clear()
                    database.backupRestoreDecisionDao().put(BackupRestoreDecision(record.id, false))
                }
                wallpaperStore?.discardPrepared(record.preparedWallpaper)
            } else teaching?.reloadPersisted() // Resolved journal: retain any later user edits.
            settings.publishAndRelease(); teaching?.publishAndRelease()
            mutableRestoreStatus.value = BackupRestoreStatus.READY
            completed = true
            cleanupResolved(record)
        } catch (failure: Throwable) { throw BackupRecoveryRequired(failure) }
        finally { if (!completed) {
            settings?.releaseWithoutPublishing(); teaching?.releaseWithoutPublishing()
            mutableRestoreStatus.value = BackupRestoreStatus.RECOVERY_REQUIRED
        } }
    }

    private suspend fun cleanupResolved(record: BackupRestoreUndo) {
        try {
            wallpaperStore?.finishRestore(settingsRepository.settings.value.customWallpaper,
                record.wallpaperReference, record.preparedWallpaper)
            database.withTransaction {
                if (journal?.read()?.id == record.id) journal.clear()
                database.backupRestoreDecisionDao().delete(record.id)
            }
        } catch (_: Exception) {
            // A durable decision makes leftover cleanup harmless; retry on the next export/startup.
        }
    }

    private data class RestoreRuntime(val mutex: Mutex, val status: MutableStateFlow<BackupRestoreStatus>)
    private companion object { val runtimes = java.util.WeakHashMap<TaktDatabase, RestoreRuntime>() }

    private suspend fun replaceDatabase(payload: BackupPayload) {
        val courseDao = database.courseDao()
        val scheduleDao = database.scheduleDao()
        val gradeDao = database.gradeDao()
        val studyDao = database.studyContentDao()
        val examDao = database.examDao()

        database.studentHubDao().clear()
        payload.studentHub?.let { database.studentHubDao().put(com.kpyruy.takt.core.database.StudentHubEntity(payloadJson = StudentHubCodec.json.encodeToString(it))) }
        examDao.deleteAllMaterials()
        examDao.deleteAllExamInfo()
        studyDao.deleteAllNotes()
        studyDao.deleteAllTasks()
        gradeDao.deleteAllOverrides()
        gradeDao.deleteAllScales()
        gradeDao.deleteAllItems()
        scheduleDao.deleteAllAbsences()
        scheduleDao.deleteAllExceptions()
        scheduleDao.deleteAllOneOffEvents()
        scheduleDao.deleteAllRules()
        courseDao.deleteAllImportDecisions()
        courseDao.deleteAllAliases()
        courseDao.deleteAll()

        courseDao.insertAll(payload.courses.map { it.toEntity() })
        courseDao.upsertAliases(payload.courseIdAliases.map { com.kpyruy.takt.core.database.CourseIdAliasEntity(it.oldId, it.courseId) })
        courseDao.upsertImportDecisions(payload.courseImportDecisions.map { com.kpyruy.takt.core.database.CourseImportDecisionEntity(it) })
        scheduleDao.upsertAbsences(payload.lessonAbsences.map { it.toEntity() })
        scheduleDao.upsertRules(payload.scheduleRules.map { it.toEntity() })
        scheduleDao.upsertOneOffEvents(payload.oneOffEvents.map { it.toEntity() })
        scheduleDao.upsertExceptions(payload.scheduleExceptions.map { it.toEntity() })
        gradeDao.upsertItems(payload.gradeItems.map { it.toEntity() })
        gradeDao.upsertScales(payload.gradeScales.map { it.toEntity() })
        gradeDao.upsertOverrides(payload.gradeOverrides.map { it.toEntity() })
        studyDao.upsertTasks(payload.studyTasks.map { it.toEntity() })
        studyDao.upsertNotes(payload.courseNotes.map { it.toEntity() })
        examDao.upsertExamInfo(payload.examInfo.map { it.toEntity() })
        examDao.upsertMaterials(payload.examMaterials.map { it.toEntity() })
    }
}
