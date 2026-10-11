package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Explicitly choosing an existing canonical record must not adopt a rejected duplicate later. */
@Entity(tableName = "course_import_decisions")
data class CourseImportDecisionEntity(@PrimaryKey val courseId: String)
