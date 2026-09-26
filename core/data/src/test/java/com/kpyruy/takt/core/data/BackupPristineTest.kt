package com.kpyruy.takt.core.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPristineTest {
    @Test fun emptyNewInstallCanRestoreEvenAfterChoosingAppearance() {
        assertTrue(BackupPayload(settings = BackupSettings(themeFamily = "PURPLE")).hasNoUserContent())
    }

    @Test fun aUserCoursePreventsSilentReplacementByBackup() {
        val course = BackupCourse("own", "MATH", "Математика", 5, 1, "enrolled", "COMPULSORY")
        assertFalse(BackupPayload(courses = listOf(course)).hasNoUserContent())
    }
}
