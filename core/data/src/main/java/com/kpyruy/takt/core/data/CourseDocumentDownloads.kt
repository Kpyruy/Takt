package com.kpyruy.takt.core.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.kpyruy.takt.core.model.CourseDocumentFile
import com.kpyruy.takt.core.model.Course
import kotlinx.coroutines.Dispatchers
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
class CourseDocumentDownloads(context: Context, private val documents: TaktDocumentStore, private val accountScope: Long = 0L) {
    private val context = context.applicationContext
    private val resolver get() = context.contentResolver
    private val preferences = this.context.getSharedPreferences("takt_course_downloads", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val cache get() = File(context.cacheDir, "uis_documents").apply { check(isDirectory || mkdirs()) }

    val canSave: Boolean get() = documents.isConnected

    suspend fun cached(course: Course, document: CourseDocumentFile, repository: CourseDocumentsRepository,
        refresh: Boolean = false): CachedCourseDocument = mutex.withLock { withContext(Dispatchers.IO) {
        val key = "$accountScope-${key(course, document)}"
        val name = preferences.getString("cache-name:$key", null)
        val old = name?.takeIf { it == File(it).name }?.let { File(cache, it) }?.takeIf { it.isFile }
        if (!refresh && old != null) return@withContext result(old,
            preferences.getString("display:$key", document.fileName ?: document.title)!!,
            preferences.getString("mime:$key", document.mimeType) ?: "application/octet-stream")
        val temporary = File.createTempFile("partial-", ".tmp", cache)
        try {
            val downloaded = temporary.outputStream().use { repository.download(document, it) }
            currentCoroutineContext().ensureActive()
            val display = CourseDocumentPaths.fileName(downloaded.fileName)
            val target = File(cache, "$key-$display")
            check(temporary.renameTo(target)) { "Could not publish document preview" }
            old?.takeIf { it != target }?.delete()
            val mime = downloaded.mimeType ?: document.mimeType ?: "application/octet-stream"
            preferences.edit().putString("cache-name:$key", target.name).putString("display:$key", display)
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
                    preferences.edit().remove(entry).remove("display:$oldKey").remove("mime:$oldKey").apply()
                }
            }
            result(target, display, mime)
        } finally { temporary.delete() }
    } }

    suspend fun saved(course: Course, document: CourseDocumentFile): Uri? = withContext(Dispatchers.IO) {
        val uri = preferences.getString("saved:${key(course, document)}", null)?.let(Uri::parse) ?: return@withContext null
        try { resolver.openFileDescriptor(uri, "r")?.use { uri } } catch (_: Exception) { null }
    }

    suspend fun save(course: Course, document: CourseDocumentFile, source: CachedCourseDocument): Uri = mutex.withLock {
        withContext(Dispatchers.IO) {
            check(documents.isConnected) { "Connect Documents/Takt before downloading" }
            val digest = source.file.inputStream().use { input ->
                val hash = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(32 * 1024)
                while (true) { currentCoroutineContext().ensureActive(); val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
                hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            }
            if (preferences.getString("saved-hash:${key(course, document)}", null) == digest)
                saved(course, document)?.let { return@withContext it }
            val uri = documents.saveDownloadedDocument(course.code, source.file, source.fileName, source.mimeType)
            preferences.edit().putString("saved:${key(course, document)}", uri.toString())
                .putString("saved-hash:${key(course, document)}", digest).apply()
            uri
        }
    }

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
