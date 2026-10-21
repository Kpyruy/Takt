package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.database.TaktDatabase

/** Call inside the same transaction as a dependency write so import cannot race a stale editor. */
internal suspend fun TaktDatabase.resolveCourseId(courseId: String): String {
    val actual = courseDao().getAliasesSnapshot().firstOrNull { it.oldId == courseId }?.courseId ?: courseId
    require(courseDao().getAllSnapshot().any { it.id == actual }) { UiKey.TEXT_THIS_COURSE_IS_NO_LONGER_AVAILABLE_REFRESH_THE_COURSE_LIST }
    return actual
}
