package com.kpyruy.takt.feature.subjects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.ResolvedScheduleEvent
import com.kpyruy.takt.core.model.ScheduleEventStatus
import com.kpyruy.takt.core.model.ScheduleResolver
import java.time.LocalDateTime

/** Uses the same parity, period, exception and visibility rules as the calendar. */
@Composable
internal fun rememberNextCourseLesson(
    repository: ScheduleRepository,
    settings: AppSettings,
    courses: List<Course>,
    courseId: String,
    now: LocalDateTime,
): ResolvedScheduleEvent? {
    val rules by remember(repository) { repository.observeRules() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val exceptions by remember(repository) { repository.observeExceptions() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val oneOffEvents by remember(repository) { repository.observeOneOffEvents() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val absences by remember(repository) { repository.observeAbsences() }.collectAsStateWithLifecycle(initialValue = emptyList())
    return remember(rules, exceptions, oneOffEvents, absences, settings, courses, courseId, now) {
        val currentSemester = settings.effectiveCurrentSemester(courses)
        val semesters = courses.associate { it.id to settings.academicSemester(it, currentSemester) }
        (0L..28L).asSequence().flatMap { offset ->
            val date = now.toLocalDate().plusDays(offset)
            settings.filterScheduleEvents(ScheduleResolver.eventsForDate(
                rules, exceptions, oneOffEvents, date, settings.effectiveParity(date), absences,
                ruleAllowed = { rule, occurrence -> settings.allowsRecurringLesson(rule, semesters, occurrence) },
            )).asSequence()
        }.filter { it.courseId == courseId && it.status != ScheduleEventStatus.CANCELLED && !it.isAbsent &&
            LocalDateTime.of(it.date, it.endTime).isAfter(now) }
            .minWithOrNull(compareBy<ResolvedScheduleEvent> { it.date }.thenBy { it.startTime }.thenBy { it.id })
    }
}
