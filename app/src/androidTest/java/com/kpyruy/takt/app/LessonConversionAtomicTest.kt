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
    @Test fun convertingBackNeverResurrectsOldOverridesOrAttendance() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaktDatabase::class.java).build()
        try {
            val repo = RoomScheduleRepository(db)
            val date = LocalDate.of(2026, 11, 19)
            val rule = ScheduleRule("series", null, "Series", date.dayOfWeek, LocalTime.of(8, 0), LocalTime.of(9, 0), ScheduleRecurrence.WEEKLY)
            repo.upsertRule(rule)
            repo.upsertException(ScheduleException("cancel", rule.id, date, ScheduleExceptionType.CANCELLED))
            repo.upsertException(ScheduleException("move", rule.id, date.plusWeeks(1), ScheduleExceptionType.MOVED, date.plusWeeks(1).plusDays(1)))
            db.scheduleDao().upsertAbsences(listOf(com.kpyruy.takt.core.database.LessonAbsenceEntity(rule.id, date.toEpochDay(), false)))
            val once = OneOffScheduleEvent(rule.id, null, rule.title, date, rule.startTime, rule.endTime, type = OneOffScheduleEventType.EXTRA)
            repo.convertRuleToOneOff(rule.id, once)
            assertTrue(db.scheduleDao().getExceptionsSnapshot().isEmpty())
            assertTrue(db.scheduleDao().getAbsencesSnapshot().isEmpty())
            repo.convertOneOffToRule(rule.id, rule)
            val event = ScheduleResolver.eventsForDate(repo.observeRules().first(), repo.observeExceptions().first(), emptyList(), date).single()
            assertEquals(ScheduleEventStatus.NORMAL, event.status)
        } finally { db.close() }
    }

    @Test fun failedOverrideCleanupRollsBackRuleAndAttendanceDeletion() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TaktDatabase::class.java).build()
        try {
            val repo = RoomScheduleRepository(db)
            val date = LocalDate.of(2026, 11, 19)
            val rule = ScheduleRule("series", null, "Series", date.dayOfWeek, LocalTime.of(8, 0), LocalTime.of(9, 0), ScheduleRecurrence.WEEKLY)
            repo.upsertRule(rule)
            repo.upsertException(ScheduleException("cancel", rule.id, date, ScheduleExceptionType.CANCELLED))
            db.scheduleDao().upsertAbsences(listOf(com.kpyruy.takt.core.database.LessonAbsenceEntity(rule.id, date.toEpochDay(), false)))
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_cleanup BEFORE DELETE ON schedule_exceptions BEGIN SELECT RAISE(ABORT, 'injected'); END")
            assertTrue(runCatching { repo.deleteRule(rule.id) }.isFailure)
            assertEquals(listOf(rule), repo.observeRules().first())
            assertEquals(1, db.scheduleDao().getExceptionsSnapshot().size)
            assertEquals(1, db.scheduleDao().getAbsencesSnapshot().size)
        } finally { db.close() }
    }

}
