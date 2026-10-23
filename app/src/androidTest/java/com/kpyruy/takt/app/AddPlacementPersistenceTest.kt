package com.kpyruy.takt.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.data.SharedPreferencesAppSettingsRepository
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AddPlacementPersistenceTest {
    @Test fun selectedPlacementSurvivesReopenAndAtomicRestore() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "add-placement-${UUID.randomUUID()}"
        try {
            val repo = SharedPreferencesAppSettingsRepository(context, name)
            repo.setAppearance(AppSettings(addButtonPlacement = AddButtonPlacement.FLOATING))
            val reopened = SharedPreferencesAppSettingsRepository(context, name)
            assertEquals(AddButtonPlacement.FLOATING, reopened.settings.value.addButtonPlacement)
            val lease = reopened.beginBackupRestore()
            lease.persist(reopened.settings.value.copy(addButtonPlacement = AddButtonPlacement.CENTER))
            lease.publishAndRelease()
            assertEquals(AddButtonPlacement.CENTER, SharedPreferencesAppSettingsRepository(context, name).settings.value.addButtonPlacement)
        } finally { context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit() }
    }
}
