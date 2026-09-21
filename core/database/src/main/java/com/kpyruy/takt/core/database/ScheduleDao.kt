package com.kpyruy.takt.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_rules ORDER BY dayOfWeek, startMinute")
    fun observeRules(): Flow<List<ScheduleRuleEntity>>

    @Query("SELECT * FROM schedule_one_off ORDER BY dateEpochDay, startMinute")
    fun observeOneOffEvents(): Flow<List<OneOffScheduleEventEntity>>

    @Query("SELECT * FROM schedule_exceptions ORDER BY dateEpochDay")
    fun observeExceptions(): Flow<List<ScheduleExceptionEntity>>

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

    @Query("DELETE FROM schedule_rules WHERE id = :id")
    suspend fun deleteRule(id: String)

    @Query("DELETE FROM schedule_exceptions WHERE id = :id")
    suspend fun deleteException(id: String)

    @Query("DELETE FROM schedule_exceptions WHERE ruleId = :ruleId AND dateEpochDay = :dateEpochDay")
    suspend fun deleteExceptionForOccurrence(ruleId: String, dateEpochDay: Long)

    @Query("SELECT COUNT(*) FROM schedule_rules")
    suspend fun countRules(): Int

    @Query("SELECT COUNT(*) FROM schedule_one_off")
    suspend fun countOneOffEvents(): Int
}
