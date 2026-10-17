package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReminderPlannerTest {
    private val zone = ZoneId.of("Europe/Bratislava")
    private val now = Instant.parse("2026-10-14T06:00:00Z")
    private val date = LocalDate.of(2026, 10, 22)
    private val course = Course("c", "C", "Course", 5, 3, CourseStatus.ENROLLED)
    private val test = GradeItem("g", "c", "Test", GradeItemType.TEST, 0.0, 10.0,
        dueDate = date, completed = false)
    private val config = ReminderSettings(enabled = true).withRule(ReminderCategory.TEST,
        ReminderRule(true, listOf(10_080, 1_440)))
    private fun snapshot(settings: ReminderSettings = config, items: List<GradeItem> = listOf(test),
        tasks: List<StudyTask> = emptyList(), rules: List<ScheduleRule> = emptyList(),
        exceptions: List<ScheduleException> = emptyList(), absences: List<LessonAbsence> = emptyList(),
        oneOff: List<OneOffScheduleEvent> = emptyList()) = PlanningSnapshot(listOf(course), absences,
        rules, oneOff, exceptions, tasks, items, AppSettings(reminders = settings))
    private fun plan(snapshot: PlanningSnapshot) = ReminderPlanner.plan(snapshot, now, zone)

    @Test fun defaultOffAndAcademicMasterOffPlanNothing() {
        assertTrue(plan(snapshot(ReminderSettings())).isEmpty())
        assertTrue(plan(snapshot(config.copy(academicEnabled = false))).isEmpty())
    }
    @Test fun multipleUniqueOffsetsUseConfiguredDateOnlyTimeAndStableIdentities() {
        val alarms = plan(snapshot(config.copy(dateOnlyTime = LocalTime.of(9, 15))))
        assertEquals(2, alarms.size)
        assertEquals(Instant.parse("2026-10-15T07:15:00Z"), alarms.first().fireAt)
        assertEquals(listOf(10_080L, 1_440L), alarms.map { it.offsetMinutes })
        assertEquals(alarms.map { it.id }, plan(snapshot(items = listOf(test.copy(title = "Renamed")))) .map { it.id })
        assertEquals(1, plan(snapshot(config.withRule(ReminderCategory.TEST, ReminderRule(true, listOf(1440, 1440, 0, -1))))).size)
    }
    @Test fun submittedAndAwaitingWorkAndCompletedTasksAreExcluded() {
        assertTrue(plan(snapshot(items = listOf(test.copy(completed = true), test.copy(id = "waiting", awaitingResult = true)),
            tasks = listOf(StudyTask("t", "c", "Done", null, date, true)))).isEmpty())
    }
    @Test fun legacyMidtermAndScoredTaskAliasesDoNotCreateDuplicateWork() {
        val task = StudyTask("t", "c", "Test", null, date, false, maxPoints = 10.0)
        val alarms = plan(snapshot(items = listOf(test.copy(type = GradeItemType.MIDTERM)), tasks = listOf(task)))
        assertEquals(2, alarms.size)
        assertTrue(alarms.all { it.category == ReminderCategory.TEST })
    }
    @Test fun independentTaskLessonEventAndReminderSwitches() {
        val lesson = OneOffScheduleEvent("lesson", null, "Lesson", date, LocalTime.NOON,
            LocalTime.of(13, 0), type = OneOffScheduleEventType.EXTRA)
        val event = lesson.copy(id = "event", lessonType = LessonType.EVENT)
        val reminder = lesson.copy(id = "reminder", type = OneOffScheduleEventType.REMINDER, lessonType = LessonType.EVENT)
        val settings = config.copy(academicEnabled = false).withRule(ReminderCategory.LESSON, ReminderRule(false, listOf(30)))
        val alarms = plan(snapshot(settings, oneOff = listOf(lesson, event, reminder),
            tasks = listOf(StudyTask("t", "c", "Task", null, date, false))))
        assertEquals(setOf(ReminderCategory.TASK, ReminderCategory.EVENT, ReminderCategory.REMINDER), alarms.map { it.category }.toSet())
    }
    @Test fun linkedTestUsesMovedLessonTimeAndCancelledOrAbsentLessonSuppressesIt() {
        val rule = ScheduleRule("r", "c", "Lesson", date.dayOfWeek, LocalTime.of(10, 0),
            LocalTime.of(11, 0), ScheduleRecurrence.WEEKLY)
        val linked = test.copy(lessonId = "r")
        val moved = ScheduleException("e", "r", date, ScheduleExceptionType.MOVED,
            date.plusDays(1), LocalTime.of(14, 0), LocalTime.of(15, 0))
        val alarms = plan(snapshot(items = listOf(linked), rules = listOf(rule), exceptions = listOf(moved)))
            .filter { it.category == ReminderCategory.TEST }
        assertEquals(date.plusDays(1).atTime(14, 0).atZone(zone).toInstant(), alarms.first().occursAt)
        assertTrue(plan(snapshot(items = listOf(linked), rules = listOf(rule),
            exceptions = listOf(moved.copy(type = ScheduleExceptionType.CANCELLED)))) .none { it.category == ReminderCategory.TEST })
        assertTrue(plan(snapshot(items = listOf(linked), rules = listOf(rule),
            absences = listOf(LessonAbsence("r", date, false)))) .none { it.category == ReminderCategory.TEST })
    }
    @Test fun inactiveTeachingPeriodDoesNotPlanRecurringLessons() {
        val rule = ScheduleRule("r", "c", "Lesson", date.dayOfWeek, LocalTime.NOON,
            LocalTime.of(13, 0), ScheduleRecurrence.WEEKLY)
        val base = snapshot(items = emptyList(), rules = listOf(rule))
        val outside = base.copy(settings = base.settings.copy(semesterPeriods = mapOf(3 to
            SemesterPeriod(studyStart = date.minusMonths(2), studyEnd = date.minusMonths(1)))))
        assertTrue(plan(outside).isEmpty())
    }
    @Test fun dateOnlyDayOffsetRetainsLocalClockAcrossDstAndReplansInNewTimezone() {
        val item = test.copy(dueDate = LocalDate.of(2026, 10, 26))
        val oneDay = config.withRule(ReminderCategory.TEST, ReminderRule(true, listOf(1440)))
        val base = snapshot(oneDay, listOf(item))
        assertEquals(LocalTime.of(9, 0), plan(base).single().fireAt.atZone(zone).toLocalTime())
        val utc = ReminderPlanner.plan(base, now, ZoneOffset.UTC).single()
        assertNotEquals(plan(base).single().fireAt, utc.fireAt)
        assertEquals(plan(base).single().id, utc.id)
    }
    @Test fun longLeadIsIncludedWhenOccurrenceIsBeyondDeliveryHorizon() {
        val later = test.copy(dueDate = LocalDate.of(2026, 11, 30))
        val longLead = config.withRule(ReminderCategory.TEST, ReminderRule(true, listOf(60 * 1440)))
        assertEquals(1, ReminderPlanner.plan(snapshot(longLead, listOf(later)),
            Instant.parse("2026-09-30T00:00:00Z"), zone).size)
    }
    @Test fun linkedTestCreatedBeforeOrAfterMoveKeepsEveryOffsetAndNonMovedLinksStillWork() {
        val rule = ScheduleRule("r", "c", "Lesson", date.dayOfWeek, LocalTime.of(10, 0),
            LocalTime.of(11, 0), ScheduleRecurrence.WEEKLY)
        val moved = ScheduleException("e", "r", date, ScheduleExceptionType.MOVED,
            date.plusDays(1), LocalTime.of(14, 0), LocalTime.of(15, 0))
        for (savedDate in listOf(date, date.plusDays(1))) {
            val alarms = plan(snapshot(items = listOf(test.copy(lessonId = "r", dueDate = savedDate)),
                rules = listOf(rule), exceptions = listOf(moved))).filter { it.category == ReminderCategory.TEST }
            assertEquals(listOf(10080L, 1440L), alarms.map { it.offsetMinutes })
            assertTrue(alarms.all { it.occursAt == Instant.parse("2026-10-23T12:00:00Z") })
        }
        val ordinary = plan(snapshot(items = listOf(test.copy(lessonId = "r")), rules = listOf(rule)))
            .filter { it.category == ReminderCategory.TEST }
        assertEquals(2, ordinary.size)
        assertTrue(ordinary.all { it.occursAt == Instant.parse("2026-10-22T08:00:00Z") })
        assertTrue(plan(snapshot(items = listOf(test.copy(lessonId = "missing")))).none { it.category == ReminderCategory.TEST })
    }

    private fun examInfo(link: String? = "g", at: LocalTime? = LocalTime.of(14, 0)) =
        ExamInfo("c", link, date.plusDays(1), at, null, null)
    private fun examSnapshot(item: GradeItem? = test.copy(type = GradeItemType.EXAM, dueDate = null),
        info: ExamInfo = examInfo()): PlanningSnapshot = snapshot(items = listOfNotNull(item))
        .copy(examInfos = listOf(info))

    @Test fun examMetadataOverridesStaleOrMissingAssessmentDateWithoutDuplicateOrGradeMutation() {
        for (due in listOf(null, date)) {
            val item = test.copy(type = GradeItemType.EXAM, dueDate = due, maxPoints = 37.0, contributesToGrade = false)
            val base = examSnapshot(item)
            val alarms = plan(base).filter { it.category == ReminderCategory.EXAM }
            assertEquals(1, alarms.size)
            assertEquals(Instant.parse("2026-10-23T12:00:00Z"), alarms.single().occursAt)
            val edited = plan(base.copy(examInfos = listOf(examInfo().copy(date = date.plusDays(2), startTime = LocalTime.of(16, 0)))))
                .single { it.category == ReminderCategory.EXAM }
            assertEquals(Instant.parse("2026-10-24T14:00:00Z"), edited.occursAt)
            assertNotEquals(alarms.single().deliveryKey, edited.deliveryKey)
            assertEquals(listOf(item), base.assessments)
        }
    }
    @Test fun metadataOnlyAndDateOnlyExamsNeedNoSyntheticPointsAndSubmittedLinkNeverReappears() {
        val metadataOnly = examSnapshot(item = null, info = examInfo(link = null))
        assertEquals(1, plan(metadataOnly).count { it.category == ReminderCategory.EXAM })
        assertTrue(metadataOnly.assessments.isEmpty())
        val dateOnly = plan(examSnapshot(info = examInfo(at = null))).single { it.category == ReminderCategory.EXAM }
        assertTrue(dateOnly.dateOnly)
        assertEquals(Instant.parse("2026-10-23T07:00:00Z"), dateOnly.occursAt)
        for (item in listOf(test.copy(type = GradeItemType.EXAM, completed = true),
            test.copy(type = GradeItemType.EXAM, awaitingResult = true))) {
            assertTrue(plan(examSnapshot(item)).none { it.category == ReminderCategory.EXAM })
            assertTrue(plan(examSnapshot(item, examInfo(link = null))).none { it.category == ReminderCategory.EXAM })
        }
        assertTrue(plan(examSnapshot(info = examInfo().copy(date = null))).none { it.category == ReminderCategory.EXAM })
    }

    @Test fun shortLeadsUseElapsedTimeAndWholeDaysUseCalendarTimeAcrossBothDstTransitions() {
        data class Case(val target: String, val now: String, val expected30: String, val expected60: String,
            val expectedDay: String, val expectedWeek: String)
        val cases = listOf(
            Case("2026-03-29T03:15", "2026-03-01T00:00:00Z", "2026-03-29T00:45:00Z", "2026-03-29T00:15:00Z",
                "2026-03-28T02:15:00Z", "2026-03-22T02:15:00Z"),
            Case("2026-10-25T03:15", "2026-10-01T00:00:00Z", "2026-10-25T01:45:00Z", "2026-10-25T01:15:00Z",
                "2026-10-24T01:15:00Z", "2026-10-18T01:15:00Z"))
        for (case in cases) {
            val target = LocalDateTime.parse(case.target)
            val event = OneOffScheduleEvent("dst", null, "DST", target.toLocalDate(), target.toLocalTime(),
                LocalTime.of(4, 0), type = OneOffScheduleEventType.REMINDER)
            val base = snapshot(config.withRule(ReminderCategory.REMINDER, ReminderRule(true, listOf(30, 60, 1440, 10080))),
                items = emptyList(), oneOff = listOf(event))
            val alarms = ReminderPlanner.plan(base, Instant.parse(case.now), zone).associateBy { it.offsetMinutes }
            for ((offset, expected) in mapOf(30L to case.expected30, 60L to case.expected60,
                1440L to case.expectedDay, 10080L to case.expectedWeek)) {
                assertEquals("${case.target} lead $offset", Instant.parse(expected), alarms.getValue(offset).fireAt)
                assertTrue(alarms.getValue(offset).fireAt.isBefore(alarms.getValue(offset).occursAt))
            }
        }
    }
    @Test fun timezoneKeepsConsumptionKeyButActualLocalTimeEditChangesIt() {
        val base = snapshot()
        val before = plan(base)
        val utc = ReminderPlanner.plan(base, now, ZoneOffset.UTC)
        assertEquals(before.map { it.deliveryKey }, utc.map { it.deliveryKey })
        assertNotEquals(before.first().fireAt, utc.first().fireAt)
        val moved = plan(base.copy(settings = base.settings.copy(reminders = config.copy(dateOnlyTime = LocalTime.of(10, 0)))))
        assertNotEquals(before.first().deliveryKey, moved.first().deliveryKey)
    }

}
