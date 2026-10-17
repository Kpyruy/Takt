package com.kpyruy.takt.core.data

import android.content.Context
import com.kpyruy.takt.core.model.ReminderCategory
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class StoredReminder(val id: String, val sourceKey: String, val category: String, val title: String,
    val courseTitle: String?, val occursAt: Long, val fireAt: Long, val offsetMinutes: Long, val dateOnly: Boolean,
    val localTarget: String? = null) {
    fun model() = PlannedReminder(id, sourceKey, ReminderCategory.valueOf(category), title, courseTitle,
        Instant.ofEpochMilli(occursAt), Instant.ofEpochMilli(fireAt), offsetMinutes, dateOnly, localTarget?.let(LocalDateTime::parse))
}

@Serializable
private data class DeliveryStamp(val key: String, val retainUntil: Long)

/** Access only from the coordinator's IO dispatcher under its mutex. */
class ReminderAlarmStore(context: Context, name: String = "takt_reminder_alarms_v1") {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    fun read(): List<PlannedReminder> = runCatching {
        json.decodeFromString<List<StoredReminder>>(preferences.getString("planned", null) ?: "[]").map { it.model() }
    }.getOrDefault(emptyList())
    fun write(planned: List<PlannedReminder>) {
        val raw = json.encodeToString(planned.map { StoredReminder(it.id, it.sourceKey, it.category.name, it.title,
            it.courseTitle, it.occursAt.toEpochMilli(), it.fireAt.toEpochMilli(), it.offsetMinutes, it.dateOnly, it.localTarget?.toString()) })
        if (preferences.getString("planned", null) != raw) check(preferences.edit().putString("planned", raw).commit())
    }
    fun delivered(now: Instant): Set<String> = stamps().filter { it.retainUntil > now.toEpochMilli() }.map { it.key }.toSet()
    fun markDelivered(reminder: PlannedReminder, now: Instant) {
        val retained = stamps().filter { it.retainUntil > now.toEpochMilli() && it.key != reminder.deliveryKey }
        val next = retained + DeliveryStamp(reminder.deliveryKey, reminder.occursAt.plusSeconds(86_400).toEpochMilli())
        check(preferences.edit().putString("delivered", json.encodeToString(next)).commit())
    }
    fun prune(now: Instant) {
        val original = stamps()
        val retained = original.filter { it.retainUntil > now.toEpochMilli() }
        if (retained != original) check(preferences.edit().putString("delivered", json.encodeToString(retained)).commit())
    }
    private fun stamps(): List<DeliveryStamp> = runCatching {
        json.decodeFromString<List<DeliveryStamp>>(preferences.getString("delivered", null) ?: "[]")
    }.getOrDefault(emptyList())
}
