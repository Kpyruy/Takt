package com.kpyruy.takt.core.data

import androidx.room.withTransaction
import com.kpyruy.takt.core.database.CourseDocumentsEntity
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface CourseDocumentsStore {
    suspend fun read(ownerKey: String, courseCode: String): CourseDocumentListing?
    suspend fun write(ownerKey: String, courseCode: String, listing: CourseDocumentListing)
}
class InMemoryCourseDocumentsStore : CourseDocumentsStore {
    private val entries = mutableMapOf<Pair<String, String>, CourseDocumentListing>()
    override suspend fun read(ownerKey: String, courseCode: String) = synchronized(entries) { entries[ownerKey to courseCode] }
    override suspend fun write(ownerKey: String, courseCode: String, listing: CourseDocumentListing) {
        synchronized(entries) { entries[ownerKey to courseCode] = listing }
    }
}
class RoomCourseDocumentsStore(private val database: TaktDatabase) : CourseDocumentsStore {
    override suspend fun read(ownerKey: String, courseCode: String) = database.courseDocumentsDao().read()?.let {
        DocumentCatalogCodec.decode(it.payloadJson).firstOrNull { entry -> entry.ownerKey == ownerKey && entry.courseCode == courseCode }?.listing?.toModel()
    }
    override suspend fun write(ownerKey: String, courseCode: String, listing: CourseDocumentListing) {
        database.withTransaction {
            val old = database.courseDocumentsDao().read()?.let { DocumentCatalogCodec.decode(it.payloadJson) }.orEmpty()
            val next = BackupDocumentCourse(ownerKey, courseCode, listing.toBackupListing())
            if (old.firstOrNull { it.ownerKey == ownerKey && it.courseCode == courseCode } == next) return@withTransaction
            val updated = old.filterNot { it.ownerKey == ownerKey && it.courseCode == courseCode } + next
            val bounded = updated.groupBy { it.ownerKey }.values.flatMap { it.takeLast(32) }.takeLast(128)
            database.courseDocumentsDao().put(CourseDocumentsEntity(payloadJson = DocumentCatalogCodec.encode(bounded)))
        }
    }
}
@Serializable
data class BackupDocumentCourse(val ownerKey: String, val courseCode: String, val listing: BackupDocumentListing)
@Serializable
data class BackupDocumentFolder(val id: String, val label: String, val path: String)
@Serializable
data class BackupDocumentFile(val folderId: String, val documentId: String, val title: String, val mimeType: String,
    val fileName: String? = null, val sizeLabel: String? = null, val folderPath: String? = null, val downloadedFileName: String? = null, val revision: String? = null,
    val downloadedRevision: String? = null)
@Serializable
data class BackupDocumentListing(val files: List<BackupDocumentFile> = emptyList(), val foldersFound: Boolean = false,
    val folderUrl: String? = null, val truncated: Boolean = false, val roots: List<BackupDocumentFolder> = emptyList()) {
    fun toModel() = CourseDocumentListing(files.map { CourseDocumentFile(it.folderId,it.documentId,it.title,it.mimeType,
        it.fileName,it.sizeLabel,it.folderPath,it.downloadedFileName,it.revision,it.downloadedRevision) },foldersFound,folderUrl,truncated,
        roots.map { CourseDocumentFolder(it.id,it.label,it.path) })
}
internal fun CourseDocumentListing.toBackupListing() = BackupDocumentListing(files.map {
    BackupDocumentFile(it.folderId,it.documentId,it.title,it.mimeType,it.fileName,it.sizeLabel,it.folderPath,it.downloadedFileName,it.revision,it.downloadedRevision)
},foldersFound,folderUrl,truncated,roots.map { BackupDocumentFolder(it.id,it.label,it.path) })
internal object DocumentCatalogCodec {
    private const val MAX_BYTES = 8_000_000
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(value: List<BackupDocumentCourse>): String {
        validate(value)
        return json.encodeToString(value).also { require(it.toByteArray().size <= MAX_BYTES) }
    }
    fun decode(raw: String): List<BackupDocumentCourse> {
        require(raw.toByteArray().size <= MAX_BYTES)
        return json.decodeFromString<List<BackupDocumentCourse>>(raw).also(::validate)
    }
    fun validate(entries: List<BackupDocumentCourse>) {
        require(entries.size <= 128 && entries.distinctBy { it.ownerKey to it.courseCode }.size == entries.size)
        require(entries.groupBy { it.ownerKey }.all { it.value.size <= 32 })
        fun id(value: String) { require(value.matches(Regex("[1-9][0-9]{0,19}"))) }
        entries.forEach { entry ->
            require(entry.ownerKey.matches(Regex("[0-9a-f]{64}")) && entry.courseCode.isNotBlank() && entry.courseCode.length <= 128)
            val listing = entry.listing
            require(listing.files.size <= 512 && listing.roots.size <= 16)
            require(listing.files.distinctBy { it.folderId to it.documentId }.size == listing.files.size)
            require(listing.roots.distinctBy { it.id }.size == listing.roots.size)
            listing.folderUrl?.let { url ->
                val link = com.kpyruy.takt.core.data.uis.UisDocumentsParser.link(url)
                require(link != null && link.documentId == null && link.parameters.keys.all { it in setOf("ds","id","lang","on") })
            }
            listing.roots.forEach { id(it.id); require(it.label.length <= 2000 && it.path.length <= 4000) }
            listing.files.forEach { file ->
                id(file.folderId); id(file.documentId)
                require(file.title.isNotBlank() && file.title.length <= 2000 && file.mimeType.length in 1..256)
                require(file.folderPath == null || file.folderPath.length <= 4000)
                require(file.sizeLabel == null || file.sizeLabel.length <= 128)
                require(file.revision == null || file.revision.length <= 512)
                require(file.downloadedRevision == null || file.downloadedRevision.length <= 512)
                listOfNotNull(file.fileName,file.downloadedFileName).forEach { require(it.length <= 512 && it == java.io.File(it).name && !it.contains('\\') && !it.any(Char::isISOControl)) }
            }
        }
    }
}
