package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.StudyTask
import kotlinx.coroutines.flow.Flow

interface StudyContentRepository {
    fun observeAllTasks(): Flow<List<StudyTask>>
    fun observeTasks(courseId: String): Flow<List<StudyTask>>
    fun observeNotes(courseId: String): Flow<List<CourseNote>>
    suspend fun upsertTask(task: StudyTask)
    suspend fun setTaskCompleted(id: String, completed: Boolean)
    suspend fun deleteTask(id: String)
    suspend fun upsertNote(note: CourseNote)
    suspend fun deleteNote(id: String)
}
