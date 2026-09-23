package com.kpyruy.takt.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM lesson_absences ORDER BY dateEpochDay")
    fun observeAbsences(): Flow<List<LessonAbsenceEntity>>

    @Query("SELECT * FROM lesson_absences ORDER BY dateEpochDay")
    suspend fun getAbsencesSnapshot(): List<LessonAbsenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbsences(items: List<LessonAbsenceEntity>)

    @Query("DELETE FROM lesson_absences WHERE eventId = :eventId AND dateEpochDay = :date AND isOneOff = :isOneOff")
    suspend fun deleteAbsence(eventId: String, date: Long, isOneOff: Boolean)

    @Query("DELETE FROM lesson_absences WHERE eventId = :eventId AND isOneOff = :isOneOff")
    suspend fun deleteEventAbsences(eventId: String, isOneOff: Boolean)

    @Query("DELETE FROM lesson_absences")
    suspend fun deleteAllAbsences()

    @Query("SELECT * FROM schedule_rules ORDER BY dayOfWeek, startMinute")
    fun observeRules(): Flow<List<ScheduleRuleEntity>>

    @Query("SELECT * FROM schedule_one_off ORDER BY dateEpochDay, startMinute")
    fun observeOneOffEvents(): Flow<List<OneOffScheduleEventEntity>>

    @Query("SELECT * FROM schedule_exceptions ORDER BY dateEpochDay")
    fun observeExceptions(): Flow<List<ScheduleExceptionEntity>>

    @Query("SELECT * FROM schedule_rules ORDER BY dayOfWeek, startMinute")
    suspend fun getRulesSnapshot(): List<ScheduleRuleEntity>

    @Query("SELECT * FROM schedule_one_off ORDER BY dateEpochDay, startMinute")
    suspend fun getOneOffEventsSnapshot(): List<OneOffScheduleEventEntity>

    @Query("SELECT * FROM schedule_exceptions ORDER BY dateEpochDay")
    suspend fun getExceptionsSnapshot(): List<ScheduleExceptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(item: ScheduleRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(items: List<ScheduleRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOneOffEvent(item: OneOffScheduleEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOneOffEvents(items: List<OneOffScheduleEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertException(item: ScheduleExceptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExceptions(items: List<ScheduleExceptionEntity>)

    @Query("DELETE FROM schedule_rules WHERE id = :id")
    suspend fun deleteRule(id: String)

    @Query("DELETE FROM schedule_one_off WHERE id = :id")
    suspend fun deleteOneOffEvent(id: String)

    @Query("DELETE FROM schedule_exceptions WHERE id = :id")
    suspend fun deleteException(id: String)

    @Query("DELETE FROM schedule_exceptions WHERE ruleId = :ruleId AND dateEpochDay = :dateEpochDay")
    suspend fun deleteExceptionForOccurrence(ruleId: String, dateEpochDay: Long)

    @Query("DELETE FROM schedule_rules")
    suspend fun deleteAllRules()

    @Query("DELETE FROM schedule_one_off")
    suspend fun deleteAllOneOffEvents()

    @Query("DELETE FROM schedule_exceptions")
    suspend fun deleteAllExceptions()

    @Query("SELECT COUNT(*) FROM schedule_rules")
    suspend fun countRules(): Int

    @Query("SELECT COUNT(*) FROM schedule_one_off")
    suspend fun countOneOffEvents(): Int
}
