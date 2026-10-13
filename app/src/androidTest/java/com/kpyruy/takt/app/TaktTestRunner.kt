package com.kpyruy.takt.app

import android.content.Context
import android.app.Application
import androidx.test.runner.AndroidJUnitRunner
import kotlinx.coroutines.runBlocking
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.i18n.TaktI18n

/** Keep existing screen tests on their explicit review fixture. */
class TaktTestRunner : AndroidJUnitRunner() {
    override fun callApplicationOnCreate(app: Application) {
        super.callApplicationOnCreate(app)
        // Performance checks exercise the real fresh-install setup, not the seeded review route.
        if (androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("performanceFreshStart") == "true") return
        app.getSharedPreferences("takt_onboarding", Context.MODE_PRIVATE)
            .edit().putBoolean("complete", true).commit()
        // Use the Application-owned container: constructing and closing another container
        // races its eagerly collecting planning flows against the closed Room database.
        val data = (app as TaktApplication).dataContainer
        runBlocking { data.seedIfNeeded() }
        // Standalone component tests have no MainActivity to apply their review language.
        // MainActivity still reads and applies the actual saved user preference itself.
        TaktI18n.use(AppLanguage.UKRAINIAN)
    }
}
