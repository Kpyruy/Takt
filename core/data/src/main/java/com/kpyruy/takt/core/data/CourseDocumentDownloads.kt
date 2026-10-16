package com.kpyruy.takt.core.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
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
class CourseDocumentDownloads(context: Context, private val documents: TaktDocumentStore) {
    private val context = context.applicationContext
    private val resolver get() = context.contentResolver
    private val preferences = this.context.getSharedPreferences("takt_course_downloads", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val cache get() = File(context.cacheDir, "uis_documents").apply { check(isDirectory || mkdirs()) }

    val canSave: Boolean get() = documents.isConnected || Build.VERSION.SDK_INT >= 29 || selectedTree() != null

    fun selectTree(uri: Uri) {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        require(DocumentFile.fromTreeUri(context, uri)?.canWrite() == true)
        preferences.edit().putString("tree", uri.toString()).apply()
    }

    suspend fun cached(course: Course, document: CourseDocumentFile, repository: CourseDocumentsRepository,
        refresh: Boolean = false): CachedCourseDocument = mutex.withLock { withContext(Dispatchers.IO) {
        val key = key(course, document)
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
            result(target, display, mime)
        } finally { temporary.delete() }
    } }

    suspend fun saved(course: Course, document: CourseDocumentFile): Uri? = withContext(Dispatchers.IO) {
        val uri = preferences.getString("saved:${key(course, document)}", null)?.let(Uri::parse) ?: return@withContext null
        try { resolver.openFileDescriptor(uri, "r")?.use { uri } } catch (_: Exception) { null }
    }

    suspend fun save(course: Course, document: CourseDocumentFile, source: CachedCourseDocument): Uri = mutex.withLock {
        withContext(Dispatchers.IO) {
            saved(course, document)?.let { return@withContext it }
            val uri = when {
                documents.isConnected -> documents.saveDownloadedDocument(course.code, source.file, source.fileName, source.mimeType)
                Build.VERSION.SDK_INT >= 29 -> saveToDownloads(course.code, source)
                else -> saveToTree(course.code, source)
            }
            preferences.edit().putString("saved:${key(course, document)}", uri.toString()).apply()
            uri
        }
    }

    private fun result(file: File, display: String, mime: String) = CachedCourseDocument(
        FileProvider.getUriForFile(context, "${context.packageName}.files", file), display, mime, file)

    @androidx.annotation.RequiresApi(29)
    private suspend fun saveToDownloads(code: String, source: CachedCourseDocument): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, source.fileName)
            put(MediaStore.Downloads.MIME_TYPE, source.mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, CourseDocumentPaths.downloadsPath(code))
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        try {
            source.file.inputStream().use { input -> resolver.openOutputStream(uri, "w").use { output ->
                copyCancellable(input, requireNotNull(output))
            } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            return uri
        } catch (failure: Throwable) { resolver.delete(uri, null, null); throw failure }
    }

    private suspend fun saveToTree(code: String, source: CachedCourseDocument): Uri {
        val selected = requireNotNull(selectedTree()) { "Choose a downloads folder" }
        val root = if (selected.name == "Takt") selected else selected.findFile("Takt") ?: selected.createDirectory("Takt")
        val folder = root?.findFile(CourseDocumentPaths.courseFolder(code)) ?: root?.createDirectory(CourseDocumentPaths.courseFolder(code))
        val target = requireNotNull(folder?.createFile(source.mimeType, source.fileName))
        try {
            source.file.inputStream().use { input -> resolver.openOutputStream(target.uri, "w").use { output ->
                copyCancellable(input, requireNotNull(output))
            } }
            return target.uri
        } catch (failure: Throwable) { target.delete(); throw failure }
    }

    private fun selectedTree(): DocumentFile? = preferences.getString("tree", null)?.let { raw ->
        val uri = Uri.parse(raw)
        if (resolver.persistedUriPermissions.none { it.uri == uri && it.isReadPermission && it.isWritePermission }) null
        else DocumentFile.fromTreeUri(context, uri)?.takeIf { it.canWrite() }
    }
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
