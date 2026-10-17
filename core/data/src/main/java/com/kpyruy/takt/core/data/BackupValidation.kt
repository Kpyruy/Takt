package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import java.time.LocalDate
import java.time.LocalTime

/**
 * Checks portable data before any restore side effects. Missing optional fields keep their codec
 * defaults; explicit invalid values and dangling links are never silently repaired. Historical
 * exception and assessment links may outlive deleted source records; they remain inert metadata.
 * Active ownership links must resolve. Profile cache entries may outlive teaching-team membership.
 */
internal fun BackupPayload.validated(): BackupPayload {
    section("version") {
        require(version in 1..BackupPayload.CURRENT_VERSION) { "Unsupported Takt backup version: $version" }
    }
    val courseIds = courses.map { it.id }.toSet()
    section("courses") {
        unique(courses.map { it.id })
        courses.forEach {
            require(it.code.isNotBlank() && it.title.isNotBlank()) { "Course code and title are required" }
            require(it.credits >= 0 && it.semester > 0) { "Invalid credits or semester" }
            require(CourseStatus.entries.any { status -> status.storageValue == it.status }) { "Invalid status" }
            enumValue<CourseRequirementType>(it.requirementType)
            enumValue<CourseGradingType>(it.gradingType)
            it.passFailResult?.let { value -> enumValue<PassFailResult>(value) }
            it.officialGrade?.let { value -> enumValue<GradeLetter>(value) }
            it.fulfilledOnEpochDay?.let(::date)
        }
    }
    section("courseIdAliases") {
        unique(courseIdAliases.map { it.oldId })
        require(courseIdAliases.all { it.oldId !in courseIds && it.courseId in courseIds }) { "Invalid course redirects" }
    }
    section("courseImportDecisions") {
        unique(courseImportDecisions)
        require(courseImportDecisions.all { it in courseIds && isCanonicalUisCourseId(it) }) { "Invalid UIS import choices" }
    }
    val rules = scheduleRules.associateBy { it.id }
    val events = oneOffEvents.associateBy { it.id }
    section("scheduleRules") {
        unique(scheduleRules.map { it.id })
        scheduleRules.forEach {
            courseLink(it.courseId, courseIds)
            require(it.title.isNotBlank() && it.dayOfWeek in 1..7) { "Invalid title or day of week" }
            timeRange(it.startMinute, it.endMinute)
            enumValue<ScheduleRecurrence>(it.recurrence)
            enumValue<LessonType>(it.lessonType)
        }
    }
    section("oneOffEvents") {
        unique(oneOffEvents.map { it.id })
        require(events.keys.none { it in rules }) { "A lesson ID cannot identify both a rule and a one-off event" }
        oneOffEvents.forEach {
            courseLink(it.courseId, courseIds)
            require(it.title.isNotBlank()) { "Event title is required" }
            date(it.dateEpochDay)
            timeRange(it.startMinute, it.endMinute)
            enumValue<OneOffScheduleEventType>(it.type)
            enumValue<LessonType>(it.lessonType)
        }
    }
    section("scheduleExceptions") {
        unique(scheduleExceptions.map { it.id })
        require(scheduleExceptions.map { it.ruleId to it.dateEpochDay }.distinct().size == scheduleExceptions.size) {
            "Duplicate overrides for the same occurrence"
        }
        scheduleExceptions.forEach {
            require(it.ruleId.isNotBlank()) { "Missing rule key" }
            val rule = rules[it.ruleId]
            date(it.dateEpochDay)
            it.replacementDateEpochDay?.let(::date)
            it.replacementStartMinute?.let(::minute)
            it.replacementEndMinute?.let(::minute)
            when (enumValue<ScheduleExceptionType>(it.type)) {
                ScheduleExceptionType.CANCELLED -> require(it.replacementDateEpochDay == null &&
                    it.replacementStartMinute == null && it.replacementEndMinute == null && it.replacementRoom == null) {
                    "Cancelled occurrence contains move metadata"
                }
                // Null components deliberately inherit the source occurrence, as in ScheduleResolver.
                ScheduleExceptionType.MOVED -> {
                    val start = it.replacementStartMinute ?: rule?.startMinute
                    val end = it.replacementEndMinute ?: rule?.endMinute
                    if (start != null && end != null) timeRange(start, end)
                }
            }
        }
    }
    section("lessonAbsences") {
        require(lessonAbsences.distinct().size == lessonAbsences.size) { "Duplicate attendance keys" }
        lessonAbsences.forEach {
            date(it.dateEpochDay)
            if (it.isOneOff) {
                val event = requireNotNull(events[it.eventId]) { "Missing one-off event ${it.eventId}" }
                require(event.dateEpochDay == it.dateEpochDay) { "Attendance date differs from one-off event" }
            } else require(it.eventId in rules) { "Missing rule ${it.eventId}" }
        }
    }
    section("gradeItems") {
        unique(gradeItems.map { it.id })
        gradeItems.forEach {
            courseLink(it.courseId, courseIds)
            require(it.title.isNotBlank()) { "Grade title is required" }
            enumValue<GradeItemType>(it.type)
            points(it.earnedPoints, it.maxPoints, it.minimumPointsForExam, allowBonus = true)
            require(it.durationMinutes == null || it.durationMinutes > 0) { "Invalid duration" }
            it.dueDateEpochDay?.let(::date)
            it.lessonId?.let { id ->
                val linkedCourse = rules[id]?.courseId ?: events[id]?.courseId
                // Historical assessment links can outlive deleted lessons or a progress-only merge.
                require((id !in rules && id !in events) || linkedCourse == it.courseId) { "Lesson belongs to another course" }
            }
        }
    }
    section("gradeScales") {
        unique(gradeScales.map { it.courseId })
        gradeScales.forEach {
            courseLink(it.courseId, courseIds)
            val thresholds = listOf(it.aMin, it.bMin, it.cMin, it.dMin, it.eMin, 0.0)
            require(thresholds.all { value -> value.isFinite() && value in 0.0..100.0 } &&
                thresholds.zipWithNext().all { (a, b) -> a > b }) { "Invalid grade thresholds" }
        }
    }
    section("gradeOverrides") {
        unique(gradeOverrides.map { it.courseId })
        gradeOverrides.forEach { courseLink(it.courseId, courseIds); enumValue<GradeLetter>(it.grade) }
    }
    section("studyTasks") {
        unique(studyTasks.map { it.id })
        studyTasks.forEach {
            courseLink(it.courseId, courseIds)
            require(it.title.isNotBlank()) { "Task title is required" }
            if (it.maxPoints == null) require(it.earnedPoints == null && it.minimumPointsForExam == null) { "Points need a maximum" }
            else points(it.earnedPoints, it.maxPoints, it.minimumPointsForExam)
            it.dueDateEpochDay?.let(::date)
        }
    }
    section("courseNotes") {
        unique(courseNotes.map { it.id })
        courseNotes.forEach {
            courseLink(it.courseId, courseIds)
            require(it.title.isNotBlank()) { "Note title is required" }
            it.attachments.forEach { attachment ->
                require(attachment.name.isNotBlank() && attachment.uri.isNotBlank() && attachment.mimeType.isNotBlank()) {
                    "Invalid attachment metadata"
                }
                if (attachment.mimeType == NoteAttachment.WEB_LINK_MIME) require(
                    runCatching { NoteAttachment.webLink(attachment.uri) }.isSuccess) { "Invalid attachment web link" }
            }
        }
    }
    val grades = gradeItems.associateBy { it.id }
    section("examInfo") {
        unique(examInfo.map { it.courseId })
        examInfo.forEach {
            courseLink(it.courseId, courseIds)
            it.gradeItemId?.let { id ->
                val grade = grades[id]
                require(id.isNotBlank() && (grade == null || grade.courseId == it.courseId)) { "Grade item belongs to another course" }
            }
            it.dateEpochDay?.let(::date)
            it.startMinute?.let(::minute)
            it.endMinute?.let(::minute)
            if (it.startMinute != null && it.endMinute != null) timeRange(it.startMinute, it.endMinute)
            require(it.maxAttempts in 1..3 && it.attemptNumber in 1..it.maxAttempts) { "Invalid exam attempt" }
        }
    }
    section("examMaterials") {
        unique(examMaterials.map { it.id })
        examMaterials.forEach {
            courseLink(it.courseId, courseIds)
            require(it.title.isNotBlank() && it.uri.isNotBlank()) { "Invalid material metadata" }
        }
    }
    section("teaching") {
        teaching?.validated()
        val codes = courses.map { teachingCode(it.code) }.toSet()
        require(teaching?.teams.orEmpty().all { teachingCode(it.courseCode) in codes }) { "Teaching team references a missing course" }
    }
    section("settings") { settings.validate(courseIds) }
    return this
}

