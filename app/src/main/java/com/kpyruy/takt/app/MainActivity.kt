package com.kpyruy.takt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kpyruy.takt.core.ui.theme.TaktTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dataContainer = (application as TaktApplication).dataContainer
        setContent {
            TaktTheme {
                TaktApp(
                    repository = dataContainer.studyPlanRepository,
                    scheduleRepository = dataContainer.scheduleRepository,
                    gradeRepository = dataContainer.gradeRepository,
                    studyContentRepository = dataContainer.studyContentRepository,
                    settingsRepository = dataContainer.settingsRepository,
                )
            }
        }
    }
}
