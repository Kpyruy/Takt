package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CourseWorkTest {
    @Test fun actionableWorkIncludesTestsLabsAndProjectsButNotExams() {
        val items = listOf(
            grade("test", GradeItemType.TEST),
            grade("lab", GradeItemType.LAB),
            grade("project", GradeItemType.PROJECT),
            grade("exam", GradeItemType.EXAM),
        )

        assertEquals(listOf("test", "lab", "project"), CourseWork.actionable(items).map { it.id })
    }

    @Test fun testMarkerMatchesOnlySubjectAndTestDate() {
        val friday = LocalDate.of(2026, 9, 25)
        val test = grade("test", GradeItemType.TEST).copy(dueDate = friday, lessonId = "lesson")
        val lesson = ResolvedScheduleEvent(
            id = "lesson", courseId = "course", title = "Lesson", date = friday,
            startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 30),
            room = null, status = ScheduleEventStatus.NORMAL,
        )

        val lessons = listOf(lesson, lesson.copy(id = "later", startTime = LocalTime.of(12, 0)))
        assertTrue(CourseWork.hasTestOnLesson(lesson, lessons, listOf(test)))
        assertFalse(CourseWork.hasTestOnLesson(lessons[1], lessons, listOf(test)))
        assertFalse(CourseWork.hasTestOnLesson(lesson.copy(date = friday.plusWeeks(1)), lessons, listOf(test)))
        assertFalse(CourseWork.hasTestOnLesson(lesson.copy(courseId = "other"), lessons, listOf(test)))
        assertFalse(CourseWork.hasTestOnLesson(lesson.copy(status = ScheduleEventStatus.CANCELLED), lessons, listOf(test)))
        assertFalse(CourseWork.hasTestOnLesson(lesson, lessons, listOf(test.copy(type = GradeItemType.LAB))))
        val legacy = test.copy(lessonId = null)
        assertTrue(CourseWork.hasTestOnLesson(lesson, lessons, listOf(legacy)))
        assertFalse(CourseWork.hasTestOnLesson(lessons[1], lessons, listOf(legacy)))
    }

    private fun grade(id: String, type: GradeItemType) = GradeItem(
        id = id,
        courseId = "course",
        title = id,
        type = type,
        earnedPoints = 0.0,
        maxPoints = 10.0,
        completed = false,
    )
}
