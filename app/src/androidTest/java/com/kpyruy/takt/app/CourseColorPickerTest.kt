package com.kpyruy.takt.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.CourseColor
import com.kpyruy.takt.core.ui.components.CourseColorPicker
import com.kpyruy.takt.core.ui.components.LocalCourseColorKeys
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CourseColorPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun paletteSelectsColorAndAutomaticReset() {
        var choice: String? = "TEAL"
        compose.setContent {
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme { CourseColorPicker("TEAL", onSelect = { choice = it }, onDismiss = {}) }
        }
        compose.onNodeWithTag("course-color-TEAL").assertIsSelected()
        captureStudiesScreenshot("course-color-palette.png")
        compose.onNodeWithTag("course-color-ROSE").performClick()
        compose.runOnIdle { assertEquals("ROSE", choice) }
        compose.onNodeWithTag("course-color-automatic").performClick()
        compose.runOnIdle { assertEquals(null, choice) }
    }

    @Test fun courseAccentResolverUsesPersistedColorForIdOnlyConsumers() {
        var resolved = Color.Unspecified
        compose.setContent {
            TaktTheme {
                CompositionLocalProvider(LocalCourseColorKeys provides mapOf("course" to "TEAL")) {
                    resolved = taktSubjectColor("course")
                }
            }
        }
        compose.runOnIdle { assertEquals(Color(CourseColor.TEAL.argb), resolved) }
    }
}
