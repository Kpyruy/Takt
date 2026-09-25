package com.kpyruy.takt.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.model.HomeWorkPeriod
import com.kpyruy.takt.core.ui.components.SectionCard

@Composable
internal fun SettingsNavigationTile(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun themeFamilyLabel(value: ThemeFamily) = when (value) {
    ThemeFamily.BLUE -> "Синя"
    ThemeFamily.GREEN -> "Зелена"
    ThemeFamily.PURPLE -> "Фіолетова"
    ThemeFamily.WARM -> "Тепла"
    ThemeFamily.MONOCHROME -> "Монохром"
}

internal fun themeModeLabel(value: AppThemeMode) = when (value) {
    AppThemeMode.SYSTEM -> "як телефон"
    AppThemeMode.LIGHT -> "світла"
    AppThemeMode.DARK -> "темна"
}

internal fun cardAppearanceLabel(value: CardAppearance) = when (value) {
    CardAppearance.ELEVATED -> "підняті картки"
    CardAppearance.TONAL_FILLED -> "заливка"
}

internal fun cancellationStyleLabel(value: CancellationDisplayStyle) = when (value) {
    CancellationDisplayStyle.STRIKETHROUGH -> "закреслені скасування"
    CancellationDisplayStyle.HIDDEN -> "приховані скасування"
    CancellationDisplayStyle.MARKED -> "позначені скасування"
}

internal fun weekLayoutLabel(value: WeekLayout) = when (value) {
    WeekLayout.TIMETABLE -> "таймтейбл"
    WeekLayout.COMPACT_LIST -> "список"
}

internal fun taskPeriodLabel(value: HomeWorkPeriod) = when (value) {
    HomeWorkPeriod.SEVEN_DAYS -> "7 днів"
    HomeWorkPeriod.FOURTEEN_DAYS -> "14 днів"
    HomeWorkPeriod.ALL -> "усі дати"
    HomeWorkPeriod.CUSTOM -> "свій період"
}
