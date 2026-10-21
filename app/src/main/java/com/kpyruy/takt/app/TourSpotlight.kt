package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.UiKey
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun BoxScope.TourSpotlight(
    step: GuidedTourStep,
    bounds: Rect?,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    nextEnabled: Boolean = true,
    onReplay: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val height = with(density) { maxHeight.toPx() }
        val width = with(density) { maxWidth.toPx() }
        val inset = with(density) { 6.dp.toPx() }
        val hole = bounds?.let {
            Rect((it.left - inset).coerceIn(0f, width), (it.top - inset).coerceIn(0f, height),
                (it.right + inset).coerceIn(0f, width), (it.bottom + inset).coerceIn(0f, height))
        }?.takeIf { it.width > 0 && it.height > 0 }
        val cardOnTop = hole != null && hole.center.y > height * .58f
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = .50f))
            hole?.let {
                val radius = CornerRadius(18.dp.toPx())
                drawRoundRect(Color.Transparent, it.topLeft, it.size, radius, blendMode = BlendMode.Clear)
                drawRoundRect(Color.White.copy(alpha = .95f), it.topLeft, it.size, radius, style = Stroke(2.dp.toPx()))
                drawRoundRect(accent.copy(alpha = .45f), it.topLeft, it.size, radius, style = Stroke(4.dp.toPx()))
            }
        }
        GlassSurface(Modifier.align(if (cardOnTop) Alignment.TopCenter else Alignment.BottomCenter)
            .fillMaxWidth().padding(12.dp).heightIn(max = maxHeight * .49f)
            .testTag("tour-card-${step.name}")
            .semantics { paneTitle = step.title.text(); isTraversalGroup = true },
            shape = RoundedCornerShape(26.dp), tint = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t(UiKey.TEXT_TOUR_OF_TAKT) + " · ${step.ordinal + 1}/${GuidedTourStep.entries.size}",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(t(UiKey.TEXT_EXAMPLES_YOUR_DATA_STAYS_UNCHANGED), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TaktIconButton(Icons.Outlined.Close, t(UiKey.TEXT_EXIT_TOUR), onExit,
                        modifier = Modifier.testTag("tour-exit"))
                }
                LinearProgressIndicator(progress = { (step.ordinal + 1f) / GuidedTourStep.entries.size },
                    modifier = Modifier.fillMaxWidth().height(3.dp), gapSize = 0.dp, drawStopIndicator = {})
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(step.title.text(), style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite })
                    Text(step.body.text(), style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onBack, enabled = step.ordinal > 0, modifier = Modifier.testTag("tour-back")) {
                        Text(t(UiKey.ACTION_BACK))
                    }
                    if (onReplay != null) TextButton(onClick = onReplay, enabled = nextEnabled,
                        modifier = Modifier.testTag("tour-replay")) { Text(t(UiKey.TEXT_REPLAY)) }
                    TaktPrimaryButton(onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tour-next")) {
                        Text(if (step == GuidedTourStep.entries.last()) t(UiKey.TEXT_FINISH) else t(UiKey.TEXT_NEXT))
                    }
                }
            }
        }
        // A semantic marker allows instrumentation to assert that the measured spotlight is in the viewport.
        if (hole != null) Box(Modifier.offset { androidx.compose.ui.unit.IntOffset(hole.left.toInt(), hole.top.toInt()) }
            .size(with(density) { hole.width.toDp() }, with(density) { hole.height.toDp() }).testTag("tour-spotlight"))
    }
}
