package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.ScheduleRule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeRules(): Flow<List<ScheduleRule>>
    fun observeOneOffEvents(): Flow<List<OneOffScheduleEvent>>
    suspend fun upsertRule(rule: ScheduleRule)
    suspend fun upsertRules(rules: List<ScheduleRule>)
    suspend fun upsertOneOffEvent(event: OneOffScheduleEvent)
    suspend fun upsertOneOffEvents(events: List<OneOffScheduleEvent>)
    suspend fun deleteRule(id: String)
}
