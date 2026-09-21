package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.CourseDao
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.PassFailResult
import kotlinx.coroutines.flow.map

class RoomStudyPlanRepository(private val dao: CourseDao) : StudyPlanRepository {
    override fun observeCourses() =
        dao.observeAll().map { items -> items.map(CourseEntity::toDomain) }

    override fun observeSemester(semester: Int) =
        dao.observeSemester(semester).map { items -> items.map(CourseEntity::toDomain) }

    override fun observeCourse(courseId: String) =
        dao.observeById(courseId).map { it?.toDomain() }

    override suspend fun updateStatus(courseId: String, status: CourseStatus) {
        dao.updateStatus(courseId, status.storageValue)
    }

    override suspend fun setGradingType(
        courseId: String,
        gradingType: CourseGradingType,
    ) {
        dao.updateGradingType(courseId, gradingType.name)
        if (gradingType != CourseGradingType.PASS_FAIL) {
            dao.updatePassFailResult(courseId, null)
        }
    }

    override suspend fun setPassFailResult(
        courseId: String,
        result: PassFailResult?,
    ) {
        dao.updatePassFailResult(courseId, result?.name)
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
    gradingType = runCatching { CourseGradingType.valueOf(gradingType) }
        .getOrDefault(CourseGradingType.CONTINUOUS_LETTER),
    passFailResult = passFailResult?.let { stored ->
        runCatching { PassFailResult.valueOf(stored) }.getOrNull()
    },
)
