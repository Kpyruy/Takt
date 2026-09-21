package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeScale
import kotlinx.coroutines.flow.Flow

interface GradeRepository {
    fun observeItems(courseId: String): Flow<List<GradeItem>>
    fun observeRecentItems(limit: Int): Flow<List<GradeItem>>
    fun observeScale(courseId: String): Flow<GradeScale>
    suspend fun upsertItem(item: GradeItem)
    suspend fun deleteItem(id: String)
    suspend fun setScale(courseId: String, scale: GradeScale)
}
