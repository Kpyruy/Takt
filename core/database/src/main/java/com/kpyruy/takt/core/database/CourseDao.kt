package com.kpyruy.takt.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY semester, title")
    fun observeAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semester = :semester ORDER BY title")
    fun observeSemester(semester: Int): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :courseId OR id = (SELECT courseId FROM course_id_aliases WHERE oldId = :courseId) LIMIT 1")
    fun observeById(courseId: String): Flow<CourseEntity?>

    @Query("SELECT * FROM courses ORDER BY semester, title")
    suspend fun getAllSnapshot(): List<CourseEntity>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseEntity>)

    @Query("UPDATE courses SET status = :status WHERE id = :courseId OR id = (SELECT courseId FROM course_id_aliases WHERE oldId = :courseId)")
    suspend fun updateStatus(courseId: String, status: String)

    @Query("UPDATE courses SET gradingType = :gradingType WHERE id = :courseId OR id = (SELECT courseId FROM course_id_aliases WHERE oldId = :courseId)")
    suspend fun updateGradingType(courseId: String, gradingType: String)

    @Query("UPDATE courses SET passFailResult = :result WHERE id = :courseId OR id = (SELECT courseId FROM course_id_aliases WHERE oldId = :courseId)")
    suspend fun updatePassFailResult(courseId: String, result: String?)

    @Query("UPDATE courses SET officialGrade = :grade, fulfilledOnEpochDay = :fulfilledOnEpochDay WHERE id = :courseId OR id = (SELECT courseId FROM course_id_aliases WHERE oldId = :courseId)")
    suspend fun updateOfficialResult(courseId: String, grade: String?, fulfilledOnEpochDay: Long?)

    @Query("UPDATE courses SET iconKey = :iconKey WHERE id = :courseId OR id = (SELECT courseId FROM course_id_aliases WHERE oldId = :courseId)")
    suspend fun updateIcon(courseId: String, iconKey: String?)

    @Query("UPDATE courses SET colorKey = :colorKey WHERE id = :courseId OR id = (SELECT courseId FROM course_id_aliases WHERE oldId = :courseId)")
    suspend fun updateColor(courseId: String, colorKey: String?)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteById(courseId: String)

    @Query("SELECT * FROM course_id_aliases")
    suspend fun getAliasesSnapshot(): List<CourseIdAliasEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAliases(items: List<CourseIdAliasEntity>)

    @Query("UPDATE course_id_aliases SET courseId = :newId WHERE courseId = :oldId")
    suspend fun remapAliases(oldId: String, newId: String)

    @Query("SELECT * FROM course_import_decisions")
    suspend fun getImportDecisionsSnapshot(): List<CourseImportDecisionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportDecisions(items: List<CourseImportDecisionEntity>)

    @Query("DELETE FROM course_import_decisions WHERE courseId = :courseId")
    suspend fun deleteImportDecision(courseId: String)

    @Query("DELETE FROM course_import_decisions")
    suspend fun deleteAllImportDecisions()

    @Query("DELETE FROM course_id_aliases")
    suspend fun deleteAllAliases()

    @Query("DELETE FROM courses")
    suspend fun deleteAll()
}
