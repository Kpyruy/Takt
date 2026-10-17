package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.ExamMaterial
import kotlinx.coroutines.flow.Flow

interface ExamRepository {
    /** Includes scheduling metadata even when no scored assessment has a due date. */
    fun observeAllExamInfo(): Flow<List<ExamInfo>>

    fun observeExamInfo(courseId: String): Flow<ExamInfo?>
    fun observeMaterials(courseId: String): Flow<List<ExamMaterial>>
    suspend fun upsertExamInfo(info: ExamInfo)
    suspend fun upsertMaterial(material: ExamMaterial)
    suspend fun deleteMaterial(id: String)
}
