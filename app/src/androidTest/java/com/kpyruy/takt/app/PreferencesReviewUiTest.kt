package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.home.HomeScreen
import com.kpyruy.takt.feature.settings.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PreferencesReviewUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer

    @Test fun remindersOnlyDifferenceShowsBeforeAndAfterInsteadOfAnEmptySection() {
        val local = BackupPayload(courses = emptyList(), settings = BackupSettings())
        val source = local.copy(settings = local.settings.copy(reminders = BackupReminderSettings(
            enabled = true, academicEnabled = false, dateOnlyTime = "08:30",
            rules = listOf(BackupReminderRule("TEST", false, listOf(10080, 1440))),
        )))
        assertEquals(setOf(BackupSection.PREFERENCES), BackupMerger.changedSections(local, source))
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            TaktI18n.use(AppLanguage.SLOVAK)
            TaktTheme(AppSettings(language = AppLanguage.SLOVAK)) {
                TaktGlassBackground { BackupReviewPage(BackupReviewState(
                    BackupPayloadCodec.encode(local), BackupPayloadCodec.encode(source), BackupReviewSource.DOCUMENTS,
                ), {}, {}) }
            }
        } }
        compose.onNodeWithText("Vyber zdroj pre každú časť").assertDoesNotExist()
        compose.onNodeWithText("Pripojený trezor Documents/Takt sa po použití automaticky aktualizuje.").assertDoesNotExist()
        compose.onNodeWithTag("backup-expand-PREFERENCES").performScrollTo().performClick()
        compose.onNodeWithTag("backup-preference-reminders-enabled").performScrollTo().assertTextContains("Vypnuté", substring = true)
        compose.onNodeWithTag("backup-preference-reminders-enabled").assertTextContains("Zapnuté", substring = true)
        capture("comparison-reminders.png")
        compose.onNodeWithTag("backup-preference-reminders-academic").performScrollTo().assertExists()
        compose.onNodeWithTag("backup-preference-reminders-time").performScrollTo().assertTextContains("08:30", substring = true)
        compose.onNodeWithTag("backup-preference-reminders-TEST-enabled").performScrollTo().assertExists()
        compose.onNodeWithTag("backup-preference-reminders-TEST-timing").performScrollTo().assertTextContains("1 týždeň", substring = true)
        compose.onNodeWithText("10080").assertDoesNotExist()
    }

    @Test fun otherPreferencesShowIndividualValuesAndResolvePortableCourseSelections() {
        val local = BackupPayload(courses = listOf(BackupCourse("phone-physics", "PHY", "Fyzika", 6, 1,
            "enrolled", "COMPULSORY")), settings = BackupSettings())
        val source = local.copy(courses = local.courses.map { it.copy(id = "file-physics") },
            settings = local.settings.copy(cancellationStyle = "HIDDEN", showHiddenLessons = true,
                parityOverride = "ODD", weekLayout = "COMPACT_LIST",
                homeWorkFilter = BackupHomeWorkFilter(period = "CUSTOM", courseIds = listOf("file-physics"),
                    types = listOf("LAB", "TASK"), includeCompleted = true, includeUndated = false,
                    fromDate = "2026-10-17", toDate = "2026-10-24"),
                uisProgressFrequency = "DAILY", uisSubjectFrequency = "WEEKLY",
                uisPeriodFrequency = "MONTHLY", uisTimetableFrequency = "MANUAL",
                uisApplyProgressAutomatically = false))
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            TaktI18n.use(AppLanguage.SLOVAK)
            TaktTheme(AppSettings(language = AppLanguage.SLOVAK)) {
                TaktGlassBackground { BackupReviewPage(BackupReviewState(
                    BackupPayloadCodec.encode(local), BackupPayloadCodec.encode(source), BackupReviewSource.JSON_FILE,
                ), {}, {}) }
            }
        } }
        compose.onNodeWithTag("backup-expand-PREFERENCES").performScrollTo().performClick()
        listOf("cancellation", "hidden", "parity", "week", "filter-period", "filter-types",
            "filter-completed", "filter-undated", "uis-progress", "uis-subjects", "uis-periods",
            "uis-timetable", "uis-automatic").forEach { key ->
            compose.onNodeWithTag("backup-preference-$key").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("backup-preference-filter-courses").performScrollTo()
            .assertTextContains("PHY · Fyzika", substring = true)
        compose.onNodeWithTag("backup-preference-filter-from").performScrollTo()
            .assertTextContains("17.10.2026", substring = true)
        compose.onNodeWithTag("backup-preference-filter-to").performScrollTo()
            .assertTextContains("24.10.2026", substring = true)
        compose.onNodeWithText("file-physics", substring = true).assertDoesNotExist()
        compose.onNodeWithText("COMPACT_LIST", substring = true).assertDoesNotExist()
        compose.onNodeWithText("MONTHLY", substring = true).assertDoesNotExist()
    }

    @Test fun teachingBackupReviewShowsCourseTeamAndBothConsultationHours() {
        val local = BackupPayload(courses = listOf(BackupCourse("physics", "PHY", "Fyzika", 6, 1,
            "enrolled", "COMPULSORY")), teaching = BackupTeaching(
            teams = listOf(BackupTeachingTeam("PHY", listOf(BackupTeacher("12345", "Anna Nová", listOf("lecturer"))))),
            profiles = listOf(BackupTeacherProfile("12345", "SLOVAK", "Anna Nová", consultationHours = "Pondelok 10:00")),
        ))
        val source = local.copy(teaching = local.teaching!!.copy(
            teams = listOf(BackupTeachingTeam("PHY", listOf(BackupTeacher("12345", "Anna Nová", listOf("examiner"))))),
            profiles = local.teaching!!.profiles.map { it.copy(consultationHours = "Utorok 14:00", officeNumber = "B 203") },
        ))
        assertEquals(setOf(BackupSection.TEACHERS), BackupMerger.changedSections(local, source))
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            TaktI18n.use(AppLanguage.SLOVAK)
            TaktTheme(AppSettings(language = AppLanguage.SLOVAK)) {
                TaktGlassBackground { BackupReviewPage(BackupReviewState(
                    BackupPayloadCodec.encode(local), BackupPayloadCodec.encode(source), BackupReviewSource.JSON_FILE,
                ), {}, {}) }
            }
        } }
        compose.onNodeWithTag("backup-expand-TEACHERS").performScrollTo().performClick()
        compose.onNodeWithText("PHY · Vyučujúci").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pondelok 10:00", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Utorok 14:00", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("B 203", substring = true).performScrollTo().assertIsDisplayed()
        capture("comparison-teachers.png")
    }

    @Test fun homeFilterKeepsDoneAndHasNoResetEvenAfterChangingFilters() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.settingsRepository.setLanguage(AppLanguage.SLOVAK)
            data.settingsRepository.setHomeWorkFilter(HomeWorkFilter(includeCompleted = true))
            data.awaitRestoredPlanning()
        }
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            val settings by data.settingsRepository.settings.collectAsStateWithLifecycle()
            TaktI18n.use(settings.language)
            TaktTheme(settings) { TaktGlassBackground {
                HomeScreen(data.planningSnapshot, data.studyContentRepository, data.settingsRepository,
                    {}, {}, onOpenCourse = {}, onOpenAssessment = {}, onEventLongClick = {})
            } }
        } }
        compose.onNodeWithTag("home-tasks-filter").performScrollTo().performClick()
        compose.onNodeWithText("Obnoviť filtre úloh").assertDoesNotExist()
        compose.onNodeWithTag("home-filter-done").performScrollTo().assertIsDisplayed()
        capture("filter-done-only.png")
        compose.onNodeWithTag("home-filter-done").performClick()
        compose.onNodeWithTag("home-filter-done").assertDoesNotExist()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(200)
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        compose.activity.openFileOutput(name, 0).use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
