package com.kpyruy.takt.core.model

/** Memory-only UIS metadata. Downloading an attachment is always a separate user action. */
data class CourseDocumentFile(
    val folderId: String,
    val documentId: String,
    val title: String,
    val mimeType: String,
    val fileName: String? = null,
    val sizeLabel: String? = null,
    val folderPath: String? = null,
) {
    init {
        require(folderId.matches(Regex("[1-9][0-9]*")))
        require(documentId.matches(Regex("[1-9][0-9]*")))
    }
    val downloadUrl: String get() = "https://is.stuba.sk/auth/dok_server/slozka.pl?id=$folderId;download=$documentId"
}

data class CourseDocumentListing(
    val files: List<CourseDocumentFile>,
    val foldersFound: Boolean,
    val folderUrl: String? = null,
    val truncated: Boolean = false,
)

data class DownloadedCourseDocument(val fileName: String, val mimeType: String, val byteCount: Long)
