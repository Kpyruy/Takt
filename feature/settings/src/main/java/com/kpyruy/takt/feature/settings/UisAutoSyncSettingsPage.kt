package com.kpyruy.takt.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.ui.graphics.vector.ImageVector
import com.kpyruy.takt.core.ui.components.GlassSurface
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
import com.kpyruy.takt.core.ui.components.TaktIconButton
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
            TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад"), onBack)
        })
        FrequencyChoice(t("Прогрес"), Icons.Outlined.BarChart, settings.progress,
            UisRefreshFrequency.entries.toList()) { onChange(settings.copy(progress = it)) }
        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSyncIcon(Icons.Outlined.AutoMode)
                Text(t("Автоматично застосовувати прогрес"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = settings.applyProgressAutomatically,
                    onCheckedChange = { onChange(settings.copy(applyProgressAutomatically = it)) })
            }
        }
        FrequencyChoice(t("Предмети"), Icons.Outlined.MenuBook, settings.subjects,
            UisRefreshFrequency.entries.toList()) { onChange(settings.copy(subjects = it)) }
        FrequencyChoice(t("Періоди навчання"), Icons.Outlined.CalendarMonth, settings.periods,
            UisRefreshFrequency.entries.toList()) { onChange(settings.copy(periods = it)) }
        FrequencyChoice(t("Розклад"), Icons.Outlined.Schedule, settings.timetable,
            listOf(UisRefreshFrequency.MANUAL, UisRefreshFrequency.TEACHING_START)) {
            onChange(settings.copy(timetable = it))
        }
    }
}

@Composable
private fun FrequencyChoice(title: String, icon: ImageVector, selected: UisRefreshFrequency,
    options: List<UisRefreshFrequency>, onSelected: (UisRefreshFrequency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    SectionCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSyncIcon(icon)
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        }
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(t(selected.label()), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp))
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

@Composable
private fun SettingsSyncIcon(icon: ImageVector) {
    GlassSurface(Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

internal fun UisRefreshFrequency.label(): String = when (this) {
    UisRefreshFrequency.MANUAL -> "Вручну"
    UisRefreshFrequency.EVERY_ENTRY -> "Кожен вхід"
    UisRefreshFrequency.DAILY -> "Щодня"
    UisRefreshFrequency.WEEKLY -> "Щотижня"
    UisRefreshFrequency.MONTHLY -> "Щомісяця"
    UisRefreshFrequency.TEACHING_START -> "На початку навчання"
}
