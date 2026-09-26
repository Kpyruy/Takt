package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.CourseDao
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.PassFailResult
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID

class RoomStudyPlanRepository(private val dao: CourseDao) : StudyPlanRepository {
    override suspend fun addCourse(title: String, code: String, credits: Int, semester: Int): String {
        val cleanTitle = title.trim()
        val cleanCode = code.trim().uppercase(Locale.ROOT)
        require(cleanTitle.isNotBlank()) { "Вкажи назву предмета" }
        require(cleanCode.isNotBlank() && cleanCode.length <= 32 &&
            cleanCode.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }) {
            "Код: до 32 літер, цифр, крапок або дефісів"
        }
        require(credits in 0..60) { "Кредити мають бути від 0 до 60" }
        require(semester in 1..30) { "Семестр має бути від 1 до 30" }
        require(dao.getAllSnapshot().none { it.code.equals(cleanCode, ignoreCase = true) }) {
            "Предмет з таким кодом уже є"
        }
        val id = UUID.randomUUID().toString()
        dao.insertAll(listOf(CourseEntity(id, cleanCode, cleanTitle, credits, semester,
            CourseStatus.ENROLLED.storageValue, CourseRequirementType.COMPULSORY.name, null)))
        return id
    }

    override fun observeCourses() =
        dao.observeAll().map { items -> items.map(CourseEntity::toDomain) }

    override fun observeSemester(semester: Int) =
        dao.observeSemester(semester).map { items -> items.map(CourseEntity::toDomain) }

    override fun observeCourse(courseId: String) =
        dao.observeById(courseId).map { it?.toDomain() }

    override suspend fun setIcon(courseId: String, iconKey: String?) {
        dao.updateIcon(courseId, iconKey)
    }

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
    iconKey = iconKey,
    gradingType = runCatching { CourseGradingType.valueOf(gradingType) }
        .getOrDefault(CourseGradingType.CONTINUOUS_LETTER),
    passFailResult = passFailResult?.let { stored ->
        runCatching { PassFailResult.valueOf(stored) }.getOrNull()
    },
)
