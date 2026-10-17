package com.kpyruy.takt.core.data

import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase

import com.kpyruy.takt.core.database.ExamInfoEntity
import com.kpyruy.takt.core.database.ExamMaterialEntity
import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.ExamMaterial
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.map

class RoomExamRepository(
    private val database: TaktDatabase,
) : ExamRepository {
    private val dao = database.examDao()
    override fun observeAllExamInfo() = database.invalidationTracker
        .createFlow("exam_info", emitInitialState = true)
        .map { dao.getExamInfoSnapshot().map { it.toDomain() } }
    override fun observeExamInfo(courseId: String) =
        dao.observeExamInfo(courseId).map { it?.toDomain() }

    override fun observeMaterials(courseId: String) =
        dao.observeMaterials(courseId).map { items -> items.map { it.toDomain() } }

    override suspend fun upsertExamInfo(info: ExamInfo) {
        database.withTransaction {
            dao.upsertExamInfo(info.copy(courseId = database.resolveCourseId(info.courseId)).toEntity())
        }
    }

    override suspend fun upsertMaterial(material: ExamMaterial) {
        database.withTransaction {
            dao.upsertMaterial(material.copy(courseId = database.resolveCourseId(material.courseId)).toEntity())
        }
    }

    override suspend fun deleteMaterial(id: String) {
        dao.deleteMaterial(id)
    }
}

internal fun ExamInfoEntity.toDomain() = ExamInfo(
    courseId = courseId,
    gradeItemId = gradeItemId,
    date = dateEpochDay?.let(LocalDate::ofEpochDay),
    startTime = startMinute?.let(::minuteToTime),
    endTime = endMinute?.let(::minuteToTime),
    room = room,
    attemptNumber = attemptNumber,
    maxAttempts = maxAttempts,
    notes = notes,
)

internal fun ExamInfo.toEntity() = ExamInfoEntity(
    courseId = courseId,
    gradeItemId = gradeItemId,
    dateEpochDay = date?.toEpochDay(),
    startMinute = startTime?.toMinuteOfDay(),
    endMinute = endTime?.toMinuteOfDay(),
    room = room,
    attemptNumber = attemptNumber,
    maxAttempts = maxAttempts,
    notes = notes,
)

internal fun ExamMaterialEntity.toDomain() = ExamMaterial(
    id = id,
    courseId = courseId,
    title = title,
    uri = uri,
)

internal fun ExamMaterial.toEntity() = ExamMaterialEntity(
    id = id,
    courseId = courseId,
    title = title,
    uri = uri,
)

private fun minuteToTime(minuteOfDay: Int): LocalTime =
    LocalTime.ofSecondOfDay(minuteOfDay.toLong() * 60L)

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
