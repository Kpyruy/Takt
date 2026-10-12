package com.kpyruy.takt.app

import android.os.Handler
import android.os.HandlerThread
import android.os.Debug
import android.view.FrameMetrics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class PopulatedGlassPerformanceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun populatedRootsSubjectTabsAndScrollingStayResponsive() {
        val data = (compose.activity.application as TaktApplication).dataContainer
        runBlocking { data.seedLiquidGlassReview(); data.settingsRepository.setLanguage(AppLanguage.ENGLISH) }
        val frames = CopyOnWriteArrayList<Long>()
        val thread = HandlerThread("TaktPopulatedFrames").apply { start() }
        val listener = android.view.Window.OnFrameMetricsAvailableListener { _, metric, _ ->
            frames.add(metric.getMetric(FrameMetrics.TOTAL_DURATION))
        }
        compose.activity.window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        val report = mutableListOf<String>()
        fun measure(name: String, block: () -> Unit) {
            val start = System.nanoTime()
            block(); compose.waitForIdle()
            val ms = (System.nanoTime() - start) / 1_000_000.0
            report += "$name=$ms ms"
            assertTrue("$name stalled for $ms ms", ms < 5_000)
        }
        try {
            for (label in listOf("Calendar", "Subjects", "Progress", "Today", "Subjects")) {
                measure("root_$label") { compose.onAllNodes(hasText(label) and hasClickAction()).onLast().performClick() }
                compose.onNodeWithTag("root-content").performTouchInput { swipeUp(durationMillis = 600) }
                compose.onNodeWithTag("root-content").performTouchInput { swipeDown(durationMillis = 600) }
            }
            compose.onNode(hasText("Фізика") and hasClickAction()).performClick()
            for (tab in listOf("Tasks", "Exam", "Notes", "Overview")) {
                measure("subject_$tab") { compose.onNodeWithText(tab).performClick() }
                compose.onRoot().performTouchInput { swipeUp(durationMillis = 600) }
            }
        } finally {
            compose.activity.window.removeOnFrameMetricsAvailableListener(listener)
            thread.quitSafely()
            val times = frames.sorted()
            if (times.isNotEmpty()) {
                report += "frames=${times.size}; p50_ms=${times[times.size / 2] / 1e6}; p95_ms=${times[(times.size * .95).toInt().coerceAtMost(times.lastIndex)] / 1e6}"
            }
            val memory = Debug.MemoryInfo().also(Debug::getMemoryInfo)
            report += "pss_kb=${memory.totalPss}"
            InstrumentationRegistry.getInstrumentation().targetContext.openFileOutput("populated-performance.txt", 0)
                .use { it.write(report.joinToString("\n").toByteArray()) }
        }
    }
}
