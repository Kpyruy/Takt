package com.kpyruy.takt.core.data

import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.database.LessonAbsenceEntity
import com.kpyruy.takt.core.model.LessonAbsence
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.absenceKey
import com.kpyruy.takt.core.database.OneOffScheduleEventEntity
import com.kpyruy.takt.core.database.ScheduleDao
import com.kpyruy.takt.core.database.ScheduleExceptionEntity
import com.kpyruy.takt.core.database.ScheduleRuleEntity
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.OneOffScheduleEventType
import com.kpyruy.takt.core.model.ScheduleException
import com.kpyruy.takt.core.model.ScheduleExceptionType
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.model.ScheduleRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.map

class RoomScheduleRepository(
    private val database: TaktDatabase,
    private val dao: ScheduleDao = database.scheduleDao(),
) : ScheduleRepository {
    override fun observeAbsences() = dao.observeAbsences().map { items -> items.map { LessonAbsence(it.eventId, LocalDate.ofEpochDay(it.dateEpochDay), it.isOneOff) } }

    override suspend fun setAbsent(event: ResolvedScheduleEvent, absent: Boolean) {
        val key = event.absenceKey()
        if (absent) dao.upsertAbsences(listOf(LessonAbsenceEntity(key.eventId, key.date.toEpochDay(), key.isOneOff)))
        else dao.deleteAbsence(key.eventId, key.date.toEpochDay(), key.isOneOff)
    }

    override fun observeRules() = dao.observeRules().map { items -> items.map { it.toDomain() } }

    override fun observeOneOffEvents() =
        dao.observeOneOffEvents().map { items -> items.map { it.toDomain() } }

    override fun observeExceptions() =
        dao.observeExceptions().map { items -> items.map { it.toDomain() } }

    override suspend fun replaceSchedule(rules: List<ScheduleRule>, events: List<OneOffScheduleEvent>) {
        require(rules.map { it.id }.distinct().size == rules.size && events.map { it.id }.distinct().size == events.size) {
            "UIS повернув заняття з повторними ідентифікаторами. Повтори перевірку."
        }
        require(rules.all { it.id.isNotBlank() && it.title.isNotBlank() && it.startTime < it.endTime } &&
            events.all { it.id.isNotBlank() && it.title.isNotBlank() && it.startTime < it.endTime }) {
            "UIS повернув некоректний розклад. Повтори перевірку."
        }
        val newRules = rules.map { it.toEntity() }
        val newEvents = events.map { it.toEntity() }
        database.withTransaction {
            val courseIds = database.courseDao().getAllSnapshot().map { it.id }.toSet()
            val aliases = database.courseDao().getAliasesSnapshot().associate { it.oldId to it.courseId }
            val preparedRules = newRules.map { it.copy(courseId = it.courseId?.let { id -> aliases[id] ?: id }) }
            val preparedEvents = newEvents.map { it.copy(courseId = it.courseId?.let { id -> aliases[id] ?: id }) }
            require((preparedRules.mapNotNull { it.courseId } + preparedEvents.mapNotNull { it.courseId }).all { it in courseIds }) {
                "Спочатку імпортуй відповідні предмети UIS, потім повтори заміну розкладу."
            }
            dao.deleteAllExceptions()
            dao.deleteAllAbsences()
            dao.deleteAllRules()
            dao.deleteAllOneOffEvents()
            dao.upsertRules(preparedRules)
            dao.upsertOneOffEvents(preparedEvents)
        }
    }

    override suspend fun upsertRule(rule: ScheduleRule) {
        database.withTransaction {
            val actualId = rule.courseId?.let { database.resolveCourseId(it) }
            dao.upsertRule(rule.copy(courseId = actualId).toEntity())
        }
    }

    override suspend fun upsertRules(rules: List<ScheduleRule>) {
        database.withTransaction {
            val prepared = rules.map { item ->
                item.copy(courseId = item.courseId?.let { database.resolveCourseId(it) }).toEntity()
            }
            dao.upsertRules(prepared)
        }
    }

    override suspend fun upsertOneOffEvent(event: OneOffScheduleEvent) {
        database.withTransaction {
            val actualId = event.courseId?.let { database.resolveCourseId(it) }
            dao.upsertOneOffEvent(event.copy(courseId = actualId).toEntity())
        }
    }

    override suspend fun upsertOneOffEvents(events: List<OneOffScheduleEvent>) {
        database.withTransaction {
            val prepared = events.map { item ->
                item.copy(courseId = item.courseId?.let { database.resolveCourseId(it) }).toEntity()
            }
            dao.upsertOneOffEvents(prepared)
        }
    }

    override suspend fun convertRuleToOneOff(ruleId: String, event: OneOffScheduleEvent) {
        require(event.id == ruleId)
        database.withTransaction {
            upsertOneOffEvent(event)
            deleteRule(ruleId)
        }
    }

    override suspend fun convertOneOffToRule(eventId: String, rule: ScheduleRule) {
        require(rule.id == eventId)
        database.withTransaction {
            upsertRule(rule)
            deleteOneOffEvent(eventId)
        }
    }

    override suspend fun deleteOneOffEvent(id: String) = database.withTransaction {
        dao.deleteOneOffEvent(id)
        dao.deleteEventAbsences(id, true)
    }

    override suspend fun upsertException(exception: ScheduleException) {
        dao.upsertException(exception.toEntity())
    }

    override suspend fun deleteRule(id: String) = database.withTransaction {
        dao.deleteRule(id)
        dao.deleteRuleExceptions(id)
        dao.deleteEventAbsences(id, false)
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
    lessonType = LessonType.fromStorage(lessonType),
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
    lessonType = lessonType.name,
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
    lessonType = LessonType.fromStorage(lessonType),
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
    lessonType = lessonType.name,
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
