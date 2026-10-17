package com.kpyruy.takt.core.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** One anchored choice/action menu. Place beside its trigger inside a Box. */
@Composable
fun TaktChoiceMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = expanded
    if (!visible.currentState && !visible.targetState) return

    val transition = updateTransition(visible, label = "choice-menu")
    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow) },
        label = "scale",
    ) { if (it) 1f else .94f }
    val alpha by transition.animateFloat(transitionSpec = { tween(110) }, label = "fade") { if (it) 1f else 0f }
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val maxWidth = with(density) { window.width.toDp() }.minus(16.dp).coerceAtLeast(1.dp)
    val maxHeight = with(density) { window.height.toDp() }.minus(48.dp).coerceAtLeast(1.dp)
    val position = remember(density) {
        with(density) { TaktChoiceMenuPositionProvider(8.dp.roundToPx(), 4.dp.roundToPx()) }
    }
    val shape = RoundedCornerShape(22.dp)
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < .3f
    // Explicit neutral tint also handles popups that have no sampled wallpaper pixels.
    val material = if (dark) Color(0xFF25252B) else Color(0xFFF4F4F6)
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = focusable, dismissOnBackPress = true,
            dismissOnClickOutside = true, clippingEnabled = true, usePlatformDefaultWidth = false),
    ) {
        // A neutral translucent wash keeps text legible while retaining the shared frost and rim.
        GlassSurface(
            modifier = modifier.widthIn(min = 196.dp.coerceAtMost(maxWidth), max = 320.dp.coerceAtMost(maxWidth))
                .width(IntrinsicSize.Max).heightIn(max = maxHeight)
                .testTag("takt-choice-menu")
                .graphicsLayer {
                    scaleX = scale; scaleY = scale; this.alpha = alpha
                    transformOrigin = TransformOrigin(.5f, 0f)
                },
            shape = shape,
            tint = material,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                Column(Modifier.background(material.copy(alpha = if (dark) .38f else .46f), shape)
                    .verticalScroll(rememberScrollState()).padding(vertical = 4.dp), content = content)
            }
        }
    }
}

/** Full-width, wrapping 48 dp targets with separators and clipped press treatment. */
@Composable
fun TaktChoiceMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val separator = colors.outlineVariant.copy(alpha = .28f)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed) colors.primary.copy(alpha = .10f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled,
                role = Role.Button, onClick = onClick)
            .drawBehind {
                val inset = 12.dp.toPx()
                drawLine(separator, Offset(inset, size.height), Offset(size.width - inset, size.height), 1.dp.toPx())
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val opacity = if (enabled) 1f else .38f
        CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant.copy(alpha = opacity)) {
            leadingIcon?.invoke()
        }
        Box(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface.copy(alpha = opacity)) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge, text)
            }
        }
        CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant.copy(alpha = opacity)) {
            trailingIcon?.invoke()
        }
    }
}

/** Prefer below/above and logical start/end, then clamp partially scrolled anchors to the window. */
internal class TaktChoiceMenuPositionProvider(private val margin: Int, private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val minX = margin.coerceAtMost((windowSize.width - popupContentSize.width).coerceAtLeast(0) / 2)
        val minY = margin.coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0) / 2)
        val maxX = (windowSize.width - popupContentSize.width - minX).coerceAtLeast(minX)
        val maxY = (windowSize.height - popupContentSize.height - minY).coerceAtLeast(minY)
        val left = anchorBounds.left
        val right = anchorBounds.right - popupContentSize.width
        val horizontal = if (layoutDirection == LayoutDirection.Ltr) listOf(left, right) else listOf(right, left)
        val x = horizontal.firstOrNull { it in minX..maxX } ?: horizontal.first().coerceIn(minX, maxX)
        val below = anchorBounds.bottom + gap
        val above = anchorBounds.top - popupContentSize.height - gap
        val y = listOf(below, above).firstOrNull { it in minY..maxY } ?: below.coerceIn(minY, maxY)
        return IntOffset(x, y)
    }
}
