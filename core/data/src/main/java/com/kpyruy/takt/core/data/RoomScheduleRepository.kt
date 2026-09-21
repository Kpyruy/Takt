package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.OneOffScheduleEventEntity
import com.kpyruy.takt.core.database.ScheduleDao
import com.kpyruy.takt.core.database.ScheduleExceptionEntity
import com.kpyruy.takt.core.database.ScheduleRuleEntity
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.OneOffScheduleEventType
import com.kpyruy.takt.core.model.ScheduleException
import com.kpyruy.takt.core.model.ScheduleExceptionType
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

    override fun observeExceptions() =
        dao.observeExceptions().map { items -> items.map { it.toDomain() } }

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

    override suspend fun deleteOneOffEvent(id: String) {
        dao.deleteOneOffEvent(id)
    }

    override suspend fun upsertException(exception: ScheduleException) {
        dao.upsertException(exception.toEntity())
    }

    override suspend fun deleteRule(id: String) {
        dao.deleteRule(id)
    }

    override suspend fun deleteException(id: String) {
        dao.deleteException(id)
    }

    override suspend fun deleteExceptionForOccurrence(ruleId: String, date: LocalDate) {
        dao.deleteExceptionForOccurrence(ruleId, date.toEpochDay())
    }
}

internal fun ScheduleRuleEntity.toDomain() = ScheduleRule(
    id = id,
    courseId = courseId,
    title = title,
    dayOfWeek = DayOfWeek.of(dayOfWeek),
    startTime = startMinute.toLocalTime(),
    endTime = endMinute.toLocalTime(),
    recurrence = ScheduleRecurrence.valueOf(recurrence),
    room = room,
)

internal fun ScheduleRule.toEntity() = ScheduleRuleEntity(
    id = id,
    courseId = courseId,
    title = title,
    dayOfWeek = dayOfWeek.value,
    startMinute = startTime.toMinuteOfDay(),
    endMinute = endTime.toMinuteOfDay(),
    recurrence = recurrence.name,
    room = room,
)

internal fun OneOffScheduleEventEntity.toDomain() = OneOffScheduleEvent(
    id = id,
    courseId = courseId,
    title = title,
    date = LocalDate.ofEpochDay(dateEpochDay),
    startTime = startMinute.toLocalTime(),
    endTime = endMinute.toLocalTime(),
    room = room,
    type = OneOffScheduleEventType.valueOf(type),
)

internal fun OneOffScheduleEvent.toEntity() = OneOffScheduleEventEntity(
    id = id,
    courseId = courseId,
    title = title,
    dateEpochDay = date.toEpochDay(),
    startMinute = startTime.toMinuteOfDay(),
    endMinute = endTime.toMinuteOfDay(),
    room = room,
    type = type.name,
)

internal fun ScheduleExceptionEntity.toDomain() = ScheduleException(
    id = id,
    ruleId = ruleId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    type = ScheduleExceptionType.valueOf(type),
    replacementDate = replacementDateEpochDay?.let(LocalDate::ofEpochDay),
    replacementStartTime = replacementStartMinute?.toLocalTime(),
    replacementEndTime = replacementEndMinute?.toLocalTime(),
    replacementRoom = replacementRoom,
)

internal fun ScheduleException.toEntity() = ScheduleExceptionEntity(
    id = id,
    ruleId = ruleId,
    dateEpochDay = date.toEpochDay(),
    type = type.name,
    replacementDateEpochDay = replacementDate?.toEpochDay(),
    replacementStartMinute = replacementStartTime?.toMinuteOfDay(),
    replacementEndMinute = replacementEndTime?.toMinuteOfDay(),
    replacementRoom = replacementRoom,
)

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)
