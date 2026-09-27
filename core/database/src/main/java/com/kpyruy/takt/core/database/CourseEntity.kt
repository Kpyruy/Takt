package com.kpyruy.takt.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val code: String,
    val title: String,
    val credits: Int,
    val semester: Int,
    val status: String,
    val requirementType: String,
    val syllabusUrl: String?,
    @ColumnInfo(defaultValue = "'CONTINUOUS_LETTER'")
    val gradingType: String = "CONTINUOUS_LETTER",
    val passFailResult: String? = null,
    val iconKey: String? = null,
    val titleEn: String? = null,
    val titleSk: String? = null,
)
