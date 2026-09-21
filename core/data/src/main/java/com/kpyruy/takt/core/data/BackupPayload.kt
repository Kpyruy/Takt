package com.kpyruy.takt.core.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val courses: List<BackupCourse> = emptyList(),
    val scheduleRules: List<BackupScheduleRule> = emptyList(),
    val oneOffEvents: List<BackupOneOffEvent> = emptyList(),
    val scheduleExceptions: List<BackupScheduleException> = emptyList(),
    val gradeItems: List<BackupGradeItem> = emptyList(),
    val gradeScales: List<BackupGradeScale> = emptyList(),
    val studyTasks: List<BackupStudyTask> = emptyList(),
    val courseNotes: List<BackupCourseNote> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

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
    val room: String? = null,
)

@Serializable
data class BackupOneOffEvent(
    val id: String,
    val courseId: String? = null,
    val title: String,
    val dateEpochDay: Long,
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
data class BackupStudyTask(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String? = null,
    val dueDateEpochDay: Long? = null,
    val completed: Boolean,
)

@Serializable
data class BackupCourseNote(
    val id: String,
    val courseId: String,
    val title: String,
    val content: String,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class BackupSettings(
    val cancellationStyle: String = "STRIKETHROUGH",
    val showHiddenLessons: Boolean = false,
    val parityOverride: String = "AUTO",
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
        require(payload.version == BackupPayload.CURRENT_VERSION) {
            "Unsupported Takt backup version: ${payload.version}"
        }
        return payload
    }
}
