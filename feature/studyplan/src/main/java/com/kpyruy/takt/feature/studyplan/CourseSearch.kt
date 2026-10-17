package com.kpyruy.takt.feature.studyplan

import com.kpyruy.takt.core.model.Course

/** Search the complete catalog, including alternate UIS names and closed semesters. */
internal fun searchCourses(courses: List<Course>, query: String): List<Course> {
    val term = query.trim()
    return courses.filter { course ->
        listOfNotNull(course.title, course.titleSk, course.titleEn, course.code)
            .any { it.contains(term, ignoreCase = true) }
    }.sortedWith(compareBy<Course> { it.semester }.thenBy { it.title })
}
