package com.kpyruy.takt.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "student_hub")
data class StudentHubEntity(@PrimaryKey val id: Int = 1, val payloadJson: String)

@Dao
interface StudentHubDao {
    @Query("SELECT * FROM student_hub WHERE id = 1")
    fun observe(): Flow<StudentHubEntity?>
    @Query("SELECT * FROM student_hub WHERE id = 1")
    suspend fun read(): StudentHubEntity?
    @Upsert suspend fun put(value: StudentHubEntity)
    @Query("DELETE FROM student_hub") suspend fun clear()
}
