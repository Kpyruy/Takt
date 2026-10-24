package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.model.UiKey
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class CourseDocumentsView(
    val listing: CourseDocumentListing? = null,
    val localFiles: List<LocalCourseDocument> = emptyList(),
    val savedUris: Map<String, android.net.Uri> = emptyMap(),
)

private suspend fun storedDocumentsView(course: Course, activeCodes: Set<String>, repository: CourseDocumentsRepository,
    downloads: CourseDocumentDownloads, fallback: CourseDocumentListing? = null): CourseDocumentsView {
    val listing = repository.cachedDocuments(course, activeCodes) ?: fallback
    val files = downloads.localFiles(course)
    val saved = listing?.files.orEmpty().mapNotNull { doc ->
        downloads.saved(course, doc)?.let { "${doc.folderId}:${doc.documentId}" to it }
    }.toMap()
    return CourseDocumentsView(listing, files, saved)
}

@Composable
fun CourseDocumentsPanel(
    course: Course,
    activeCourseCodes: Set<String>,
    repository: CourseDocumentsRepository?,
    downloads: CourseDocumentDownloads,
    connected: Boolean,
    authenticate: (String, (DeviceAuthenticationResult) -> Unit) -> Unit,
    onSettings: () -> Unit,
    onFolderSettings: () -> Unit = onSettings,
    downloadsReady: Boolean = downloads.canSave,
) {
    if (repository == null) return
    val onlineEligible = course.status == CourseStatus.ENROLLED && course.code in activeCourseCodes
    var prepared by remember(course.id, downloads, repository) { mutableStateOf<CourseDocumentsView?>(null) }
    var open by remember(course.id) { mutableStateOf(false) }
    LaunchedEffect(course, activeCourseCodes, downloadsReady, downloads, open) {
        if (!open) try {
            prepared = storedDocumentsView(course, activeCourseCodes, repository, downloads, prepared?.listing)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Opening the sheet can retry an unavailable local catalog. */ }
    }
    if (!onlineEligible && prepared?.localFiles.isNullOrEmpty()) return
    SectionCard {
        Row(Modifier.fillMaxWidth().testTag("course-documents").clickable { open = true }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t(UiKey.TEXT_COURSE_DOCUMENTS), style = MaterialTheme.typography.titleSmall)
                Text(t(UiKey.TEXT_MATERIALS_FROM_THE_UIS_DOCUMENT_SERVER), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
    if (open) CourseDocumentsSheet(course, activeCourseCodes, repository, downloads, connected && onlineEligible, authenticate,
        onSettings, onFolderSettings, downloadsReady, prepared ?: CourseDocumentsView(), { prepared = it }, { open = false })
}

@Composable
private fun CourseDocumentsSheet(course: Course, activeCourseCodes: Set<String>, repository: CourseDocumentsRepository,
    downloads: CourseDocumentDownloads, connected: Boolean,
    authenticate: (String, (DeviceAuthenticationResult) -> Unit) -> Unit,
    onSettings: () -> Unit, onFolderSettings: () -> Unit, downloadsReady: Boolean,
    initialView: CourseDocumentsView, onPrepared: (CourseDocumentsView) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var listing by remember { mutableStateOf(initialView.listing) }
    var failure by remember { mutableStateOf<Throwable?>(null) }
    var loading by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var working by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(initialView.savedUris.keys) }
    var savedUris by remember { mutableStateOf(initialView.savedUris) }
    var localFiles by remember { mutableStateOf(initialView.localFiles) }
    val snackbar = remember { SnackbarHostState() }
    var changes by remember { mutableStateOf(CourseDocumentChanges()) }
    var showChanges by remember { mutableStateOf(false) }
    fun fileKey(file: CourseDocumentFile) = "${file.folderId}:${file.documentId}"
    fun linkedLocal(local: LocalCourseDocument): Boolean = local.uri in savedUris.values ||
        listing?.files.orEmpty().any { doc -> listOfNotNull(doc.fileName, doc.downloadedFileName).any { name ->
            CourseDocumentLocalNames.matches(local.fileName, name)
        } }
    suspend fun openFile(uri: android.net.Uri, mime: String) = withContext(Dispatchers.Main.immediate) {
        val result = NoteAttachmentOpener.launch(context, NoteAttachmentOpener.fileIntent(uri, mime))
        if (result != NoteAttachmentOpenResult.Opened) Toast.makeText(context,
            t(UiKey.TEXT_COULD_NOT_OPEN_THE_FILE_INSTALL_AN_APP_FOR_THIS_FORMAT), Toast.LENGTH_LONG).show()
    }
    suspend fun showSaved() = withContext(Dispatchers.Main.immediate) {
        Toast.makeText(context, t(UiKey.TEXT_SAVED_IN_TAKT) + "/${course.code}", Toast.LENGTH_LONG).show()
    }
    var folderRequired by remember { mutableStateOf(false) }
    fun handle(document: CourseDocumentFile, save: Boolean) {
        scope.launch {
            var networkWork = false
            failure = null
            folderRequired = !downloads.canSave
            try {
                val local = downloads.available(course, document)
                if (!save && local != null) { openFile(local.uri, local.mimeType); return@launch }
                if (save && !connected && local != null) {
                    openFile(local.uri, local.mimeType)
                    return@launch
                }
                if (!connected) throw TeachingAuthenticationRequired()
                if (!downloads.canSave) { folderRequired = true; return@launch }
                if (working != null) return@launch
                folderRequired = false; working = fileKey(document); networkWork = true
                val cached = downloads.cached(course, document, repository, refresh = save)
                if (save) {
                    val uri = downloads.save(course, document, cached)
                    downloads.available(course, document)?.let { local ->
                        repository.rememberDownloaded(course, document.copy(fileName = cached.fileName), local.fileName)
                        listing = listing?.copy(files = listing!!.files.map {
                            if (fileKey(it) == fileKey(document)) it.copy(fileName = cached.fileName, downloadedFileName = local.fileName) else it
                        })
                    }
                    savedUris = savedUris + (fileKey(document) to uri)
                    saved = saved + fileKey(document)
                    localFiles = downloads.localFiles(course)
                    onPrepared(CourseDocumentsView(listing, localFiles, savedUris))
                    showSaved()
                } else openFile(cached.uri, cached.mimeType)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (problem: Exception) { android.util.Log.w("CourseDocuments", "Attachment action failed", problem); failure = problem }
            finally { if (networkWork) working = null }
        }
    }
    LaunchedEffect(downloadsReady) { if (downloadsReady) folderRequired = false }
    LaunchedEffect(course.id, activeCourseCodes, reload, connected, downloadsReady) {
        loading = true
        try {
            val stored = storedDocumentsView(course, activeCourseCodes, repository, downloads, listing)
            val localUrisAtStart = stored.localFiles.map { it.uri }.toSet()
            val availableAtStart = mutableSetOf<String>()
            // Publish one coherent local view: raw inventory must not precede its saved UIS links.
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                listing = stored.listing
                savedUris = stored.savedUris
                saved = stored.savedUris.keys
                localFiles = stored.localFiles
            }
            onPrepared(stored)
            stored.listing?.let { cached ->
                availableAtStart += stored.savedUris.keys
                cached.files.filterNot { fileKey(it) in stored.savedUris }.forEach { doc ->
                    if (downloads.available(course, doc) != null) availableAtStart += fileKey(doc)
                }
            }
            if (!connected) return@LaunchedEffect
            failure = null
            var baseline = listing
            var found = repository.documents(course, activeCourseCodes, refresh = true)
            val foundUris = found.files.mapNotNull { doc -> downloads.saved(course, doc)?.let { fileKey(doc) to it } }.toMap()
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                listing = found
                savedUris = foundUris
                saved = foundUris.keys
            }
            // Resolve real attachment names only when an unindexed local file could belong
            // to this listing. The cached list and local opening remain usable throughout.
            for (document in found.files) {
                val unlinked = localFiles.filterNot(::linkedLocal)
                if (unlinked.isEmpty()) break
                if (unlinked.none {
                        it.mimeType == document.mimeType || it.mimeType == "application/octet-stream" || document.mimeType == "application/octet-stream"
                    }) continue
                try {
                    val resolved = repository.resolveDocument(course, document)
                    // Learning a real attachment name is association metadata, not a teacher edit.
                    baseline = baseline?.let { before -> before.copy(files = before.files.map { old ->
                        if (fileKey(old) == fileKey(resolved) && old.fileName == CourseDocumentLocalNames.guessedName(old))
                            old.copy(fileName = resolved.fileName) else old
                    }) }
                    found = found.copy(files = found.files.map { if (fileKey(it) == fileKey(document)) resolved else it })
                    listing = found
                    downloads.saved(course, resolved)?.let { uri ->
                        savedUris = savedUris + (fileKey(resolved) to uri)
                        saved = savedUris.keys
                        downloads.available(course, resolved)?.let { repository.rememberDownloaded(course, resolved, it.fileName) }
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Keep unrelated/local-only files accessible if UIS metadata is unavailable. */ }
            }
            val updatedBytes = mutableSetOf<String>()
            for (document in found.files.filter { doc ->
                val local = downloads.available(course, doc)
                local != null && (fileKey(doc) in availableAtStart || local.uri in localUrisAtStart)
            }) {
                try {
                    if (downloads.refreshAvailable(course, document, repository)) updatedBytes += fileKey(document)
                    if (downloads.saved(course, document) != null)
                        downloads.available(course, document)?.let {
                            val originalName = downloads.cached(course, document, repository).fileName
                            repository.rememberDownloaded(course, document.copy(fileName = originalName), it.fileName)
                        }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (problem: Exception) { android.util.Log.w("CourseDocuments", "Background file refresh failed", problem) }
            }
            savedUris = found.files.mapNotNull { doc -> downloads.saved(course, doc)?.let { fileKey(doc) to it } }.toMap()
            saved = savedUris.keys
            localFiles = downloads.localFiles(course)
            val detected = baseline?.let { CourseDocumentChanges.between(it, found, updatedBytes) }
                ?: CourseDocumentChanges(updated = found.files.filter { fileKey(it) in updatedBytes })
            if (!detected.isEmpty) scope.launch {
                val lines = buildList {
                    if (detected.added.isNotEmpty()) add(t(uiMessage(UiKey.DOCUMENTS_ADDED, detected.added.take(3).joinToString(", ") { it.title })))
                    if (detected.updated.isNotEmpty()) add(t(uiMessage(UiKey.DOCUMENTS_UPDATED, detected.updated.take(3).joinToString(", ") { it.title })))
                    if (detected.removed.isNotEmpty()) add(t(uiMessage(UiKey.DOCUMENTS_REMOVED, detected.removed.take(3).joinToString(", ") { it.title })))
                }
                if (snackbar.showSnackbar(lines.joinToString("\n"), t(UiKey.DOCUMENTS_CHANGE_DETAILS), duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                    changes = detected; showChanges = true
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (problem: Exception) {
            android.util.Log.w("CourseDocuments", "Document listing failed", problem)
            if (listing == null) failure = problem
        } finally {
            loading = false
            onPrepared(CourseDocumentsView(listing, localFiles, savedUris))
        }
    }
    TaktFullSheet(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().testTag("course-documents-sheet").verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t(UiKey.TEXT_COURSE_DOCUMENTS), style = MaterialTheme.typography.titleLarge)
                        val fileCount = (listing?.files?.size ?: 0) + localFiles.count { !linkedLocal(it) }
                        Text(course.code + if (listing != null || fileCount > 0) " · ${t(UiKey.TEXT_FILES)}: $fileCount" else "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    TaktIconButton(Icons.Outlined.Refresh, t(UiKey.ACTION_UPDATE), { reload++ }, enabled = !loading && working == null)
                    TaktIconButton(Icons.Outlined.Close, t(UiKey.ACTION_CLOSE), onDismiss)
                }
                if (!connected && listing == null && localFiles.isEmpty()) {
                    Text(t(UiKey.TEXT_CONNECT_UIS_TO_SEE_COURSE_DOCUMENTS))
                    TextButton(onClick = onSettings, modifier = Modifier.testTag("documents-connect")) { Text(t(UiKey.TEXT_OPEN_SETTINGS)) }
                }
                if (!downloads.canSave || folderRequired) {
                    Text(t(UiKey.TEXT_CONNECT_DOCUMENTS_TAKT_IN_SETTINGS_TO_OPEN_AND_SAVE_FILES),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("documents-folder-required"))
                    TextButton(onClick = onFolderSettings, modifier = Modifier.testTag("documents-folder-settings")) { Text(t(UiKey.TEXT_CONNECT_DOCUMENTS)) }
                }
                AnimatedVisibility(loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("documents-loading"))
                }
                failure?.let { problem ->
                    Text(t(if (working == null && listing != null) UiKey.TEXT_COULD_NOT_OPEN_OR_SAVE_THE_DOCUMENT_TRY_AGAIN
                        else UiKey.TEXT_COULD_NOT_LOAD_DOCUMENTS_CHECK_YOUR_CONNECTION), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = {
                        when (problem) {
                            is TeachingAuthenticationRequired -> authenticate(t(UiKey.TEXT_UIS_DOCUMENTS)) { if (it == DeviceAuthenticationResult.SUCCESS) reload++ }
                            is TeachingSessionUnavailable -> onSettings()
                            else -> reload++
                        }
                    }, modifier = Modifier.testTag("documents-retry")) {
                        Text(t(when (problem) {
                            is TeachingAuthenticationRequired -> UiKey.TEXT_CONFIRM_ACCESS
                            is TeachingSessionUnavailable -> UiKey.TEXT_OPEN_SETTINGS
                            else -> UiKey.TEXT_TRY_AGAIN
                        }))
                    }
                }
                val unlinkedLocal = localFiles.filterNot(::linkedLocal)
                if (unlinkedLocal.isNotEmpty()) {
                    unlinkedLocal.forEach { local ->
                        SectionCard(Modifier.animateContentSize()) {
                            Row(Modifier.fillMaxWidth().clickable { scope.launch { openFile(local.uri, local.mimeType) } }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.DownloadDone, null, tint = MaterialTheme.colorScheme.primary)
                                Text(local.fileName, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
                listing?.let { result ->
                    if (result.files.isEmpty() && !loading) Text(t(if (result.foldersFound)
                        UiKey.TEXT_THERE_ARE_NO_FILES_IN_THIS_COURSE_FOLDER_YET else UiKey.TEXT_THIS_COURSE_FOLDER_WAS_NOT_FOUND_IN_UIS),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    result.files.forEach { document ->
                        val busy = working == fileKey(document)
                        SectionCard(Modifier.animateContentSize()) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(if (document.mimeType == "application/pdf") Icons.Outlined.PictureAsPdf else Icons.Outlined.InsertDriveFile,
                                    null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).testTag("course-document-${document.documentId}")
                                    .clickable(enabled = working == null || fileKey(document) in saved) { handle(document, false) }.padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(document.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                    val type = (document.fileName ?: document.title).substringAfterLast('.', "").uppercase()
                                        .takeIf { it.length in 1..8 } ?: if (document.mimeType == "application/pdf") "PDF" else t(UiKey.TEXT_FILE)
                                    Text(listOfNotNull(type, document.sizeLabel).joinToString(" · "),
                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    document.folderPath?.takeIf { it.isNotBlank() && result.files.map { file -> file.folderId }.distinct().size > 1 }?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                else TaktIconButton(if (fileKey(document) in saved) Icons.Outlined.DownloadDone else Icons.Outlined.Download,
                                    t(if (fileKey(document) in saved) UiKey.TEXT_SAVED else UiKey.TEXT_DOWNLOAD), {
                                        handle(document, true)
                                    }, enabled = working == null && (connected || fileKey(document) in saved), modifier = Modifier.testTag("document-download-${document.documentId}"))
                            }
                        }
                    }
                    if (result.truncated) Text(t(UiKey.TEXT_SOME_MATERIALS_ARE_SHOWN_VIEW_THE_REST_IN_UIS),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    result.folderUrl?.let { url ->
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) { Text(t(UiKey.TEXT_COURSE_FOLDER_IN_UIS)) }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(12.dp).testTag("documents-updates"))
        }
    }
    if (showChanges) AlertDialog(onDismissRequest = { showChanges = false },
        title = { Text(t(UiKey.DOCUMENTS_CHANGED_TITLE)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            changes.added.forEach { Text(t(uiMessage(UiKey.DOCUMENTS_ADDED, it.title))) }
            changes.updated.forEach { Text(t(uiMessage(UiKey.DOCUMENTS_UPDATED, it.title))) }
            changes.removed.forEach { Text(t(uiMessage(UiKey.DOCUMENTS_REMOVED, it.title))) }
        } }, confirmButton = { TextButton(onClick = { showChanges = false }) { Text(t(UiKey.ACTION_CLOSE)) } })
}
