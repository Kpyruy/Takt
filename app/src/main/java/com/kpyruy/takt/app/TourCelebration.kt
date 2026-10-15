package com.kpyruy.takt.app

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import com.kpyruy.takt.core.ui.i18n.t
import kotlin.math.sin

@Composable
internal fun TourCelebration(onFinish: () -> Unit) {
    val progress = remember { Animatable(if (ValueAnimator.areAnimatorsEnabled()) 0f else 1f) }
    LaunchedEffect(Unit) { if (progress.value < 1f) progress.animateTo(1f, tween(2400, easing = LinearEasing)) }
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("tour-completed"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val time = progress.value
            if (time < 1f) repeat(56) { index ->
                val seed = ((index * 37) % 101) / 100f
                val local = ((time - seed * .16f) / .84f).coerceIn(0f, 1f)
                val x = size.width * (((index * 61) % 103) / 102f) + sin(local * 9f + index) * 22.dp.toPx()
                val y = -40.dp.toPx() + local * (size.height + 80.dp.toPx())
                val shard = Size((5 + index % 5).dp.toPx(), (9 + index % 8).dp.toPx())
                val color = colors[index % colors.size].copy(alpha = ((1f - local) * .82f).coerceIn(0f, 1f))
                rotate(index * 23f + local * 240f, Offset(x, y)) {
                    drawRoundRect(color, Offset(x, y), shard, CornerRadius(2.dp.toPx()))
                    drawRoundRect(Color.White.copy(alpha = color.alpha * .7f), Offset(x, y), shard,
                        CornerRadius(2.dp.toPx()), style = Stroke(.8.dp.toPx()))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassSurface(Modifier.size(88.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Check, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(t("Туторіал завершено"), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(t("Приємного користування аплікацією"), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            TaktPrimaryButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().testTag("tour-done")) {
                Text(t("До Takt"))
            }
        }
    }
}
