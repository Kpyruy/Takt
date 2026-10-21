package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Bundle
import android.content.res.Configuration
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kpyruy.takt.core.data.BackupRestoreStatus
import com.kpyruy.takt.core.data.UisAutoSyncOutcome
import com.kpyruy.takt.core.model.DeviceAuthenticationResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : FragmentActivity() {
    private lateinit var deviceAuthenticator: DeviceAuthenticator
    private var foreground by mutableStateOf(false)
    private var appEntry by mutableStateOf(0)
    private var unlockMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceAuthenticator = DeviceAuthenticator(this)
        enableEdgeToEdge()

        val taktApplication = application as TaktApplication
        val dataContainer = taktApplication.dataContainer
        val initialSettings = dataContainer.settingsRepository.settings.value
        val systemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawable(taktWindowBackgroundColor(initialSettings, systemDark).toDrawable())
        TaktI18n.use(initialSettings.language)
        setContent {
            PreferResponsiveDisplay(window)
            var showOnboarding by remember { mutableStateOf<Boolean?>(null) }
            var showGuidedTour by rememberSaveable { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            val restoreStatus by dataContainer.backupRepository.restoreStatus.collectAsStateWithLifecycle()
            var showRecovery by remember { mutableStateOf(false) }
            LaunchedEffect(restoreStatus) {
                if (restoreStatus == BackupRestoreStatus.RECOVERY_REQUIRED) showRecovery = true
                if (restoreStatus == BackupRestoreStatus.READY) showRecovery = false
                if (showOnboarding == null && restoreStatus != BackupRestoreStatus.RECOVERY_REQUIRED) {
                    try { showOnboarding = dataContainer.firstRunRepository.shouldShow() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { showRecovery = true }
                }
            }
            if (showRecovery) AlertDialog(onDismissRequest = {},
                title = { Text(t(UiKey.TEXT_DATA_RECOVERY_IS_UNFINISHED)) },
                text = { Text(t(UiKey.TEXT_CHECK_THE_AVAILABLE_STORAGE_ON_YOUR_DEVICE_AND_RETRY_RECOVERY_B0444189)) },
                confirmButton = { Button(enabled = restoreStatus != BackupRestoreStatus.RESTORING, onClick = {
                    scope.launch {
                        try {
                            dataContainer.backupRepository.recoverPendingRestore()
                            if (showOnboarding == null) showOnboarding = dataContainer.firstRunRepository.shouldShow()
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { showRecovery = true }
                    }
                }) { Text(t(UiKey.TEXT_RETRY_RECOVERY)) } })
            val settings by dataContainer.settingsRepository.settings.collectAsStateWithLifecycle()
            val releaseState by taktApplication.appReleaseRepository.state.collectAsStateWithLifecycle()
            val hasUniversityAccount by dataContainer.universityAccountRepository.hasAccount.collectAsStateWithLifecycle()
            // Setup owns its unsaved language draft until the final settings are committed.
            SideEffect { if (showOnboarding != true) TaktI18n.use(settings.language) }
            LaunchedEffect(hasUniversityAccount, taktApplication.deviceUnlocked, foreground) {
                if (hasUniversityAccount && !taktApplication.deviceUnlocked && foreground && !deviceAuthenticator.isShowing) {
                    requestUnlock()
                }
            }
            LaunchedEffect(hasUniversityAccount, taktApplication.deviceUnlocked, foreground, showOnboarding, showGuidedTour, appEntry, restoreStatus) {
                if (restoreStatus == BackupRestoreStatus.READY && hasUniversityAccount && taktApplication.deviceUnlocked && foreground && showOnboarding == false && !showGuidedTour) {
                    val repository = dataContainer.universityAccountRepository
                    val outcome = try { repository.runAutoSyncIfDue() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { UisAutoSyncOutcome.FAILED }
                    if (outcome == UisAutoSyncOutcome.NEEDS_DEVICE_AUTH) {
                        val result = suspendCancellableCoroutine<DeviceAuthenticationResult> { continuation ->
                            authenticateForAccount(t(UiKey.TEXT_CHECK_UIS)) { value ->
                                if (continuation.isActive) continuation.resume(value)
                            }
                        }
                        if (result == DeviceAuthenticationResult.SUCCESS) {
                            try { repository.runAutoSyncIfDue() }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { /* The next foreground check can retry. */ }
                        }
                    }
                }
            }

            LaunchedEffect(hasUniversityAccount, taktApplication.deviceUnlocked, foreground,
                showOnboarding, showGuidedTour, appEntry, releaseState.automaticChecks) {
                if ((!hasUniversityAccount || taktApplication.deviceUnlocked) && foreground && showOnboarding == false && !showGuidedTour) {
                    taktApplication.appReleaseRepository.check()
                }
            }

            TaktTheme(settings = settings) {
                if (showOnboarding != true || (hasUniversityAccount && !taktApplication.deviceUnlocked)) {
                    TaktSystemBars(window)
                }
                TaktGlassBackground(modifier = Modifier.fillMaxSize(),
                    enabled = showOnboarding != true || (hasUniversityAccount && !taktApplication.deviceUnlocked)) {
                    when {
                        hasUniversityAccount && !taktApplication.deviceUnlocked -> DeviceLockScreen(unlockMessage, ::requestUnlock)
                        showOnboarding == true -> FirstRunScreen(
                            settings = settings,
                            onSystemBarBackground = { window.applyTaktSystemBars(it) },
                            universityAccountRepository = dataContainer.universityAccountRepository,
                            authenticateDevice = ::authenticateForAccount,
                            onFinish = { appearance, startTour -> scope.launch {
                                dataContainer.settingsRepository.setAppearance(appearance)
                                dataContainer.settingsRepository.setLanguage(appearance.language)
                                dataContainer.firstRunRepository.complete()
                                showGuidedTour = startTour
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
                                            } else t(UiKey.TEXT_THERE_ARE_NO_SUBJECTS_IN_THE_COPY_CUSTOMIZE_TAKT_FOR_YOURSELF)
                                        }
                                        DocumentSyncStatus.CONFLICT -> t(UiKey.TEXT_DATA_DIFFERS_CHECK_THE_BACKUP_IN_SETTINGS)
                                        else -> t(UiKey.TEXT_FAILED_TO_RESTORE_DATA)
                                    }
                                }.getOrElse { it.message ?: uiMessage(UiKey.TEXT_FAILED_TO_RESTORE_DATA) }
                            },
                        )
                        showOnboarding == false && showGuidedTour -> GuidedTourScreen(
                            dataContainer, settings, taktApplication.appReleaseRepository, ::authenticateForAccount,
                            onFinish = { showGuidedTour = false })
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
                            appReleaseRepository = taktApplication.appReleaseRepository,
                            onStartTour = { showGuidedTour = true },
                        )
                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appEntry++
        foreground = true
    }

    override fun onStop() {
        foreground = false
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) {
            val taktApplication = application as TaktApplication
            taktApplication.deviceUnlocked = false
            taktApplication.dataContainer.universityAccountRepository.session.disconnect()
        }
        super.onDestroy()
    }

    private fun requestUnlock() {
        if (deviceAuthenticator.isShowing) return
        deviceAuthenticator.authenticate(t(UiKey.TEXT_UNLOCK_TAKT)) { result ->
            when (result) {
                DeviceAuthenticationResult.SUCCESS -> { (application as TaktApplication).deviceUnlocked = true; unlockMessage = null }
                DeviceAuthenticationResult.CANCELLED -> unlockMessage = uiMessage(UiKey.TEXT_VERIFICATION_CANCELLED)
                DeviceAuthenticationResult.DEVICE_LOCK_REQUIRED -> unlockMessage =
                    uiMessage(UiKey.TEXT_SET_UP_A_PHONE_PIN_OR_PASSWORD_TO_CONTINUE)
                DeviceAuthenticationResult.ERROR -> unlockMessage = uiMessage(UiKey.TEXT_COULD_NOT_VERIFY_YOUR_IDENTITY)
            }
        }
    }

    private fun authenticateForAccount(
        title: String,
        onResult: (DeviceAuthenticationResult) -> Unit,
    ) {
        deviceAuthenticator.authenticate(title) { result ->
            if (result == DeviceAuthenticationResult.SUCCESS) (application as TaktApplication).deviceUnlocked = true
            onResult(result)
        }
    }
}
