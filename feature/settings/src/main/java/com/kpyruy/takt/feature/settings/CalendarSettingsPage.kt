package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.ui.i18n.t

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem

@Composable
internal fun CalendarSettingsPage(
    settings: AppSettings,
    onBack: () -> Unit,
    onCancellationStyle: (CancellationDisplayStyle) -> Unit,
    onShowHiddenLessons: (Boolean) -> Unit,
    onParityOverride: (ParityOverride) -> Unit,
    onWeekLayout: (WeekLayout) -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = t("Календар і розклад"),
            navigation = { TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад"), onBack) },
        )
        SettingsSectionTitle(t("Вигляд тижня"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                WeekLayout.TIMETABLE to t("Таймтейбл"),
                WeekLayout.COMPACT_LIST to t("Список"),
            ).forEach { (layout, label) ->
                CalendarChoiceCard(
                    title = label,
                    selected = settings.weekLayout == layout,
                    modifier = Modifier.weight(1f),
                    onClick = { onWeekLayout(layout) },
                ) {
                    if (layout == WeekLayout.TIMETABLE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("09:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Box(Modifier.size(3.dp, 25.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Text(t("Лекція"), style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(t("ПН · 2 пари"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(t("09:00  Лекція"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        SettingsSectionTitle(t("Скасовані пари"))
        CalendarPopupChoice(
            selected = settings.cancellationStyle,
            options = listOf(
                CancellationDisplayStyle.STRIKETHROUGH to t("Закреслювати"),
                CancellationDisplayStyle.MARKED to t("Позначати"),
                CancellationDisplayStyle.HIDDEN to t("Ховати"),
            ),
            onSelected = onCancellationStyle,
        ) { style ->
            when (style) {
                CancellationDisplayStyle.STRIKETHROUGH -> Text(t("09:00  Теорія права"),
                    textDecoration = TextDecoration.LineThrough, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                CancellationDisplayStyle.MARKED -> Text(t("09:00  Теорія права  ·  Скасовано"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CancellationDisplayStyle.HIDDEN -> Text(t("Скасовані пари зникнуть із розкладу"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (settings.cancellationStyle == CancellationDisplayStyle.HIDDEN) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(t("Показувати приховані"), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(checked = settings.showHiddenLessons, onCheckedChange = onShowHiddenLessons)
                }
            }
        }
        SettingsSectionTitle(t("Парність тижня"))
        val parityOptions = listOf(
            ParityOverride.AUTO to t("Автоматично"),
            ParityOverride.EVEN to t("Завжди парний"),
            ParityOverride.ODD to t("Завжди непарний"),
        )
        CalendarPopupChoice(settings.parityOverride, parityOptions, onParityOverride)
    }
}

@Composable
private fun <T> CalendarPopupChoice(
    selected: T,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
    preview: @Composable (T) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
            onClick = { expanded = true }) {
            Row(Modifier.heightIn(min = 58.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(options.first { it.first == selected }.second, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    preview(selected)
                }
                Icon(Icons.Outlined.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TaktChoiceMenu(expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                TaktChoiceMenuItem(text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(label)
                        preview(value)
                    }
                }, trailingIcon = {
                    if (value == selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary)
                }, onClick = {
                    expanded = false
                    onSelected(value)
                })
            }
        }
    }
}

@Composable
private fun CalendarChoiceCard(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.heightIn(min = 66.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
            }
            content()
        }
    }
}
