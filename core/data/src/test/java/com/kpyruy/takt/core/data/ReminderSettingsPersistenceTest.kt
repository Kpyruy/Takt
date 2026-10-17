package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class ReminderSettingsPersistenceTest {
    private val settings = AppSettings(reminders = ReminderSettings(enabled = true,
        academicEnabled = false, dateOnlyTime = LocalTime.of(8, 45))
        .withRule(ReminderCategory.TEST, ReminderRule(true, listOf(10_080, 1440)))
        .withRule(ReminderCategory.EVENT, ReminderRule(false, listOf(45))))
    @Test fun storedSettingsRoundTripAndOldOrInvalidValuesDefaultOff() {
        assertEquals(settings.reminders, ReminderSettingsCodec.decode(ReminderSettingsCodec.encode(settings.reminders)))
        assertEquals(ReminderSettings(), ReminderSettingsCodec.decode(null))
        assertEquals(ReminderSettings(), ReminderSettingsCodec.decode("broken"))
        assertFalse(StoredSettingsCodec.decode(null, false, null).reminders.enabled)
    }
    @Test fun backupRoundTripAndOldBackupDefaultOff() {
        val payload = BackupPayload(courses = emptyList(), settings = settings.toBackup())
        assertEquals(settings, BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload)).settings.toModel())
        assertFalse(BackupPayloadCodec.decode("""{"courses":[]} """).settings.toModel().reminders.enabled)
    }
    @Test fun appearanceRestoreKeepsLocalRemindersButPreferencesRestoresThem() {
        val local = BackupPayload(courses = emptyList(), settings = settings.toBackup())
        val incoming = local.copy(settings = AppSettings().toBackup())
        assertEquals(settings.reminders, BackupMerger.merge(local, incoming,
            setOf(BackupSection.APPEARANCE)).settings.toModel().reminders)
        assertEquals(ReminderSettings(), BackupMerger.merge(local, incoming,
            setOf(BackupSection.PREFERENCES)).settings.toModel().reminders)
    }
}
