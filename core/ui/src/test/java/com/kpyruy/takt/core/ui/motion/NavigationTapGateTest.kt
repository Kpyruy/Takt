package com.kpyruy.takt.core.ui.motion

import org.junit.Assert.*
import org.junit.Test

class NavigationTapGateTest {
    @Test fun firstTapIsImmediateAndAlternatingSpamDoesNotQueueOrExtendLock() {
        var time = 0L
        val gate = NavigationTapGate(360) { time }
        assertTrue(gate.tryAcquire())
        for (t in listOf(1L, 20, 70, 140, 250, 359)) {
            time = t; assertFalse("Tap at $t must be ignored", gate.tryAcquire())
        }
        time = 360; assertTrue("Ignored taps must not restart the wait", gate.tryAcquire())
        assertFalse(gate.tryAcquire())
        time = 719; assertFalse(gate.tryAcquire())
        time = 720; assertTrue(gate.tryAcquire())
    }
    @Test fun independentRailsAndReturnFromBackgroundRemainResponsive() {
        var time = 100L
        val roots = NavigationTapGate(360) { time }
        val subjectTabs = NavigationTapGate(360) { time }
        assertTrue(roots.tryAcquire()); assertTrue(subjectTabs.tryAcquire())
        time += 60_000
        assertTrue(roots.tryAcquire()); assertTrue(subjectTabs.tryAcquire())
    }
}
