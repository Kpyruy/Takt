package com.kpyruy.takt.feature.studyplan

import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseSearchTest {
    private val fulfilled = Course("uis-old", "TPAR_6B", "Theory of programming", 6, 1,
        CourseStatus.FULFILLED, titleSk = "Teória programovania", titleEn = "Theory of programming")
    private val enrolled = Course("uis-current", "POIS_6B", "Počítačové siete", 6, 5, CourseStatus.ENROLLED)
    private val planned = Course("local-plan", "DBS", "Databázy", 5, 6, CourseStatus.PLANNED)
    private val courses = listOf(planned, enrolled, fulfilled)

    @Test fun findsClosedCourseByCodeAcrossSemestersAndPreservesCanonicalId() {
        assertEquals(listOf("uis-old"), searchCourses(courses, "  tpar_6b  ").map { it.id })
    }

    @Test fun searchesAlternateUisNameEvenWhenDisplayedNameUsesAnotherLanguage() {
        assertEquals(listOf(fulfilled), searchCourses(courses, "teória"))
        assertEquals(listOf(fulfilled), searchCourses(courses, "PROGRAMMING"))
    }

    @Test fun clearingQueryRestoresCompleteCatalogWithoutChangingStatusesOrInputOrder() {
        assertEquals(emptyList<Course>(), searchCourses(courses, "missing"))
        assertEquals(listOf(fulfilled, enrolled, planned), searchCourses(courses, ""))
        assertEquals(listOf(planned, enrolled, fulfilled), courses)
        assertEquals(CourseStatus.FULFILLED, fulfilled.status)
    }
}
