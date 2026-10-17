package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class TeachingBackupUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private val teacher = BackupTeacher("71659", "Ing. Martin Bartoň, PhD.", listOf("instructor"))
    private val snapshot get() = BackupTeaching(
        listOf(BackupTeachingTeam("TPAR_6B", listOf(teacher))),
        listOf(BackupTeacherProfile("71659", "SLOVAK", teacher.name, officeNumber = "T02 4.108", consultationHours = "Pondelok 10:00–11:00")))
    private fun seed() = runBlocking {
        data.seedLiquidGlassReview()
        data.database.courseDao().insertAll(data.database.courseDao().getAllSnapshot().map {
            if (it.id == "glass-programming") it.copy(code = "TPAR_6B", title = "Technické prostriedky automatického riadenia",
                syllabusUrl = "https://is.stuba.sk/auth/katalog/syllabus.pl?predmet=441254") else it
        })
        data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
        data.teachingStore.replace(snapshot)
        data.awaitRestoredPlanning()
    }
    @Test fun roomBackupRestoresTeachingAndConsultationsIntoTheLiveRepositoryAndRestart() {
        seed()
        val actual = data.universityAccountRepository.teachingRepository
        runBlocking {
            val raw = data.backupRepository.exportJson()
            val payload = BackupPayloadCodec.decode(raw)
            assertEquals(snapshot, payload.teaching)
            assertFalse(raw.contains("UISAuth"))
            data.teachingStore.replace(BackupTeaching())
            data.backupRepository.importJson(raw)
            val course = data.studyPlanRepository.observeCourses().first().first { it.code == "TPAR_6B" }
            assertEquals(teacher.name, actual.savedTeachers(course)?.single()?.name)
            assertEquals("Pondelok 10:00–11:00", actual.savedProfile(teacher.toModel(), AppLanguage.SLOVAK)?.consultationHours)
            val restarted = TeachingStore(File(compose.activity.filesDir, "teaching.json"))
            assertEquals(snapshot, restarted.snapshot.value)
            data.backupRepository.importJson(BackupPayloadCodec.encode(payload.copy(version = 9, teaching = null)))
            assertEquals(snapshot, data.teachingStore.snapshot.value)
        }
    }
    @Test fun savedTeachersAndHoursShowBeforeNetworkFinishesThenRefreshChangesThem() {
        seed()
        val teamCalls = AtomicInteger(); val profileCalls = AtomicInteger()
        val teamGate = CompletableDeferred<Unit>(); val profileGate = CompletableDeferred<Unit>()
        val actual = data.universityAccountRepository.teachingRepository
        val directory = object : CourseTeachingRepository by actual {
            override suspend fun teachers(course: Course, refresh: Boolean): List<CourseTeacher> {
                assertTrue(refresh); teamCalls.incrementAndGet(); teamGate.await()
                data.teachingStore.replace(snapshot.copy(teams = listOf(BackupTeachingTeam("TPAR_6B", listOf(teacher.copy(name = "Ing. Martin Bartoň – aktualizované"))))))
                return actual.savedTeachers(course)!!
            }
            override suspend fun profile(teacher: CourseTeacher, language: AppLanguage, refresh: Boolean): TeacherProfile {
                assertTrue(refresh); profileCalls.incrementAndGet(); profileGate.await()
                val next = snapshot.copy(profiles = snapshot.profiles.map { it.copy(consultationHours = "Utorok 12:00–13:00") })
                data.teachingStore.replace(next)
                return actual.savedProfile(teacher, language)!!
            }
            override suspend fun photo(personId: String): ByteArray? = null
        }
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            TaktI18n.use(AppLanguage.SLOVAK)
            TaktTheme(AppSettings(language = AppLanguage.SLOVAK, themeMode = AppThemeMode.DARK, cardAppearance = CardAppearance.GLASS)) {
                TaktGlassBackground {
                    SubjectDetailScreen(data.studyPlanRepository, data.gradeRepository, data.scheduleRepository,
                        data.settingsRepository, data.studyContentRepository, data.examRepository, data.documentStore,
                        uisManaged = true, courseId = "glass-programming", onBack = {}, teachingRepository = directory)
                }
            }
        } }
        try {
            compose.waitUntil(8_000) { teamCalls.get() == 1 }
            compose.onNodeWithTag("course-teacher-71659").performScrollTo().assertIsDisplayed()
            assertFalse(teamGate.isCompleted)
            capture("teaching-saved-team.png")
            teamGate.complete(Unit)
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Ing. Martin Bartoň – aktualizované").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("course-teacher-71659").performClick()
            compose.waitUntil(5_000) { profileCalls.get() == 1 }
            compose.onNodeWithTag("teacher-hours").performScrollTo()
            compose.onNodeWithText("Pondelok 10:00–11:00").assertIsDisplayed()
            assertFalse(profileGate.isCompleted)
            capture("teaching-saved-hours.png")
            profileGate.complete(Unit)
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Utorok 12:00–13:00").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Utorok 12:00–13:00").assertIsDisplayed()
            capture("teaching-refreshed-hours.png")
            assertEquals(1, teamCalls.get()); assertEquals(1, profileCalls.get())
        } finally { teamGate.complete(Unit); profileGate.complete(Unit) }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(200)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
