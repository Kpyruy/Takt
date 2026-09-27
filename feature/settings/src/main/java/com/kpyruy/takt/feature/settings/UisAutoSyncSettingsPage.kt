package com.kpyruy.takt.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.UisAutoSyncSettings
import com.kpyruy.takt.core.model.UisRefreshFrequency
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun UisAutoSyncSettingsPage(
    settings: UisAutoSyncSettings,
    onChange: (UisAutoSyncSettings) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader(title = t("Автооновлення UIS"), navigation = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад")) }
        })
        FrequencyChoice(t("Прогрес"), settings.progress,
            UisRefreshFrequency.entries.toList()) { onChange(settings.copy(progress = it)) }
        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t("Автоматично застосовувати прогрес"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = settings.applyProgressAutomatically,
                    onCheckedChange = { onChange(settings.copy(applyProgressAutomatically = it)) })
            }
        }
        FrequencyChoice(t("Предмети"), settings.subjects,
            UisRefreshFrequency.entries.toList()) { onChange(settings.copy(subjects = it)) }
        FrequencyChoice(t("Періоди навчання"), settings.periods,
            UisRefreshFrequency.entries.toList()) { onChange(settings.copy(periods = it)) }
        FrequencyChoice(t("Розклад"), settings.timetable,
            listOf(UisRefreshFrequency.MANUAL, UisRefreshFrequency.TEACHING_START)) {
            onChange(settings.copy(timetable = it))
        }
    }
}

@Composable
private fun FrequencyChoice(title: String, selected: UisRefreshFrequency,
    options: List<UisRefreshFrequency>, onSelected: (UisRefreshFrequency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    SectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(t(selected.label()), Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { value ->
                DropdownMenuItem(text = { Text(t(value.label())) }, onClick = {
                    expanded = false
                    onSelected(value)
                })
            }
            }
        }
    }
}

internal fun UisRefreshFrequency.label(): String = when (this) {
    UisRefreshFrequency.MANUAL -> "Вручну"
    UisRefreshFrequency.DAILY -> "Щодня"
    UisRefreshFrequency.WEEKLY -> "Щотижня"
    UisRefreshFrequency.MONTHLY -> "Щомісяця"
    UisRefreshFrequency.TEACHING_START -> "На початку навчання"
}
