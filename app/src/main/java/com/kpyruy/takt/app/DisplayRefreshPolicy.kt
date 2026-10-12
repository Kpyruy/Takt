package com.kpyruy.takt.app

import android.view.Window

/** Follow the device's fastest mode at its current resolution; never invent a frame rate. */
internal fun Window.preferResponsiveDisplay() {
    val panel = decorView.display ?: return
    val current = panel.mode
    val best = panel.supportedModes
        .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
        .maxByOrNull { it.refreshRate }
        ?: return
    val params = attributes
    if (params.preferredDisplayModeId != best.modeId || params.preferredRefreshRate != best.refreshRate) {
        params.preferredDisplayModeId = best.modeId
        params.preferredRefreshRate = best.refreshRate
        attributes = params
    }
}
