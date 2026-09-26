package com.kpyruy.takt.app

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.TaktDataContainer
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialSeedPersistenceTest {
    @Test
    fun freshInstallStaysEmptyAndAcceptsOwnCourse() = runBlocking {
        val deviceContext = InstrumentationRegistry.getInstrumentation()
            .targetContext.createDeviceProtectedStorageContext()
        val context = object : ContextWrapper(deviceContext) {
            override fun getApplicationContext(): Context = this
        }
        context.deleteDatabase("takt.db")
        context.getSharedPreferences("takt_onboarding", Context.MODE_PRIVATE).edit().clear().commit()
        val data = TaktDataContainer(context)
        try {
            assertTrue(data.firstRunRepository.shouldShow())
            assertEquals(0, data.database.courseDao().count())
            assertEquals(0, data.database.scheduleDao().countRules())
            assertEquals(0, data.database.scheduleDao().countOneOffEvents())
            val id = data.studyPlanRepository.addCourse("Математика", "MAT_1", 5, 1)
            assertEquals(1, data.database.courseDao().count())
            assertTrue(runCatching { data.studyPlanRepository.addCourse("Інша", "mat_1", 3, 1) }.isFailure)
            data.scheduleRepository.upsertRule(ScheduleRule("own-lesson", id, "Математика",
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 30), ScheduleRecurrence.WEEKLY))
            assertEquals(1, data.database.scheduleDao().countRules())
            assertTrue(!data.firstRunRepository.shouldShow())
        } finally {
            data.database.close()
            context.deleteDatabase("takt.db")
            context.getSharedPreferences("takt_onboarding", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
