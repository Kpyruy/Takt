package com.kpyruy.takt.core.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UisProgressAutoMergeTest {
    @Test fun appliesRemoteOnlyWhenLocalStillMatchesLastAcceptedUisValue() {
        assertTrue(UisProgressAutoMerge.shouldApply("enrolled", "fulfilled", "enrolled"))
        assertFalse(UisProgressAutoMerge.shouldApply("planned", "fulfilled", "enrolled"))
        assertFalse(UisProgressAutoMerge.shouldApply("enrolled", "fulfilled", null))
        assertFalse(UisProgressAutoMerge.shouldApply("enrolled", "fulfilled", "enrolled", "FAILED", "PASSED", "none"))
        assertTrue(UisProgressAutoMerge.shouldApply("enrolled", "fulfilled", "enrolled", "none", "PASSED", "none"))
        assertTrue(UisProgressAutoMerge.shouldApply("fulfilled", "fulfilled", "fulfilled", "none", "PASSED", "none"))
        assertFalse(UisProgressAutoMerge.shouldApply("fulfilled", "fulfilled", "fulfilled", "FAILED", "PASSED", "none"))
        assertTrue(UisProgressAutoMerge.canRemember("fulfilled", "fulfilled"))
    }
}
