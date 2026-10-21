package com.kpyruy.takt.core.ui.components

import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps a failed editor open. The caller closes its draft only after its write succeeds. */
class SaveAction(private val scope: CoroutineScope) {
    private val writes = Mutex()
    private var pending = 0
    var saving by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    fun dismissError() { failed = false }
    fun launch(action: suspend () -> Unit) {
        if (saving) return
        queue(action)
    }
    /** Immediate settings/toggles are serialized, so a rapid different change is never dropped. */
    fun queue(action: suspend () -> Unit) {
        pending++
        saving = true
        failed = false
        scope.launch {
            try { writes.withLock { action() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true }
            finally { pending--; saving = pending > 0 }
        }
    }
}

@Composable
fun rememberSaveAction(): SaveAction {
    val scope = rememberCoroutineScope()
    return remember(scope) { SaveAction(scope) }
}

@Composable
fun SaveErrorDialog(action: SaveAction) {
    if (action.failed) AlertDialog(onDismissRequest = action::dismissError,
        modifier = Modifier.testTag("save-error-dialog"),
        title = { Text(t("Could not save changes")) },
        text = { Text(t("Your changes are still in the form. Check available storage and try saving again.")) },
        confirmButton = { TextButton(onClick = action::dismissError, modifier = Modifier.testTag("save-error-dismiss")) {
            Text(t("Return to changes"))
        } })
}
