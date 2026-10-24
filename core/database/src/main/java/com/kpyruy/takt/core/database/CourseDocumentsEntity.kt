package com.kpyruy.takt.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "course_documents")
data class CourseDocumentsEntity(@PrimaryKey val id: Int = 1, val payloadJson: String)

@Dao
interface CourseDocumentsDao {
    @Query("SELECT * FROM course_documents WHERE id = 1") fun observe(): Flow<CourseDocumentsEntity?>
    @Query("SELECT * FROM course_documents WHERE id = 1") suspend fun read(): CourseDocumentsEntity?
    @Upsert suspend fun put(value: CourseDocumentsEntity)
    @Query("DELETE FROM course_documents") suspend fun clear()
}
