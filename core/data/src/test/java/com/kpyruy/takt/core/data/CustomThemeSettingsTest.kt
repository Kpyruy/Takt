package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CustomColorTheme
import com.kpyruy.takt.core.model.ThemeFamily
import org.junit.Assert.*
import org.junit.Test

class CustomThemeSettingsTest {
    private val rose = CustomColorTheme("rose-id", "My rose", "#B52D67")

    @Test fun explicitThreeColorsSurvivePreferencesAndBackupRoundTrips() {
        val custom = CustomColorTheme("three", "Independent", "#B52D67", "#147D64", "#7048C8")
        assertEquals(listOf(custom), CustomThemesCodec.decode(CustomThemesCodec.encode(listOf(custom))))
        val settings = AppSettings(customThemes = listOf(custom), selectedCustomThemeId = custom.id)
        val restored = BackupPayloadCodec.decode(BackupPayloadCodec.encode(BackupPayload(settings = settings.toBackup())))
            .settings.toModel()
        assertEquals(custom, restored.selectedCustomTheme)
        assertEquals(settings, restored)
    }

    @Test fun oldThreeFieldCustomThemeJsonStillUsesDerivedRoleDefaults() {
        val raw = """[{"id":"legacy","name":"Ocean","primaryHex":"#0099AA"}]"""
        val stored = CustomThemesCodec.decode(raw).single()
        assertNull(stored.secondaryHex)
        assertNull(stored.tertiaryHex)
        val restored = BackupPayloadCodec.decode("""{"version":1,"courses":[],"settings":{
            "customThemes":$raw,"selectedCustomThemeId":"legacy"}}""").settings.toModel()
        assertEquals(stored, restored.selectedCustomTheme)
    }

    @Test fun secondaryAndTertiaryOnlyChangesAreReviewedAndRestoredAsAppearance() {
        val theme = BackupCustomColorTheme("id", "Ocean", "#007E91", "#147D64", "#7048C8")
        val local = BackupPayload(settings = BackupSettings(language = "SLOVAK", currentSemester = 3,
            customThemes = listOf(theme), selectedCustomThemeId = theme.id))
        val incoming = local.copy(settings = local.settings.copy(customThemes = listOf(
            theme.copy(secondaryHex = "#AE4724", tertiaryHex = "#9B6500"))))
        assertEquals(setOf(BackupSection.APPEARANCE), BackupMerger.changedSections(local, incoming))
        val restored = BackupMerger.merge(local, incoming, setOf(BackupSection.APPEARANCE)).settings
        assertEquals(incoming.settings.customThemes, restored.customThemes)
        assertEquals("SLOVAK", restored.language)
        assertEquals(3, restored.currentSemester)
        assertEquals(local.settings, BackupMerger.merge(local, incoming, setOf(BackupSection.PREFERENCES)).settings)
    }

    @Test fun customThemeAndSelectionRoundTripThroughPreferencesCodec() {
        val decoded = StoredSettingsCodec.decode(null, false, null,
            themeFamily = "CYAN", customThemes = CustomThemesCodec.encode(listOf(rose)),
            selectedCustomThemeId = rose.id)
        assertEquals(listOf(rose), decoded.customThemes)
        assertEquals(rose, decoded.selectedCustomTheme)
        assertEquals(ThemeFamily.CYAN, decoded.themeFamily)
    }

    @Test fun customThemeAndSelectionRoundTripThroughBackupJson() {
        val settings = AppSettings(themeFamily = ThemeFamily.AMBER,
            customThemes = listOf(rose), selectedCustomThemeId = rose.id)
        val payload = BackupPayload(settings = settings.toBackup())
        assertEquals(settings, BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload)).settings.toModel())
    }

    @Test fun olderBackupsHaveNoCustomThemesAndKeepTheirPreset() {
        val restored = BackupPayloadCodec.decode("""{"version":1,"courses":[],"settings":{"themeFamily":"GREEN"}}""").settings.toModel()
        assertEquals(ThemeFamily.GREEN, restored.themeFamily)
        assertTrue(restored.customThemes.isEmpty())
        assertNull(restored.selectedCustomThemeId)
    }

    @Test fun invalidEntriesAndDanglingSelectionAreDiscarded() {
        val raw = """[{"id":"good","name":" Ocean ","primaryHex":"#0099aa"},
            {"id":"bad","name":"","primaryHex":"#123456"},
            {"id":"good","name":"Duplicate","primaryHex":"#654321"},
            {"id":"bad-color","name":"Bad","primaryHex":"#zzzzzz"}]"""
        val decoded = StoredSettingsCodec.decode(null, false, null,
            customThemes = raw, selectedCustomThemeId = "missing")
        assertEquals(listOf(CustomColorTheme("good", "Ocean", "#0099AA")), decoded.customThemes)
        assertNull(decoded.selectedCustomThemeId)
        assertTrue(CustomThemesCodec.decode("broken json").isEmpty())
    }
}
