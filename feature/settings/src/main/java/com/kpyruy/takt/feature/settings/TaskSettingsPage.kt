package com.kpyruy.takt.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.HomeWorkFilter
import com.kpyruy.takt.core.model.HomeWorkPeriod
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktDatePickerField

@Composable
internal fun TaskSettingsPage(
    filter: HomeWorkFilter,
    onBack: () -> Unit,
    onChange: (HomeWorkFilter) -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = "Задачі",
            subtitle = "Те, що бачиш на головній, залишиться таким і після перезапуску.",
            navigation = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") } },
        )
        SettingsSectionTitle("Показувати задачі за")
        listOf(
            HomeWorkPeriod.SEVEN_DAYS to "7 днів",
            HomeWorkPeriod.FOURTEEN_DAYS to "14 днів",
            HomeWorkPeriod.ALL to "Усі дати",
            HomeWorkPeriod.CUSTOM to "Свій період",
        ).forEach { (period, label) ->
            val selected = filter.period == period
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onChange(filter.copy(period = period)) },
                shape = MaterialTheme.shapes.medium,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(Modifier.heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (filter.period == HomeWorkPeriod.CUSTOM) {
            TaktDatePickerField("Від", filter.fromDate, { onChange(filter.copy(fromDate = it)) }, Modifier.fillMaxWidth())
            TaktDatePickerField("До", filter.toDate, { onChange(filter.copy(toDate = it)) }, Modifier.fillMaxWidth())
        }
        SettingsSectionTitle("Що включати")
        SectionCard {
            TaskSwitchRow("Без дати", "Задачі, яким ще не задано дедлайн.", filter.includeUndated) {
                onChange(filter.copy(includeUndated = it))
            }
            TaskSwitchRow("Виконані", "Залишати завершені задачі в списку.", filter.includeCompleted) {
                onChange(filter.copy(includeCompleted = it))
            }
        }
        Text("Предмети та типи задач можна швидко відфільтрувати біля списку на головній.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (filter != HomeWorkFilter()) {
            OutlinedButton(onClick = { onChange(HomeWorkFilter()) }, modifier = Modifier.fillMaxWidth()) {
                Text("Скинути фільтри задач")
            }
        }
    }
}

@Composable
private fun TaskSwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
