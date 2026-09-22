package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.CourseNoteEntity
import com.kpyruy.takt.core.database.StudyContentDao
import com.kpyruy.takt.core.database.StudyTaskEntity
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.StudyTask
import java.time.LocalDate
import kotlinx.coroutines.flow.map

class RoomStudyContentRepository(
    private val dao: StudyContentDao,
) : StudyContentRepository {
    override fun observeAllTasks() =
        dao.observeAllTasks().map { items -> items.map { it.toDomain() } }

    override fun observeTasks(courseId: String) =
        dao.observeTasks(courseId).map { items -> items.map { it.toDomain() } }

    override fun observeNotes(courseId: String) =
        dao.observeNotes(courseId).map { items -> items.map { it.toDomain() } }

    override suspend fun upsertTask(task: StudyTask) {
        dao.upsertTask(task.toEntity())
    }

    override suspend fun setTaskCompleted(id: String, completed: Boolean) {
        dao.setTaskCompleted(id, completed)
    }

    override suspend fun deleteTask(id: String) {
        dao.deleteTask(id)
    }

    override suspend fun upsertNote(note: CourseNote) {
        dao.upsertNote(note.toEntity())
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
)

internal fun StudyTask.toEntity() = StudyTaskEntity(
    id = id,
    courseId = courseId,
    title = title,
    description = description,
    dueDateEpochDay = dueDate?.toEpochDay(),
    completed = completed,
    requiredForExam = requiredForExam,
)

internal fun CourseNoteEntity.toDomain() = CourseNote(
    id = id,
    courseId = courseId,
    title = title,
    content = content,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun CourseNote.toEntity() = CourseNoteEntity(
    id = id,
    courseId = courseId,
    title = title,
    content = content,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
