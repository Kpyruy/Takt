package com.kpyruy.takt.feature.settings

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.time.LocalDate
import java.time.temporal.WeekFields

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
            title = "Календар і розклад",
            subtitle = "Вибери, що бачити та як читати свій тиждень.",
            navigation = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") } },
        )
        SettingsSectionTitle("Вигляд тижня")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                WeekLayout.TIMETABLE to "Таймтейбл",
                WeekLayout.COMPACT_LIST to "Список",
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
                            Text("Лекція", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("ПН · 2 пари", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("09:00  Лекція", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        SettingsSectionTitle("Скасовані пари")
        Text("Одна й та сама пара в трьох варіантах", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            CancellationDisplayStyle.STRIKETHROUGH to "Закреслювати",
            CancellationDisplayStyle.MARKED to "Позначати",
            CancellationDisplayStyle.HIDDEN to "Ховати",
        ).forEach { (style, label) ->
            CalendarChoiceCard(
                title = label,
                selected = settings.cancellationStyle == style,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCancellationStyle(style) },
            ) {
                when (style) {
                    CancellationDisplayStyle.STRIKETHROUGH -> Text("09:00  Теорія права",
                        textDecoration = TextDecoration.LineThrough, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CancellationDisplayStyle.MARKED -> Text("09:00  Теорія права  ·  Скасовано",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CancellationDisplayStyle.HIDDEN -> Text("Пара не відображатиметься в розкладі",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (settings.cancellationStyle == CancellationDisplayStyle.HIDDEN) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Показувати приховані", fontWeight = FontWeight.SemiBold)
                        Text("Скасовані пари залишаться в календарі для довідки.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.showHiddenLessons, onCheckedChange = onShowHiddenLessons)
                }
            }
        }
        SettingsSectionTitle("Парність тижня")
        val week = LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear())
        Text("Зараз тиждень №$week за календарем ISO.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            ParityOverride.AUTO to "Автоматично",
            ParityOverride.EVEN to "Завжди парний",
            ParityOverride.ODD to "Завжди непарний",
        ).forEach { (parity, label) ->
            CalendarChoiceCard(
                title = label,
                selected = settings.parityOverride == parity,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onParityOverride(parity) },
            ) {
                Text(when (parity) {
                    ParityOverride.AUTO -> "За номером календарного тижня"
                    ParityOverride.EVEN -> "Для розкладу з парними тижнями"
                    ParityOverride.ODD -> "Для розкладу з непарними тижнями"
                }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
