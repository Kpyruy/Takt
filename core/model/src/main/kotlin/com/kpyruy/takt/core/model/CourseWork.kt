package com.kpyruy.takt.core.model

import java.time.LocalDate

object CourseWork {
    /** Explicit assessments take precedence over matching legacy scored tasks. */
    fun scoredItems(tasks: List<StudyTask>, assessments: List<GradeItem>): List<GradeItem> =
        assessments + tasksNotRepresentedByAssessments(tasks, assessments).mapNotNull { it.asScoredGradeItem() }

    fun tasksNotRepresentedByAssessments(
        tasks: List<StudyTask>,
        assessments: List<GradeItem>,
    ): List<StudyTask> {
        val availableMatches = assessments.asSequence()
            .filterNot { it.type == GradeItemType.EXAM }
            .groupingBy { WorkIdentity(it.courseId, it.title.trim().lowercase(), it.dueDate, it.maxPoints) }
            .eachCount().toMutableMap()
        return tasks.filter { task ->
            val maximum = task.maxPoints ?: return@filter true
            val key = WorkIdentity(task.courseId, task.title.trim().lowercase(), task.dueDate, maximum)
            val matches = availableMatches[key] ?: 0
            if (matches == 0) true else {
                availableMatches[key] = matches - 1
                false
            }
        }
    }

    private data class WorkIdentity(
        val courseId: String,
        val title: String,
        val dueDate: LocalDate?,
        val maxPoints: Double,
    )

    fun allSubmitted(tasks: List<StudyTask>, assessments: List<GradeItem>): Boolean =
        (tasks.isNotEmpty() || assessments.isNotEmpty()) &&
            tasksNotRepresentedByAssessments(tasks, assessments).all { it.completed } &&
            assessments.all { it.completed }

    /** The home task feed omits exams, which are presented separately there. */
    fun actionable(items: List<GradeItem>): List<GradeItem> =
        items.filterNot { it.type == GradeItemType.EXAM }

    fun hasTestOnLesson(
        event: ResolvedScheduleEvent,
        dayEvents: List<ResolvedScheduleEvent>,
        items: List<GradeItem>,
    ): Boolean {
        val courseId = event.courseId ?: return false
        if (event.status == ScheduleEventStatus.CANCELLED) return false
        val firstLessonId = dayEvents.asSequence()
            .filter { it.courseId == courseId && it.status != ScheduleEventStatus.CANCELLED }
            .minWithOrNull(compareBy<ResolvedScheduleEvent> { it.startTime }.thenBy { it.id })?.id
        return items.any { item ->
            item.courseId == courseId && item.dueDate == event.date &&
                (item.type == GradeItemType.TEST || item.type == GradeItemType.MIDTERM) &&
                (item.lessonId ?: firstLessonId) == event.id
        }
    }
}
