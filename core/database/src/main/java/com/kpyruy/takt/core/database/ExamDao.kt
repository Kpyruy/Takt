package com.kpyruy.takt.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exam_info WHERE courseId = :courseId LIMIT 1")
    fun observeExamInfo(courseId: String): Flow<ExamInfoEntity?>

    @Query("SELECT * FROM exam_materials WHERE courseId = :courseId ORDER BY title")
    fun observeMaterials(courseId: String): Flow<List<ExamMaterialEntity>>

    @Query("SELECT * FROM exam_info ORDER BY courseId")
    suspend fun getExamInfoSnapshot(): List<ExamInfoEntity>

    @Query("SELECT * FROM exam_materials ORDER BY courseId, title")
    suspend fun getMaterialsSnapshot(): List<ExamMaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExamInfo(item: ExamInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExamInfo(items: List<ExamInfoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterial(item: ExamMaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterials(items: List<ExamMaterialEntity>)

    @Query("DELETE FROM exam_materials WHERE id = :id")
    suspend fun deleteMaterial(id: String)

    @Query("DELETE FROM exam_info")
    suspend fun deleteAllExamInfo()

    @Query("DELETE FROM exam_materials")
    suspend fun deleteAllMaterials()
}
