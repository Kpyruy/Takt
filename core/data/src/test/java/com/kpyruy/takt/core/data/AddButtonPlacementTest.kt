package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import org.junit.Assert.*
import org.junit.Test

class AddButtonPlacementTest {
    @Test fun placementIsPortableAndOnlyImportedWithAppearance() {
        val local = BackupPayload(settings = AppSettings().toBackup())
        val file = BackupPayload(settings = AppSettings(addButtonPlacement = AddButtonPlacement.FLOATING).toBackup())
        val roundTrip = BackupPayloadCodec.decode(BackupPayloadCodec.encode(file))
        assertEquals(AddButtonPlacement.FLOATING, roundTrip.settings.toModel().addButtonPlacement)
        assertEquals(setOf(BackupSection.APPEARANCE), BackupMerger.changedSections(local, roundTrip))
        assertEquals("FLOATING", BackupMerger.merge(local, roundTrip, setOf(BackupSection.APPEARANCE)).settings.addButtonPlacement)
        assertEquals("CENTER", BackupMerger.merge(local, roundTrip, setOf(BackupSection.PREFERENCES)).settings.addButtonPlacement)
    }
    @Test fun legacyDefaultsAndCorruptPreferenceAreSafe() {
        assertEquals("CENTER", BackupPayloadCodec.decode("""{"courses":[]}""").settings.addButtonPlacement)
        assertEquals(AddButtonPlacement.CENTER, StoredSettingsCodec.decode(null, false, null, addButtonPlacement = "invalid").addButtonPlacement)
        assertThrows(IllegalArgumentException::class.java) {
            BackupPayloadCodec.decode(BackupPayloadCodec.encode(BackupPayload(settings = BackupSettings(addButtonPlacement = "invalid"))))
        }
    }
}
