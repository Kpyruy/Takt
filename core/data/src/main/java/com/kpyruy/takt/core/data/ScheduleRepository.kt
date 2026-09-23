package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.ScheduleException
import com.kpyruy.takt.core.model.ScheduleRule
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeAbsences(): Flow<List<com.kpyruy.takt.core.model.LessonAbsence>>
    suspend fun setAbsent(event: com.kpyruy.takt.core.model.ResolvedScheduleEvent, absent: Boolean)

    fun observeRules(): Flow<List<ScheduleRule>>
    fun observeOneOffEvents(): Flow<List<OneOffScheduleEvent>>
    fun observeExceptions(): Flow<List<ScheduleException>>
    suspend fun upsertRule(rule: ScheduleRule)
    suspend fun upsertRules(rules: List<ScheduleRule>)
    suspend fun upsertOneOffEvent(event: OneOffScheduleEvent)
    suspend fun upsertOneOffEvents(events: List<OneOffScheduleEvent>)
    suspend fun deleteOneOffEvent(id: String)
    suspend fun upsertException(exception: ScheduleException)
    suspend fun deleteRule(id: String)
    suspend fun deleteException(id: String)
    suspend fun deleteExceptionForOccurrence(ruleId: String, date: LocalDate)
}
