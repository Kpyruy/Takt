package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.ExamMaterial
import kotlinx.coroutines.flow.Flow

interface ExamRepository {
    fun observeExamInfo(courseId: String): Flow<ExamInfo?>
    fun observeMaterials(courseId: String): Flow<List<ExamMaterial>>
    suspend fun upsertExamInfo(info: ExamInfo)
    suspend fun upsertMaterial(material: ExamMaterial)
    suspend fun deleteMaterial(id: String)
}
