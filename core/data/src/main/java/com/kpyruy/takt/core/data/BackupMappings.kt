package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.database.CourseNoteEntity
import com.kpyruy.takt.core.database.ExamInfoEntity
import com.kpyruy.takt.core.database.ExamMaterialEntity
import com.kpyruy.takt.core.database.GradeItemEntity
import com.kpyruy.takt.core.database.GradeOverrideEntity
import com.kpyruy.takt.core.database.GradeScaleEntity
import com.kpyruy.takt.core.database.OneOffScheduleEventEntity
import com.kpyruy.takt.core.database.ScheduleExceptionEntity
import com.kpyruy.takt.core.database.ScheduleRuleEntity
import com.kpyruy.takt.core.database.StudyTaskEntity
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CardAppearance
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride
import com.kpyruy.takt.core.model.ThemeFamily
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.model.HomeWorkPeriod
import com.kpyruy.takt.core.model.HomeWorkFilter
import com.kpyruy.takt.core.model.SemesterPeriod
import com.kpyruy.takt.core.model.AssessmentPhaseMode
import com.kpyruy.takt.core.model.GradeItemType
import java.time.LocalDate

internal fun com.kpyruy.takt.core.database.LessonAbsenceEntity.toBackup() = BackupLessonAbsence(eventId, dateEpochDay, isOneOff)
internal fun BackupLessonAbsence.toEntity() = com.kpyruy.takt.core.database.LessonAbsenceEntity(eventId, dateEpochDay, isOneOff)

internal fun CourseEntity.toBackup() = BackupCourse(
    id = id,
    code = code,
    title = title,
    credits = credits,
    semester = semester,
    status = status,
    requirementType = requirementType,
    syllabusUrl = syllabusUrl,
    gradingType = gradingType,
    passFailResult = passFailResult,
    iconKey = iconKey,
)

internal fun BackupCourse.toEntity() = CourseEntity(
    id = id,
    code = code,
    title = title,
    credits = credits,
    semester = semester,
    status = status,
    requirementType = requirementType,
    syllabusUrl = syllabusUrl,
    gradingType = gradingType,
    passFailResult = passFailResult,
    iconKey = iconKey,
)

internal fun ScheduleRuleEntity.toBackup() = BackupScheduleRule(
    id = id,
    courseId = courseId,
    title = title,
    dayOfWeek = dayOfWeek,
    startMinute = startMinute,
    endMinute = endMinute,
    recurrence = recurrence,
    lessonType = lessonType,
    room = room,
)

internal fun BackupScheduleRule.toEntity() = ScheduleRuleEntity(
    id = id,
    courseId = courseId,
    title = title,
    dayOfWeek = dayOfWeek,
    startMinute = startMinute,
    endMinute = endMinute,
    recurrence = recurrence,
    lessonType = lessonType,
    room = room,
)

internal fun OneOffScheduleEventEntity.toBackup() = BackupOneOffEvent(
    id = id,
    courseId = courseId,
    title = title,
    dateEpochDay = dateEpochDay,
    lessonType = lessonType,
    startMinute = startMinute,
    endMinute = endMinute,
    room = room,
    type = type,
)

internal fun BackupOneOffEvent.toEntity() = OneOffScheduleEventEntity(
    id = id,
    courseId = courseId,
    title = title,
    dateEpochDay = dateEpochDay,
    lessonType = lessonType,
    startMinute = startMinute,
    endMinute = endMinute,
    room = room,
    type = type,
)

internal fun ScheduleExceptionEntity.toBackup() = BackupScheduleException(
    id = id,
    ruleId = ruleId,
    dateEpochDay = dateEpochDay,
    type = type,
    replacementDateEpochDay = replacementDateEpochDay,
    replacementStartMinute = replacementStartMinute,
    replacementEndMinute = replacementEndMinute,
    replacementRoom = replacementRoom,
)

internal fun BackupScheduleException.toEntity() = ScheduleExceptionEntity(
    id = id,
    ruleId = ruleId,
    dateEpochDay = dateEpochDay,
    type = type,
    replacementDateEpochDay = replacementDateEpochDay,
    replacementStartMinute = replacementStartMinute,
    replacementEndMinute = replacementEndMinute,
    replacementRoom = replacementRoom,
)

