package com.kpyruy.takt.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.AppReleaseError
import com.kpyruy.takt.core.data.AppReleaseRepository
import com.kpyruy.takt.core.data.GitHubReleaseClient
import com.kpyruy.takt.core.model.AppRelease
import com.kpyruy.takt.core.model.AppVersion
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@Composable
internal fun AppUpdatesPage(repository: AppReleaseRepository, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val state by repository.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var openError by remember { mutableStateOf(false) }
    var manuallyChecked by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader(title = t("Оновлення додатка"), navigation = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад")) }
        })
        SectionCard {
            Text(t("Встановлена версія"), style = MaterialTheme.typography.titleMedium)
            Text("Takt " + state.installedVersion.ifBlank { t("Невідомо") })
            Text(t("Останній реліз на GitHub"), style = MaterialTheme.typography.titleMedium)
            Text(state.latest?.tag ?: t("Ще не перевірено"))
            if (state.availableUpdate != null) {
                Text(t("Доступне оновлення"), color = MaterialTheme.colorScheme.primary)
            } else if (state.latest != null && AppVersion.parse(state.installedVersion) != null) {
                Text(t("Встановлено актуальну версію"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.availableUpdate?.let { release ->
                Button(onClick = { openError = !openRelease(context, release) }, modifier = Modifier.fillMaxWidth()) {
                    Text(t(if (release.apkUrl != null) "Відкрити завантаження APK" else "Відкрити реліз на GitHub"))
                }
                Text(t("Завантаження й встановлення підтверджуєш самостійно."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (openError) Text(t("Не вдалося відкрити посилання"), color = MaterialTheme.colorScheme.error)
        }
        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Автоматично перевіряти оновлення"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = state.automaticChecks, onCheckedChange = repository::setAutomaticChecks)
            }
            Text(t("Раз на добу під час відкриття Takt. Перевіряється лише публічний реліз на GitHub."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SectionCard {
            Text(t("Остання успішна перевірка"), style = MaterialTheme.typography.titleMedium)
            Text(state.lastSuccessfulCheck?.let {
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(TaktI18n.locale).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))
            } ?: t("Ще не перевірено"))
            OutlinedButton(onClick = {
                openError = false
                manuallyChecked = true
                scope.launch { repository.check(force = true) }
            }, enabled = !state.checking, modifier = Modifier.fillMaxWidth()) {
                Text(t(if (state.checking) "Перевіряємо…" else "Перевірити зараз"))
            }
            if (manuallyChecked && !state.checking) state.error?.let {
                Text(t(it.message()), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AppUpdatePrompt(release: AppRelease, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var openError by remember(release.tag) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("Доступне оновлення Takt")) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t("Нова версія") + ": " + release.tag)
            Text(t("Завантаження й встановлення підтверджуєш самостійно."))
            if (openError) Text(t("Не вдалося відкрити посилання"), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(onClick = {
            if (openRelease(context, release)) onDismiss() else openError = true
        }) { Text(t(if (release.apkUrl != null) "Відкрити завантаження APK" else "Відкрити реліз на GitHub")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Пізніше")) } },
    )
}

private fun openRelease(context: Context, release: AppRelease): Boolean {
    val url = release.downloadUrl
    if (!GitHubReleaseClient.isTrustedReleaseUrl(url, apk = release.apkUrl != null, tag = release.tag)) return false
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    } catch (_: ActivityNotFoundException) { false }
    catch (_: SecurityException) { false }
}

private fun AppReleaseError.message(): String = when (this) {
    AppReleaseError.NETWORK -> "Не вдалося перевірити оновлення. Перевір інтернет і спробуй ще раз."
    AppReleaseError.RATE_LIMITED -> "GitHub тимчасово обмежив перевірки. Спробуй пізніше."
    AppReleaseError.INVALID_RESPONSE -> "GitHub повернув нерозпізнаний реліз. Спробуй пізніше."
    AppReleaseError.UNAVAILABLE -> "Реліз на GitHub зараз недоступний. Спробуй пізніше."
}
