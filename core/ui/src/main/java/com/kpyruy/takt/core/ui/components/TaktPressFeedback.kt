package com.kpyruy.takt.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Rounded controls compress gently instead of painting a rectangular Material ripple. */
@Composable
fun Modifier.taktPressFeedback(source: MutableInteractionSource, scaleDown: Float = .965f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = .78f, stiffness = 650f), label = "glass-press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Shared foundation feedback, including semester headers and selectors with no custom source. */
object TaktPressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = PressNode(interactionSource)
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 7341
}

private class PressNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private val scale = Animatable(1f)
    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collectLatest { event ->
                val target = when (event) {
                    is PressInteraction.Press -> .98f
                    is PressInteraction.Release, is PressInteraction.Cancel -> 1f
                    else -> return@collectLatest
                }
                scale.animateTo(target, spring(dampingRatio = .8f, stiffness = 650f))
            }
        }
    }
    override fun ContentDrawScope.draw() {
        scale(scale.value) { this@draw.drawContent() }
    }
}
