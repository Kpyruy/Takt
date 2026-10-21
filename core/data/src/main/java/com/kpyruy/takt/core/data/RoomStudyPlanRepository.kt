package com.kpyruy.takt.core.data

import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.database.CourseImportDecisionEntity
import com.kpyruy.takt.core.database.CourseIdAliasEntity
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.model.GradeLetter
import java.time.LocalDate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.util.Locale
import java.util.UUID

class RoomStudyPlanRepository(
    private val database: TaktDatabase,
    private val settingsRepository: AppSettingsRepository? = null,
) : StudyPlanRepository {
    private val dao = database.courseDao()
    override suspend fun upsertImportedCourses(courses: List<Course>, selectedLocalIds: Map<String, String>): Int {
        courses.forEach { if (!isCanonicalUisCourseId(it.id)) throw InvalidUisCourseId(it.code) }
        require(courses.map { it.id }.distinct().size == courses.size) { "Duplicate UIS course IDs. Try importing again." }
        require(courses.map { it.code.uppercase(Locale.ROOT) }.distinct().size == courses.size) {
            "UIS returned ambiguous course codes. Try importing again."
        }
        return database.withTransaction {
            val existing = dao.getAllSnapshot()
            val decisions = dao.getImportDecisionsSnapshot().map { it.courseId }.toSet()
            val adoptedTargets = dao.getAliasesSnapshot().map { it.courseId }.toSet() + decisions
            val imports = courses.map { course ->
                val target = existing.firstOrNull { it.id == course.id }
                val candidates = existing.filter { it.code.equals(course.code, true) &&
                    (!it.id.startsWith("uis:") || it.id.substringBeforeLast(':') == course.id.substringBeforeLast(':')) }
                val chosen = selectedLocalIds[course.id]?.let { selected ->
                    candidates.firstOrNull { it.id == selected }
                        ?: error("The selected course is no longer available for ${course.code}. Check again.")
                }
                val offline = candidates.filter { it.id != course.id }
                val source = chosen ?: when {
                    target != null && target.id in adoptedTargets -> target
                    candidates.size > 1 -> throw AmbiguousUisCourse(course.code, candidates.map { it.toDomain() })
                    offline.size == 1 -> offline.single()
                    else -> target
                }
                Triple(course, source, target)
            }
            imports.forEach { (course, previous, target) ->
                if (previous != null && previous.id != course.id) migrateReferences(previous.id, course.id)
                dao.insertAll(listOf(CourseEntity(
                    id = course.id,
                    code = course.code,
                    title = course.title,
                    credits = course.credits,
                    semester = course.semester,
                    status = course.status.storageValue,
                    requirementType = course.requirementType.name,
                    syllabusUrl = course.syllabusUrl,
                    gradingType = course.gradingType.name,
                    passFailResult = course.passFailResult?.name ?: previous?.passFailResult ?: target?.passFailResult,
                    iconKey = previous?.iconKey ?: target?.iconKey,
                    titleEn = course.titleEn ?: previous?.titleEn ?: target?.titleEn,
                    titleSk = course.titleSk ?: previous?.titleSk ?: target?.titleSk,
                    officialGrade = course.officialGrade?.name ?: previous?.officialGrade ?: target?.officialGrade,
                    fulfilledOnEpochDay = course.fulfilledOn?.toEpochDay() ?: previous?.fulfilledOnEpochDay ?: target?.fulfilledOnEpochDay,
                )))
                if (course.id in selectedLocalIds || previous?.id in decisions) {
                    dao.upsertImportDecisions(listOf(CourseImportDecisionEntity(course.id)))
                    if (previous != null && previous.id != course.id) dao.deleteImportDecision(previous.id)
                }
                if (previous != null && previous.id != course.id) {
                    dao.remapAliases(previous.id, course.id)
                    dao.upsertAliases(listOf(CourseIdAliasEntity(previous.id, course.id)))
                    dao.deleteById(previous.id)
                }
            }
            imports.size
        }
    }

    private suspend fun migrateReferences(oldId: String, newId: String) {
        val grade = database.gradeDao()
        val exam = database.examDao()
        // Single-course settings cannot be combined without choosing which personal value to retain.
        // Reject conflicting values before touching dependencies; the enclosing transaction preserves both courses.
        val scales = grade.getScalesSnapshot()
        val oldScale = scales.firstOrNull { it.courseId == oldId }
        val newScale = scales.firstOrNull { it.courseId == newId }
        require(oldScale == null || newScale == null || oldScale.copy(courseId = newId) == newScale) {
            "This course already has a different grading scale. Reconcile the scales before importing from UIS."
        }
        val overrides = grade.getOverridesSnapshot()
        val oldOverride = overrides.firstOrNull { it.courseId == oldId }
        val newOverride = overrides.firstOrNull { it.courseId == newId }
        require(oldOverride == null || newOverride == null || oldOverride.grade == newOverride.grade) {
            "This course already has a different manual result. Reconcile the results before importing from UIS."
        }
        val exams = exam.getExamInfoSnapshot()
        val oldExam = exams.firstOrNull { it.courseId == oldId }
        val newExam = exams.firstOrNull { it.courseId == newId }
        require(oldExam == null || newExam == null || oldExam.copy(courseId = newId) == newExam) {
            "This course already has different exam details. Reconcile the details before importing from UIS."
        }
        database.scheduleDao().remapRules(oldId, newId)
        database.scheduleDao().remapOneOffEvents(oldId, newId)
        database.studyContentDao().remapTasks(oldId, newId)
        database.studyContentDao().remapNotes(oldId, newId)
        grade.remapItems(oldId, newId)
        exam.remapMaterials(oldId, newId)
        oldScale?.let { grade.upsertScale(it.copy(courseId = newId)); grade.deleteScale(oldId) }
        oldOverride?.let { grade.upsertOverride(it.copy(courseId = newId)); grade.deleteOverride(oldId) }
        oldExam?.let { exam.upsertExamInfo(it.copy(courseId = newId)); exam.deleteExamInfo(oldId) }
    }

    override suspend fun importedCourseChoices(): Set<String> = dao.getImportDecisionsSnapshot().map { it.courseId }.toSet()

    override suspend fun addCourse(title: String, code: String, credits: Int, semester: Int): String {
        val cleanTitle = title.trim()
        val cleanCode = code.trim().uppercase(Locale.ROOT)
        require(cleanTitle.isNotBlank()) { "Enter the course name" }
        require(cleanCode.isNotBlank() && cleanCode.length <= 32 &&
            cleanCode.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }) {
            "Code: up to 32 letters, digits, dots or hyphens"
        }
        require(credits in 0..60) { "Credits must be between 0 and 60" }
        require(semester in 1..30) { "Semester must be between 1 and 30" }
        require(dao.getAllSnapshot().none { it.code.equals(cleanCode, ignoreCase = true) }) {
            "A course with this code already exists"
        }
        val id = UUID.randomUUID().toString()
        dao.insertAll(listOf(CourseEntity(id, cleanCode, cleanTitle, credits, semester,
            CourseStatus.ENROLLED.storageValue, CourseRequirementType.COMPULSORY.name, null)))
        return id
    }

    override fun observeCourses() = if (settingsRepository == null)
        dao.observeAll().map { items -> items.map(CourseEntity::toDomain) }
    else combine(dao.observeAll(), settingsRepository.settings) { items, settings ->
        items.map { settings.displayCourse(it.toDomain()) }
    }

    override fun observeSemester(semester: Int) = if (settingsRepository == null)
        dao.observeSemester(semester).map { items -> items.map(CourseEntity::toDomain) }
    else combine(dao.observeSemester(semester), settingsRepository.settings) { items, settings ->
        items.map { settings.displayCourse(it.toDomain()) }
    }

    override fun observeCourse(courseId: String) = if (settingsRepository == null)
        dao.observeById(courseId).map { it?.toDomain() }
    else combine(dao.observeById(courseId), settingsRepository.settings) { item, settings ->
        item?.toDomain()?.let(settings::displayCourse)
    }

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

    override suspend fun setOfficialResult(courseId: String, grade: GradeLetter?, fulfilledOn: LocalDate?) {
        database.withTransaction {
            val resolvedId = dao.getAliasesSnapshot().firstOrNull { it.oldId == courseId }?.courseId ?: courseId
            val existing = dao.getAllSnapshot().firstOrNull { it.id == resolvedId } ?: return@withTransaction
            dao.updateOfficialResult(courseId, grade?.name ?: existing.officialGrade,
                fulfilledOn?.toEpochDay() ?: existing.fulfilledOnEpochDay)
        }
    }
}

private fun com.kpyruy.takt.core.model.AppSettings.displayCourse(course: Course) =
    course.copy(title = displayCourseTitle(course))

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
    titleEn = titleEn,
    titleSk = titleSk,
    officialGrade = officialGrade?.let { runCatching { GradeLetter.valueOf(it) }.getOrNull() },
    fulfilledOn = fulfilledOnEpochDay?.let(LocalDate::ofEpochDay),
)
