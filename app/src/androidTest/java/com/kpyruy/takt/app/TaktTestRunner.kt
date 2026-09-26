package com.kpyruy.takt.app

import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.kpyruy.takt.core.data.TaktDataContainer
import kotlinx.coroutines.runBlocking

/** Keep existing screen tests on their explicit review fixture. */
class TaktTestRunner : AndroidJUnitRunner() {
    override fun onStart() {
        targetContext.getSharedPreferences("takt_onboarding", Context.MODE_PRIVATE)
            .edit().putBoolean("complete", true).commit()
        val data = TaktDataContainer(targetContext)
        runBlocking { data.seedIfNeeded() }
        data.database.close()
        super.onStart()
    }
}
