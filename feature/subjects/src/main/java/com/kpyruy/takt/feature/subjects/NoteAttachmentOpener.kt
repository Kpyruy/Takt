package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.model.UiKey
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.model.NoteAttachment
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface NoteAttachmentOpenResult {
    data object Opened : NoteAttachmentOpenResult
    data object NoApplication : NoteAttachmentOpenResult
    data object Unavailable : NoteAttachmentOpenResult
    data class Failed(val cause: Exception) : NoteAttachmentOpenResult
}

/** Launch directly; package visibility queries cannot establish whether ACTION_VIEW will work. */
object NoteAttachmentOpener {
    fun webLinkIntent(url: String): Intent {
        // Keep model validation (including no credentials), then normalize Android's case-sensitive scheme.
        val safe = NoteAttachment.webLink(url)
        return Intent(Intent.ACTION_VIEW, Uri.parse(safe.uri).normalizeScheme())
    }

    fun fileIntent(uri: Uri, mimeType: String): Intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun launch(context: Context, intent: Intent): NoteAttachmentOpenResult = try {
        val activity = context.activityOrNull()
        val launchIntent = Intent(intent).apply {
            if (activity == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        (activity ?: context).startActivity(launchIntent)
        NoteAttachmentOpenResult.Opened
    } catch (_: ActivityNotFoundException) {
        NoteAttachmentOpenResult.NoApplication
    } catch (error: Exception) {
        NoteAttachmentOpenResult.Failed(error)
    }
}

private fun Context.activityOrNull(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/** The card and reader use the same resolution, URI grant, and error handling. */
@Composable
internal fun rememberNoteAttachmentOpener(documentStore: TaktDocumentStore): (NoteAttachment) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return { attachment ->
        scope.launch {
            val result = try {
                if (attachment.isWebLink) {
                    NoteAttachmentOpener.launch(context, NoteAttachmentOpener.webLinkIntent(attachment.uri))
                } else {
                    val uri = documentStore.resolve(attachment.uri)
                    if (uri == null) NoteAttachmentOpenResult.Unavailable
                    else NoteAttachmentOpener.launch(context, NoteAttachmentOpener.fileIntent(uri, attachment.mimeType))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                NoteAttachmentOpenResult.Failed(error)
            }
            val message = when (result) {
                NoteAttachmentOpenResult.Opened -> null
                NoteAttachmentOpenResult.NoApplication -> if (attachment.isWebLink)
                    t(UiKey.TEXT_NO_APP_CAN_OPEN_THIS_LINK) else t(UiKey.TEXT_THERE_IS_NO_APPLICATION_TO_OPEN_THE_FILE)
                NoteAttachmentOpenResult.Unavailable -> t(UiKey.TEXT_THE_FILE_IS_NOT_AVAILABLE)
                is NoteAttachmentOpenResult.Failed -> {
                    Log.e("NoteAttachmentOpener", "Could not open note attachment", result.cause)
                    if (attachment.isWebLink) t(UiKey.TEXT_COULD_NOT_OPEN_THE_LINK) else t(UiKey.TEXT_FAILED_TO_OPEN_FILE)
                }
            }
            if (message != null) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
