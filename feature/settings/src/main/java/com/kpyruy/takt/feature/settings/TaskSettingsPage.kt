package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.ui.i18n.t

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.TaskAlt
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
            title = t("Задачі"),
            navigation = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад")) } },
        )
        SettingsSectionTitle(t("Показувати задачі за"))
        val periods = listOf(
            HomeWorkPeriod.SEVEN_DAYS to t("7 днів"),
            HomeWorkPeriod.FOURTEEN_DAYS to t("14 днів"),
            HomeWorkPeriod.ALL to t("Усі дати"),
            HomeWorkPeriod.CUSTOM to t("Свій період"),
        )
        periods.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (period, label) ->
                    val selected = filter.period == period
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onChange(filter.copy(period = period)) },
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(Modifier.heightIn(min = 72.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            if (selected) Icon(Icons.Outlined.Check, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        if (filter.period == HomeWorkPeriod.CUSTOM) {
            TaktDatePickerField(t("Від"), filter.fromDate, { onChange(filter.copy(fromDate = it)) }, Modifier.fillMaxWidth())
            TaktDatePickerField(t("До"), filter.toDate, { onChange(filter.copy(toDate = it)) }, Modifier.fillMaxWidth())
        }
        SettingsSectionTitle(t("Що включати"))
        TaskToggleCard(t("Без дати"), Icons.Outlined.EventNote, filter.includeUndated) {
            onChange(filter.copy(includeUndated = it))
        }
        TaskToggleCard(t("Виконані"), Icons.Outlined.TaskAlt, filter.includeCompleted) {
            onChange(filter.copy(includeCompleted = it))
        }
        if (filter != HomeWorkFilter()) {
            OutlinedButton(onClick = { onChange(HomeWorkFilter()) }, modifier = Modifier.fillMaxWidth()) {
                Text(t("Скинути фільтри задач"))
            }
        }
    }
}

@Composable
private fun TaskToggleCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.heightIn(min = 68.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(9.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
