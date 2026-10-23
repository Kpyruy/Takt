package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.model.GradeLetter
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface StudyPlanRepository {
    suspend fun upsertImportedCourses(courses: List<Course>, selectedLocalIds: Map<String, String> = emptyMap()): Int
    suspend fun importedCourseChoices(): Set<String>
    suspend fun addCourse(title: String, code: String, credits: Int, semester: Int): String
    fun observeCourses(): Flow<List<Course>>
    fun observeSemester(semester: Int): Flow<List<Course>>
    fun observeCourse(courseId: String): Flow<Course?>
    suspend fun setIcon(courseId: String, iconKey: String?)
    suspend fun setColor(courseId: String, colorKey: String?)
    suspend fun updateStatus(courseId: String, status: CourseStatus)
    suspend fun setGradingType(courseId: String, gradingType: CourseGradingType)
    suspend fun setPassFailResult(courseId: String, result: PassFailResult?)
    suspend fun setOfficialResult(courseId: String, grade: GradeLetter?, fulfilledOn: LocalDate?)
}

class InvalidUisCourseId(val code: String) : IllegalArgumentException(
    "UIS did not return a valid ID for ${code}. Try importing again.")

class AmbiguousUisCourse(val code: String, val candidates: List<Course>) : IllegalStateException(
    "There are several local courses for ${code}. Choose the matching course.")

fun isCanonicalUisCourseId(id: String): Boolean = Regex("uis:[1-9][0-9]*:[1-9][0-9]*").matches(id)

/** A code match is scoped to the selected study; local offline records have no study scope yet. */
fun Course.matchesUisCourse(remote: Course): Boolean = code.equals(remote.code, true) &&
    (!id.startsWith("uis:") || id.substringBeforeLast(':') == remote.id.substringBeforeLast(':'))

fun List<Course>.matchingUisCourse(remote: Course): Course? = firstOrNull { it.id == remote.id }
    ?: filter { it.matchesUisCourse(remote) }.singleOrNull()

/** Metadata refresh cannot bypass the user's automatic-progress policy. */
fun prepareAutomaticCourseImports(remote: List<Course>, local: List<Course>, importMetadata: Boolean): List<Course> =
    remote.mapNotNull { course ->
        val previous = local.matchingUisCourse(course)
        when {
            previous == null -> course.takeIf { importMetadata }
            !importMetadata -> previous.copy(id = course.id)
            else -> course.copy(status = previous.status, passFailResult = previous.passFailResult,
                officialGrade = previous.officialGrade, fulfilledOn = previous.fulfilledOn, colorKey = previous.colorKey)
        }
    }
