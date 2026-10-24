package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseDocumentFile
import com.kpyruy.takt.core.model.CourseDocumentListing
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.DownloadedCourseDocument
import java.io.OutputStream
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val defaultDocumentsGeneration = MutableStateFlow(0L).asStateFlow()
// A fresh process starts in a random positive range; instances and account changes are monotonic within it.
private val documentsGenerationSequence = AtomicLong(SecureRandom().nextLong().ushr(2) + 1)

interface CourseDocumentsRepository {
    val accountGeneration: StateFlow<Long> get() = defaultDocumentsGeneration
    val cacheOwnerKey: String? get() = null
    suspend fun cachedDocuments(course: Course, activeCourseCodes: Set<String>): CourseDocumentListing? = null
    suspend fun rememberDownloaded(course: Course, document: CourseDocumentFile, fileName: String) {}
    suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean = false): CourseDocumentListing
    suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument
}

/** Bounded, account-scoped catalog with portable metadata and no attachment bytes. */
internal class CachedCourseDocumentsRepository @JvmOverloads constructor(
    private val load: suspend (Course, Set<String>, Boolean) -> CourseDocumentListing,
    private val stream: suspend (CourseDocumentFile, OutputStream) -> DownloadedCourseDocument,
    private val store: CourseDocumentsStore = InMemoryCourseDocumentsStore(),
    private val ownerKey: () -> String? = { null },
    private val loadKnown: (suspend (Course, Set<String>, Boolean, List<com.kpyruy.takt.core.model.CourseDocumentFolder>) -> CourseDocumentListing)? = null,
    private val stableWrite: suspend (suspend () -> Unit) -> Unit = { it() },
) : CourseDocumentsRepository {
    private val mutex = Mutex()
    private val monitor = Any()
    private val mutableAccountGeneration = MutableStateFlow(documentsGenerationSequence.incrementAndGet())
    override val accountGeneration: StateFlow<Long> = mutableAccountGeneration.asStateFlow()
    private var generation = mutableAccountGeneration.value
    private val listings = linkedMapOf<String, CourseDocumentListing>()
    override val cacheOwnerKey get() = ownerKey()
    private fun key(course: Course) = "${ownerKey()}:${course.id}:${course.code}"

    override suspend fun cachedDocuments(course: Course, activeCourseCodes: Set<String>): CourseDocumentListing? {
        if (course.status != CourseStatus.ENROLLED || course.code !in activeCourseCodes) return null
        val owner = ownerKey()
        val revision = synchronized(monitor) {
            if (owner == null) return listings[key(course)]
            generation
        }
        val cached = store.read(requireNotNull(owner), course.code)
        return synchronized(monitor) {
            if (revision != generation || owner != ownerKey()) null else {
                if (cached == null) listings.remove(key(course)) else listings[key(course)] = cached
                cached
            }
        }
    }

    fun clear() = synchronized(monitor) {
        generation = documentsGenerationSequence.incrementAndGet()
        listings.clear()
        mutableAccountGeneration.value = generation
    }

    override suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean): CourseDocumentListing = mutex.withLock {
        if (course.status != CourseStatus.ENROLLED || course.code !in activeCourseCodes)
            return@withLock CourseDocumentListing(emptyList(), false)
        val key = key(course)
        val owner = ownerKey()
        val previous = cachedDocuments(course, activeCourseCodes)
        val revision = synchronized(monitor) {
            if (!refresh) listings[key]?.let { return@withLock it }
            generation
        }
        val loaded = loadKnown?.invoke(course, activeCourseCodes.toSet(), refresh, previous?.roots.orEmpty())
            ?: load(course, activeCourseCodes.toSet(), refresh)
        val savedNames = previous?.files.orEmpty().associateBy { it.folderId to it.documentId }
        val result = loaded.copy(files = loaded.files.map { file -> file.copy(downloadedFileName =
            savedNames[file.folderId to file.documentId]?.downloadedFileName ?: file.downloadedFileName) })
        if (owner != null) stableWrite {
            synchronized(monitor) { check(revision == generation && owner == ownerKey()) { "UIS account changed while loading documents" } }
            val current = store.read(owner, course.code)
            check(current == previous) { "Document catalog changed while refreshing" }
            store.write(owner, course.code, result)
        }
        synchronized(monitor) {
            check(revision == generation) { "UIS account changed while loading documents" }
            listings.remove(key); listings[key] = result
            while (listings.size > 32) listings.remove(listings.keys.first())
        }
        result
    }

    override suspend fun rememberDownloaded(course: Course, document: CourseDocumentFile, fileName: String): Unit = mutex.withLock {
        val owner = ownerKey() ?: return@withLock
        val revision = synchronized(monitor) { generation }
        var updated: CourseDocumentListing? = null
        stableWrite {
            synchronized(monitor) { check(revision == generation && owner == ownerKey()) }
            // Restore may have completed while this operation waited for the coordinator.
            // Merge only into the catalog read under the same stable-data lease as the write.
            val current = store.read(owner, course.code)
            if (current != null) {
                val next = current.copy(files = current.files.map {
                    if (it.folderId == document.folderId && it.documentId == document.documentId)
                        it.copy(downloadedFileName = fileName) else it
                })
                store.write(owner, course.code, next)
                updated = next
            }
        }
        synchronized(monitor) {
            check(revision == generation)
            updated?.let { listings[key(course)] = it }
        }
    }

    override suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument {
        val revision = synchronized(monitor) { generation }
        // Only documents in this account's explicitly loaded course listing can be downloaded.
        check(synchronized(monitor) { listings.values.any { listing -> listing.files.any {
            it.folderId == document.folderId && it.documentId == document.documentId
        } } }) { "Load this course's documents before downloading" }
        val result = stream(document, output)
        synchronized(monitor) { check(revision == generation) { "UIS account changed during download" } }
        return result
    }
}
