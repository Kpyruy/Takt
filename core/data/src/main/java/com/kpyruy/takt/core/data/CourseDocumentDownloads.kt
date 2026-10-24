package com.kpyruy.takt.core.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.kpyruy.takt.core.model.CourseDocumentFile
import com.kpyruy.takt.core.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

/** An explicitly saved public copy is separate from temporary previews and never exported in backups. */
data class CachedCourseDocument(val uri: Uri, val fileName: String, val mimeType: String, val file: File)
data class LocalCourseDocument(val uri: Uri, val fileName: String, val mimeType: String)
class CourseDocumentDownloads(context: Context, private val documents: TaktDocumentStore, private val accountScope: Long = 0L, private val ownerKey: String? = null) {
    private val context = context.applicationContext
    private val resolver get() = context.contentResolver
    private val preferences = this.context.getSharedPreferences("takt_course_downloads", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val cache get() = File(context.cacheDir, "uis_documents").apply { check(isDirectory || mkdirs()) }

    val canSave: Boolean get() = documents.isConnected

    suspend fun cached(course: Course, document: CourseDocumentFile, repository: CourseDocumentsRepository,
        refresh: Boolean = false): CachedCourseDocument = mutex.withLock { withContext(Dispatchers.IO) {
        val key = "${ownerKey ?: accountScope}-${key(course, document)}"
        val name = preferences.getString("cache-name:$key", null)
        val old = name?.takeIf { it == File(it).name }?.let { File(cache, it) }?.takeIf { it.isFile }
        if (!refresh && old != null && (document.contentRevision == null || preferences.getString("revision:$key", null) == document.contentRevision)) return@withContext result(old,
            preferences.getString("display:$key", document.fileName ?: document.title)!!,
            preferences.getString("mime:$key", document.mimeType) ?: "application/octet-stream")
        val temporary = File.createTempFile("partial-", ".tmp", cache)
        try {
            val downloaded = temporary.outputStream().use { repository.download(document, it) }
            currentCoroutineContext().ensureActive()
            val display = CourseDocumentPaths.fileName(downloaded.fileName)
            val diskKey = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
                .take(12).joinToString("") { "%02x".format(it.toInt() and 255) }
            val target = File(cache, "$diskKey-${java.util.UUID.randomUUID()}-$display")
            check(temporary.renameTo(target)) { "Could not publish document preview" }
            old?.takeIf { it != target }?.let { previous -> cleanupScope.launch { delay(60_000); previous.delete() } }
            val mime = downloaded.mimeType ?: document.mimeType ?: "application/octet-stream"
            preferences.edit().putString("cache-name:$key", target.name).putString("display:$key", display)
                .putString("revision:$key", document.contentRevision)
                .putString("mime:$key", mime).apply()
            // A bounded preview cache; public copies are unaffected by eviction.
            var bytes = target.length(); var count = 1
            cache.listFiles().orEmpty().filter { it != target }.sortedByDescending(File::lastModified).forEach { file ->
                if (file.name.startsWith("partial-") || count >= 8 || bytes + file.length() > 150L * 1024 * 1024) file.delete()
                else { count++; bytes += file.length() }
            }
            preferences.all.keys.filter { it.startsWith("cache-name:") }.forEach { entry ->
                val cachedName = preferences.getString(entry, null)
                if (cachedName == null || !File(cache, cachedName).isFile) {
                    val oldKey = entry.removePrefix("cache-name:")
                    preferences.edit().remove(entry).remove("display:$oldKey").remove("mime:$oldKey").remove("revision:$oldKey").apply()
                }
            }
            result(target, display, mime)
        } finally { temporary.delete() }
    } }

    private fun savedKey(course: Course, document: CourseDocumentFile) =
        if (ownerKey == null) key(course, document) else "$ownerKey-${key(course, document)}"

    suspend fun saved(course: Course, document: CourseDocumentFile): Uri? = withContext(Dispatchers.IO) {
        if (!documents.isConnected) return@withContext null
        val key = savedKey(course, document)
        val indexed = preferences.getString("saved:$key", null)?.let(Uri::parse)
        if (indexed != null) try {
            val name = preferences.getString("saved-name:$key", null) ?: documents.sourceDisplayName(indexed)
            if (documents.findDownloadedDocument(course.code, listOf(name)) == indexed)
                resolver.openFileDescriptor(indexed, "r")?.use { return@withContext indexed }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (_: Exception) { }
        // Upgrade the old device index only after verifying the file belongs to this selected course folder.
        if (ownerKey != null) preferences.getString("saved:${key(course,document)}", null)?.let { raw ->
            try {
                val legacy = Uri.parse(raw); val name = documents.sourceDisplayName(legacy)
                if (documents.findDownloadedDocument(course.code, listOf(name)) == legacy) {
                    resolver.openFileDescriptor(legacy,"r")?.use {
                        preferences.edit().putString("saved:$key",legacy.toString()).putString("saved-name:$key",name)
                            .putString("saved-mime:$key",resolver.getType(legacy) ?: document.mimeType).apply()
                        return@withContext legacy
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (_: Exception) { }
        }
        val recovered = documents.findDownloadedDocument(course.code,
            listOfNotNull(document.downloadedFileName, preferences.getString("saved-name:$key", null), document.fileName)
                .map(CourseDocumentPaths::fileName)) ?: return@withContext null
        preferences.edit().putString("saved:$key", recovered.toString())
            .putString("saved-name:$key", documents.sourceDisplayName(recovered))
            .putString("saved-mime:$key", resolver.getType(recovered) ?: document.mimeType).apply()
        recovered
    }

    suspend fun localFiles(course: Course): List<LocalCourseDocument> = documents.downloadedDocuments(course.code)
        .filterNot {
            val entry = "retired:${it.uri}"
            if (!preferences.contains(entry)) false else {
                // Providers can reuse a deleted document's URI for a later download.
                if (preferences.all.any { (key, value) -> key.startsWith("saved:") && value == it.uri.toString() }) {
                    preferences.edit().remove(entry).apply()
                    return@filterNot false
                }
                if (preferences.getLong(entry, Long.MAX_VALUE) <= System.currentTimeMillis() && documents.removeDownloadedDocument(it.uri))
                    preferences.edit().remove(entry).apply()
                true
            }
        }

    /** Local reads never wait for the network/download mutex. */
    suspend fun available(course: Course, document: CourseDocumentFile): LocalCourseDocument? = withContext(Dispatchers.IO) {
        val saved = saved(course, document)
        if (saved != null) return@withContext LocalCourseDocument(saved,
            preferences.getString("saved-name:${savedKey(course,document)}", null) ?: documents.sourceDisplayName(saved),
            preferences.getString("saved-mime:${savedKey(course,document)}", null) ?: document.mimeType)
        val key = "${ownerKey ?: accountScope}-${key(course, document)}"
        val name = preferences.getString("cache-name:$key", null)
        val file = name?.takeIf { it == File(it).name }?.let { File(cache, it) }?.takeIf { it.isFile } ?: return@withContext null
        LocalCourseDocument(result(file, document.fileName ?: document.title,
            preferences.getString("mime:$key", null) ?: document.mimeType).uri,
            preferences.getString("display:$key", null) ?: document.fileName ?: document.title,
            preferences.getString("mime:$key", null) ?: document.mimeType)
    }

    suspend fun refreshAvailable(course: Course, document: CourseDocumentFile, repository: CourseDocumentsRepository): Boolean {
        if (saved(course, document) != null) return refreshSaved(course, document, repository)
        val local = available(course, document) ?: return false
        suspend fun digest(uri: Uri) = withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { input ->
                val hash = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(32 * 1024)
                while (true) { currentCoroutineContext().ensureActive(); val n = input.read(buffer); if (n < 0) break; hash.update(buffer, 0, n) }
                hash.digest().toList()
            }
        }
        val before = digest(local.uri)
        val fresh = cached(course, document, repository, refresh = true)
        return before != digest(fresh.uri)
    }

    suspend fun refreshSaved(course: Course, document: CourseDocumentFile, repository: CourseDocumentsRepository): Boolean {
        val old = saved(course, document) ?: return false
        val before = withContext(Dispatchers.IO) { resolver.openInputStream(old)!!.use(::contentHash) }
        val source = cached(course, document, repository, refresh = true)
        val after = withContext(Dispatchers.IO) { source.file.inputStream().use(::contentHash) }
        save(course, document, source)
        return before != after
    }

    suspend fun save(course: Course, document: CourseDocumentFile, source: CachedCourseDocument): Uri = mutex.withLock {
        withContext(Dispatchers.IO) {
            check(documents.isConnected) { "Connect Documents/Takt before downloading" }
            val previous = saved(course, document)
            val savedKey = savedKey(course, document)
            val digest = source.file.inputStream().use { input ->
                val hash = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(32 * 1024)
                while (true) { currentCoroutineContext().ensureActive(); val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
                hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            }
            val existingHash = previous?.let { uri ->
                try { resolver.openInputStream(uri)?.use { input ->
                    val hash = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(32 * 1024)
                    while (true) { currentCoroutineContext().ensureActive(); val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
                    hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                } } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (_: Exception) { null }
            }
            if (previous != null && existingHash == digest &&
                documents.sourceDisplayName(previous) == CourseDocumentPaths.fileName(source.fileName)) {
                preferences.edit().putString("saved-hash:$savedKey", digest).apply()
                return@withContext previous
            }
            val uri = documents.saveDownloadedDocument(course.code, source.file, source.fileName, source.mimeType)
            currentCoroutineContext().ensureActive()
            check(preferences.edit().putString("saved:$savedKey", uri.toString())
                .remove("retired:$uri")
                .putString("saved-hash:$savedKey", digest)
                .putString("saved-name:$savedKey", documents.sourceDisplayName(uri))
                .putString("saved-mime:$savedKey", source.mimeType).commit()) { "Could not index saved document" }
            if (previous != null && previous != uri) {
                preferences.edit().putLong("retired:$previous", 0L).apply()
                if (documents.removeDownloadedDocument(previous)) preferences.edit().remove("retired:$previous").apply()
            }
            uri
        }
    }

    private fun contentHash(input: InputStream): String {
        val hash = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(32 * 1024)
        while (true) { val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
        return hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    private companion object { val cleanupScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO) }

    private fun result(file: File, display: String, mime: String) = CachedCourseDocument(
        FileProvider.getUriForFile(context, "${context.packageName}.files", file), display, mime, file)

    private fun key(course: Course, document: CourseDocumentFile): String = MessageDigest.getInstance("SHA-256")
        .digest("${course.code}:${document.folderId}:${document.documentId}".toByteArray())
        .take(12).joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal suspend fun copyCancellable(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(32 * 1024)
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) break
        output.write(buffer, 0, count)
    }
}
