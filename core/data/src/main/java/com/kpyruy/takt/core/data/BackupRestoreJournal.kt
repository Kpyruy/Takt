package com.kpyruy.takt.core.data

import android.system.Os
import android.system.OsConstants
import android.util.AtomicFile
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.normalizeWallpaperReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

interface BackupSettingsParticipant {
    val restoreJournalDirectory: File
    val restoreNamespace: String
    fun beginBackupRestore(): BackupSettingsLease
}
interface BackupSettingsLease {
    val before: AppSettings
    /** Durable write without notifying UI/background consumers. Caller runs on IO. */
    fun persist(settings: AppSettings)
    fun publishAndRelease()
    /** Keep other writers blocked until recovery resolves the pending journal. */
    fun releaseWithoutPublishing()
}

@Serializable
data class BackupRestoreUndo(
    val id: String,
    val settings: BackupSettings,
    val uisCurrentSemester: Int? = null,
    val wallpaperReference: String? = null,
    val teaching: BackupTeaching? = null,
    val teachingFileWasPresent: Boolean = false,
    val preparedWallpaper: String? = null,
) {
    fun validated(): BackupRestoreUndo {
        require(runCatching { java.util.UUID.fromString(id).toString() == id }.getOrDefault(false))
        require(uisCurrentSemester == null || uisCurrentSemester > 0)
        require(wallpaperReference == null || normalizeWallpaperReference(wallpaperReference) == wallpaperReference)
        require(preparedWallpaper == null || normalizeWallpaperReference(preparedWallpaper) == preparedWallpaper)
        teaching?.validated()
        return this
    }
}

/** Private metadata-only undo record. Room's matching decision is the single logical commit point. */
class BackupRestoreJournal(val file: File) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }
    private val atomic = AtomicFile(file)
    fun exists() = file.exists() || File(file.path + ".bak").exists()
    fun read(): BackupRestoreUndo? {
        if (!exists()) return null
        val bytes = atomic.openRead().use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val size = input.read(buffer); if (size < 0) break
                require(output.size() + size <= 8_000_000) { "Restore journal is too large" }
                output.write(buffer, 0, size)
            }
            output.toByteArray()
        }
        return json.decodeFromString<BackupRestoreUndo>(bytes.toString(Charsets.UTF_8)).validated()
    }
    fun write(undo: BackupRestoreUndo) {
        val bytes = json.encodeToString(undo.validated()).toByteArray(Charsets.UTF_8)
        require(bytes.size <= 8_000_000) { "Restore journal is too large" }
        val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) }
        catch (failure: Throwable) { atomic.failWrite(output); throw failure }
        syncDirectory(file.parentFile!!)
        check(read() == undo) { "Could not persist restore journal" }
    }
    fun clear() {
        atomic.delete()
        syncDirectory(file.parentFile!!)
        check(!exists()) { "Could not remove restore journal" }
    }
}

internal fun syncDirectory(directory: File) {
    val descriptor = Os.open(directory.path, OsConstants.O_RDONLY, 0)
    try { Os.fsync(descriptor) } finally { Os.close(descriptor) }
}

enum class BackupRestoreStatus { READY, RESTORING, RECOVERY_REQUIRED }
class BackupRecoveryRequired(cause: Throwable) : IllegalStateException("Backup recovery must finish before data can be used", cause)
