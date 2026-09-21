package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.database.CourseNoteEntity
import com.kpyruy.takt.core.database.GradeItemEntity
import com.kpyruy.takt.core.database.GradeOverrideEntity
import com.kpyruy.takt.core.database.GradeScaleEntity
import com.kpyruy.takt.core.database.OneOffScheduleEventEntity
import com.kpyruy.takt.core.database.ScheduleExceptionEntity
import com.kpyruy.takt.core.database.ScheduleRuleEntity
import com.kpyruy.takt.core.database.StudyTaskEntity
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride

internal fun CourseEntity.toBackup() = BackupCourse(
    id = id,
    code = code,
    title = title,
    credits = credits,
    semester = semester,
    status = status,
    requirementType = requirementType,
    syllabusUrl = syllabusUrl,
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
)

internal fun ScheduleRuleEntity.toBackup() = BackupScheduleRule(
    id = id,
    courseId = courseId,
    title = title,
    dayOfWeek = dayOfWeek,
    startMinute = startMinute,
    endMinute = endMinute,
    recurrence = recurrence,
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
    room = room,
)

internal fun OneOffScheduleEventEntity.toBackup() = BackupOneOffEvent(
    id = id,
    courseId = courseId,
    title = title,
    dateEpochDay = dateEpochDay,
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
)

internal fun BackupGradeItem.toEntity() = GradeItemEntity(
    id = id,
    courseId = courseId,
    title = title,
    type = type,
    earnedPoints = earnedPoints,
    maxPoints = maxPoints,
    recordedAtEpochMillis = recordedAtEpochMillis,
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
)

internal fun BackupStudyTask.toEntity() = StudyTaskEntity(
    id = id,
    courseId = courseId,
    title = title,
    description = description,
    dueDateEpochDay = dueDateEpochDay,
    completed = completed,
)

internal fun CourseNoteEntity.toBackup() = BackupCourseNote(
    id = id,
    courseId = courseId,
    title = title,
    content = content,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun BackupCourseNote.toEntity() = CourseNoteEntity(
    id = id,
    courseId = courseId,
    title = title,
    content = content,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

internal fun AppSettings.toBackup() = BackupSettings(
    cancellationStyle = cancellationStyle.name,
    showHiddenLessons = showHiddenLessons,
    parityOverride = parityOverride.name,
)

internal fun BackupSettings.toModel() = AppSettings(
    cancellationStyle = runCatching { CancellationDisplayStyle.valueOf(cancellationStyle) }
        .getOrDefault(CancellationDisplayStyle.STRIKETHROUGH),
    showHiddenLessons = showHiddenLessons,
    parityOverride = runCatching { ParityOverride.valueOf(parityOverride) }
        .getOrDefault(ParityOverride.AUTO),
)
