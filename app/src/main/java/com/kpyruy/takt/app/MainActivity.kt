package com.kpyruy.takt.app

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.data.DocumentSyncStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dataContainer = (application as TaktApplication).dataContainer
        setContent {
            var showOnboarding by remember { mutableStateOf<Boolean?>(null) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(dataContainer) {
                showOnboarding = dataContainer.firstRunRepository.shouldShow()
            }
            val settings by dataContainer.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )

            TaktTheme(settings = settings) {
                val lightSystemBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightSystemBars
                        isAppearanceLightNavigationBars = lightSystemBars
                    }
                }
                when (showOnboarding) {
                    true -> FirstRunScreen(
                        settings = settings,
                        onFinish = { appearance -> scope.launch {
                            dataContainer.settingsRepository.setAppearance(
                                appearance.themeMode, appearance.themeFamily, appearance.cardAppearance)
                            dataContainer.firstRunRepository.complete()
                            showOnboarding = false
                        } },
                        onRestore = { uri ->
                            runCatching {
                                when (dataContainer.documentStore.connect(uri)) {
                                    DocumentSyncStatus.READY -> {
                                        if (dataContainer.studyPlanRepository.observeCourses().first().isNotEmpty()) {
                                            dataContainer.firstRunRepository.complete()
                                            showOnboarding = false
                                            null
                                        } else "У копії немає предметів. Налаштуй Takt для себе."
                                    }
                                    DocumentSyncStatus.CONFLICT -> "Дані відрізняються. Перевір копію в налаштуваннях."
                                    else -> "Не вдалося відновити дані"
                                }
                            }.getOrElse { it.message ?: "Не вдалося відновити дані" }
                        },
                    )
                    false -> TaktApp(
                        repository = dataContainer.studyPlanRepository,
                        scheduleRepository = dataContainer.scheduleRepository,
                        gradeRepository = dataContainer.gradeRepository,
                        studyContentRepository = dataContainer.studyContentRepository,
                        examRepository = dataContainer.examRepository,
                        settingsRepository = dataContainer.settingsRepository,
                        backupRepository = dataContainer.backupRepository,
                        documentStore = dataContainer.documentStore,
                    )
                    null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