private fun BackupSettings.validate(courseIds: Set<String>) {
    enumValue<CancellationDisplayStyle>(cancellationStyle)
    enumValue<ParityOverride>(parityOverride)
    enumValue<CardAppearance>(cardAppearance)
    enumValue<ThemeFamily>(themeFamily)
    enumValue<AppThemeMode>(themeMode)
    enumValue<WeekLayout>(weekLayout)
    enumValue<AppLanguage>(language)
    enumValue<CourseNameLanguage>(courseNameLanguage)
    require(enumValue<CourseNameLanguage>(ukrainianCourseNameFallback) != CourseNameLanguage.FOLLOW_APP) { "Invalid course language fallback" }
    enumValue<UisRefreshFrequency>(uisProgressFrequency)
    enumValue<UisRefreshFrequency>(uisSubjectFrequency)
    enumValue<UisRefreshFrequency>(uisPeriodFrequency)
    require(enumValue<UisRefreshFrequency>(uisTimetableFrequency) in setOf(UisRefreshFrequency.MANUAL, UisRefreshFrequency.TEACHING_START)) {
        "Invalid timetable refresh frequency"
    }
    require(currentSemester == null || currentSemester > 0) { "Invalid current semester" }
    require(semesterPeriods.map { it.semester }.distinct().size == semesterPeriods.size) { "Duplicate semester periods" }
    semesterPeriods.forEach {
        require(it.semester > 0) { "Invalid semester" }
        val period = SemesterPeriod(it.studyStart?.let(LocalDate::parse), it.studyEnd?.let(LocalDate::parse),
            it.examStart?.let(LocalDate::parse), it.examEnd?.let(LocalDate::parse), enumValue(it.assessmentMode))
        require(period.hasValidDates()) { "Invalid semester dates" }
    }
    homeWorkFilter.let {
        // Explicit legacy spelling already supported by BackupMappings.
        if (it.period != "SEVEN_CLASS_DAYS") enumValue<HomeWorkPeriod>(it.period)
        // Saved filters can retain a deleted course; these are preferences, not row ownership.
        require(it.courseId == null || it.courseId.isNotBlank())
        unique(it.courseIds)
        require(it.types.distinct().size == it.types.size) { "Duplicate homework types" }
        it.types.forEach { type -> if (type != "TASK") enumValue<GradeItemType>(type) }
        val from = it.fromDate?.let(LocalDate::parse)
        val to = it.toDate?.let(LocalDate::parse)
        require(from == null || to == null || from <= to) { "Invalid homework date range" }
    }
    unique(customThemes.map { it.id })
    customThemes.forEach {
        require(CustomColorTheme(it.id, it.name, it.primaryHex, it.secondaryHex, it.tertiaryHex).normalizedOrNull() != null) {
            "Invalid custom theme"
        }
    }
    require(selectedCustomThemeId == null || customThemes.any { it.id == selectedCustomThemeId }) { "Missing selected custom theme" }
    reminders.let {
        val time = LocalTime.parse(it.dateOnlyTime)
        require(time.second == 0 && time.nano == 0) { "Reminder time must use whole minutes" }
        val categories = it.rules.map { rule -> if (rule.category == "MIDTERM") ReminderCategory.TEST else enumValue<ReminderCategory>(rule.category) }
        require(categories.distinct().size == categories.size) { "Duplicate reminder categories" }
        it.rules.forEach { rule ->
            require(rule.offsetsMinutes.distinct().size == rule.offsetsMinutes.size &&
                rule.offsetsMinutes.all { offset -> offset in 1..MAX_REMINDER_LEAD_MINUTES }) { "Invalid reminder offsets" }
        }
    }
    require(wallpaper == null || wallpaperRecorded) { "Invalid wallpaper metadata" }
    wallpaper?.decodedBytes()
}

