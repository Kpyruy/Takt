package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.RoomStudentHubStore
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Approved concepts rendered through the actual navigation, Room store and local repository. */
class StudiesIntegrationUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val data get()=(compose.activity.application as TaktApplication).dataContainer
    @Test fun approvedNativeFlowUsesPersistentDataAndRestoresDetailDraft() {
        runBlocking {
            data.seedLiquidGlassReview()
            data.universityAccountRepository.remove()
            data.settingsRepository.setLanguage(AppLanguage.ENGLISH)
            data.settingsRepository.setThemeMode(AppThemeMode.LIGHT)
            data.settingsRepository.setThemeFamily(ThemeFamily.BLUE)
            data.database.courseDao().insertAll(data.database.courseDao().getAllSnapshot().map { course ->
                val title=when(course.id){"glass-physics"->"Physics";"glass-programming"->"Control Systems";"glass-math"->"Mathematics";else->course.title}
                course.copy(title=title,titleEn=title)
            })
            val source=StudentHubSnapshot(StudentIdentity("15","Alex Novak","Slovak University of Technology"),
                listOf(StudentStudy("101","Automation & Control",degree="Bc.",periodLabel="2025–2028",active=true,currentSemester=3,
                    earnedCredits=86,requiredCredits=185,titleEn="Industrial Informatics, Automation and Robotics",titleSk="Priemyselná informatika, automatizácia a robotika",feesLoaded=true,scholarshipsLoaded=true,
                    fees=listOf(StudentFee("isic","101","ISIC renewal",2200,paidMinor=0,status=StudentFeeStatus.UNPAID,dueDate=LocalDate.of(2026,10,31),issuedDate=LocalDate.of(2026,10,10),recordUrl="https://is.stuba.sk/auth/student/financovani.pl?studium=101;detail=1")),
                    scholarships=listOf(ScholarshipPayment("oct","101","Merit scholarship",20000,paidOn=LocalDate.of(2026,10,12))),updatedAtEpochMillis=1792741260000),
                    StudentStudy("202","Applied Informatics",titleEn="Applied Informatics",titleSk="Aplikovaná informatika",degree="Bc.",periodLabel="2023–2025",active=false,earnedCredits=78,requiredCredits=180)),
                currentStudyId="101",viewedStudyId="101",showPreviousStudies=true)
            RoomStudentHubStore(data.database).write(source)
            data.awaitRestoredPlanning()
        }
        compose.waitForIdle()
        compose.onNode(hasText("Studies") and hasClickAction()).performClick()
        compose.waitUntil(5_000){compose.onAllNodesWithTag("studies-study-selector").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("root-add").assertIsDisplayed()
        compose.onNodeWithTag("studies-credit-card").assertIsDisplayed()
        capture("studies-overview.png")
        compose.onNodeWithTag("studies-study-selector").performClick();capture("studies-switcher-long.png")
        compose.onNodeWithTag("studies-selector-done").performClick()
        compose.onNodeWithTag("studies-tab-1").performClick();capture("studies-progress.png")
        compose.onNodeWithTag("studies-tab-2").performClick();capture("studies-finances.png")
        compose.onNodeWithTag("studies-fee-isic").performScrollTo().performClick()
        compose.onNodeWithTag("studies-fee-detail").assertExists()
        compose.onNodeWithTag("root-navigation").assertDoesNotExist();capture("studies-isic.png")
        compose.onNodeWithTag("studies-back").performClick()
        compose.onNodeWithTag("studies-tab-2").assertIsSelected()
        compose.onNodeWithTag("studies-tab-0").performClick()
        compose.onNodeWithTag("studies-profile").performScrollTo().performClick()
        compose.onNodeWithTag("studies-profile-hero").assertIsDisplayed();capture("studies-profile.png")
        compose.onNodeWithTag("studies-edit-profile").performClick()
        compose.onNodeWithTag("studies-display-name").performTextReplacement("Student display")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("studies-display-name").assertTextContains("Student display")
        compose.onNodeWithText("Apply").performClick()
        compose.waitUntil(5_000){runBlocking { RoomStudentHubStore(data.database).read()?.displayName=="Student display" }}
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Student display").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Student display").assertExists()
        compose.onNodeWithText("Alex Novak").assertExists()
        assertEquals("101",runBlocking { RoomStudentHubStore(data.database).read()!!.currentStudyId })
    }
    private fun capture(name:String) {
        compose.waitForIdle()
        captureStudiesScreenshot(name)
    }
}

/** Wait for Android's rendered frame as well as Compose's semantics before taking a native screenshot. */
internal fun captureStudiesScreenshot(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val frameCommitted = java.util.concurrent.CountDownLatch(1)
    instrumentation.runOnMainSync {
        val activity = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).single()
        val view = activity.window.decorView
        if (android.os.Build.VERSION.SDK_INT >= 29 && view.isHardwareAccelerated) {
            view.viewTreeObserver.registerFrameCommitCallback { frameCommitted.countDown() }
        } else {
            view.postOnAnimation { view.postOnAnimation { frameCommitted.countDown() } }
        }
        view.postInvalidateOnAnimation()
    }
    check(frameCommitted.await(5, java.util.concurrent.TimeUnit.SECONDS)) { "Native frame did not commit" }
    // Dialog window fade-in runs on Android's clock, independently of Compose's test clock.
    instrumentation.uiAutomation.waitForIdle(300, 5_000)
    val bitmap = instrumentation.uiAutomation.takeScreenshot()
    instrumentation.targetContext.openFileOutput(name, 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
}
