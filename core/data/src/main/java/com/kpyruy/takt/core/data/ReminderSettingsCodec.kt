package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import java.time.LocalTime
import kotlinx.serialization.Serializable

/** Small versioned preference record; startup does not parse notification JSON on the UI thread. */
object ReminderSettingsCodec {
    fun encode(settings: ReminderSettings): String = settings.normalized().let { clean ->
        listOf("v1", clean.enabled.toString(), clean.academicEnabled.toString(), clean.dateOnlyTime.toString(),
            ReminderCategory.entries.joinToString(";") { category -> clean.rule(category).let { rule ->
                "${category.name}:${rule.enabled}:${rule.offsetsMinutes.joinToString(",")}"
            } }).joinToString("|")
    }

    fun decode(raw: String?): ReminderSettings {
        if (raw == null) return ReminderSettings()
        return runCatching {
            val fields = raw.split('|')
            require(fields.size == 5 && fields[0] == "v1")
            val rules = fields[4].split(';').mapNotNull { record ->
                val parts = record.split(':')
                if (parts.size != 3) return@mapNotNull null
                val category = ReminderCategory.entries.firstOrNull { it.name == parts[0] || (it == ReminderCategory.TEST && parts[0] == "MIDTERM") }
                    ?: return@mapNotNull null
                category to ReminderRule(parts[1].toBooleanStrict(), parts[2].split(',').mapNotNull(String::toLongOrNull))
            }.toMap()
            ReminderSettings(fields[1].toBooleanStrict(), fields[2].toBooleanStrict(), LocalTime.parse(fields[3]), rules).normalized()
        }.getOrDefault(ReminderSettings())
    }
}

@Serializable
data class BackupReminderSettings(
    val enabled: Boolean = false,
    val academicEnabled: Boolean = true,
    val dateOnlyTime: String = "09:00",
    val rules: List<BackupReminderRule> = emptyList(),
)

@Serializable
data class BackupReminderRule(val category: String, val enabled: Boolean = true, val offsetsMinutes: List<Long> = emptyList())

internal fun ReminderSettings.toBackup(): BackupReminderSettings = normalized().let { clean ->
    BackupReminderSettings(clean.enabled, clean.academicEnabled, clean.dateOnlyTime.toString(),
        ReminderCategory.entries.map { category -> clean.rule(category).let {
            BackupReminderRule(category.name, it.enabled, it.offsetsMinutes)
        } })
}

internal fun BackupReminderSettings.toModel(): ReminderSettings = ReminderSettings(enabled, academicEnabled,
    runCatching { LocalTime.parse(dateOnlyTime) }.getOrDefault(LocalTime.of(9, 0)),
    rules.mapNotNull { record ->
        ReminderCategory.entries.firstOrNull { it.name == record.category || (it == ReminderCategory.TEST && record.category == "MIDTERM") }
            ?.let { it to ReminderRule(record.enabled, record.offsetsMinutes).normalized() }
    }.toMap()).normalized()
