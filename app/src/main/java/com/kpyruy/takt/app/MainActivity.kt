package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.i18n.t

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Bundle
import android.content.res.Configuration
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
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
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.core.ui.theme.taktWindowBackgroundColor
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.data.DocumentSyncStatus
import com.kpyruy.takt.core.model.DeviceAuthenticationResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : FragmentActivity() {
    private lateinit var deviceAuthenticator: DeviceAuthenticator
    private var foreground by mutableStateOf(false)
    private var unlocked by mutableStateOf(false)
    private var unlockMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceAuthenticator = DeviceAuthenticator(this)
        enableEdgeToEdge()

        val dataContainer = (application as TaktApplication).dataContainer
        val initialSettings = dataContainer.settingsRepository.settings.value
        val systemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawable(taktWindowBackgroundColor(initialSettings, systemDark).toDrawable())
        TaktI18n.use(initialSettings.language)
        setContent {
            var showOnboarding by remember { mutableStateOf<Boolean?>(null) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(dataContainer) {
                showOnboarding = dataContainer.firstRunRepository.shouldShow()
            }
            val settings by dataContainer.settingsRepository.settings.collectAsStateWithLifecycle()
            val hasUniversityAccount by dataContainer.universityAccountRepository.hasAccount.collectAsStateWithLifecycle()
            SideEffect { TaktI18n.use(settings.language) }
            LaunchedEffect(hasUniversityAccount, unlocked, foreground) {
                if (hasUniversityAccount && !unlocked && foreground && !deviceAuthenticator.isShowing) {
                    requestUnlock()
                }
            }

            TaktTheme(settings = settings) {
                val lightSystemBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
                SideEffect {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightSystemBars
                        isAppearanceLightNavigationBars = lightSystemBars
                    }
                }
                when {
                    hasUniversityAccount && !unlocked -> DeviceLockScreen(unlockMessage, ::requestUnlock)
                    showOnboarding == true -> FirstRunScreen(
                        settings = settings,
                        universityAccountRepository = dataContainer.universityAccountRepository,
                        authenticateDevice = ::authenticateForAccount,
                        onFinish = { appearance -> scope.launch {
                            dataContainer.settingsRepository.setAppearance(
                                appearance.themeMode, appearance.themeFamily, appearance.cardAppearance)
                            dataContainer.settingsRepository.setLanguage(appearance.language)
                            dataContainer.firstRunRepository.complete()
                            showOnboarding = false
                        } },
                        onRestore = { uri ->
                            runCatching {
                                when (dataContainer.documentStore.connect(uri)) {
                                    DocumentSyncStatus.READY -> {
                                        if (dataContainer.studyPlanRepository.observeCourses().first().isNotEmpty()) {
                                            dataContainer.awaitRestoredPlanning()
                                            dataContainer.firstRunRepository.complete()
                                            showOnboarding = false
                                            null
                                        } else t("У копії немає предметів. Налаштуй Takt для себе.")
                                    }
                                    DocumentSyncStatus.CONFLICT -> t("Дані відрізняються. Перевір копію в налаштуваннях.")
                                    else -> t("Не вдалося відновити дані")
                                }
                            }.getOrElse { it.message ?: t("Не вдалося відновити дані") }
                        },
                    )
                    showOnboarding == false -> TaktApp(
                        planningSnapshot = dataContainer.planningSnapshot,
                        repository = dataContainer.studyPlanRepository,
                        scheduleRepository = dataContainer.scheduleRepository,
                        gradeRepository = dataContainer.gradeRepository,
                        studyContentRepository = dataContainer.studyContentRepository,
                        examRepository = dataContainer.examRepository,
                        settingsRepository = dataContainer.settingsRepository,
                        backupRepository = dataContainer.backupRepository,
                        documentStore = dataContainer.documentStore,
                        universityAccountRepository = dataContainer.universityAccountRepository,
                        authenticateDevice = ::authenticateForAccount,
                    )
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        foreground = true
    }

    override fun onStop() {
        foreground = false
        if (::deviceAuthenticator.isInitialized && !deviceAuthenticator.isShowing) unlocked = false
        super.onStop()
    }

    private fun requestUnlock() {
        if (deviceAuthenticator.isShowing) return
        deviceAuthenticator.authenticate(t("Розблокувати Takt")) { result ->
            when (result) {
                DeviceAuthenticationResult.SUCCESS -> { unlocked = true; unlockMessage = null }
                DeviceAuthenticationResult.CANCELLED -> unlockMessage = t("Підтвердження скасовано")
                DeviceAuthenticationResult.DEVICE_LOCK_REQUIRED -> unlockMessage =
                    t("Для доступу налаштуй PIN або пароль телефона")
                DeviceAuthenticationResult.ERROR -> unlockMessage = t("Не вдалося підтвердити особу")
            }
        }
    }

    private fun authenticateForAccount(
        title: String,
        onResult: (DeviceAuthenticationResult) -> Unit,
    ) {
        deviceAuthenticator.authenticate(title) { result ->
            if (result == DeviceAuthenticationResult.SUCCESS) unlocked = true
            onResult(result)
        }
    }
}

@androidx.compose.runtime.Composable
private fun DeviceLockScreen(message: String?, onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Text(t("Takt заблоковано"), style = MaterialTheme.typography.headlineMedium)
            Text(t("Підтвердь особу через захист телефона, щоб відкрити свої дані."),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onUnlock) { Text(t("Розблокувати")) }
        }
    }
}
