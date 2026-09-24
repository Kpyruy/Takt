package com.kpyruy.takt.app

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.TaktDataContainer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialSeedPersistenceTest {
    @Test
    fun emptyScheduleAfterFirstLaunchStaysEmpty() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation()
            .targetContext.createDeviceProtectedStorageContext()
        context.deleteDatabase("takt.db")
        context.getSharedPreferences("takt_bootstrap", Context.MODE_PRIVATE).edit().clear().commit()
        val data = TaktDataContainer(context)
        try {
            data.seedIfNeeded()
            assertTrue(data.database.scheduleDao().countRules() > 0)
            data.database.scheduleDao().deleteAllRules()
            data.database.scheduleDao().deleteAllOneOffEvents()
            data.seedIfNeeded()
            assertEquals(0, data.database.scheduleDao().countRules())
            assertEquals(0, data.database.scheduleDao().countOneOffEvents())
        } finally {
            data.database.close()
            context.deleteDatabase("takt.db")
            context.getSharedPreferences("takt_bootstrap", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
