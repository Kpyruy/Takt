package com.kpyruy.takt.feature.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.data.uis.UisResult
import com.kpyruy.takt.core.model.DeviceAuthenticationResult
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.launch

@Composable
internal fun UisConnectionControls(repository: UniversityAccountRepository, authenticate: DeviceAuthenticationRequest) {
    val hasAccount by repository.hasAccount.collectAsStateWithLifecycle()
    val state by repository.session.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var authenticationPending by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    if (!hasAccount) return
    Text(t(when (state) {
        UisResult.DISCONNECTED -> "UIS не підключено"
        UisResult.CONNECTING -> "Підключення до UIS…"
        UisResult.CONNECTED -> "Вхід у UIS виконано"
        UisResult.SECOND_FACTOR -> "Введи код підтвердження UIS"
        UisResult.INVALID_CREDENTIALS -> "UIS відхилив логін або пароль"
        UisResult.EXPIRED -> "Сесія UIS завершилась. Увійди повторно."
        UisResult.UNAVAILABLE -> "UIS недоступний. Локальні дані залишаються доступними."
        UisResult.UNEXPECTED_RESPONSE -> "Не вдалося розпізнати відповідь UIS"
    }))
    localError?.let { Text(t(it)) }
    when (state) {
        UisResult.CONNECTING -> CircularProgressIndicator()
        UisResult.SECOND_FACTOR -> {
            OutlinedTextField(value = code, onValueChange = { code = it.take(32) },
                label = { Text(t("Код підтвердження")) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val entered = code
                code = ""
                scope.launch { repository.session.submitCode(entered) }
            }, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(t("Підтвердити код")) }
        }
        UisResult.CONNECTED -> OutlinedButton(onClick = {
            scope.launch { repository.session.check() }
        }, modifier = Modifier.fillMaxWidth()) { Text(t("Перевірити сесію")) }
        else -> Button(onClick = {
            authenticationPending = true
            localError = null
            authenticate(t("Увійти в UIS")) { result ->
                authenticationPending = false
                if (result == DeviceAuthenticationResult.SUCCESS) {
                    val credentials = runCatching { repository.readAfterAuthentication() }.getOrNull()
                    if (credentials != null) scope.launch { repository.session.login(credentials) }
                    else localError = "Не вдалося прочитати дані UIS. Перевір захист телефона."
                } else localError = "Підтвердження скасовано"
            }
        }, enabled = !authenticationPending, modifier = Modifier.fillMaxWidth()) { Text(t("Увійти в UIS")) }
    }
    if (state in listOf(UisResult.CONNECTED, UisResult.SECOND_FACTOR, UisResult.CONNECTING)) {
        TextButton(onClick = { code = ""; repository.session.disconnect() }, modifier = Modifier.fillMaxWidth()) {
            Text(t("Закрити сесію UIS"))
        }
    }
}
