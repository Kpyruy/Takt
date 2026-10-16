package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseDocumentFile
import com.kpyruy.takt.core.model.CourseDocumentListing
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.DownloadedCourseDocument
import java.io.OutputStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface CourseDocumentsRepository {
    suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean = false): CourseDocumentListing
    suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument
}

/** Bounded, account-scoped memory cache; documents never enter study backups. */
internal class CachedCourseDocumentsRepository(
    private val load: suspend (Course, Set<String>, Boolean) -> CourseDocumentListing,
    private val stream: suspend (CourseDocumentFile, OutputStream) -> DownloadedCourseDocument,
) : CourseDocumentsRepository {
    private val mutex = Mutex()
    private val monitor = Any()
    private var generation = 0L
    private val listings = linkedMapOf<String, CourseDocumentListing>()

    fun clear() = synchronized(monitor) { generation++; listings.clear() }

    override suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean): CourseDocumentListing = mutex.withLock {
        if (course.status != CourseStatus.ENROLLED || course.code !in activeCourseCodes)
            return@withLock CourseDocumentListing(emptyList(), false)
        val key = "${course.id}:${course.code}:${activeCourseCodes.sorted().joinToString(",")}"
        val revision = synchronized(monitor) {
            if (!refresh) listings[key]?.let { return@withLock it }
            generation
        }
        val result = load(course, activeCourseCodes.toSet(), refresh)
        synchronized(monitor) {
            check(revision == generation) { "UIS account changed while loading documents" }
            listings.remove(key); listings[key] = result
            while (listings.size > 32) listings.remove(listings.keys.first())
        }
        result
    }

    override suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument = mutex.withLock {
        val revision = synchronized(monitor) { generation }
        // Only documents in this account's explicitly loaded course listing can be downloaded.
        check(synchronized(monitor) { listings.values.any { listing -> listing.files.any {
            it.folderId == document.folderId && it.documentId == document.documentId
        } } }) { "Load this course's documents before downloading" }
        val result = stream(document, output)
        synchronized(monitor) { check(revision == generation) { "UIS account changed during download" } }
        result
    }
}
