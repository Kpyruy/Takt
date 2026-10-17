package com.kpyruy.takt.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CustomColorTheme
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.ui.components.preparedGlassKey
import org.junit.Assert.*
import org.junit.Test

class TaktCustomThemeTest {
    @Test fun editingALegacyThemeMakesItsDerivedRolesIndependentWithoutChangingIdentity() {
        val legacy = CustomColorTheme("old", "Rose", "#B52D67")
        val editable = taktEditableCustomTheme(legacy)
        assertEquals(legacy.id, editable.id)
        assertEquals(legacy.name, editable.name)
        assertEquals(legacy.primaryHex, editable.primaryHex)
        assertNotNull(editable.secondaryHex)
        assertNotNull(editable.tertiaryHex)
        fun colors(theme: CustomColorTheme) = taktThemePreviewColors(
            AppSettings(customThemes = listOf(theme), selectedCustomThemeId = theme.id), false)
        assertEquals(colors(editable).drop(1), colors(editable.copy(primaryHex = "#007E91")).drop(1))
        assertNull(legacy.secondaryHex) // Opening the editor never mutates the stored legacy object.
    }

    @Test fun aLegacyOneColorThemeStillMatchesItsPresetInBothModes() {
        val legacy = CustomColorTheme("old", "Rose", "#B52D67")
        val settings = AppSettings(customThemes = listOf(legacy), selectedCustomThemeId = legacy.id)
        listOf(false, true).forEach { dark ->
            assertEquals(taktThemePreviewColors(ThemeFamily.ROSE, dark), taktThemePreviewColors(settings, dark))
        }
    }

    @Test fun editingOneExplicitRoleDoesNotChangeTheOtherTwoRoles() {
        val theme = CustomColorTheme("id", "Independent", "#B52D67", "#147D64", "#7048C8")
        fun colors(theme: CustomColorTheme, dark: Boolean) = taktThemePreviewColors(
            AppSettings(customThemes = listOf(theme), selectedCustomThemeId = theme.id), dark)
        listOf(false, true).forEach { dark ->
            val original = colors(theme, dark)
            listOf(theme.copy(primaryHex = "#007E91"), theme.copy(secondaryHex = "#AE4724"),
                theme.copy(tertiaryHex = "#9B6500")).forEachIndexed { changedRole, changed ->
                val updated = colors(changed, dark)
                original.indices.forEach { role ->
                    if (role == changedRole) assertNotEquals(original[role], updated[role])
                    else assertEquals(original[role], updated[role])
                }
            }
        }
    }

    @Test fun extremeThreeColorPalettesStayReadableInBothModes() {
        listOf("#FFFFFF", "#000000", "#FFFF00", "#00FFFF", "#FF00FF").forEach { seed ->
            val theme = CustomColorTheme("id", "Theme", seed, seed, seed)
            val palette = paletteFor(AppSettings(customThemes = listOf(theme), selectedCustomThemeId = theme.id))
            listOf(palette.light, palette.dark).forEach { colors ->
                listOf(colors.primary to colors.onPrimary, colors.secondary to colors.onSecondary,
                    colors.tertiary to colors.onTertiary, colors.primaryContainer to colors.onPrimaryContainer,
                    colors.secondaryContainer to colors.onSecondaryContainer,
                    colors.tertiaryContainer to colors.onTertiaryContainer).forEach { (a, b) ->
                    assertTrue("$seed $a / $b", contrast(a, b) >= 4.5f)
                }
            }
        }
    }

    @Test fun existingFivePrimaryColorsStayExact() {
        val expected = mapOf(ThemeFamily.BLUE to Color(0xFF145BFA), ThemeFamily.GREEN to Color(0xFF147D64),
            ThemeFamily.PURPLE to Color(0xFF7048C8), ThemeFamily.WARM to Color(0xFFAE4724),
            ThemeFamily.MONOCHROME to Color(0xFF303846))
        expected.forEach { (family, primary) -> assertEquals(primary, paletteFor(family).light.primary) }
    }

    @Test fun extremeCustomSeedsProduceReadableLightAndDarkAccents() {
        listOf("#FFFFFF", "#000000", "#FFFF00", "#00FFFF", "#FF00FF", "#B52D67").forEach { seed ->
            val custom = CustomColorTheme("id", "Theme", seed)
            val palette = paletteFor(AppSettings(customThemes = listOf(custom), selectedCustomThemeId = custom.id))
            assertTrue("light $seed", contrast(palette.light.primary, palette.light.onPrimary) >= 4.5f)
            assertTrue("dark $seed", contrast(palette.dark.primary, palette.dark.onPrimary) >= 4.5f)
            assertTrue("light container $seed", contrast(palette.light.primaryContainer, palette.light.onPrimaryContainer) >= 4.5f)
            assertTrue("dark container $seed", contrast(palette.dark.primaryContainer, palette.dark.onPrimaryContainer) >= 4.5f)
        }
    }

    @Test fun newAndCustomThemesSharePreparedNeutralAssets() {
        val neutral = preparedGlassKey(false, ThemeFamily.MONOCHROME, false)
        listOf(ThemeFamily.ROSE, ThemeFamily.AMBER, ThemeFamily.CYAN).forEach {
            assertEquals(neutral, preparedGlassKey(false, it, false))
        }
        ThemeFamily.entries.forEach { assertEquals(neutral, preparedGlassKey(false, it, true)) }
        assertNotEquals(neutral, preparedGlassKey(true, ThemeFamily.BLUE, true))
        assertNotEquals(neutral, preparedGlassKey(false, ThemeFamily.BLUE, false))
    }

    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)
}
