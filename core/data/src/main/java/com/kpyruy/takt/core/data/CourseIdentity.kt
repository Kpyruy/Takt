package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.TaktDatabase

/** Call inside the same transaction as a dependency write so import cannot race a stale editor. */
internal suspend fun TaktDatabase.resolveCourseId(courseId: String): String {
    val actual = courseDao().getAliasesSnapshot().firstOrNull { it.oldId == courseId }?.courseId ?: courseId
    require(courseDao().getAllSnapshot().any { it.id == actual }) { "Предмет більше не доступний. Онови список предметів." }
    return actual
}
