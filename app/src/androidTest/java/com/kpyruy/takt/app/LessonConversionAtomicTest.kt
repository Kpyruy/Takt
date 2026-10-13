package com.kpyruy.takt.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.data.RoomScheduleRepository
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class LessonConversionAtomicTest {
    @Test fun rejectedSourceDeleteRollsBackNewOneOffAndSuccessfulRetrySwapsOnce() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaktDatabase::class.java).build()
        try {
            val repo = RoomScheduleRepository(db)
            val rule = ScheduleRule("atomic", null, "Original series", DayOfWeek.THURSDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 30), ScheduleRecurrence.WEEKLY)
            val once = OneOffScheduleEvent("atomic", null, "Single lesson", LocalDate.of(2026, 11, 19),
                rule.startTime, rule.endTime, type = OneOffScheduleEventType.BLOCK_ACTION)
            repo.upsertRule(rule)
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_conversion BEFORE DELETE ON schedule_rules BEGIN SELECT RAISE(ABORT, 'conversion rejected'); END")
            assertTrue(runCatching { repo.convertRuleToOneOff(rule.id, once) }.isFailure)
            assertEquals(listOf(rule), repo.observeRules().first())
            assertTrue(repo.observeOneOffEvents().first().isEmpty())
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_conversion")
            repo.convertRuleToOneOff(rule.id, once)
            assertTrue(repo.observeRules().first().isEmpty())
            assertEquals(listOf(once), repo.observeOneOffEvents().first())
            repo.convertOneOffToRule(once.id, rule)
            assertEquals(listOf(rule), repo.observeRules().first())
            assertTrue(repo.observeOneOffEvents().first().isEmpty())
        } finally { db.close() }
    }
}
