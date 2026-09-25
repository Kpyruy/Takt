package com.kpyruy.takt.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemesterPeriodTest {
    private val period = SemesterPeriod(
        studyStart = LocalDate.of(2026, 9, 1),
        studyEnd = LocalDate.of(2026, 12, 18),
        examStart = LocalDate.of(2027, 1, 11),
        examEnd = LocalDate.of(2027, 2, 5),
    )

    @Test fun recurringLessonsStopAtStudyEndIncludingHolidayGap() {
        assertTrue(period.allowsRecurringLesson(LocalDate.of(2026, 9, 1)))
        assertTrue(period.allowsRecurringLesson(LocalDate.of(2026, 12, 18)))
        assertFalse(period.allowsRecurringLesson(LocalDate.of(2026, 12, 19)))
        assertFalse(period.allowsRecurringLesson(LocalDate.of(2027, 1, 11)))
    }

    @Test fun automaticExamPhaseBeginsOnExamStartAfterBreak() {
        assertEquals(AssessmentPhase.STUDY, period.assessmentPhase(LocalDate.of(2027, 1, 10)))
        assertEquals(AssessmentPhase.EXAM, period.assessmentPhase(LocalDate.of(2027, 1, 11)))
        assertEquals(AssessmentPhase.EXAM, period.assessmentPhase(LocalDate.of(2027, 2, 6)))
    }

    @Test fun manualPhaseWinsAndUnconfiguredTermPreservesLessons() {
        assertEquals(AssessmentPhase.EXAM, period.copy(assessmentMode = AssessmentPhaseMode.EXAM)
            .assessmentPhase(LocalDate.of(2026, 10, 1)))
        assertEquals(AssessmentPhase.STUDY, period.copy(assessmentMode = AssessmentPhaseMode.STUDY)
            .assessmentPhase(LocalDate.of(2027, 1, 11)))
        assertTrue(SemesterPeriod().allowsRecurringLesson(LocalDate.of(2028, 1, 1)))
        assertFalse(period.copy(examEnd = LocalDate.of(2027, 1, 1)).hasValidDates())
    }

    @Test fun semesterLookupOnlyLimitsMatchingCourse() {
        val rule = ScheduleRule("r", "course-5", "Lesson", DayOfWeek.MONDAY,
            LocalTime.of(9, 0), LocalTime.of(10, 0), ScheduleRecurrence.WEEKLY)
        val settings = AppSettings(semesterPeriods = mapOf(3 to period))
        val late = LocalDate.of(2027, 3, 1)
        assertTrue(settings.allowsRecurringLesson(rule, mapOf("course-5" to 5), late))
        assertFalse(settings.allowsRecurringLesson(rule, mapOf("course-5" to 3), late))
    }

    @Test fun assessmentPhaseChangesLetterViewButNotPassFailFormat() {
        assertEquals(CourseGradingType.CONTINUOUS_LETTER,
            CourseGradingType.EXAM_LETTER.forPhase(AssessmentPhase.STUDY))
        assertEquals(CourseGradingType.EXAM_LETTER,
            CourseGradingType.CONTINUOUS_LETTER.forPhase(AssessmentPhase.EXAM))
        assertEquals(CourseGradingType.PASS_FAIL,
            CourseGradingType.PASS_FAIL.forPhase(AssessmentPhase.EXAM))
    }

    @Test fun unconfiguredSemesterKeepsLegacyExamCoursesInExamView() {
        val course = Course("legacy", "LEGACY", "Legacy exam", 5, 3, CourseStatus.ENROLLED,
            gradingType = CourseGradingType.EXAM_LETTER)
        val today = LocalDate.of(2026, 9, 25)
        assertEquals(AssessmentPhase.EXAM, AppSettings().assessmentPhase(course, today))
        assertEquals(AssessmentPhase.STUDY,
            AppSettings(semesterPeriods = mapOf(3 to period)).assessmentPhase(course, today))
    }
}
