package com.kpyruy.takt.feature.settings

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
    val preview by repository.syncPreview.collectAsStateWithLifecycle()
    val lastCheck by repository.lastCheckOutcome.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    if (state !in listOf(UisResult.DISCONNECTED, UisResult.CONNECTING)) Text(t(when (state) {
        UisResult.DISCONNECTED -> "UIS is not connected"
        UisResult.CONNECTING -> "Connecting to UIS…"
        UisResult.CONNECTED -> "Signed in to UIS"
        UisResult.SECOND_FACTOR -> "Enter your UIS verification code"
        UisResult.INVALID_CREDENTIALS -> "UIS rejected the username or password"
        UisResult.EXPIRED -> "Your UIS session expired. Sign in again."
        UisResult.UNAVAILABLE -> "UIS is unavailable. Local data remains accessible."
        UisResult.UNEXPECTED_RESPONSE -> "Could not recognize the UIS response"
    }))
    failure?.let { Text(uisFailureMessage(it), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    if (importProgress.running) Text(t("Loading UIS data…"))
    if (importProgress.running && state != UisResult.CONNECTING) {
        UisLoadingOverlay(onCancel = { repository.session.disconnect() }, title = "Loading UIS data…")
    }
    importProgress.importedCourses?.let { count ->
        Text(t("Imported ${count} courses"))
    }
    if (importProgress.earnedCredits != null && importProgress.requiredCredits != null) {
        Text(t("UIS credits: ${importProgress.earnedCredits} of ${importProgress.requiredCredits}"))
    }
    importProgress.error?.let { Text(t(it), color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
    if (preview == null && lastCheck == UisAutoSyncOutcome.APPLIED && !importProgress.running) {
        Text(t("UIS check complete. No changes to review."))
    }
    if (importProgress.error != null && state == UisResult.CONNECTED) {
        OutlinedButton(onClick = { scope.launch { repository.retryImport() } }, modifier = Modifier.fillMaxWidth()) {
            Text(t("Retry import"))
        }
    }
    when (state) {
        UisResult.CONNECTING -> {
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(t("Sign in to UIS")) }
            UisLoadingOverlay(onCancel = { repository.session.disconnect() })
        }
        UisResult.SECOND_FACTOR -> {
            OutlinedTextField(value = code, onValueChange = { code = it.take(32) },
                label = { Text(t("Verification code")) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val entered = code
                code = ""
                scope.launch { repository.submitCode(entered) }
            }, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(t("Confirm code")) }
        }
        UisResult.CONNECTED -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scope.launch { repository.checkProgressNow() } },
                enabled = preview == null, modifier = Modifier.fillMaxWidth()) {
                Text(t("Check UIS progress"))
            }
            OutlinedButton(onClick = { scope.launch { repository.checkTimetableNow() } },
                enabled = preview == null, modifier = Modifier.fillMaxWidth()) {
                Text(t("Get timetable from UIS"))
            }
            OutlinedButton(onClick = { scope.launch { repository.checkAndImport() } },
                enabled = preview == null, modifier = Modifier.fillMaxWidth()) {
                Text(t("Check all UIS data"))
            }
        }
        else -> Button(onClick = onLogin, enabled = canLogin && !authenticating,
            modifier = Modifier.fillMaxWidth().testTag("uis-sign-in")) { Text(t("Sign in to UIS")) }
    }
    if (state in listOf(UisResult.CONNECTED, UisResult.SECOND_FACTOR)) {
        TextButton(onClick = { code = ""; repository.session.disconnect() }, modifier = Modifier.fillMaxWidth()) {
            Text(t("Close UIS session"))
        }
    }
    UisSyncReview(repository)
}
