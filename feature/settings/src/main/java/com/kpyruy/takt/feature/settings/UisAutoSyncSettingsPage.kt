package com.kpyruy.takt.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        Text(t("Перевірка запускається, коли застосунок відкрито й розблоковано. Якщо UIS попросить код, введи його в налаштуваннях акаунта."),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        FrequencyChoice(t("Прогрес"), settings.progress,
            UisRefreshFrequency.entries.toList()) { onChange(settings.copy(progress = it)) }
        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("Автоматично застосовувати прогрес"), style = MaterialTheme.typography.titleMedium)
                    Text(t("Оновлювати статуси й кредити лише там, де локальні дані не змінювались після UIS. Конфлікти залишаться на перегляд."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
        Text(t("Зміни предметів, періодів і розкладу перевіряються автоматично, але застосовуються лише після твого вибору."),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FrequencyChoice(title: String, selected: UisRefreshFrequency,
    options: List<UisRefreshFrequency>, onSelected: (UisRefreshFrequency) -> Unit) {
    SectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { value ->
                FilterChip(selected = value == selected, onClick = { onSelected(value) },
                    label = { Text(t(value.label())) })
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
