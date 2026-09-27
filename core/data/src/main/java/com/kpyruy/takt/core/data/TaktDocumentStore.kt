package com.kpyruy.takt.core.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.kpyruy.takt.core.database.TaktDatabase
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class DocumentSyncStatus { DISCONNECTED, READY, CONFLICT, ERROR }

/** A user-granted Documents tree containing Takt/data.json and lazy subject folders. */
class TaktDocumentStore(
    private val context: Context,
    private val database: TaktDatabase,
    private val backupRepository: BackupRepository,
    private val settingsRepository: AppSettingsRepository,
) {
    private val preferences = context.getSharedPreferences("takt_documents", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _status = MutableStateFlow(DocumentSyncStatus.DISCONNECTED)
    val status: StateFlow<DocumentSyncStatus> = _status
    private val _unmigratedMaterials = MutableStateFlow(0)
    val unmigratedMaterials: StateFlow<Int> = _unmigratedMaterials
    private val resolver get() = context.contentResolver

    val isConnected: Boolean
        get() = preferences.getString("tree_uri", null)?.let { raw ->
            val uri = Uri.parse(raw)
            resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission && it.isWritePermission }
        } ?: false

    suspend fun connect(treeUri: Uri): DocumentSyncStatus = mutex.withLock {
        withContext(Dispatchers.IO) {
            resolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            val selected = DocumentFile.fromTreeUri(context, treeUri) ?: error("Папку не знайдено")
            require(selected.name == "Documents" || selected.name == "Takt") {
                "Оберіть папку Documents або Documents/Takt"
            }
            val root = if (selected.name == "Takt") selected
                else selected.findFile("Takt") ?: selected.createDirectory("Takt")
            requireNotNull(root) { "Не вдалося створити Documents/Takt" }
            require(root.isDirectory) { "Takt має бути папкою" }
            preferences.edit().putString("tree_uri", treeUri.toString()).apply()
            val remote = readBestBackup(root)
            val local = backupRepository.exportJson()
            when {
                remote == null -> {
                    require(!hasBackupFile(root)) { "Копія даних пошкоджена. Файл не перезаписано." }
                    writeBackup(root, local)
                }
                sameData(remote, local) -> {
                    if (BackupPayloadCodec.decode(remote).version < BackupPayload.CURRENT_VERSION || !mainIsValid(root)) writeBackup(root, local)
                    else rememberHash(remote)
                }
                isPristine(local) -> {
                    backupRepository.importJson(remote)
                    if (BackupPayloadCodec.decode(remote).version < BackupPayload.CURRENT_VERSION || !mainIsValid(root)) {
                        writeBackup(root, backupRepository.exportJson())
                    } else rememberHash(remote)
                }
                else -> {
                    _status.value = DocumentSyncStatus.CONFLICT
                    return@withContext DocumentSyncStatus.CONFLICT
                }
            }
            if (migrateLegacyMaterials()) writeBackup(root, backupRepository.exportJson())
            _status.value = DocumentSyncStatus.READY
            DocumentSyncStatus.READY
        }
    }

    suspend fun resume() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val root = rootOrNull()
            if (root == null) {
                _status.value = DocumentSyncStatus.DISCONNECTED
                return@withContext
            }
            val remote = readBestBackup(root)
            if (remote == null && hasBackupFile(root)) {
                _status.value = DocumentSyncStatus.ERROR
            } else if (remote != null && preferences.getString("last_hash", null) != hash(remote)) {
                if (sameData(remote, backupRepository.exportJson())) {
                    rememberHash(remote)
                    _status.value = DocumentSyncStatus.READY
                } else _status.value = DocumentSyncStatus.CONFLICT
            } else {
                _status.value = DocumentSyncStatus.READY
            }
            if (_status.value == DocumentSyncStatus.READY && migrateLegacyMaterials()) {
                writeBackup(root, backupRepository.exportJson())
            }
        }
    }

    suspend fun restoreFromDocuments() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val remote = readBestBackup(requireRoot()) ?: error("У Documents/Takt немає даних")
            backupRepository.importJson(remote)
            rememberHash(remote)
            if (migrateLegacyMaterials()) writeBackup(requireRoot(), backupRepository.exportJson())
            _status.value = DocumentSyncStatus.READY
        }
    }

    /** Read a stable candidate without changing local data or acknowledging a conflict. */
    suspend fun readBackupForReview(): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            readBestBackup(requireRoot()) ?: error("У Documents/Takt немає даних")
        }
    }

    suspend fun importSelectedFromDocuments(
        reviewedRaw: String,
        expectedLocalRaw: String,
        fromBackup: Set<BackupSection>,
    ) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val current = readBestBackup(requireRoot()) ?: error("У Documents/Takt немає даних")
            check(BackupPayloadCodec.decode(current) == BackupPayloadCodec.decode(reviewedRaw)) {
                "Копія в Documents/Takt змінилась. Переглянь відмінності ще раз."
            }
            backupRepository.importSelectedJson(reviewedRaw, expectedLocalRaw, fromBackup)
            val merged = backupRepository.exportJson()
            if (sameData(current, merged)) {
                rememberHash(current)
                _status.value = DocumentSyncStatus.READY
            } else {
                _status.value = DocumentSyncStatus.CONFLICT
            }
        }
    }

    suspend fun saveCurrentToDocuments() = mutex.withLock {
        withContext(Dispatchers.IO) {
            migrateLegacyMaterials()
            writeBackup(requireRoot(), backupRepository.exportJson())
            _status.value = DocumentSyncStatus.READY
        }
    }

    @OptIn(FlowPreview::class)
    fun startAutoSync(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            runCatching { resume() }.onFailure { _status.value = DocumentSyncStatus.ERROR }
            val tables = arrayOf(
                "courses", "schedule_rules", "schedule_one_off", "schedule_exceptions",
                "lesson_absences", "grade_items", "grade_scales", "grade_overrides",
                "study_tasks", "course_notes", "exam_info", "exam_materials",
            )
            merge(
                database.invalidationTracker.createFlow(*tables, emitInitialState = false).map { Unit },
                settingsRepository.settings.map { Unit },
            ).debounce(350).collect {
                runCatching { syncIfChanged() }.onFailure { _status.value = DocumentSyncStatus.ERROR }
            }
        }
    }

    private suspend fun syncIfChanged() = mutex.withLock {
        if (_status.value != DocumentSyncStatus.READY) return@withLock
        withContext(Dispatchers.IO) {
            val root = rootOrNull() ?: run {
                _status.value = DocumentSyncStatus.DISCONNECTED
                return@withContext
            }
            val localBeforeMigration = backupRepository.exportJson()
            val remote = readBestBackup(root)
            val known = preferences.getString("last_hash", null)
            if (remote == null && hasBackupFile(root)) {
                _status.value = DocumentSyncStatus.ERROR
                return@withContext
            }
            if (remote != null && hash(remote) != known) {
                if (sameData(remote, localBeforeMigration)) rememberHash(remote)
                else {
                    _status.value = DocumentSyncStatus.CONFLICT
                    return@withContext
                }
            }
            val local = if (migrateLegacyMaterials()) backupRepository.exportJson() else localBeforeMigration
            if (remote == null || !sameData(remote, local) || !mainIsValid(root)) writeBackup(root, local)
        }
    }

    suspend fun copyMaterial(courseCode: String, source: Uri): String = withContext(Dispatchers.IO) {
        val root = requireRoot()
        val folderName = safeCourseCode(courseCode)
        val folder = root.findFile(folderName) ?: root.createDirectory(folderName)
            ?: error("Не вдалося створити папку предмета")
        val displayName = sourceName(source)
        val fileName = UUID.randomUUID().toString().take(8) + "-" + safeFileName(displayName)
        val mime = resolver.getType(source) ?: "application/octet-stream"
        val target = folder.createFile(mime, fileName) ?: error("Не вдалося створити файл")
        try {
            resolver.openInputStream(source).use { input ->
                requireNotNull(input) { "Не вдалося прочитати файл" }
                resolver.openOutputStream(target.uri, "wt").use { output ->
                    requireNotNull(output) { "Не вдалося записати файл" }
                    input.copyTo(output)
                }
            }
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
        TaktMaterialReference.encode(folderName, target.name ?: fileName)
    }

    suspend fun resolve(uri: String): Uri? = withContext(Dispatchers.IO) {
        if (!uri.startsWith("takt://")) return@withContext runCatching { Uri.parse(uri) }.getOrNull()
        val path = TaktMaterialReference.decode(uri) ?: return@withContext null
        rootOrNull()?.findFile(path.folderName)?.findFile(path.fileName)?.uri
    }

    fun displayName(uri: String): String =
        if (uri.startsWith("takt://")) TaktMaterialReference.decode(uri)?.fileName.orEmpty().substringAfter('-')
        else Uri.parse(uri).lastPathSegment.orEmpty().substringAfterLast('/')

    suspend fun sourceDisplayName(uri: Uri): String = withContext(Dispatchers.IO) { sourceName(uri) }
    suspend fun sourceMimeType(uri: Uri): String = withContext(Dispatchers.IO) {
        resolver.getType(uri) ?: "application/octet-stream"
    }

    private fun rootOrNull(): DocumentFile? {
        val raw = preferences.getString("tree_uri", null) ?: return null
        val uri = Uri.parse(raw)
        if (!isConnected) return null
        val selected = DocumentFile.fromTreeUri(context, uri) ?: return null
        return if (selected.name == "Takt") selected else selected.findFile("Takt") ?: selected.createDirectory("Takt")
    }

    private fun requireRoot() = rootOrNull() ?: error("Спочатку підключіть Documents/Takt у налаштуваннях")

    private fun readBestBackup(root: DocumentFile): String? {
        for (name in listOf("data.json", "data.previous.json")) {
            val file = root.findFile(name) ?: continue
            val raw = runCatching { resolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            if (raw != null && runCatching { BackupPayloadCodec.decode(raw) }.isSuccess) return raw
        }
        return null
    }

    private fun hasBackupFile(root: DocumentFile) =
        root.findFile("data.json") != null || root.findFile("data.previous.json") != null

    private fun mainIsValid(root: DocumentFile): Boolean = root.findFile("data.json")?.let { main ->
        runCatching {
            resolver.openInputStream(main.uri)?.bufferedReader()?.use { BackupPayloadCodec.decode(it.readText()) }
                ?: error("Не вдалося прочитати data.json")
        }.isSuccess
    } ?: false

    private fun writeBackup(root: DocumentFile, raw: String) {
        BackupPayloadCodec.decode(raw)
        val main = root.findFile("data.json")
        if (main != null && mainIsValid(root)) {
            val previous = root.findFile("data.previous.json") ?: root.createFile("application/json", "data.previous.json")
                ?: error("Не вдалося створити попередню копію")
            resolver.openInputStream(main.uri).use { input ->
                resolver.openOutputStream(previous.uri, "wt").use { output ->
                    requireNotNull(input); requireNotNull(output)
                    input.copyTo(output)
                }
            }
        }
        val target = main ?: root.createFile("application/json", "data.json") ?: error("Не вдалося створити data.json")
        resolver.openOutputStream(target.uri, "wt")?.bufferedWriter()?.use { it.write(raw) }
            ?: error("Не вдалося записати data.json")
        require(resolver.openInputStream(target.uri)?.bufferedReader()?.use { it.readText() } == raw) {
            "Не вдалося перевірити data.json після запису"
        }
        rememberHash(raw)
    }

    private fun rememberHash(raw: String) { preferences.edit().putString("last_hash", hash(raw)).apply() }
    private fun hash(raw: String) = MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun sameData(a: String, b: String) =
        BackupPayloadCodec.decode(a).copy(version = 0) == BackupPayloadCodec.decode(b).copy(version = 0)

    private fun isPristine(raw: String): Boolean = BackupPayloadCodec.decode(raw).hasNoUserContent()

    /** Convert older persisted document URIs while their original grant still exists. */
    private suspend fun migrateLegacyMaterials(): Boolean {
        val courses = database.courseDao().getAllSnapshot().associate { it.id to it.code }
        var changed = false
        var unavailable = 0
        for (material in database.examDao().getMaterialsSnapshot()) {
            val original = Uri.parse(material.uri)
            if (original.scheme != "content" && original.scheme != "file") continue
            val code = courses[material.courseId]
            val copied = code?.let { runCatching { copyMaterial(it, original) }.getOrNull() }
            if (copied == null) unavailable++
            else {
                database.examDao().upsertMaterial(material.copy(uri = copied))
                changed = true
            }
        }
        _unmigratedMaterials.value = unavailable
        return changed
    }

    private fun sourceName(uri: Uri): String {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0) ?: "material"
        }
        return uri.lastPathSegment ?: "material"
    }

    private fun safeCourseCode(code: String): String = code.replace(Regex("[^A-Za-z0-9_-]"), "_").take(60)
    private fun safeFileName(name: String): String =
        name.filterNot { it == '/' || it == '\\' || it.isISOControl() }.take(100).ifBlank { "material" }
}

internal fun BackupPayload.hasNoUserContent(): Boolean =
    courses.isEmpty() && scheduleRules.isEmpty() && oneOffEvents.isEmpty() &&
        lessonAbsences.isEmpty() && scheduleExceptions.isEmpty() && gradeItems.isEmpty() &&
        gradeScales.isEmpty() && gradeOverrides.isEmpty() && studyTasks.isEmpty() &&
        courseNotes.isEmpty() && examInfo.isEmpty() && examMaterials.isEmpty()