internal fun GradeItemEntity.toBackup() = BackupGradeItem(
    id = id,
    courseId = courseId,
    title = title,
    type = type,
    earnedPoints = earnedPoints,
    maxPoints = maxPoints,
    recordedAtEpochMillis = recordedAtEpochMillis,
    dueDateEpochDay = dueDateEpochDay,
    completed = completed,
    requiredForExam = requiredForExam,
    minimumPointsForExam = minimumPointsForExam,
    lessonId = lessonId,
    durationMinutes = durationMinutes,
)

internal fun BackupGradeItem.toEntity() = GradeItemEntity(
    id = id,
    courseId = courseId,
    title = title,
    type = type,
    earnedPoints = earnedPoints,
    maxPoints = maxPoints,
    recordedAtEpochMillis = recordedAtEpochMillis,
    dueDateEpochDay = dueDateEpochDay,
    completed = completed,
    requiredForExam = requiredForExam,
    minimumPointsForExam = minimumPointsForExam,
    lessonId = lessonId,
    durationMinutes = durationMinutes,
)

internal fun GradeScaleEntity.toBackup() = BackupGradeScale(
    courseId = courseId,
    aMin = aMin,
    bMin = bMin,
    cMin = cMin,
    dMin = dMin,
    eMin = eMin,
)

internal fun BackupGradeScale.toEntity() = GradeScaleEntity(
    courseId = courseId,
    aMin = aMin,
    bMin = bMin,
    cMin = cMin,
    dMin = dMin,
    eMin = eMin,
)

internal fun GradeOverrideEntity.toBackup() = BackupGradeOverride(
    courseId = courseId,
    grade = grade,
)

internal fun BackupGradeOverride.toEntity() = GradeOverrideEntity(
    courseId = courseId,
    grade = grade,
)

internal fun StudyTaskEntity.toBackup() = BackupStudyTask(
    id = id,
    courseId = courseId,
    title = title,
    description = description,
    dueDateEpochDay = dueDateEpochDay,
    completed = completed,
    requiredForExam = requiredForExam,
    earnedPoints = earnedPoints,
    maxPoints = maxPoints,
    minimumPointsForExam = minimumPointsForExam,
)

internal fun BackupStudyTask.toEntity() = StudyTaskEntity(
    id = id,
    courseId = courseId,
    title = title,
    description = description,
    dueDateEpochDay = dueDateEpochDay,
    completed = completed,
    requiredForExam = requiredForExam,
    earnedPoints = earnedPoints,
    maxPoints = maxPoints,
    minimumPointsForExam = minimumPointsForExam,
)

internal fun CourseNoteEntity.toBackup() = BackupCourseNote(
    id = id,
    courseId = courseId,
    title = title,
    content = content,
    updatedAtEpochMillis = updatedAtEpochMillis,
    attachments = NoteAttachmentCodec.decode(attachmentsJson).map { it.toBackup() },
)

internal fun BackupCourseNote.toEntity() = CourseNoteEntity(
    id = id,
    courseId = courseId,
    title = title,
    content = content,
    updatedAtEpochMillis = updatedAtEpochMillis,
    attachmentsJson = NoteAttachmentCodec.encode(attachments.map { it.toModel() }),
)

internal fun ExamInfoEntity.toBackup() = BackupExamInfo(
    courseId = courseId,
    gradeItemId = gradeItemId,
    dateEpochDay = dateEpochDay,
    startMinute = startMinute,
    endMinute = endMinute,
    room = room,
    attemptNumber = attemptNumber,
    maxAttempts = maxAttempts,
    notes = notes,
)

internal fun BackupExamInfo.toEntity() = ExamInfoEntity(
    courseId = courseId,
    gradeItemId = gradeItemId,
    dateEpochDay = dateEpochDay,
    startMinute = startMinute,
    endMinute = endMinute,
    room = room,
    attemptNumber = attemptNumber,
    maxAttempts = maxAttempts,
    notes = notes,
)

internal fun ExamMaterialEntity.toBackup() = BackupExamMaterial(
    id = id,
    courseId = courseId,
    title = title,
    uri = uri,
)

internal fun BackupExamMaterial.toEntity() = ExamMaterialEntity(
    id = id,
    courseId = courseId,
    title = title,
    uri = uri,
)

