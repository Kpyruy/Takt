package com.kpyruy.takt.core.data

import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase

import com.kpyruy.takt.core.database.CourseNoteEntity
import com.kpyruy.takt.core.database.StudyTaskEntity
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.StudyTask
import java.time.LocalDate
import kotlinx.coroutines.flow.map

class RoomStudyContentRepository(
    private val database: TaktDatabase,
) : StudyContentRepository {
    private val dao = database.studyContentDao()
    override fun observeAllTasks() =
        dao.observeAllTasks().map { items -> items.map { it.toDomain() } }

    override fun observeTasks(courseId: String) =
        dao.observeTasks(courseId).map { items -> items.map { it.toDomain() } }

    override fun observeNotes(courseId: String) =
        dao.observeNotes(courseId).map { items -> items.map { it.toDomain() } }

    override suspend fun upsertTask(task: StudyTask) {
        database.withTransaction {
            dao.upsertTask(task.copy(courseId = database.resolveCourseId(task.courseId)).toEntity())
        }
    }

    override suspend fun setTaskCompleted(id: String, completed: Boolean) {
        dao.setTaskCompleted(id, completed)
    }

    override suspend fun deleteTask(id: String) {
        dao.deleteTask(id)
    }

    override suspend fun upsertNote(note: CourseNote) {
        database.withTransaction {
            dao.upsertNote(note.copy(courseId = database.resolveCourseId(note.courseId)).toEntity())
        }
    }

    override suspend fun deleteNote(id: String) {
        dao.deleteNote(id)
    }
}

internal fun StudyTaskEntity.toDomain() = StudyTask(
    id = id,
    courseId = courseId,
    title = title,
    description = description,
    dueDate = dueDateEpochDay?.let(LocalDate::ofEpochDay),
    completed = completed,
    requiredForExam = requiredForExam,
    earnedPoints = earnedPoints,
    maxPoints = maxPoints,
    minimumPointsForExam = minimumPointsForExam,
)

internal fun StudyTask.toEntity() = StudyTaskEntity(
    id = id,
    courseId = courseId,
    title = title,
    description = description,
    dueDateEpochDay = dueDate?.toEpochDay(),
    completed = completed,
    requiredForExam = requiredForExam,
    earnedPoints = earnedPoints,
    maxPoints = maxPoints,
    minimumPointsForExam = minimumPointsForExam,
)

internal fun CourseNoteEntity.toDomain() = CourseNote(
    id = id,
    courseId = courseId,
    title = title,
    content = content,
    updatedAtEpochMillis = updatedAtEpochMillis,
    attachments = NoteAttachmentCodec.decode(attachmentsJson),
)

internal fun CourseNote.toEntity() = CourseNoteEntity(
    id = id,
    courseId = courseId,
    title = title,
    content = content,
    updatedAtEpochMillis = updatedAtEpochMillis,
    attachmentsJson = NoteAttachmentCodec.encode(attachments),
)
