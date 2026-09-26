package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.PassFailResult
import kotlinx.coroutines.flow.Flow

interface StudyPlanRepository {
    suspend fun addCourse(title: String, code: String, credits: Int, semester: Int): String
    fun observeCourses(): Flow<List<Course>>
    fun observeSemester(semester: Int): Flow<List<Course>>
    fun observeCourse(courseId: String): Flow<Course?>
    suspend fun setIcon(courseId: String, iconKey: String?)
    suspend fun updateStatus(courseId: String, status: CourseStatus)
    suspend fun setGradingType(courseId: String, gradingType: CourseGradingType)
    suspend fun setPassFailResult(courseId: String, result: PassFailResult?)
}
