package com.kpyruy.takt.core.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val courses: List<BackupCourse> = emptyList(),
    val scheduleRules: List<BackupScheduleRule> = emptyList(),
    val oneOffEvents: List<BackupOneOffEvent> = emptyList(),
    val lessonAbsences: List<BackupLessonAbsence> = emptyList(),
    val scheduleExceptions: List<BackupScheduleException> = emptyList(),
    val gradeItems: List<BackupGradeItem> = emptyList(),
    val gradeScales: List<BackupGradeScale> = emptyList(),
    val gradeOverrides: List<BackupGradeOverride> = emptyList(),
    val studyTasks: List<BackupStudyTask> = emptyList(),
    val courseNotes: List<BackupCourseNote> = emptyList(),
    val examInfo: List<BackupExamInfo> = emptyList(),
    val examMaterials: List<BackupExamMaterial> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
) {
    companion object {
        const val CURRENT_VERSION = 3
    }
}

@Serializable
data class BackupLessonAbsence(val eventId: String, val dateEpochDay: Long, val isOneOff: Boolean)

@Serializable
data class BackupCourse(
    val id: String,
    val code: String,
    val title: String,
    val credits: Int,
    val semester: Int,
    val status: String,
    val requirementType: String,
    val syllabusUrl: String? = null,
    val gradingType: String = "CONTINUOUS_LETTER",
    val passFailResult: String? = null,
    val iconKey: String? = null,
)

@Serializable
data class BackupScheduleRule(
    val id: String,
    val courseId: String? = null,
    val title: String,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val recurrence: String,
    val lessonType: String = "UNSPECIFIED",
    val room: String? = null,
)

@Serializable
data class BackupOneOffEvent(
    val id: String,
    val courseId: String? = null,
    val title: String,
    val dateEpochDay: Long,
    val lessonType: String = "UNSPECIFIED",
    val startMinute: Int,
    val endMinute: Int,
    val room: String? = null,
    val type: String,
)

@Serializable
data class BackupScheduleException(
    val id: String,
    val ruleId: String,
    val dateEpochDay: Long,
    val type: String,
    val replacementDateEpochDay: Long? = null,
    val replacementStartMinute: Int? = null,
    val replacementEndMinute: Int? = null,
    val replacementRoom: String? = null,
)

@Serializable
data class BackupGradeItem(
    val id: String,
    val courseId: String,
    val title: String,
    val type: String,
    val earnedPoints: Double,
    val maxPoints: Double,
    val recordedAtEpochMillis: Long,
    val dueDateEpochDay: Long? = null,
    val completed: Boolean = true,
    val requiredForExam: Boolean = false,
    val minimumPointsForExam: Double? = null,
    val lessonId: String? = null,
)

@Serializable
data class BackupGradeScale(
    val courseId: String,
    val aMin: Double,
    val bMin: Double,
    val cMin: Double,
    val dMin: Double,
    val eMin: Double,
)

@Serializable
data class BackupGradeOverride(
    val courseId: String,
    val grade: String,
)

@Serializable
data class BackupStudyTask(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String? = null,
    val dueDateEpochDay: Long? = null,
    val completed: Boolean,
    val requiredForExam: Boolean = false,
    val earnedPoints: Double? = null,
    val maxPoints: Double? = null,
    val minimumPointsForExam: Double? = null,
)

@Serializable
data class BackupCourseNote(
    val id: String,
    val courseId: String,
    val title: String,
    val content: String,
    val updatedAtEpochMillis: Long,
    val attachments: List<BackupNoteAttachment> = emptyList(),
)

@Serializable
data class BackupNoteAttachment(val name: String, val uri: String, val mimeType: String)

@Serializable
data class BackupExamInfo(
    val courseId: String,
    val gradeItemId: String? = null,
    val dateEpochDay: Long? = null,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val room: String? = null,
    val attemptNumber: Int = 1,
    val maxAttempts: Int = 3,
    val notes: String = "",
)

@Serializable
data class BackupExamMaterial(
    val id: String,
    val courseId: String,
    val title: String,
    val uri: String,
)

@Serializable
data class BackupSettings(
    val cancellationStyle: String = "STRIKETHROUGH",
    val showHiddenLessons: Boolean = false,
    val parityOverride: String = "AUTO",
    val cardAppearance: String = "ELEVATED",
    val themeFamily: String = "BLUE",
    val themeMode: String = "SYSTEM",
    val weekLayout: String = "TIMETABLE",
)

object BackupPayloadCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(payload: BackupPayload): String = json.encodeToString(payload)

    fun decode(raw: String): BackupPayload {
        val payload = json.decodeFromString<BackupPayload>(raw)
        require(payload.version in 1..BackupPayload.CURRENT_VERSION) {
            "Unsupported Takt backup version: ${payload.version}"
        }
        require(payload.courses.isNotEmpty()) {
            "Backup does not contain a study plan"
        }
        return payload
    }
}
