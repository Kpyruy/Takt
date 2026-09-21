package com.kpyruy.takt.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Query("SELECT * FROM grade_items WHERE courseId = :courseId ORDER BY recordedAtEpochMillis DESC, title")
    fun observeItems(courseId: String): Flow<List<GradeItemEntity>>

    @Query("SELECT * FROM grade_items ORDER BY recordedAtEpochMillis DESC, title LIMIT :limit")
    fun observeRecentItems(limit: Int): Flow<List<GradeItemEntity>>

    @Query("SELECT * FROM grade_scales WHERE courseId = :courseId LIMIT 1")
    fun observeScale(courseId: String): Flow<GradeScaleEntity?>

    @Query("SELECT * FROM grade_overrides WHERE courseId = :courseId LIMIT 1")
    fun observeOverride(courseId: String): Flow<GradeOverrideEntity?>

    @Query("SELECT * FROM grade_items ORDER BY recordedAtEpochMillis DESC, title")
    suspend fun getItemsSnapshot(): List<GradeItemEntity>

    @Query("SELECT * FROM grade_scales ORDER BY courseId")
    suspend fun getScalesSnapshot(): List<GradeScaleEntity>

    @Query("SELECT * FROM grade_overrides ORDER BY courseId")
    suspend fun getOverridesSnapshot(): List<GradeOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: GradeItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<GradeItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScale(scale: GradeScaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScales(scales: List<GradeScaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(item: GradeOverrideEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverrides(items: List<GradeOverrideEntity>)

    @Query("DELETE FROM grade_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM grade_overrides WHERE courseId = :courseId")
    suspend fun deleteOverride(courseId: String)

    @Query("DELETE FROM grade_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM grade_scales")
    suspend fun deleteAllScales()

    @Query("DELETE FROM grade_overrides")
    suspend fun deleteAllOverrides()
}
