package com.kpyruy.takt.feature.studyplan

import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class SemesterStateTest {
    @After fun resetLanguage() = TaktI18n.use(AppLanguage.ENGLISH)

    @Test fun activeSubjectCountsUseUkrainianForms() {
        TaktI18n.use(AppLanguage.UKRAINIAN)
        assertEquals("1 активний предмет", activeCoursesLabel(1))
        assertEquals("2 активні предмети", activeCoursesLabel(2))
        assertEquals("5 активних предметів", activeCoursesLabel(5))
        assertEquals("11 активних предметів", activeCoursesLabel(11))
        assertEquals("21 активний предмет", activeCoursesLabel(21))
    }

    @Test fun enrolledCoursesInSeveralTermsDoNotCreateSeveralCurrentSemesters() {
        val retake = listOf(Course("old", "OLD", "Retake", 5, 3, CourseStatus.ENROLLED))
        val current = listOf(Course("now", "NOW", "Current", 5, 5, CourseStatus.ENROLLED))

        assertEquals(SemesterState.FUTURE, semesterState(retake, isCurrent = false))
        assertEquals(SemesterState.ACTIVE, semesterState(current, isCurrent = true))
    }
}
