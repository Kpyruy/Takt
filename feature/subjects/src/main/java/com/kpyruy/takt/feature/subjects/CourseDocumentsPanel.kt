package com.kpyruy.takt.feature.subjects

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@Composable
fun CourseDocumentsPanel(
    course: Course,
    activeCourseCodes: Set<String>,
    repository: CourseDocumentsRepository?,
    downloads: CourseDocumentDownloads,
    connected: Boolean,
    authenticate: (String, (DeviceAuthenticationResult) -> Unit) -> Unit,
    onSettings: () -> Unit,
) {
    if (course.status != CourseStatus.ENROLLED || course.code !in activeCourseCodes || repository == null) return
    var open by remember(course.id) { mutableStateOf(false) }
    SectionCard {
        Row(Modifier.fillMaxWidth().testTag("course-documents").clickable { open = true }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t("Документи предмета"), style = MaterialTheme.typography.titleSmall)
                Text(t("Матеріали з документ-сервера UIS"), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
    if (open) CourseDocumentsSheet(course, activeCourseCodes, repository, downloads, connected, authenticate,
        onSettings, { open = false })
}

@Composable
private fun CourseDocumentsSheet(course: Course, activeCourseCodes: Set<String>, repository: CourseDocumentsRepository,
    downloads: CourseDocumentDownloads, connected: Boolean,
    authenticate: (String, (DeviceAuthenticationResult) -> Unit) -> Unit,
    onSettings: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var listing by remember { mutableStateOf<CourseDocumentListing?>(null) }
    var failure by remember { mutableStateOf<Throwable?>(null) }
    var loading by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var working by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(setOf<String>()) }
    var pendingSave by remember { mutableStateOf<CourseDocumentFile?>(null) }
    fun handle(document: CourseDocumentFile, save: Boolean) {
        if (working != null) return
        scope.launch {
            working = document.documentId; failure = null
            try {
                val cached = downloads.cached(course, document, repository, refresh = reload > 0)
                if (save) {
                    downloads.save(course, document, cached)
                    saved = saved + document.documentId
                    Toast.makeText(context, t("Збережено в папку Takt") + "/${course.code}", Toast.LENGTH_LONG).show()
                } else {
                    val result = NoteAttachmentOpener.launch(context, NoteAttachmentOpener.fileIntent(cached.uri, cached.mimeType))
                    if (result != NoteAttachmentOpenResult.Opened) Toast.makeText(context,
                        t("Не вдалося відкрити файл. Установи застосунок для цього формату."), Toast.LENGTH_LONG).show()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (problem: Exception) { failure = problem }
            finally { working = null }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val pending = pendingSave; pendingSave = null
        if (uri != null && pending != null) {
            try { downloads.selectTree(uri); handle(pending, true) }
            catch (problem: Exception) { failure = problem }
        }
    }
    LaunchedEffect(course.id, activeCourseCodes, reload, connected) {
        if (!connected) return@LaunchedEffect
        loading = true; failure = null
        try {
            val found = repository.documents(course, activeCourseCodes, refresh = reload > 0)
            listing = found
            saved = found.files.filter { downloads.saved(course, it) != null }.map { it.documentId }.toSet()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (problem: Exception) { failure = problem }
        finally { loading = false }
    }
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().testTag("course-documents-sheet").verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("Документи предмета"), style = MaterialTheme.typography.titleLarge)
                    Text(course.code, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                TaktIconButton(Icons.Outlined.Refresh, t("Оновити"), { reload++ }, enabled = !loading && working == null)
                TaktIconButton(Icons.Outlined.Close, t("Закрити"), onDismiss)
            }
            if (!connected) {
                Text(t("Підключи UIS, щоб бачити документи предмета."))
                TextButton(onClick = onSettings, modifier = Modifier.testTag("documents-connect")) { Text(t("Відкрити налаштування")) }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("documents-loading"))
            failure?.let { problem ->
                Text(t(if (working == null && listing != null) "Не вдалося відкрити або зберегти документ. Спробуй ще раз."
                    else "Не вдалося завантажити документи. Перевір з’єднання."), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = {
                    when (problem) {
                        is TeachingAuthenticationRequired -> authenticate(t("Документи UIS")) { if (it == DeviceAuthenticationResult.SUCCESS) reload++ }
                        is TeachingSessionUnavailable -> onSettings()
                        else -> reload++
                    }
                }, modifier = Modifier.testTag("documents-retry")) {
                    Text(t(when (problem) {
                        is TeachingAuthenticationRequired -> "Підтвердити доступ"
                        is TeachingSessionUnavailable -> "Відкрити налаштування"
                        else -> "Спробувати ще раз"
                    }))
                }
            }
            listing?.let { result ->
                if (result.files.isEmpty() && !loading) Text(t(if (result.foldersFound)
                    "У папці предмета поки немає файлів." else "Папку цього предмета не знайдено на UIS."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                result.files.forEach { document ->
                    val busy = working == document.documentId
                    SectionCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(if (document.mimeType == "application/pdf") Icons.Outlined.PictureAsPdf else Icons.Outlined.InsertDriveFile,
                                null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).testTag("course-document-${document.documentId}")
                                .clickable(enabled = working == null) { handle(document, false) }.padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(document.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                val type = (document.fileName ?: document.title).substringAfterLast('.', "").uppercase()
                                    .takeIf { it.length in 1..8 } ?: if (document.mimeType == "application/pdf") "PDF" else t("Файл")
                                Text(listOfNotNull(type, document.sizeLabel).joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                document.folderPath?.takeIf(String::isNotBlank)?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            else TaktIconButton(if (document.documentId in saved) Icons.Outlined.DownloadDone else Icons.Outlined.Download,
                                t(if (document.documentId in saved) "Збережено" else "Завантажити"), {
                                    if (downloads.canSave) handle(document, true)
                                    else { pendingSave = document; folderPicker.launch(null) }
                                }, enabled = working == null, modifier = Modifier.testTag("document-download-${document.documentId}"))
                        }
                    }
                }
                if (result.truncated) Text(t("Показано частину матеріалів. Решту можна переглянути на UIS."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                result.folderUrl?.let { url ->
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) { Text(t("Папка предмета в UIS")) }
                }
            }
            Text(t("Натисни назву, щоб відкрити. Стрілка зберігає файл у папку Takt з кодом предмета."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
