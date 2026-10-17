package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.*

data class PlannedReminder(
    val id: String,
    val sourceKey: String,
    val category: ReminderCategory,
    val title: String,
    val courseTitle: String?,
    val occursAt: Instant,
    val fireAt: Instant,
    val offsetMinutes: Long,
    val dateOnly: Boolean,
    /** Local source target, independent of the device timezone used to arm this generation. */
    val localTarget: LocalDateTime? = null,
) {
    /** Edits to time replace an alarm, while rename and timezone changes keep its identity. */
    val deliveryKey: String get() = localTarget?.let { "$id@local:$it" } ?: "$id@${fireAt.toEpochMilli()}"
}

/** Pure projection; it never writes sources, grading, attendance, IDs, or global UI language. */
object ReminderPlanner {
    const val HORIZON_DAYS = 35L

    fun plan(snapshot: PlanningSnapshot, now: Instant, zone: ZoneId,
        horizonDays: Long = HORIZON_DAYS): List<PlannedReminder> {
        val settings = snapshot.settings
        val config = settings.reminders.normalized()
        if (!config.enabled) return emptyList()
        require(horizonDays in 1..366)
        val lastFire = now.atZone(zone).plusDays(horizonDays).toInstant()
        val courses = snapshot.courses.associateBy { it.id }
        val currentSemester = settings.effectiveCurrentSemester(snapshot.courses)
        val semesters = snapshot.courses.associate { it.id to settings.academicSemester(it, currentSemester) }
        val oneOff = snapshot.oneOffEvents.associateBy { it.id }
        val resolved = mutableMapOf<LocalDate, List<ResolvedScheduleEvent>>()
        fun events(date: LocalDate) = resolved.getOrPut(date) {
            settings.filterScheduleEvents(ScheduleResolver.eventsForDate(snapshot.rules, snapshot.exceptions,
                snapshot.oneOffEvents, date, settings.effectiveParity(date), snapshot.absences,
                ruleAllowed = { rule, day -> settings.allowsRecurringLesson(rule, semesters, day) }))
        }
        val result = mutableListOf<PlannedReminder>()
        fun add(source: String, category: ReminderCategory, title: String, courseId: String?,
            target: LocalDateTime, dateOnly: Boolean) {
            if (!config.allows(category)) return
            val occursAt = target.atZone(zone).toInstant()
            if (!occursAt.isAfter(now)) return
            config.rule(category).offsetsMinutes.forEach { offset ->
                // Whole day/week leads use the local calendar; minute/hour leads use elapsed time.
                val fireAt = if (offset % 1440L == 0L) target.minusDays(offset / 1440L).atZone(zone).toInstant()
                    else occursAt.minusSeconds(offset * 60L)
                if (fireAt.isAfter(now) && !fireAt.isAfter(lastFire)) {
                    result += PlannedReminder(stableId(source, category, offset), source, category, title,
                        courseId?.let(courses::get)?.let(settings::displayCourseTitle), occursAt, fireAt, offset, dateOnly, target)
                }
            }
        }

        // Match against all assessments, including submitted ones, before excluding finished work.
        CourseWork.tasksNotRepresentedByAssessments(snapshot.tasks, snapshot.assessments)
            .filterNot { it.completed }.forEach { task ->
                task.dueDate?.let { date -> add("task:${task.id}:$date", ReminderCategory.TASK,
                    task.title, task.courseId, date.atTime(config.dateOnlyTime), true) }
            }
        // Metadata is the scheduling authority for its linked result, including an unknown date.
        // No synthetic GradeItem/points budget: a standalone exam is a notification source only.
        val representedExams = mutableSetOf<String>()
        snapshot.examInfos.forEach { info ->
            val exams = snapshot.assessments.filter { it.courseId == info.courseId && it.type == GradeItemType.EXAM }
            val linked = if (info.gradeItemId != null) exams.firstOrNull { it.id == info.gradeItemId }
                else exams.singleOrNull() ?: exams.filter { it.dueDate == info.date }.singleOrNull()
            linked?.let { representedExams += it.id }
            if (linked?.isSubmitted == true || (info.gradeItemId == null && exams.isNotEmpty() && exams.all { it.isSubmitted })) return@forEach
            val date = info.date ?: return@forEach
            val target = date.atTime(info.startTime ?: config.dateOnlyTime)
            add("exam:${info.courseId}:${linked?.id ?: info.gradeItemId ?: "metadata"}", ReminderCategory.EXAM,
                linked?.title ?: courses[info.courseId]?.let(settings::displayCourseTitle).orEmpty(),
                info.courseId, target, info.startTime == null)
        }
        snapshot.assessments.filterNot { it.isSubmitted || it.id in representedExams }.forEach { item ->
            val date = item.dueDate ?: return@forEach
            val category = ReminderCategory.forAssessment(item.type)
            var target = date.atTime(config.dateOnlyTime)
            var dateOnly = true
            if (item.type == GradeItemType.TEST || item.type == GradeItemType.MIDTERM) {
                // Older links save sourceDate; a test created after moving saves the actual date.
                val exception = item.lessonId?.let { id -> snapshot.exceptions.firstOrNull { it.ruleId == id && it.date == date } }
                if (exception?.type == ScheduleExceptionType.CANCELLED) return@forEach
                val targetDate = exception?.replacementDate ?: date
                val candidates = events(targetDate).filter { event -> event.courseId == item.courseId &&
                    (item.lessonId == null || (event.id == item.lessonId &&
                        ((event.sourceDate ?: event.date) == date || event.date == date))) }
                val linked = if (item.lessonId == null) candidates.filter { it.status != ScheduleEventStatus.CANCELLED && !it.isAbsent }
                    .minWithOrNull(compareBy<ResolvedScheduleEvent> { it.startTime }.thenBy { it.id })
                else candidates.firstOrNull()
                if (linked?.status == ScheduleEventStatus.CANCELLED || linked?.isAbsent == true) return@forEach
                if (item.lessonId != null && linked == null) return@forEach
                if (linked != null) { target = linked.date.atTime(linked.startTime); dateOnly = false }
            }
            add("assessment:${item.id}:$date", category, item.title, item.courseId, target, dateOnly)
        }

        val scheduleCategories = listOf(ReminderCategory.LESSON, ReminderCategory.EVENT, ReminderCategory.REMINDER)
        val longest = scheduleCategories.filter(config::allows).flatMap { config.rule(it).offsetsMinutes }.maxOrNull()
        if (longest != null) {
            val from = now.atZone(zone).toLocalDate()
            val through = lastFire.atZone(zone).toLocalDate().plusDays((longest + 1439) / 1440 + 1)
            var date = from
            while (!date.isAfter(through)) {
                events(date).filter { it.status != ScheduleEventStatus.CANCELLED && !it.isAbsent }.forEach { event ->
                    val original = if (event.status == ScheduleEventStatus.ONE_OFF) oneOff[event.id] else null
                    val category = when {
                        original?.type == OneOffScheduleEventType.REMINDER -> ReminderCategory.REMINDER
                        event.lessonType == LessonType.EVENT -> ReminderCategory.EVENT
                        else -> ReminderCategory.LESSON
                    }
                    val source = if (original != null) "oneoff:${event.id}:${event.sourceDate ?: event.date}"
                        else "lesson:${event.id}:${event.sourceDate ?: event.date}"
                    val title = if (event.id.startsWith("uis:")) event.courseId?.let(courses::get)
                        ?.let(settings::displayCourseTitle) ?: event.title else event.title
                    add(source, category, title, event.courseId, event.date.atTime(event.startTime), false)
                }
                date = date.plusDays(1)
            }
        }
        return result.distinctBy { it.id }.sortedWith(compareBy<PlannedReminder> { it.fireAt }.thenBy { it.id })
    }

    private fun stableId(source: String, category: ReminderCategory, offset: Long): String =
        MessageDigest.getInstance("SHA-256").digest("$source|${category.name}|$offset".toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
