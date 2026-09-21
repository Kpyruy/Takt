package com.kpyruy.takt.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyContentDao {
    @Query("SELECT * FROM study_tasks ORDER BY completed, dueDateEpochDay, title")
    fun observeAllTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE courseId = :courseId ORDER BY completed, dueDateEpochDay, title")
    fun observeTasks(courseId: String): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM course_notes WHERE courseId = :courseId ORDER BY updatedAtEpochMillis DESC")
    fun observeNotes(courseId: String): Flow<List<CourseNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(item: StudyTaskEntity)

    @Query("UPDATE study_tasks SET completed = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: String, completed: Boolean)

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(item: CourseNoteEntity)

    @Query("DELETE FROM course_notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}
