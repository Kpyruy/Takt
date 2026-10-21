package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.UiKey
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton as Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.kpyruy.takt.core.ui.components.TaktIconButton
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
        ScreenHeader(title = t(UiKey.TEXT_APP_UPDATES), navigation = {
            TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t(UiKey.ACTION_BACK), onBack)
        })
        SectionCard {
            UpdateVersionRow(Icons.Outlined.School, t(UiKey.TEXT_INSTALLED_VERSION), "Takt " + state.installedVersion.ifBlank { t(UiKey.TEXT_UNKNOWN) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
            UpdateVersionRow(Icons.Outlined.CloudDownload, t(UiKey.TEXT_LATEST_GITHUB_RELEASE), state.latest?.tag ?: t(UiKey.TEXT_NOT_CHECKED_YET))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
            if (state.availableUpdate != null) {
                Text(t(UiKey.TEXT_UPDATE_AVAILABLE), color = MaterialTheme.colorScheme.primary)
            } else if (state.latest != null && AppVersion.parse(state.installedVersion) != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                    Text(t(UiKey.TEXT_YOUR_VERSION_IS_UP_TO_DATE), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.availableUpdate?.let { release ->
                Button(onClick = { openError = !openRelease(context, release) }, modifier = Modifier.fillMaxWidth()) {
                    Text(t(if (release.apkUrl != null) UiKey.TEXT_OPEN_APK_DOWNLOAD else UiKey.TEXT_OPEN_RELEASE_ON_GITHUB))
                }
                Text(t(UiKey.TEXT_YOU_CONFIRM_THE_DOWNLOAD_AND_INSTALLATION),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (openError) Text(t(UiKey.TEXT_COULD_NOT_OPEN_THE_LINK), color = MaterialTheme.colorScheme.error)
        }
        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t(UiKey.TEXT_AUTOMATICALLY_CHECK_FOR_UPDATES), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = state.automaticChecks, onCheckedChange = repository::setAutomaticChecks)
            }
        }
        SectionCard {
            UpdateVersionRow(Icons.Outlined.Schedule, t(UiKey.TEXT_LAST_SUCCESSFUL_CHECK), state.lastSuccessfulCheck?.let {
                val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(TaktI18n.locale).format(date) +
                    " · " + DateTimeFormatter.ofPattern("HH:mm", TaktI18n.locale).format(date)
            } ?: t(UiKey.TEXT_NOT_CHECKED_YET))
        }
        Button(onClick = {
            openError = false
            manuallyChecked = true
            scope.launch { repository.check(force = true) }
        }, enabled = !state.checking, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Update, null)
            Text(t(if (state.checking) UiKey.TEXT_CHECKING else UiKey.TEXT_CHECK_NOW), Modifier.padding(start = 8.dp))
        }
        if (manuallyChecked && !state.checking) state.error?.let {
            Text(t(it.message()), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun AppUpdatePrompt(release: AppRelease, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var openError by remember(release.tag) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t(UiKey.TEXT_TAKT_UPDATE_AVAILABLE)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t(UiKey.TEXT_NEW_VERSION) + ": " + release.tag)
            Text(t(UiKey.TEXT_YOU_CONFIRM_THE_DOWNLOAD_AND_INSTALLATION))
            if (openError) Text(t(UiKey.TEXT_COULD_NOT_OPEN_THE_LINK), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(onClick = {
            if (openRelease(context, release)) onDismiss() else openError = true
        }) { Text(t(if (release.apkUrl != null) UiKey.TEXT_OPEN_APK_DOWNLOAD else UiKey.TEXT_OPEN_RELEASE_ON_GITHUB)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t(UiKey.TEXT_LATER)) } },
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
    AppReleaseError.NETWORK -> UiKey.TEXT_COULD_NOT_CHECK_FOR_UPDATES_CHECK_YOUR_CONNECTION_AND_TRY_AGAIN
    AppReleaseError.RATE_LIMITED -> UiKey.TEXT_GITHUB_TEMPORARILY_LIMITED_CHECKS_TRY_AGAIN_LATER
    AppReleaseError.INVALID_RESPONSE -> UiKey.TEXT_GITHUB_RETURNED_AN_UNRECOGNIZED_RELEASE_TRY_AGAIN_LATER
    AppReleaseError.UNAVAILABLE -> UiKey.TEXT_THE_GITHUB_RELEASE_IS_CURRENTLY_UNAVAILABLE_TRY_AGAIN_LATER
}

@Composable
private fun UpdateVersionRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassSurface(Modifier.size(46.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
