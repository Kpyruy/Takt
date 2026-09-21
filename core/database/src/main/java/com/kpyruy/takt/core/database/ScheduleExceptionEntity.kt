package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_exceptions")
data class ScheduleExceptionEntity(
    @PrimaryKey val id: String,
    val ruleId: String,
    val dateEpochDay: Long,
    val type: String,
    val replacementDateEpochDay: Long?,
    val replacementStartMinute: Int?,
    val replacementEndMinute: Int?,
    val replacementRoom: String?,
)
