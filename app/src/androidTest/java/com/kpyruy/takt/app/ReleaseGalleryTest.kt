package com.kpyruy.takt.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.TaktGlassBackground
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.settings.SettingsScreen
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalTime

/** Opt-in captures of production screens. All records are fictional and restricted to a review emulator. */
class ReleaseGalleryTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer

    @Test fun captureEnglishReleaseGallery() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("releaseScreenshots") == "true")
        ReleaseScreenshotSeedTest().seedEnglishShowcase()
        val today = LocalDate.now()
        runBlocking {
            data.universityAccountRepository.remove()
            data.settingsRepository.setAppearance(data.settingsRepository.settings.value.copy(
                themeMode = AppThemeMode.LIGHT, themeFamily = ThemeFamily.BLUE, cardAppearance = CardAppearance.GLASS,
                customWallpaper = null, selectedCustomThemeId = null, addButtonPlacement = AddButtonPlacement.CENTER,
                customThemes = listOf(CustomColorTheme("blue-hour", "Blue hour", "4361EE", "8093F1", "6741D9"))))
            data.settingsRepository.setWeekLayout(WeekLayout.COMPACT_LIST)
            data.settingsRepository.setUisCurrentSemester(3)
            // A fictional weekend workshop keeps the Today screenshot useful on the capture date.
            if (today.dayOfWeek.value >= 6) listOf(
                ScheduleRule("release-today-physics", "FYZI_6B", "Physics", today.dayOfWeek, LocalTime.of(9, 0), LocalTime.of(10, 30), ScheduleRecurrence.WEEKLY, "B-214", LessonType.LECTURE),
                ScheduleRule("release-today-control", "TPAR_6B", "Control Systems", today.dayOfWeek, LocalTime.of(11, 0), LocalTime.of(12, 30), ScheduleRecurrence.WEEKLY, "Lab 2", LessonType.LAB),
            ).forEach { data.scheduleRepository.upsertRule(it) }
            data.studyContentRepository.upsertNote(CourseNote("release-waves", "FYZI_6B", "Waves and oscillations",
                "Period and frequency: T = 1 / f\nWave speed: v = f × wavelength\nReview the worked examples before the next practice class.", System.currentTimeMillis()))
            val courses = data.studyPlanRepository.observeCourses().first()
            RoomStudentHubStore(data.database).write(StudentHubSnapshot(
                identity = StudentIdentity("15", "Alex Novak", "Slovak University of Technology", "104826", "Faculty of Materials Science and Technology"),
                studies = listOf(StudentStudy("101", "Industrial Informatics, Automation and Robotics", "STU", "Bc.", "2025–2028", true,
                    currentSemester = 3, earnedCredits = 86, requiredCredits = 180, courses = courses,
                    fees = listOf(StudentFee("isic", "101", "ISIC renewal", 2200, paidMinor = 0, status = StudentFeeStatus.UNPAID,
                        dueDate = today.plusDays(7), issuedDate = today.minusDays(14))),
                    scholarships = listOf(ScholarshipPayment("merit", "101", "Merit scholarship", 20000, paidOn = today.minusDays(12))),
                    feesLoaded = true, scholarshipsLoaded = true, updatedAtEpochMillis = System.currentTimeMillis()),
                    StudentStudy("202", "Applied Informatics", "STU", "Bc.", "2023–2025", false, earnedCredits = 78, requiredCredits = 180)),
                currentStudyId = "101", viewedStudyId = "101", showPreviousStudies = true))
            if (data.documentStore.isConnected) data.documentStore.saveCurrentToDocuments()
            data.awaitRestoredPlanning()
        }
        compose.waitForIdle()
        rootTab("Today")
        capture("today")
        rootTab("Calendar")
        compose.onNode(hasText("Week") and hasClickAction()).performClick()
        if (compose.onAllNodes(hasText("List") and hasClickAction()).fetchSemanticsNodes().isNotEmpty())
            compose.onNode(hasText("List") and hasClickAction()).performClick()
        capture("week-list")
        rootTab("Subjects")
        capture("subjects")
        rootTab("Studies")
        compose.onNodeWithTag("studies-credit-card").assertIsDisplayed()
        capture("studies")
        compose.onNodeWithTag("studies-tab-1").performClick()
        capture("progress-details")
        compose.onNodeWithTag("semester-toggle-3").performClick()
        capture("progress")
        compose.onNodeWithTag("studies-tab-2").performClick()
        capture("finances")
        compose.onNodeWithTag("studies-tab-0").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("studies-profile").performScrollTo().performClick()
        compose.onNodeWithTag("studies-profile-hero").assertIsDisplayed()
        capture("student-profile")
        compose.onNodeWithTag("studies-back").performClick()
        rootTab("Subjects")
        compose.onNodeWithText("Physics").performScrollTo().performClick()
        compose.onNode(hasText("Exam") and hasClickAction()).performClick()
        capture("exam")
        compose.onNode(hasText("Notes") and hasClickAction()).performClick()
        capture("notes")

        val settings = data.settingsRepository.settings.value
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme(settings) { TaktGlassBackground { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                SettingsScreen(data.settingsRepository, data.backupRepository, data.documentStore,
                    data.studyPlanRepository, data.universityAccountRepository, { _, reply -> reply(DeviceAuthenticationResult.CANCELLED) },
                    (activity.application as TaktApplication).appReleaseRepository, {}, guidedPage = "appearance")
            } } }
        } }
        capture("appearance")
        captureDocuments(settings)
    }

    private fun rootTab(label: String) {
        compose.onNode(hasText(label) and hasClickAction() and hasAnyAncestor(hasTestTag("root-navigation")))
            .performClick()
        compose.waitForIdle()
    }

    private fun captureDocuments(settings: AppSettings) {
        assertTrue("Connect Documents on the disposable emulator before release captures", data.documentStore.isConnected)
        val course = runBlocking { data.studyPlanRepository.observeCourses().first().first { it.code == "FYZI_6B" } }
        val files = listOf(
            CourseDocumentFile("98101", "98102", "Lecture slides · Waves and oscillations", "application/pdf", "lecture-slides.pdf",
                sizeLabel = "1.8 MiB", downloadedFileName = "lecture-slides.pdf", revision = "version-1", downloadedRevision = "version-1"),
            CourseDocumentFile("98101", "98103", "Laboratory guide · Motion and forces", "application/pdf", "laboratory-guide.pdf", sizeLabel = "640 KiB", revision = "version-1"))
        val repository = object : CourseDocumentsRepository {
            override suspend fun cachedDocuments(course: Course, activeCourseCodes: Set<String>) = CourseDocumentListing(files, true)
            override suspend fun documents(course: Course, activeCourseCodes: Set<String>, refresh: Boolean) = CourseDocumentListing(files, true)
            override suspend fun download(document: CourseDocumentFile, output: OutputStream): DownloadedCourseDocument = error("Screenshots do not download attachments")
        }
        val source = java.io.File(compose.activity.cacheDir, "release-lecture.pdf").apply {
            writeBytes(InstrumentationRegistry.getInstrumentation().context.assets.open("course-document.pdf").use { it.readBytes() })
        }
        runBlocking { data.documentStore.saveDownloadedDocument(course.code, source, "lecture-slides.pdf", "application/pdf") }
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme(settings) { TaktGlassBackground { Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                SubjectDetailScreen(data.studyPlanRepository, data.gradeRepository, data.scheduleRepository,
                    data.settingsRepository, data.studyContentRepository, data.examRepository, data.documentStore,
                    uisManaged = true, courseId = course.id, onBack = {}, courseDocumentsRepository = repository)
            } } }
        } }
        compose.onNodeWithTag("course-documents").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("documents-loading").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("course-document-98102").assertIsDisplayed()
        capture("documents")
        source.delete()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val dismiss = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
        if (compose.onAllNodes(dismiss).fetchSemanticsNodes().size == 1 &&
            compose.onAllNodesWithTag("course-documents-sheet").fetchSemanticsNodes().isEmpty())
            compose.onNode(dismiss).performSemanticsAction(SemanticsActions.Dismiss) { it() }
        captureStudiesScreenshot("release-04-$name.png")
    }
}
