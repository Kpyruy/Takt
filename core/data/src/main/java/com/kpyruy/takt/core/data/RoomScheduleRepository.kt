package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.OneOffScheduleEventEntity
import com.kpyruy.takt.core.database.ScheduleDao
import com.kpyruy.takt.core.database.ScheduleRuleEntity
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.OneOffScheduleEventType
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.map

class RoomScheduleRepository(
    private val dao: ScheduleDao,
) : ScheduleRepository {
    override fun observeRules() = dao.observeRules().map { items -> items.map { it.toDomain() } }

    override fun observeOneOffEvents() =
        dao.observeOneOffEvents().map { items -> items.map { it.toDomain() } }

    override suspend fun upsertRule(rule: ScheduleRule) {
        dao.upsertRule(rule.toEntity())
    }

    override suspend fun upsertRules(rules: List<ScheduleRule>) {
        dao.upsertRules(rules.map { it.toEntity() })
    }

    override suspend fun upsertOneOffEvent(event: OneOffScheduleEvent) {
        dao.upsertOneOffEvent(event.toEntity())
    }

    override suspend fun upsertOneOffEvents(events: List<OneOffScheduleEvent>) {
        dao.upsertOneOffEvents(events.map { it.toEntity() })
    }

    override suspend fun deleteRule(id: String) {
        dao.deleteRule(id)
    }
}

private fun ScheduleRuleEntity.toDomain() = ScheduleRule(
    id = id,
    courseId = courseId,
    title = title,
    dayOfWeek = DayOfWeek.of(dayOfWeek),
    startTime = startMinute.toLocalTime(),
    endTime = endMinute.toLocalTime(),
    recurrence = ScheduleRecurrence.valueOf(recurrence),
    room = room,
)

private fun ScheduleRule.toEntity() = ScheduleRuleEntity(
    id = id,
    courseId = courseId,
    title = title,
    dayOfWeek = dayOfWeek.value,
    startMinute = startTime.toMinuteOfDay(),
    endMinute = endTime.toMinuteOfDay(),
    recurrence = recurrence.name,
    room = room,
)

private fun OneOffScheduleEventEntity.toDomain() = OneOffScheduleEvent(
    id = id,
    courseId = courseId,
    title = title,
    date = LocalDate.ofEpochDay(dateEpochDay),
    startTime = startMinute.toLocalTime(),
    endTime = endMinute.toLocalTime(),
    room = room,
    type = OneOffScheduleEventType.valueOf(type),
)

private fun OneOffScheduleEvent.toEntity() = OneOffScheduleEventEntity(
    id = id,
    courseId = courseId,
    title = title,
    dateEpochDay = date.toEpochDay(),
    startMinute = startTime.toMinuteOfDay(),
    endMinute = endTime.toMinuteOfDay(),
    room = room,
    type = type.name,
)

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)
