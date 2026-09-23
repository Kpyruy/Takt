package com.kpyruy.takt.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_rules")
data class ScheduleRuleEntity(
    @PrimaryKey val id: String,
    val courseId: String?,
    val title: String,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val recurrence: String,
    val room: String?,
    @ColumnInfo(defaultValue = "'UNSPECIFIED'")
    val lessonType: String = "UNSPECIFIED",
)