private inline fun section(name: String, validate: () -> Unit) {
    try { validate() } catch (error: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid backup $name: ${error.message}", error)
    } catch (error: java.time.DateTimeException) {
        throw IllegalArgumentException("Invalid backup $name: ${error.message}", error)
    }
}

private fun unique(ids: List<String>) {
    require(ids.all { it.isNotBlank() } && ids.distinct().size == ids.size) { "Blank or duplicate keys" }
}
private fun courseLink(id: String?, courseIds: Set<String>) {
    require(id == null || id in courseIds) { "Missing course $id" }
}
private fun date(epochDay: Long) { LocalDate.ofEpochDay(epochDay) }
private fun minute(value: Int) { require(value in 0..1439) { "Invalid minute of day" } }
private fun timeRange(start: Int, end: Int) {
    minute(start); minute(end)
    require(start < end) { "End time must follow start time" }
}
private fun points(earned: Double?, maximum: Double, minimum: Double?, allowBonus: Boolean = false) {
    require(maximum.isFinite() && maximum > 0.0) { "Maximum points must be positive and finite" }
    require(earned == null || earned.isFinite() && earned >= 0.0 && (allowBonus || earned <= maximum)) { "Invalid earned points" }
    require(minimum == null || minimum.isFinite() && minimum in 0.0..maximum) { "Invalid minimum points" }
}
private inline fun <reified T : Enum<T>> enumValue(value: String): T = enumValueOf<T>(value)
