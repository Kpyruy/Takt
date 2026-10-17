package com.kpyruy.takt.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.i18n.formatDurationMinutes
import com.kpyruy.takt.core.ui.theme.TaktTheme

/** The preview borrows the shared backdrop; it never draws a second full wallpaper. */
@Composable
fun AppearanceLivePreview(settings: AppSettings, compact: Boolean = false, condensed: Boolean = false) {
    TaktTheme(settings) {
        val colors = MaterialTheme.colorScheme
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        TaktGlassBackground(Modifier.fillMaxWidth().height(
            (if (condensed) 96.dp else if (compact) 224.dp else 288.dp) * fontScale).clip(MaterialTheme.shapes.large)
            .testTag("palette-live-preview"), drawWallpaper = false) {
            if (condensed) {
                // No scaled or clipped text: compact roles leave space for editor + keyboard.
                Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(Modifier.size(32.dp).testTag("palette-preview-secondary"),
                            shape = MaterialTheme.shapes.small, color = colors.secondaryContainer) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.MenuBook, null, Modifier.size(20.dp), tint = colors.secondary)
                            }
                        }
                        Text(t("Твоя перша пара"), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Surface(Modifier.size(32.dp).testTag("palette-preview-grade"),
                            shape = MaterialTheme.shapes.small, color = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer) {
                            Box(contentAlignment = Alignment.Center) { Text("A", fontWeight = FontWeight.Bold) }
                        }
                    }
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(Modifier.weight(1f).fillMaxHeight().testTag("palette-preview-tertiary"),
                            shape = MaterialTheme.shapes.small, color = colors.tertiaryContainer,
                            contentColor = colors.onTertiaryContainer) {
                            Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = colors.tertiary)
                                Text(t("Підготувати конспект"), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        // Passive button sample: the real Apply/Save target stays in the footer.
                        Surface(Modifier.size(32.dp).testTag("palette-preview-primary"),
                            shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary) {
                            Box(contentAlignment = Alignment.Center) { Text("+", style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }
            } else SectionCard(Modifier.fillMaxSize().padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassSurface(Modifier.size(44.dp).testTag("palette-preview-secondary"), tint = colors.secondaryContainer) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.MenuBook, null, Modifier.size(25.dp), tint = colors.secondary)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(t("ПРИКЛАД"), style = MaterialTheme.typography.labelSmall, color = colors.secondary)
                        Text(t("Твоя перша пара"), style = MaterialTheme.typography.titleSmall)
                        Text("09:00–10:30", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Surface(Modifier.size(40.dp).testTag("palette-preview-grade"), shape = MaterialTheme.shapes.small,
                        color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("A", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Surface(Modifier.fillMaxWidth().testTag("palette-preview-tertiary"),
                    shape = MaterialTheme.shapes.medium, color = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer, border = BorderStroke(1.dp, colors.tertiary.copy(alpha = .35f))) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(22.dp), tint = colors.tertiary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(t("Підготувати конспект"), style = MaterialTheme.typography.labelLarge)
                            Text(t("завтра") + " · " + formatDurationMinutes(30), style = MaterialTheme.typography.bodySmall)
                        }
                        Box(Modifier.size(9.dp).background(colors.tertiary, CircleShape))
                    }
                }
                TaktPrimaryButton({}, Modifier.fillMaxWidth().testTag("palette-preview-primary")) {
                    Text(t("Додати задачу"))
                }
                if (!compact) Text(t("Основний — кнопки й оцінки · Другий — предмети · Третій — задачі та позначки"),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
