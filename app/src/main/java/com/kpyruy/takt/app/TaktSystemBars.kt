package com.kpyruy.takt.app

import android.graphics.drawable.ColorDrawable
import android.view.Window
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

/** The currently visible theme owns system contrast, including an unsaved setup draft. */
internal fun Window.applyTaktSystemBars(background: Color) {
    val light = background.luminance() > .5f
    WindowCompat.getInsetsController(this, decorView).apply {
        if (isAppearanceLightStatusBars != light) isAppearanceLightStatusBars = light
        if (isAppearanceLightNavigationBars != light) isAppearanceLightNavigationBars = light
    }
    val color = background.toArgb()
    if ((decorView.background as? ColorDrawable)?.color != color) setBackgroundDrawable(ColorDrawable(color))
}

@Composable
internal fun TaktSystemBars(window: Window) {
    val background = MaterialTheme.colorScheme.background
    SideEffect { window.applyTaktSystemBars(background) }
}
