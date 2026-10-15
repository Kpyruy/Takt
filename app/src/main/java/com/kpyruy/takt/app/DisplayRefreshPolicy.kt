package com.kpyruy.takt.app

import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

internal data class ResponsiveDisplayMode(val id: Int, val width: Int, val height: Int, val hz: Float)

/** Keep the current resolution and prefer a real advertised rate; never assume 60/120/144. */
internal fun responsiveDisplayMode(current: ResponsiveDisplayMode, supported: List<ResponsiveDisplayMode>): ResponsiveDisplayMode =
    supported.filter { it.width == current.width && it.height == current.height && it.hz.isFinite() && it.hz > 0f }
        .maxWithOrNull(compareBy<ResponsiveDisplayMode> { it.hz }.thenBy { it.id == current.id }) ?: current

internal fun Window.preferResponsiveDisplay(renderView: View) {
    val panel = renderView.display ?: return
    fun android.view.Display.Mode.candidate() = ResponsiveDisplayMode(modeId, physicalWidth, physicalHeight, refreshRate)
    val best = responsiveDisplayMode(panel.mode.candidate(), panel.supportedModes.map { it.candidate() })
    val params = attributes
    if (params.preferredDisplayModeId != best.id || params.preferredRefreshRate != best.hz) {
        params.preferredDisplayModeId = best.id
        params.preferredRefreshRate = best.hz
        attributes = params
    }
    // Compose1.8 predates preferredFrameRate. On Android15 vote from the actual drawing
    // AndroidComposeView: setting only DecorView does not propagate to its child Views.
    if (Build.VERSION.SDK_INT >= 35 && renderView.requestedFrameRate != best.hz) {
        renderView.requestedFrameRate = best.hz
    }
}

/** Reapply after attachment/focus/display changes without a per-frame timer or idle redraws. */
@Composable
internal fun PreferResponsiveDisplay(window: Window) {
    val renderView = LocalView.current
    DisposableEffect(window, renderView) {
        val manager = renderView.context.getSystemService(DisplayManager::class.java)
        val previous = if (Build.VERSION.SDK_INT >= 35) renderView.requestedFrameRate else 0f
        val refresh = Runnable { if (renderView.isAttachedToWindow) window.preferResponsiveDisplay(renderView) }
        val observer = renderView.viewTreeObserver
        val focus = ViewTreeObserver.OnWindowFocusChangeListener { focused -> if (focused) refresh.run() }
        val attach = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { refresh.run() }
            override fun onViewDetachedFromWindow(view: View) { view.removeCallbacks(refresh) }
        }
        val display = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) = Unit
            override fun onDisplayRemoved(id: Int) = Unit
            override fun onDisplayChanged(id: Int) { if (renderView.display?.displayId == id) refresh.run() }
        }
        renderView.addOnAttachStateChangeListener(attach)
        observer.addOnWindowFocusChangeListener(focus)
        manager.registerDisplayListener(display, Handler(Looper.getMainLooper()))
        renderView.post(refresh)
        onDispose {
            renderView.removeCallbacks(refresh)
            renderView.removeOnAttachStateChangeListener(attach)
            if (observer.isAlive) observer.removeOnWindowFocusChangeListener(focus)
            manager.unregisterDisplayListener(display)
            if (Build.VERSION.SDK_INT >= 35) renderView.requestedFrameRate = previous
        }
    }
}
