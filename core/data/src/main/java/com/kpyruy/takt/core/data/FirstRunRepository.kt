package com.kpyruy.takt.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Tracks setup separately from the user's courses and backup data. */
class FirstRunRepository(context: Context, private val data: TaktDataContainer) {
    private val preferences = context.applicationContext.getSharedPreferences("takt_onboarding", Context.MODE_PRIVATE)

    suspend fun shouldShow(): Boolean {
        data.backupRepository.recoverPendingRestore()
        if (preferences.getBoolean("complete", false)) return false
        val existing = data.database.courseDao().count() > 0 || data.database.scheduleDao().countRules() > 0 ||
            data.database.scheduleDao().countOneOffEvents() > 0
        if (existing) complete()
        return !existing
    }

    suspend fun complete() = withContext(Dispatchers.IO) {
        check(preferences.edit().putBoolean("complete", true).commit()) { "Не вдалося завершити налаштування" }
    }
}
