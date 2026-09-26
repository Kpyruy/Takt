package com.kpyruy.takt.core.data

import android.content.Context

/** Tracks setup separately from the user's courses and backup data. */
class FirstRunRepository(context: Context, private val data: TaktDataContainer) {
    private val preferences = context.applicationContext.getSharedPreferences("takt_onboarding", Context.MODE_PRIVATE)

    suspend fun shouldShow(): Boolean {
        if (preferences.getBoolean("complete", false)) return false
        val existing = data.database.courseDao().count() > 0 || data.database.scheduleDao().countRules() > 0 ||
            data.database.scheduleDao().countOneOffEvents() > 0
        if (existing) complete()
        return !existing
    }

    fun complete() {
        check(preferences.edit().putBoolean("complete", true).commit()) { "Не вдалося завершити налаштування" }
    }
}
