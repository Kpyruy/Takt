package com.kpyruy.takt.core.model

import java.time.LocalDate

enum class AssessmentPhase { STUDY, EXAM }
enum class AssessmentPhaseMode { AUTO, STUDY, EXAM }

fun CourseGradingType.forPhase(phase: AssessmentPhase): CourseGradingType = when {
    this == CourseGradingType.PASS_FAIL -> CourseGradingType.PASS_FAIL
    phase == AssessmentPhase.EXAM -> CourseGradingType.EXAM_LETTER
    else -> CourseGradingType.CONTINUOUS_LETTER
}

/** Boundaries are inclusive. A missing pair leaves that period unconfigured. */
data class SemesterPeriod(
    val studyStart: LocalDate? = null,
    val studyEnd: LocalDate? = null,
    val examStart: LocalDate? = null,
    val examEnd: LocalDate? = null,
    val assessmentMode: AssessmentPhaseMode = AssessmentPhaseMode.AUTO,
) {
    fun hasValidDates(): Boolean =
        (studyStart == null && studyEnd == null || studyStart != null && studyEnd != null && studyStart <= studyEnd) &&
            (examStart == null && examEnd == null || examStart != null && examEnd != null && examStart <= examEnd)

    fun allowsRecurringLesson(date: LocalDate): Boolean =
        if (studyStart != null && studyEnd != null) date >= studyStart && date <= studyEnd else true

    fun containsExamDate(date: LocalDate): Boolean =
        examStart != null && examEnd != null && date >= examStart && date <= examEnd

    fun assessmentPhase(date: LocalDate): AssessmentPhase = when (assessmentMode) {
        AssessmentPhaseMode.STUDY -> AssessmentPhase.STUDY
        AssessmentPhaseMode.EXAM -> AssessmentPhase.EXAM
        AssessmentPhaseMode.AUTO -> if (examStart != null && examEnd != null && date >= examStart) {
            AssessmentPhase.EXAM
        } else {
            AssessmentPhase.STUDY
        }
    }

    val isEmpty: Boolean get() = studyStart == null && studyEnd == null && examStart == null && examEnd == null &&
        assessmentMode == AssessmentPhaseMode.AUTO
}
