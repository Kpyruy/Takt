package com.kpyruy.takt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.ui.theme.TaktTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dataContainer = (application as TaktApplication).dataContainer
        setContent {
            val settings by dataContainer.settingsRepository.settings.collectAsState(
                initial = AppSettings()
            )

            TaktTheme(settings = settings) {
                val lightSystemBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightSystemBars
                        isAppearanceLightNavigationBars = lightSystemBars
                    }
                }
                TaktApp(
                    repository = dataContainer.studyPlanRepository,
                    scheduleRepository = dataContainer.scheduleRepository,
                    gradeRepository = dataContainer.gradeRepository,
                    studyContentRepository = dataContainer.studyContentRepository,
                    examRepository = dataContainer.examRepository,
                    settingsRepository = dataContainer.settingsRepository,
                    backupRepository = dataContainer.backupRepository,
                )
            }
        }
    }
}
