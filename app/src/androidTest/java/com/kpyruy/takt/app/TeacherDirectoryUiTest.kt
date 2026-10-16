package com.kpyruy.takt.app

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.CourseTeachingRepository
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import com.kpyruy.takt.feature.subjects.teacherEmailIntent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TeacherDirectoryUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private val team = listOf(
        CourseTeacher("71659", "Ing. Martin Bartoň, PhD.", listOf("examiner", "instructor", "lecturer")),
        CourseTeacher("116374", "Ing. Lucia Ďurišová-Rubanská", listOf("instructor")),
        CourseTeacher("111582", "Ing. Lukáš Gendiar", listOf("instructor")),
        CourseTeacher("2758", "prof. Mgr. Róbert Vrábeľ, PhD.", listOf("person responsible for course")),
    )

    private fun showSubject(failProfileOnce: Boolean = false) {
        runBlocking {
            data.seedLiquidGlassReview()
            data.database.courseDao().insertAll(data.database.courseDao().getAllSnapshot().map {
                if (it.id == "glass-physics") it.copy(syllabusUrl = "https://is.stuba.sk/auth/katalog/syllabus.pl?predmet=441254") else it
            })
            data.studyContentRepository.upsertNote(CourseNote("teacher-note", "glass-physics", "This note belongs only in Notes", "Preview must stay hidden", 0))
        }
        var fail = failProfileOnce
        val directory = object : CourseTeachingRepository {
            override suspend fun teachers(course: Course, refresh: Boolean) = team
            override suspend fun profile(teacher: CourseTeacher, language: AppLanguage, refresh: Boolean): TeacherProfile {
                if (fail) { fail = false; throw java.io.IOException("offline") }
                return TeacherProfile(teacher.personId, teacher.name,
                    "Odborný asistent CSc.,PhD. · UIAM MTF STU",
                    universityEmail = "martin.barton@stuba.sk", systemEmail = "xbarton@is.stuba.sk",
                    officeAddress = "UIAM MTF, Ulica Jána Bottu 2781/25, 91724 Trnava",
                    officeNumber = "T02 4.108", officePhone = "+421 906 068 422",
                    mailboxLastChecked = "10/15/2026 07:39")
            }
        }
        compose.activityRule.scenario.onActivity { activity ->
            TaktI18n.use(AppLanguage.SLOVAK)
            activity.setContent {
                TaktTheme(AppSettings(language = AppLanguage.SLOVAK, themeMode = AppThemeMode.DARK, themeFamily = ThemeFamily.BLUE)) {
                    TaktGlassBackground {
                        SubjectDetailScreen(data.studyPlanRepository, data.gradeRepository, data.scheduleRepository,
                            data.settingsRepository, data.studyContentRepository, data.examRepository, data.documentStore,
                            uisManaged = true, courseId = "glass-physics", onBack = {}, teachingRepository = directory)
                    }
                }
            }
        }
        compose.waitUntil(8_000) { compose.onAllNodesWithTag("course-teachers").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun overviewHasAllFourTeachersInsteadOfNotePreviewsAndProfileShowsClickableContacts() {
        showSubject()
        compose.onNodeWithText("This note belongs only in Notes").assertDoesNotExist()
        for (teacher in team) compose.onNodeWithTag("course-teacher-${teacher.personId}").performScrollTo().assertIsDisplayed()
        capture("teachers-sk-list.png")
        compose.onNodeWithTag("course-teacher-71659").performScrollTo().performClick()
        compose.onNodeWithTag("teacher-email-university").performScrollTo().assertHasClickAction()
        compose.onNodeWithText("martin.barton@stuba.sk").assertIsDisplayed()
        compose.onNodeWithTag("teacher-email-uis").assertHasClickAction()
        compose.onNodeWithText("xbarton@is.stuba.sk").assertIsDisplayed()
        capture("teachers-sk-profile-top.png")
        compose.onNodeWithTag("teacher-office").performScrollTo()
        compose.onNodeWithText("T02 4.108").assertIsDisplayed()
        compose.onNodeWithTag("teacher-mailbox-checked").performScrollTo()
        compose.onNodeWithText("15.10.2026 07:39").assertIsDisplayed()
        compose.onNodeWithText("Neuvedené v UIS").assertExists()
        capture("teachers-sk-profile-office.png")
        val intent = teacherEmailIntent("martin.barton@stuba.sk")
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("mailto", intent.data!!.scheme)
        assertEquals("martin.barton@stuba.sk", intent.data!!.schemeSpecificPart)
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("teacher-profile").assertDoesNotExist()
        compose.onNodeWithText("Poznámky").performClick()
        compose.onNodeWithText("This note belongs only in Notes").assertIsDisplayed()
    }

    @Test fun aFailedProfileCanBeRetriedWithoutLosingTheTeachingTeam() {
        showSubject(failProfileOnce = true)
        compose.onNodeWithTag("course-teacher-71659").performScrollTo().performClick()
        compose.onNodeWithTag("teacher-retry").performScrollTo().performClick()
        compose.onNodeWithTag("teacher-email-university").performScrollTo().assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(80)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
