package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.LessonAbsence
import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.ScheduleException
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.model.StudyTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** A complete first frame for Home and Calendar; null means Room has not emitted yet. */
data class PlanningSnapshot(
    val courses: List<Course>,
    val absences: List<LessonAbsence>,
    val rules: List<ScheduleRule>,
    val oneOffEvents: List<OneOffScheduleEvent>,
    val exceptions: List<ScheduleException>,
    val tasks: List<StudyTask>,
    val assessments: List<GradeItem>,
    val settings: AppSettings,
)

private data class SchedulePart(
    val courses: List<Course>,
    val absences: List<LessonAbsence>,
    val rules: List<ScheduleRule>,
    val oneOffEvents: List<OneOffScheduleEvent>,
    val exceptions: List<ScheduleException>,
)

fun observePlanningSnapshot(
    studyPlan: StudyPlanRepository,
    schedule: ScheduleRepository,
    content: StudyContentRepository,
    grades: GradeRepository,
    preferences: AppSettingsRepository,
): Flow<PlanningSnapshot> {
    val schedulePart = combine(
        studyPlan.observeCourses(), schedule.observeAbsences(), schedule.observeRules(),
        schedule.observeOneOffEvents(), schedule.observeExceptions(),
    ) { courses, absences, rules, oneOffEvents, exceptions ->
        SchedulePart(courses, absences, rules, oneOffEvents, exceptions)
    }
    return combine(
        schedulePart, content.observeAllTasks(), grades.observeAllItems(), preferences.settings,
    ) { base, tasks, assessments, settings ->
        PlanningSnapshot(base.courses, base.absences, base.rules, base.oneOffEvents,
            base.exceptions, tasks, assessments, settings)
    }
}
