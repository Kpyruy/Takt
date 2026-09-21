package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.CourseDao
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import kotlinx.coroutines.flow.map

class RoomStudyPlanRepository(private val dao: CourseDao) : StudyPlanRepository {
    override fun observeCourses() = dao.observeAll().map { items -> items.map(CourseEntity::toDomain) }

    override fun observeSemester(semester: Int) = dao.observeSemester(semester).map { items -> items.map(CourseEntity::toDomain) }

    override suspend fun updateStatus(courseId: String, status: CourseStatus) {
        dao.updateStatus(courseId, status.storageValue)
    }
}

private fun CourseEntity.toDomain() = Course(
    id = id,
    code = code,
    title = title,
    credits = credits,
    semester = semester,
    status = CourseStatus.fromStorage(status),
    requirementType = CourseRequirementType.valueOf(requirementType),
    syllabusUrl = syllabusUrl,
)
