package com.kpyruy.takt.feature.settings

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = t("Університетська система"), navigation = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Назад"))
            }
        })
        SectionCard {
            Text("UIS", style = MaterialTheme.typography.titleLarge)
            Text(t("Підключення до UIS STU"),
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
    val scope = rememberCoroutineScope()
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var authenticating by remember { mutableStateOf(false) }
    val busy = authenticating || sessionState == UisResult.CONNECTING

    LaunchedEffect(repository) {
        if (repository.hasAccount.value) {
            authenticating = true
            try {
                val result = awaitAuthentication(authenticate, t("Увійти в UIS"))
                if (result == DeviceAuthenticationResult.SUCCESS) {
                    runCatching { repository.readAfterAuthentication() }
                        .onSuccess { credentials ->
                            login = credentials?.login.orEmpty()
                            password = credentials?.password.orEmpty()
                        }
                        .onFailure { message = t("Не вдалося прочитати дані UIS. Перевір захист телефона.") }
                } else message = authError(result)
            } finally { authenticating = false }
        }
    }
    OutlinedTextField(
        value = login,
        onValueChange = { login = it; message = null; repository.session.disconnect() },
        label = { Text(t("Логін UIS")) }, singleLine = true, enabled = !busy,
        modifier = Modifier.fillMaxWidth().testTag("uis-login"),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it; message = null; repository.session.disconnect() },
        label = { Text(t("Пароль UIS")) }, singleLine = true, enabled = !busy,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !busy,
                modifier = Modifier.testTag("uis-password-visibility")) {
                Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = t(if (passwordVisible) "Приховати пароль" else "Показати пароль"))
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth().testTag("uis-password"),
    )
    UisConnectionControls(repository, canLogin = login.isNotBlank() && password.isNotEmpty(),
        authenticating = authenticating, onLogin = {
            scope.launch {
                authenticating = true
                message = null
                passwordVisible = false
                try {
                    val result = awaitAuthentication(authenticate, t("Увійти в UIS"))
                    if (result == DeviceAuthenticationResult.SUCCESS) {
                        // One local account. Submit exactly the current fields, never an older stored pair.
                        val credentials = com.kpyruy.takt.core.data.UniversityCredentials(login.trim(), password)
                        val saved = runCatching { repository.save(credentials.login, credentials.password) }
                        if (saved.isSuccess) repository.signIn(credentials)
                        else message = t("Не вдалося зберегти дані UIS. Перевір логін, пароль і захист телефона.")
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
                    val result = awaitAuthentication(authenticate, t("Видалити дані UIS"))
                    if (result == DeviceAuthenticationResult.SUCCESS) {
                        runCatching { repository.remove() }
                            .onSuccess { login = ""; password = ""; message = null }
                            .onFailure { message = t("Не вдалося видалити дані UIS") }
                    } else message = authError(result)
                } finally { authenticating = false }
            }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(t("Видалити UIS-акаунт")) }
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error) }
}

private suspend fun awaitAuthentication(
    authenticate: DeviceAuthenticationRequest,
    title: String,
): DeviceAuthenticationResult = suspendCancellableCoroutine { continuation ->
    authenticate(title) { result -> if (continuation.isActive) continuation.resume(result) }
}

private fun authError(result: DeviceAuthenticationResult): String = when (result) {
    DeviceAuthenticationResult.DEVICE_LOCK_REQUIRED -> t("Спочатку ввімкни PIN або пароль у налаштуваннях телефона")
    DeviceAuthenticationResult.CANCELLED -> t("Підтвердження скасовано")
    DeviceAuthenticationResult.ERROR -> t("Не вдалося підтвердити особу")
    DeviceAuthenticationResult.SUCCESS -> ""
}
