package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Old routes remain resolvable after UIS adopts a local course. */
@Entity(tableName = "course_id_aliases")
data class CourseIdAliasEntity(@PrimaryKey val oldId: String, val courseId: String)
