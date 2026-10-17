package com.kpyruy.takt.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class UisAccountLifecycleTest {
    @Suppress("UNCHECKED_CAST")
    @Test fun logoutCancelsAnImportPausedBeforeRoomWriteAndKeepsLocalCourses() = runBlocking {
        org.junit.Assume.assumeTrue(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk_gphone") || android.os.Build.HARDWARE == "ranchu")
        org.junit.Assume.assumeFalse(java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias("takt_uis_credentials_v1"))
        val app = ApplicationProvider.getApplicationContext<Context>()
        val prefix = "uis-lifecycle-${System.nanoTime()}"
        val context = object : android.content.ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) = app.getSharedPreferences("$prefix-$name", mode)
        }
        val db = Room.inMemoryDatabaseBuilder(context, TaktDatabase::class.java).build()
        val real = RoomStudyPlanRepository(db)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val delayed = object : StudyPlanRepository by real {
            override suspend fun upsertImportedCourses(courses: List<Course>, selectedLocalIds: Map<String, String>): Int {
                entered.complete(Unit)
                release.await() // A real suspension immediately before the real Room import.
                return real.upsertImportedCourses(courses, selectedLocalIds)
            }
        }
        val account = UniversityAccountRepository(context, delayed)
        val remote = Course("uis:999:101", "SYNTHETIC", "Synthetic UIS", 5, 1, CourseStatus.ENROLLED)
        val field = UniversityAccountRepository::class.java.getDeclaredField("mutablePreview").apply { isAccessible = true }
        val previews = field.get(account) as MutableStateFlow<UisSyncPreview?>
        try {
            val localId = real.addCourse("Local course", "LOCAL", 3, 1)
            previews.value = UisSyncPreview(listOf(remote), real.observeCourses().first(), 1,
                null, null, emptyList(), emptyList(), emptyList(), emptyList(), null, null, null, null)
            val import = launch(Dispatchers.Default) { account.applyCourse(remote.id) }
            withTimeout(5_000) { entered.await() }
            withTimeout(5_000) { account.remove() }
            release.complete(Unit)
            import.join()
            assertEquals("An old UIS import wrote after logout", listOf(localId), real.observeCourses().first().map { it.id })
            assertNull("An old action restored account preview after logout", account.syncPreview.value)
            assertFalse(account.syncAction.value.running)
            assertNull(account.syncAction.value.message)
            assertNull(account.syncAction.value.error)
            assertFalse(account.hasAccount.value)
        } finally {
            release.complete(Unit)
            account.remove()
            context.getSharedPreferences("takt_university_account", 0).edit().clear().commit()
            db.close()
        }
    }
}
