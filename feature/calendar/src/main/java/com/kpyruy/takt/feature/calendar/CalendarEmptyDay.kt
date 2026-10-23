package com.kpyruy.takt.feature.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.motion.TaktMotion
import java.time.LocalDate

/** One short entrance per selected day. Compose animation observes the platform motion scale. */
@Composable
fun CalendarEmptyDay(date: LocalDate) {
    val entrance = remember(date) { Animatable(0f) }
    LaunchedEffect(entrance) { entrance.animateTo(1f, TaktMotion.emphasized()) }
    SectionCard {
        Row(Modifier.fillMaxWidth().testTag("calendar-empty-day"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.WbSunny, contentDescription = null,
                modifier = Modifier.size(28.dp).graphicsLayer {
                    alpha = entrance.value
                    rotationZ = (1f - entrance.value) * -12f
                    scaleX = .85f + .15f * entrance.value
                    scaleY = scaleX
                }, tint = MaterialTheme.colorScheme.primary)
            Text(t(UiKey.TEXT_THERE_ARE_NO_CLASSES_ON_THIS_DAY),
                modifier = Modifier.weight(1f).graphicsLayer {
                    alpha = entrance.value
                    translationY = (1f - entrance.value) * 6.dp.toPx()
                }, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
