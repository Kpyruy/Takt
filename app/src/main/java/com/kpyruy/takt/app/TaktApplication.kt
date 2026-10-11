package com.kpyruy.takt.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kpyruy.takt.core.data.TaktDataContainer
import com.kpyruy.takt.core.data.AppReleaseRepository
import com.kpyruy.takt.core.data.GitHubReleaseClient
import com.kpyruy.takt.core.data.SharedPreferencesAppReleaseStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaktApplication : Application() {
    /** Survives activity recreation, but is reset when Android starts a new process. */
    var deviceUnlocked by mutableStateOf(false)

    lateinit var dataContainer: TaktDataContainer
        private set

    lateinit var appReleaseRepository: AppReleaseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        dataContainer = TaktDataContainer(this)
        val installedVersion = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        val releaseClient = GitHubReleaseClient.forPublicReleases()
        appReleaseRepository = AppReleaseRepository(installedVersion, SharedPreferencesAppReleaseStore(this), releaseClient::latest)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            dataContainer.documentStore.startAutoSync(this)
        }
    }
}
