package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton as Button
import androidx.compose.material3.OutlinedButton
import com.kpyruy.takt.core.ui.components.TaktTextField as OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.data.UisAutoSyncOutcome
import com.kpyruy.takt.core.data.uis.UisResult
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.launch

@Composable
internal fun UisConnectionControls(
    repository: UniversityAccountRepository,
    canLogin: Boolean,
    authenticating: Boolean,
    onLogin: () -> Unit,
) {
    val failure by repository.session.failure.collectAsStateWithLifecycle()
    val state by repository.session.state.collectAsStateWithLifecycle()
    val importProgress by repository.importProgress.collectAsStateWithLifecycle()
    val initialImportComplete by repository.initialImportComplete.collectAsStateWithLifecycle()
    val preview by repository.syncPreview.collectAsStateWithLifecycle()
    val lastCheck by repository.lastCheckOutcome.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    if (state !in listOf(UisResult.DISCONNECTED, UisResult.CONNECTING)) Text(t(when (state) {
        UisResult.DISCONNECTED -> UiKey.TEXT_UIS_IS_NOT_CONNECTED
        UisResult.CONNECTING -> UiKey.TEXT_CONNECTING_TO_UIS
        UisResult.CONNECTED -> UiKey.TEXT_SIGNED_IN_TO_UIS
        UisResult.SECOND_FACTOR -> UiKey.TEXT_ENTER_YOUR_UIS_VERIFICATION_CODE
        UisResult.INVALID_CREDENTIALS -> UiKey.TEXT_UIS_REJECTED_THE_USERNAME_OR_PASSWORD
        UisResult.EXPIRED -> UiKey.TEXT_YOUR_UIS_SESSION_EXPIRED_SIGN_IN_AGAIN
        UisResult.UNAVAILABLE -> UiKey.TEXT_UIS_IS_UNAVAILABLE_LOCAL_DATA_REMAINS_ACCESSIBLE
        UisResult.UNEXPECTED_RESPONSE -> UiKey.TEXT_COULD_NOT_RECOGNIZE_THE_UIS_RESPONSE
    }))
    failure?.let { Text(uisFailureMessage(it), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    if (importProgress.running) Text(t(UiKey.TEXT_LOADING_UIS_DATA))
    if (importProgress.running && state != UisResult.CONNECTING) {
        UisLoadingOverlay(onCancel = { repository.session.disconnect() }, title = UiKey.TEXT_LOADING_UIS_DATA)
    }
    importProgress.importedCourses?.let { count ->
        Text(t(uiMessage(UiKey.FORMAT_IMPORTED_VALUE_COURSES, count)))
    }
    if (importProgress.earnedCredits != null && importProgress.requiredCredits != null) {
        Text(t(uiMessage(UiKey.FORMAT_UIS_CREDITS_VALUE_OF_VALUE, importProgress.earnedCredits, importProgress.requiredCredits)))
    }
    importProgress.error?.let { Text(t(it), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    if (preview == null && lastCheck == UisAutoSyncOutcome.APPLIED && !importProgress.running) {
        Text(t(UiKey.TEXT_UIS_CHECK_COMPLETE_NO_CHANGES_TO_REVIEW))
    }
    if (importProgress.error != null && state == UisResult.CONNECTED) {
        OutlinedButton(onClick = { scope.launch { repository.retryImport() } }, modifier = Modifier.fillMaxWidth()) {
            Text(t(UiKey.TEXT_RETRY_IMPORT))
        }
    }
    when (state) {
        UisResult.CONNECTING -> {
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(t(UiKey.TEXT_SIGN_IN_TO_UIS)) }
            UisLoadingOverlay(onCancel = { repository.session.disconnect() })
        }
        UisResult.SECOND_FACTOR -> {
            OutlinedTextField(value = code, onValueChange = { code = it.take(32) },
                label = { Text(t(UiKey.TEXT_VERIFICATION_CODE)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val entered = code
                code = ""
                scope.launch { repository.submitCode(entered) }
            }, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(t(UiKey.TEXT_CONFIRM_CODE)) }
        }
        UisResult.CONNECTED -> if (initialImportComplete && !importProgress.running && !authenticating) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { repository.checkProgressNow() } },
                enabled = preview == null, modifier = Modifier.fillMaxWidth()) {
                Text(t(UiKey.TEXT_CHECK_UIS_PROGRESS))
            }
            OutlinedButton(onClick = { scope.launch { repository.checkTimetableNow() } },
                enabled = preview == null, modifier = Modifier.fillMaxWidth()) {
                Text(t(UiKey.TEXT_GET_TIMETABLE_FROM_UIS))
            }
            OutlinedButton(onClick = { scope.launch { repository.checkAndImport() } },
                enabled = preview == null, modifier = Modifier.fillMaxWidth()) {
                Text(t(UiKey.TEXT_CHECK_ALL_UIS_DATA))
            }
        }
        else -> Button(onClick = onLogin, enabled = canLogin && !authenticating,
            modifier = Modifier.fillMaxWidth().testTag("uis-sign-in")) { Text(t(UiKey.TEXT_SIGN_IN_TO_UIS)) }
    }
    if (state in listOf(UisResult.CONNECTED, UisResult.SECOND_FACTOR)) {
        TextButton(onClick = { code = ""; repository.session.disconnect() }, modifier = Modifier.fillMaxWidth()) {
            Text(t(UiKey.TEXT_CLOSE_UIS_SESSION))
        }
    }
    if (initialImportComplete && !importProgress.running) UisSyncReview(repository)
}
