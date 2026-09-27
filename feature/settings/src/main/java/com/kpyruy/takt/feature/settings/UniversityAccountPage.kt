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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    Text(if (hasAccount) t("Дані UIS збережено на цьому телефоні") else t("Додати UIS"),
        style = MaterialTheme.typography.titleMedium)
    if (showManagement && hasAccount) {
        OutlinedButton(onClick = {
            authenticate(t("Показати дані UIS")) { result ->
                if (result == DeviceAuthenticationResult.SUCCESS) {
                    runCatching { repository.readAfterAuthentication() }
                        .onSuccess { credentials ->
                            login = credentials?.login.orEmpty()
                            password = credentials?.password.orEmpty()
                            message = t("Дані підставлено після підтвердження")
                        }
                        .onFailure { message = t("Не вдалося прочитати дані UIS. Перевір захист телефона.") }
                } else message = authError(result)
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(t("Підставити збережені дані")) }
    }
    OutlinedTextField(
        value = login,
        onValueChange = { login = it; message = null },
        label = { Text(t("Логін UIS")) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("uis-login"),
    )
    OutlinedTextField(
        value = password,
        onValueChange = { password = it; message = null },
        label = { Text(t("Пароль UIS")) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth().testTag("uis-password"),
    )
    Button(
        onClick = {
            saving = true
            authenticate(t("Зберегти дані UIS")) { result ->
                if (result == DeviceAuthenticationResult.SUCCESS) {
                    runCatching { repository.save(login, password) }
                        .onSuccess {
                            password = ""
                            message = t("Дані UIS зашифровано і збережено")
                        }
                        .onFailure { message = t("Не вдалося зберегти дані UIS. Перевір логін, пароль і захист телефона.") }
                } else message = authError(result)
                saving = false
            }
        },
        enabled = login.isNotBlank() && password.isNotEmpty() && !saving,
        modifier = Modifier.fillMaxWidth().testTag("uis-save"),
    ) { Text(if (hasAccount) t("Оновити дані") else t("Зберегти дані")) }
    if (showManagement && hasAccount) {
        TextButton(onClick = {
            authenticate(t("Видалити дані UIS")) { result ->
                if (result == DeviceAuthenticationResult.SUCCESS) {
                    runCatching { repository.remove() }
                        .onSuccess {
                            login = ""
                            password = ""
                            message = t("Дані UIS видалено. Takt працює локально.")
                        }
                        .onFailure { message = t("Не вдалося видалити дані UIS") }
                } else message = authError(result)
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(t("Видалити UIS-акаунт")) }
    }
    UisConnectionControls(repository, authenticate)
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun authError(result: DeviceAuthenticationResult): String = when (result) {
    DeviceAuthenticationResult.DEVICE_LOCK_REQUIRED -> t("Спочатку ввімкни PIN або пароль у налаштуваннях телефона")
    DeviceAuthenticationResult.CANCELLED -> t("Підтвердження скасовано")
    DeviceAuthenticationResult.ERROR -> t("Не вдалося підтвердити особу")
    DeviceAuthenticationResult.SUCCESS -> ""
}
