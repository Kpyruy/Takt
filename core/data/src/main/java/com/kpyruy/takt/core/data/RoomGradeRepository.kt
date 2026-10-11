package com.kpyruy.takt.core.data

import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase

import com.kpyruy.takt.core.database.GradeItemEntity
import com.kpyruy.takt.core.database.GradeOverrideEntity
import com.kpyruy.takt.core.database.GradeScaleEntity
import com.kpyruy.takt.core.model.GradeBand
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeLetter
import com.kpyruy.takt.core.model.GradeScale
import java.time.LocalDate
import kotlinx.coroutines.flow.map

class RoomGradeRepository(
    private val database: TaktDatabase,
) : GradeRepository {
    private val dao = database.gradeDao()
    override fun observeItems(courseId: String) =
        dao.observeItems(courseId).map { items -> items.map { it.toDomain() } }

    override fun observeAllItems() =
        dao.observeAllItems().map { items -> items.map { it.toDomain() } }

    override fun observeRecentItems(limit: Int) =
        dao.observeRecentItems(limit).map { items -> items.map { it.toDomain() } }

    override fun observeScale(courseId: String) =
        dao.observeScale(courseId).map { it?.toDomain() ?: GradeScale.default() }

    override fun observeManualGrade(courseId: String) =
        dao.observeOverride(courseId).map { entity ->
            entity?.grade?.let { stored -> runCatching { GradeLetter.valueOf(stored) }.getOrNull() }
        }

    override suspend fun upsertItem(item: GradeItem) {
        database.withTransaction {
            dao.upsertItem(item.copy(courseId = database.resolveCourseId(item.courseId)).toEntity())
        }
    }

    override suspend fun deleteItem(id: String) {
        dao.deleteItem(id)
    }

    override suspend fun setScale(courseId: String, scale: GradeScale) {
        val minimums = scale.bands.associate { it.grade to it.minimumPercentage }
        database.withTransaction {
            val resolvedId = database.resolveCourseId(courseId)
            dao.upsertScale(
                GradeScaleEntity(
                    courseId = resolvedId,
                    aMin = minimums.getValue(GradeLetter.A),
                    bMin = minimums.getValue(GradeLetter.B),
                    cMin = minimums.getValue(GradeLetter.C),
                    dMin = minimums.getValue(GradeLetter.D),
                    eMin = minimums.getValue(GradeLetter.E),
                )
            )
        }
    }

    override suspend fun setManualGrade(courseId: String, grade: GradeLetter?) {
        database.withTransaction {
            val resolvedId = database.resolveCourseId(courseId)
            if (grade == null) dao.deleteOverride(resolvedId)
            else dao.upsertOverride(GradeOverrideEntity(courseId = resolvedId, grade = grade.name))
        }
    }
}

internal fun GradeItemEntity.toDomain() = GradeItem(
    id = id,
    courseId = courseId,
    title = title,
    type = GradeItemType.valueOf(type),
    earnedPoints = earnedPoints,
    maxPoints = maxPoints,
    recordedAtEpochMillis = recordedAtEpochMillis,
    dueDate = dueDateEpochDay?.let(LocalDate::ofEpochDay),
    completed = completed,
    requiredForExam = requiredForExam,
    minimumPointsForExam = minimumPointsForExam,
    lessonId = lessonId,
    durationMinutes = durationMinutes,
)

internal fun GradeItem.toEntity() = GradeItemEntity(
    id = id,
    courseId = courseId,
    title = title,
    type = type.name,
    earnedPoints = earnedPoints,
    maxPoints = maxPoints,
    recordedAtEpochMillis = recordedAtEpochMillis,
    dueDateEpochDay = dueDate?.toEpochDay(),
    completed = completed,
    requiredForExam = requiredForExam,
    minimumPointsForExam = minimumPointsForExam,
    lessonId = lessonId,
    durationMinutes = durationMinutes,
)

private fun GradeScaleEntity.toDomain() = GradeScale(
    listOf(
        GradeBand(GradeLetter.A, aMin),
        GradeBand(GradeLetter.B, bMin),
        GradeBand(GradeLetter.C, cMin),
        GradeBand(GradeLetter.D, dMin),
        GradeBand(GradeLetter.E, eMin),
        GradeBand(GradeLetter.FX, 0.0),
    )
)