internal fun AppSettings.toBackup() = BackupSettings(
    cancellationStyle = cancellationStyle.name,
    showHiddenLessons = showHiddenLessons,
    parityOverride = parityOverride.name,
    cardAppearance = cardAppearance.name,
    themeFamily = themeFamily.name,
    themeMode = themeMode.name,
    weekLayout = weekLayout.name,
    homeWorkFilter = homeWorkFilter.toBackup(),
    semesterPeriods = semesterPeriods.toBackup(),
    currentSemester = currentSemester,
    language = language.name,
)

internal fun BackupSettings.toModel() = AppSettings(
    cancellationStyle = runCatching { CancellationDisplayStyle.valueOf(cancellationStyle) }
        .getOrDefault(CancellationDisplayStyle.STRIKETHROUGH),
    showHiddenLessons = showHiddenLessons,
    parityOverride = runCatching { ParityOverride.valueOf(parityOverride) }
        .getOrDefault(ParityOverride.AUTO),
    cardAppearance = runCatching { CardAppearance.valueOf(cardAppearance) }
        .getOrDefault(CardAppearance.ELEVATED),
    themeFamily = runCatching { ThemeFamily.valueOf(themeFamily) }
        .getOrDefault(ThemeFamily.BLUE),
    themeMode = runCatching { AppThemeMode.valueOf(themeMode) }
        .getOrDefault(AppThemeMode.SYSTEM),
    weekLayout = runCatching { WeekLayout.valueOf(weekLayout) }
        .getOrDefault(WeekLayout.TIMETABLE),
    homeWorkFilter = homeWorkFilter.toModel(),
    semesterPeriods = semesterPeriods.toModel(),
    currentSemester = currentSemester?.takeIf { it > 0 },
    language = runCatching { com.kpyruy.takt.core.model.AppLanguage.valueOf(language) }
        .getOrDefault(com.kpyruy.takt.core.model.AppLanguage.ENGLISH),
)

internal fun Map<Int, SemesterPeriod>.toBackup(): List<BackupSemesterPeriod> = entries.sortedBy { it.key }.map { (semester, period) ->
    BackupSemesterPeriod(semester, period.studyStart?.toString(), period.studyEnd?.toString(),
        period.examStart?.toString(), period.examEnd?.toString(), period.assessmentMode.name)
}

internal fun List<BackupSemesterPeriod>.toModel(): Map<Int, SemesterPeriod> = mapNotNull { saved ->
    runCatching {
        val period = SemesterPeriod(
            studyStart = saved.studyStart?.let(LocalDate::parse),
            studyEnd = saved.studyEnd?.let(LocalDate::parse),
            examStart = saved.examStart?.let(LocalDate::parse),
            examEnd = saved.examEnd?.let(LocalDate::parse),
            assessmentMode = runCatching { AssessmentPhaseMode.valueOf(saved.assessmentMode) }
                .getOrDefault(AssessmentPhaseMode.AUTO),
        )
        (saved.semester to period).takeIf { saved.semester > 0 && period.hasValidDates() }
    }.getOrNull()
}.toMap()

internal fun HomeWorkFilter.toBackup() = BackupHomeWorkFilter(
    period = period.name,
    courseId = courseId,
    types = types.map { it?.name ?: "TASK" }.sorted(),
    includeCompleted = includeCompleted,
    includeUndated = includeUndated,
    fromDate = fromDate?.toString(),
    toDate = toDate?.toString(),
)

internal fun BackupHomeWorkFilter.toModel() = HomeWorkFilter(
    period = when (period) {
        "SEVEN_CLASS_DAYS" -> HomeWorkPeriod.SEVEN_DAYS
        else -> runCatching { HomeWorkPeriod.valueOf(period) }.getOrDefault(HomeWorkPeriod.FOURTEEN_DAYS)
    },
    courseId = courseId,
    types = buildSet {
        types.forEach { stored ->
            if (stored == "TASK") add(null)
            else GradeItemType.entries.firstOrNull { it.name == stored }?.let(::add)
        }
    },
    includeCompleted = includeCompleted,
    includeUndated = includeUndated,
    fromDate = fromDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    toDate = toDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
)
