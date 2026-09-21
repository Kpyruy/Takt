package com.kpyruy.takt.app

import android.app.Application
import com.kpyruy.takt.core.data.TaktDataContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaktApplication : Application() {
    lateinit var dataContainer: TaktDataContainer
        private set

    override fun onCreate() {
        super.onCreate()
        dataContainer = TaktDataContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            dataContainer.seedIfNeeded()
        }
    }
}
