package com.kpyruy.takt.app

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HighRefreshPolicyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun selectsRealRatesWithoutChangingResolutionOrInventing144Hz() {
        val current = ResponsiveDisplayMode(1, 1080, 2400, 60f)
        for (hz in listOf(60f, 90f, 120f, 144f, 240f)) {
            val supported = listOf(current, current.copy(id = 2, hz = hz), current.copy(id = 3, width = 1440, hz = 300f))
            val selected = responsiveDisplayMode(current, supported)
            assertEquals(hz, selected.hz, .001f)
            assertEquals(current.width, selected.width)
            assertEquals(current.height, selected.height)
        }
        assertEquals(current, responsiveDisplayMode(current, emptyList()))
        assertEquals(current, responsiveDisplayMode(current, listOf(current.copy(hz = Float.NaN), current.copy(hz = -1f))))
        assertEquals(current, responsiveDisplayMode(current, listOf(current.copy(id = 2), current)))
    }

    @Test fun actualDrawingViewRequestsItsAdvertisedRateAfterAttachmentAndRecreation() {
        fun drawingView(node: View): View? {
            if (node.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView") return node
            return if (node is ViewGroup) (0 until node.childCount).firstNotNullOfOrNull { drawingView(node.getChildAt(it)) } else null
        }
        fun verify(): View {
            var view: View? = null
            compose.waitUntil(10_000) {
                view = drawingView(compose.activity.window.decorView)
                view?.isAttachedToWindow == true
            }
            val renderer = checkNotNull(view)
            val panel = renderer.display
            val mode = panel.mode
            val expected = panel.supportedModes.filter {
                it.physicalWidth == mode.physicalWidth && it.physicalHeight == mode.physicalHeight
            }.maxOf { it.refreshRate }
            compose.waitUntil(5_000) {
                val params = compose.activity.window.attributes
                params.preferredRefreshRate == expected && (Build.VERSION.SDK_INT < 35 || renderer.requestedFrameRate == expected)
            }
            compose.runOnIdle {
                assertEquals(expected, compose.activity.window.attributes.preferredRefreshRate, .001f)
                if (Build.VERSION.SDK_INT >= 35) assertEquals(expected, renderer.requestedFrameRate, .001f)
            }
            return renderer
        }
        val first = verify()
        compose.activityRule.scenario.recreate()
        val second = verify()
        assertNotSame(first, second)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val params = compose.activity.window.attributes
        instrumentation.targetContext.openFileOutput("high-refresh-policy.txt", 0).use {
            it.write("API=${Build.VERSION.SDK_INT}; mode=${params.preferredDisplayModeId}; windowHz=${params.preferredRefreshRate}; rendererHz=${if (Build.VERSION.SDK_INT >= 35) second.requestedFrameRate else "legacy"}".toByteArray())
        }
    }
}
