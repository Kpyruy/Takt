package com.kpyruy.takt.core.model

import org.junit.Assert.*
import org.junit.Test

class AppVersionTest {
    @Test fun comparesNumericComponentsInsteadOfTagText() {
        assertTrue(AppVersion.parse("v0.10.0")!! > AppVersion.parse("0.2.0")!!)
        assertTrue(AppVersion.parse("2.0.0")!! > AppVersion.parse("1.99.99")!!)
        assertEquals(AppVersion.parse("v0.2.0"), AppVersion.parse("0.2.0+build.5"))
    }
    @Test fun rejectsAmbiguousMalformedAndPrereleaseVersions() {
        listOf("", "latest", "1.2", "01.2.3", "1.2.3-beta.1", "1.2.3.4", "-1.2.3", "1.2.3+", "1.2.3+bad space").forEach {
            assertNull(it, AppVersion.parse(it))
        }
        assertNotNull(AppVersion.parse("999999999999999999999999.2.3"))
    }
    @Test fun onlyReportsKnownNewerStableRelease() {
        val release = AppRelease("v0.3.0", "https://github.com/Kpyruy/Takt/releases/tag/v0.3.0", null)
        assertTrue(release.isNewerThan("0.2.0"))
        assertFalse(release.isNewerThan("0.3.0"))
        assertFalse(release.isNewerThan("unknown"))
    }
}
