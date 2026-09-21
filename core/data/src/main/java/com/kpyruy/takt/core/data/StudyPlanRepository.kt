package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import kotlinx.coroutines.flow.Flow

interface StudyPlanRepository {
    fun observeCourses(): Flow<List<Course>>
    fun observeSemester(semester: Int): Flow<List<Course>>
    suspend fun updateStatus(courseId: String, status: CourseStatus)
}
