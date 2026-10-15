package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen

/** GPU display list of the scrolling content. Chrome is outside this recording to avoid recursion. */
internal class TaktContentBackdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

internal val LocalTaktContentBackdrop = staticCompositionLocalOf<TaktContentBackdrop?> { null }

@Composable
fun TaktContentBackdropHost(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val layer = rememberGraphicsLayer()
    val source = remember(layer) { TaktContentBackdrop(layer) }
    CompositionLocalProvider(LocalTaktContentBackdrop provides source) {
        Box(modifier.fillMaxSize(), content = content)
    }
}

/** Apply once to the content sibling beneath fixed navigation/header, never to the chrome itself. */
@Composable
fun Modifier.captureTaktContentBackdrop(): Modifier {
    val source = LocalTaktContentBackdrop.current ?: return this
    return onGloballyPositioned { source.origin = it.positionOnScreen() }.drawWithContent {
        source.layer.record { this@drawWithContent.drawContent() }
        drawLayer(source.layer)
    }
}
