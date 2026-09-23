package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CourseChoiceTest {
    @Test
    fun choicesIncludeOnlyActiveCoursesAcrossSemesters() {
        val courses = CourseStatus.entries.mapIndexed { index, status ->
            Course("course-$index", "C$index", "Course $index", 5, index + 1, status)
        } + Course("late-active", "L", "Late active", 5, 6, CourseStatus.ENROLLED)

        assertEquals(listOf("course-1", "late-active"), courses.activeCourseChoices().map { it.id })
        assertEquals(CourseStatus.entries.size + 1, courses.size)
    }
}
