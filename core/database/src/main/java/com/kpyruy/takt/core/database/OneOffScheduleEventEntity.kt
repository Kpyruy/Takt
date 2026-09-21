package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_one_off")
data class OneOffScheduleEventEntity(
    @PrimaryKey val id: String,
    val courseId: String?,
    val title: String,
    val dateEpochDay: Long,
    val startMinute: Int,
    val endMinute: Int,
    val room: String?,
    val type: String,
)
