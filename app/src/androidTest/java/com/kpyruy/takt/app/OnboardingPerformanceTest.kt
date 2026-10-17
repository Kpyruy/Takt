package com.kpyruy.takt.app

import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.StrictMode
import android.view.FrameMetrics
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.ThemeFamily
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/** Real MainActivity + durable setup completion; opt-in, disposable emulator only. */
class OnboardingPerformanceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun freshSetupLanguagePaletteTourAndRootsRemainResponsive() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("performanceFreshStart") == "true")
        assumeTrue(android.os.Build.HARDWARE == "ranchu")
        val report = mutableListOf<String>()
        val baseline = InstrumentationRegistry.getArguments().getString("performanceBaseline") == "true"
        val frameTimes = CopyOnWriteArrayList<Long>()
        val writes = CopyOnWriteArrayList<String>()
        val frameThread = HandlerThread("TaktFrameMetrics").apply { start() }
        val strictExecutor = Executors.newSingleThreadExecutor()
        var originalPolicy: StrictMode.ThreadPolicy? = null
        val listener = android.view.Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            frameTimes.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))
        }
        compose.activityRule.scenario.onActivity {
            originalPolicy = StrictMode.getThreadPolicy()
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectDiskWrites().penaltyListener(strictExecutor) { writes.add(it.toString()) }.build())
            it.window.addOnFrameMetricsAvailableListener(listener, Handler(frameThread.looper))
            val mode = it.window.decorView.display.mode
            report += "display_hz=${mode.refreshRate}; requested_hz=${it.window.attributes.preferredRefreshRate}"
        }
        fun measure(name: String, action: () -> Unit) {
            var mainCpuBefore = 0L
            compose.activityRule.scenario.onActivity { mainCpuBefore = Debug.threadCpuTimeNanos() }
            val processCpuBefore = android.os.Process.getElapsedCpuTime()
            val start = System.nanoTime()
            action()
            compose.waitForIdle()
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            report += "$name=${"%.1f".format(java.util.Locale.US, elapsed)}ms"
            compose.activityRule.scenario.onActivity {
                report += "${name}_main_cpu_ms=${(Debug.threadCpuTimeNanos() - mainCpuBefore) / 1_000_000.0}"
            }
            report += "${name}_process_cpu_ms=${android.os.Process.getElapsedCpuTime() - processCpuBefore}"
            assertTrue("$name stalled for $elapsed ms", elapsed < if (baseline) 30_000.0 else 5_000.0)
        }
        try {
            compose.onNodeWithTag("welcome-local").performScrollTo().performClick()
            for (choice in listOf(AppLanguage.UKRAINIAN, AppLanguage.ENGLISH, AppLanguage.SLOVAK,
                AppLanguage.UKRAINIAN, AppLanguage.SLOVAK, AppLanguage.ENGLISH)) {
                measure("language_${choice.name}") {
                    compose.onNodeWithTag("onboarding-language-${choice.name}").performClick()
                }
            }
            compose.onNode(hasText("System") or hasText("Follow system")).assertExists()
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val appearance = instrumentation.uiAutomation.takeScreenshot()
            instrumentation.targetContext.openFileOutput("onboarding-appearance.png", 0).use {
                appearance.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            appearance.recycle()
            compose.onNodeWithTag("onboarding-color-open").performScrollTo().performClick()
            for (family in listOf(ThemeFamily.PURPLE, ThemeFamily.GREEN, ThemeFamily.WARM,
                ThemeFamily.MONOCHROME, ThemeFamily.ROSE, ThemeFamily.AMBER, ThemeFamily.CYAN,
                ThemeFamily.BLUE, ThemeFamily.PURPLE)) {
                compose.onNodeWithTag("onboarding-color-${family.name}").performScrollTo()
                measure("palette_${family.name}") {
                    compose.onNodeWithTag("onboarding-color-${family.name}").performClick()
                }
            }
            compose.onNodeWithTag("palette-popup-apply").performClick()
            for (choice in listOf(AppLanguage.SLOVAK, AppLanguage.ENGLISH)) {
                compose.onNodeWithTag("onboarding-language-${choice.name}").performScrollTo()
                measure("language_colored_${choice.name}") {
                    compose.onNodeWithTag("onboarding-language-${choice.name}").performClick()
                }
            }
            compose.onNodeWithTag("onboarding-primary").performClick()
            compose.onNodeWithTag("onboarding-primary").performClick()
            compose.onNodeWithTag("onboarding-primary").performClick()
            if (baseline) repeat(4) { compose.onNodeWithTag("onboarding-primary").performClick() }
            else repeat(GuidedTourStep.entries.size) {
                compose.waitUntil(8_000) { compose.onAllNodesWithTag("tour-next").fetchSemanticsNodes().any {
                    !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
                } }
                compose.onNodeWithTag("tour-next").performClick()
            }
            measure("tour_finish_to_home") {
                compose.onNodeWithTag(if (baseline) "onboarding-primary" else "tour-done").performClick()
                compose.waitUntil(if (baseline) 30_000 else 5_000) { compose.onAllNodesWithTag("root-content").fetchSemanticsNodes().isNotEmpty() }
            }
            val settings = runBlocking {
                (compose.activity.application as TaktApplication).dataContainer.settingsRepository.settings.first()
            }
            assertEquals(AppLanguage.ENGLISH, settings.language)
            assertEquals(ThemeFamily.PURPLE, settings.themeFamily)
            assertFalse(runBlocking { (compose.activity.application as TaktApplication).dataContainer.firstRunRepository.shouldShow() })
            for (root in listOf("Calendar", "Subjects", "Progress", "Today")) {
                measure("root_$root") {
                    compose.onAllNodes(hasText(root) and hasClickAction()).onLast().performClick()
                }
            }
            compose.activityRule.scenario.onActivity {
                if (!baseline) assertEquals(it.window.decorView.display.mode.refreshRate, it.window.attributes.preferredRefreshRate, .1f)
            }
            if (!baseline) assertTrue("UI thread performed disk writes: $writes", writes.isEmpty())
        } finally {
            compose.activityRule.scenario.onActivity {
                it.window.removeOnFrameMetricsAvailableListener(listener)
                originalPolicy?.let(StrictMode::setThreadPolicy)
            }
            frameThread.quitSafely()
            strictExecutor.shutdown()
            val sorted = frameTimes.sorted()
            if (sorted.isNotEmpty()) {
                fun quantile(q: Double) = sorted[((sorted.size - 1) * q).toInt()] / 1_000_000.0
                report += "frames=${sorted.size}; total_p50_ms=${quantile(.5)}; total_p95_ms=${quantile(.95)}"
            }
            val info = Debug.MemoryInfo().also(Debug::getMemoryInfo)
            report += "pss_kb=${info.totalPss}; main_disk_writes=${writes.size}"
            // Compare retained memory separately from allocations awaiting GC after a fast run.
            // This is measurement-only: production UI never forces a collection.
            Runtime.getRuntime().gc()
            System.runFinalization()
            Runtime.getRuntime().gc()
            val retained = Debug.MemoryInfo().also(Debug::getMemoryInfo)
            report += "post_gc_pss_kb=${retained.totalPss}"
            InstrumentationRegistry.getInstrumentation().targetContext.openFileOutput("onboarding-performance.txt", 0)
                .use { it.write(report.joinToString("\n").toByteArray()) }
        }
    }
}
