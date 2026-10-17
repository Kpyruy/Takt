package com.kpyruy.takt.core.model

import org.junit.Assert.*
import org.junit.Test

class CustomColorThemeTest {
    @Test fun threeIndependentColorsNormalizeAndKeepLegacyConstructorDefaults() {
        val legacy = CustomColorTheme("old", "Ocean", "#0099AA")
        assertNull(legacy.secondaryHex)
        assertNull(legacy.tertiaryHex)
        val theme = CustomColorTheme("id", " Ocean ", "0099aa", "  #d23456 ", "80af00").normalizedOrNull()!!
        assertEquals("#0099AA", theme.primaryHex)
        assertEquals("#D23456", theme.secondaryHex)
        assertEquals("#80AF00", theme.tertiaryHex)
        assertEquals(0xFFD23456.toInt(), theme.secondaryArgb)
        assertEquals(0xFF80AF00.toInt(), theme.tertiaryArgb)
    }

    @Test fun invalidExplicitRoleColorsCannotBeSilentlyChangedToDerivedColors() {
        assertNull(CustomColorTheme("id", "Ocean", "#123456", "#xyzxyz").normalizedOrNull())
        assertNull(CustomColorTheme("id", "Ocean", "#123456", tertiaryHex = "#123").normalizedOrNull())
    }

    @Test fun namesAndSixDigitColorsAreNormalizedWithoutLosingIdentity() {
        val theme = CustomColorTheme("stable-id", "  Ocean  ", "0099aa").normalizedOrNull()!!
        assertEquals("stable-id", theme.id)
        assertEquals("Ocean", theme.name)
        assertEquals("#0099AA", theme.primaryHex)
        assertEquals(0xFF0099AA.toInt(), theme.primaryArgb)
    }

    @Test fun emptyNamesAndInvalidColorsCannotBecomeThemes() {
        assertNull(CustomColorTheme("id", " ", "#123456").normalizedOrNull())
        assertNull(CustomColorTheme("id", "x".repeat(41), "#123456").normalizedOrNull())
        assertNull(CustomColorTheme("id", "Ocean", "#123").normalizedOrNull())
        assertNull(CustomColorTheme("id", "Ocean", "#GG0000").normalizedOrNull())
        assertNull(CustomColorTheme("", "Ocean", "#123456").normalizedOrNull())
    }

    @Test fun renameKeepsSelectionAndDeletingSelectedThemeFallsBackToPreset() {
        val theme = CustomColorTheme("id", "Ocean", "#007E91")
        val selected = AppSettings(themeFamily = ThemeFamily.GREEN,
            customThemes = listOf(theme), selectedCustomThemeId = theme.id)
        assertEquals("Sea", selected.copy(customThemes = listOf(theme.copy(name = "Sea"))).selectedCustomTheme?.name)
        val deleted = selected.copy(customThemes = emptyList()).normalizedAppearance()
        assertNull(deleted.selectedCustomThemeId)
        assertEquals(ThemeFamily.GREEN, deleted.themeFamily)
    }
}
