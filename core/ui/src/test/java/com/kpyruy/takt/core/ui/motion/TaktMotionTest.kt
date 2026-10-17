package com.kpyruy.takt.core.ui.motion

import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import org.junit.Assert.*
import org.junit.Test

class TaktMotionTest {
    private fun animation() = TargetBasedAnimation(TaktMotion.menu<Float>(), Float.VectorConverter, 0f, 1f)

    @Test fun navigationDoesNotThrowMostOfTheScreenAcrossInTheFirst100ms() {
        val motion = animation()
        val at100 = motion.getValueFromNanos(100_000_000)
        assertTrue("First100ms must be gradual, actual=$at100", at100 in .15f.. .65f)
        assertTrue("Keep a short, finite transition", motion.durationNanos in 324_000_000..334_000_000)
    }

    @Test fun highRefreshSamplesAreContinuousMonotonicAndReachTheSameEndpoint() {
        val motion = animation()
        for (hz in listOf(60, 90, 120, 144, 240)) {
            val samples = (0L..motion.durationNanos step (1_000_000_000L / hz)).map(motion::getValueFromNanos)
            assertEquals(0f, samples.first(), .00001f)
            assertTrue("No reverse or bounce at $hz Hz", samples.zipWithNext().all { (a,b) -> b >= a })
            assertTrue("Every refresh needs an interpolated value at $hz Hz", samples.distinct().size >= samples.size - 1)
            val largestStep = samples.zipWithNext().maxOf { (a,b) -> b - a }
            assertTrue("No abrupt frame jump at $hz Hz, actual=$largestStep", largestStep < (7f * 360 / TaktMotion.NavigationMs) / hz)
            assertEquals(1f, motion.getValueFromNanos(motion.durationNanos), .00001f)
            assertEquals(1f, motion.getValueFromNanos(motion.durationNanos + 1_000_000), .00001f)
            // Tween estimates velocity by a finite1ms difference at the endpoint.
            assertEquals(0f, motion.getVelocityVectorFromNanos(motion.durationNanos).value, .01f)
        }
    }
}
