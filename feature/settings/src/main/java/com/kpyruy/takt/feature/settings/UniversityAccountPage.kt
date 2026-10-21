package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktTextField as OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import com.kpyruy.takt.core.data.uis.UisResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.model.CourseNameLanguage
import com.kpyruy.takt.core.model.DeviceAuthenticationResult
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.i18n.t

typealias DeviceAuthenticationRequest = (String, (DeviceAuthenticationResult) -> Unit) -> Unit

@Composable
fun UniversityAccountPage(
    repository: UniversityAccountRepository,
    authenticate: DeviceAuthenticationRequest,
    onBack: () -> Unit,
) {
    val preview by repository.syncPreview.collectAsStateWithLifecycle()
    val initialImportComplete by repository.initialImportComplete.collectAsStateWithLifecycle()
    val importProgress by repository.importProgress.collectAsStateWithLifecycle()
    if (preview != null && initialImportComplete && !importProgress.running) {
        UisPreviewPage(repository, onBack)
        return
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = t(UiKey.TEXT_UNIVERSITY_SYSTEM), navigation = {
            TaktIconButton(Icons.AutoMirrored.Filled.ArrowBack, t(UiKey.ACTION_BACK), onBack)
        })
        SectionCard {
            Text(t(UiKey.TEXT_UIS), style = MaterialTheme.typography.titleLarge)
            Text(t(UiKey.TEXT_CONNECT_TO_STU_UIS),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard {
            UniversityAccountForm(repository, authenticate, showManagement = true)
        }
    }
}

@Composable
fun UniversityAccountForm(
    repository: UniversityAccountRepository,
    authenticate: DeviceAuthenticationRequest,
    showManagement: Boolean = false,
) {
    val hasAccount by repository.hasAccount.collectAsStateWithLifecycle()
    val sessionState by repository.session.state.collectAsStateWithLifecycle()
    val importProgress by repository.importProgress.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var authenticating by remember { mutableStateOf(false) }
    var firstCourseLanguage by remember { mutableStateOf(CourseNameLanguage.ENGLISH) }
    val busy = authenticating || importProgress.running || sessionState == UisResult.CONNECTING

    LaunchedEffect(repository) {
        if (repository.hasAccount.value) {
            authenticating = true
            try {
                val result = awaitAuthentication(authenticate, t(UiKey.TEXT_SIGN_IN_TO_UIS))
                if (result == DeviceAuthenticationResult.SUCCESS) {
                    runCatching { repository.readAfterAuthentication() }
                        .onSuccess { credentials ->
                            login = credentials?.login.orEmpty()
                            password = credentials?.password.orEmpty()
                        }
                        .onFailure { message = uiMessage(UiKey.TEXT_COULD_NOT_READ_UIS_CREDENTIALS_CHECK_YOUR_PHONE_SECURITY) }
                } else message = authError(result)
            } finally { authenticating = false }
        }
    }
    OutlinedTextField(
        value = login,
        onValueChange = { login = it; message = null; repository.session.disconnect() },
        label = { Text(t(UiKey.TEXT_UIS_LOGIN)) }, singleLine = true, enabled = !busy,
        modifier = Modifier.fillMaxWidth().testTag("uis-login"),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it; message = null; repository.session.disconnect() },
        label = { Text(t(UiKey.TEXT_UIS_PASSWORD)) }, singleLine = true, enabled = !busy,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !busy,
                modifier = Modifier.testTag("uis-password-visibility")) {
                Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = t(if (passwordVisible) UiKey.TEXT_HIDE_PASSWORD else UiKey.TEXT_SHOW_PASSWORD))
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth().testTag("uis-password"),
    )
    if (!hasAccount) {
        Text(t(UiKey.TEXT_WHICH_LANGUAGE_SHOULD_UIS_COURSE_NAMES_USE),
            style = MaterialTheme.typography.titleSmall)
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = firstCourseLanguage == CourseNameLanguage.ENGLISH,
                onClick = { firstCourseLanguage = CourseNameLanguage.ENGLISH }, label = { Text(t(UiKey.TEXT_ENGLISH)) })
            FilterChip(selected = firstCourseLanguage == CourseNameLanguage.SLOVAK,
                onClick = { firstCourseLanguage = CourseNameLanguage.SLOVAK }, label = { Text(t(UiKey.TEXT_SLOVEN_INA)) })
        }
    }
    UisConnectionControls(repository, canLogin = login.isNotBlank() && password.isNotEmpty(),
        authenticating = authenticating, onLogin = {
            scope.launch {
                authenticating = true
                message = null
                passwordVisible = false
                try {
                    val result = awaitAuthentication(authenticate, t(UiKey.TEXT_SIGN_IN_TO_UIS))
                    if (result == DeviceAuthenticationResult.SUCCESS) {
                        // One local account. Submit exactly the current fields, never an older stored pair.
                        val credentials = com.kpyruy.takt.core.data.UniversityCredentials(login.trim(), password)
                        if (!hasAccount) repository.setFirstCourseNameChoice(firstCourseLanguage)
                        val saved = runCatching { repository.save(credentials.login, credentials.password) }
                        if (saved.isSuccess) repository.signIn(credentials)
                        else message = uiMessage(UiKey.TEXT_COULD_NOT_SAVE_UIS_CREDENTIALS_CHECK_THE_LOGIN_PASSWORD_AND_PH_D1BCD74B)
                    } else message = authError(result)
                } finally { authenticating = false }
            }
        })
    if (showManagement && hasAccount) {
        TextButton(onClick = {
            scope.launch {
                authenticating = true
                passwordVisible = false
                try {
                    val result = awaitAuthentication(authenticate, t(UiKey.TEXT_REMOVE_UIS_CREDENTIALS))
                    if (result == DeviceAuthenticationResult.SUCCESS) {
                        runCatching { repository.remove() }
                            .onSuccess { login = ""; password = ""; message = null }
                            .onFailure { message = uiMessage(UiKey.TEXT_COULD_NOT_REMOVE_UIS_CREDENTIALS) }
                    } else message = authError(result)
                } finally { authenticating = false }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(t(UiKey.TEXT_REMOVE_UIS_ACCOUNT)) }
    }
    message?.let { Text(t(it), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error) }
}

private suspend fun awaitAuthentication(
    authenticate: DeviceAuthenticationRequest,
    title: String,
): DeviceAuthenticationResult = suspendCancellableCoroutine { continuation ->
    authenticate(title) { result -> if (continuation.isActive) continuation.resume(result) }
}

private fun authError(result: DeviceAuthenticationResult): String = when (result) {
    DeviceAuthenticationResult.DEVICE_LOCK_REQUIRED -> uiMessage(UiKey.TEXT_FIRST_ENABLE_A_PIN_OR_PASSWORD_IN_YOUR_PHONE_SETTINGS)
    DeviceAuthenticationResult.CANCELLED -> uiMessage(UiKey.TEXT_VERIFICATION_CANCELLED)
    DeviceAuthenticationResult.ERROR -> uiMessage(UiKey.TEXT_COULD_NOT_VERIFY_YOUR_IDENTITY)
    DeviceAuthenticationResult.SUCCESS -> ""
}
