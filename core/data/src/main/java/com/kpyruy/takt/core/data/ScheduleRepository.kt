package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.ScheduleException
import com.kpyruy.takt.core.model.ScheduleRule
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeRules(): Flow<List<ScheduleRule>>
    fun observeOneOffEvents(): Flow<List<OneOffScheduleEvent>>
    fun observeExceptions(): Flow<List<ScheduleException>>
    suspend fun upsertRule(rule: ScheduleRule)
    suspend fun upsertRules(rules: List<ScheduleRule>)
    suspend fun upsertOneOffEvent(event: OneOffScheduleEvent)
    suspend fun upsertOneOffEvents(events: List<OneOffScheduleEvent>)
    suspend fun upsertException(exception: ScheduleException)
    suspend fun deleteRule(id: String)
    suspend fun deleteException(id: String)
    suspend fun deleteExceptionForOccurrence(ruleId: String, date: LocalDate)
}
