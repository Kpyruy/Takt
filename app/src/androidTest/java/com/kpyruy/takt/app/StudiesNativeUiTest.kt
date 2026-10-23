package com.kpyruy.takt.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.data.GradeRepository
import kotlinx.coroutines.flow.Flow
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.studyplan.StudiesContent
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic university records: production never supplies illustrative identity or finances. */
class StudiesNativeUiTest {
    @get:Rule val compose = createComposeRule()
    private val live = Course("shared", "LIVE", "Current course", 5, 3, CourseStatus.ENROLLED)
    private val previous = Course("shared", "OLD", "Archived course", 6, 1, CourseStatus.FULFILLED)
    private val fee = StudentFee("isic", "current", "ISIC renewal", 2200, paidMinor = 0,
        status = StudentFeeStatus.UNPAID, dueDate = LocalDate.of(2026, 10, 31),
        issuedDate = LocalDate.of(2026, 10, 10), recordUrl = "https://is.stuba.sk/auth/student/platby.pl")
    private val current = StudentStudy("current", "Automation & Control", university = "STU", degree = "Bc.", periodLabel = "2025–2028", active = true,
        currentSemester = 3, earnedCredits = 86, requiredCredits = 185, courses = listOf(live), fees = listOf(fee),
        scholarships = listOf(ScholarshipPayment("october", "current", "Merit scholarship", 20000, paidOn = LocalDate.of(2026, 10, 12))),
        feesLoaded = true, scholarshipsLoaded = true)
    private val snapshot = StudentHubSnapshot(identity = StudentIdentity(name = "Synthetic Student", university = "STU Bratislava"),
        studies = listOf(current, StudentStudy("previous", "Applied Informatics", periodLabel = "2023–2025", active = false, courses = listOf(previous), currentSemester = 1)),
        currentStudyId = "current", viewedStudyId = "current", showPreviousStudies = true)

    private val forbiddenGrades = object : GradeRepository {
        override fun observeItems(courseId: String): Flow<List<GradeItem>> = error("Archived study queried live grades")
        override fun observeAllItems(): Flow<List<GradeItem>> = error("Archived study queried live grades")
        override fun observeRecentItems(limit: Int): Flow<List<GradeItem>> = error("Archived study queried live grades")
        override fun observeScale(courseId: String): Flow<GradeScale> = error("Archived study queried live grades")
        override fun observeManualGrade(courseId: String): Flow<GradeLetter?> = error("Archived study queried live grades")
        override suspend fun upsertItem(item: GradeItem) = error("Archived study mutated live grades")
        override suspend fun deleteItem(id: String) = error("Archived study mutated live grades")
        override suspend fun setScale(courseId: String, scale: GradeScale) = error("Archived study mutated live grades")
        override suspend fun setManualGrade(courseId: String, grade: GradeLetter?) = error("Archived study mutated live grades")
    }

    @After fun resetLanguage() { TaktI18n.use(AppLanguage.ENGLISH) }

    private fun show(state: StudentHubState = StudentHubState(snapshot), language: AppLanguage = AppLanguage.ENGLISH,
                     settings: AppSettings = AppSettings(), courseClick: (String) -> Unit = {}, courseStatus: (Course) -> Unit = {}, gradeRepository: GradeRepository? = null, openRecord: (String) -> Unit = {}, detailChanged: (Boolean) -> Unit = {}) {
        compose.setContent {
            TaktI18n.use(language)
            TaktTheme(settings) { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
                StudiesContent(state, settings = settings, localCourses = listOf(live), onCourseClick = courseClick, onCourseStatus = courseStatus, gradeRepository = gradeRepository,
                    onOpenPaymentRecord = openRecord, onDetailChanged = detailChanged)
            } }
        }
    }

    @Test fun overviewUsesApprovedOrderAndAllThreeStudyTabs() {
        show()
        compose.onNodeWithText("Studies").assertExists()
        compose.onNodeWithTag("studies-study-selector").assertIsDisplayed()
        compose.onNodeWithTag("studies-tabs").assertIsDisplayed()
        compose.onNodeWithTag("studies-credit-card").assertIsDisplayed()
        val selector = compose.onNodeWithTag("studies-study-selector").fetchSemanticsNode().boundsInRoot
        val tabs = compose.onNodeWithTag("studies-tabs").fetchSemanticsNode().boundsInRoot
        val credits = compose.onNodeWithTag("studies-credit-card").fetchSemanticsNode().boundsInRoot
        assertTrue(selector.bottom <= tabs.top)
        assertTrue(tabs.bottom <= credits.top)
        compose.onNodeWithTag("studies-profile").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("studies-isic").performScrollTo().assertIsDisplayed()
    }

    @Test fun feeDetailReturnsToOriginatingFinancesTabAndUsesOfficialRecord() {
        var record: String? = null
        var detailOpen = false
        show(openRecord = { record = it }, detailChanged = { detailOpen = it })
        compose.onNodeWithTag("studies-tab-2").performClick()
        compose.onNodeWithTag("studies-fee-isic").performScrollTo().performClick()
        compose.onNodeWithTag("studies-fee-detail").assertIsDisplayed()
        compose.runOnIdle { assertTrue(detailOpen) }
        compose.onNodeWithTag("studies-open-payment-record").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(fee.recordUrl, record) }
        compose.onNodeWithTag("studies-back").performScrollTo().performClick()
        compose.onNodeWithTag("studies-tab-2").assertIsSelected()
        compose.runOnIdle { assertFalse(detailOpen) }
    }

    @Test fun unknownOutstandingAmountNeverAppearsAsZero() {
        val unknown = current.copy(fees = listOf(fee.copy(status = StudentFeeStatus.UNKNOWN, paidMinor = null)))
        show(StudentHubState(snapshot.copy(studies = listOf(unknown))))
        compose.onNodeWithTag("studies-tab-2").performClick()
        compose.onNodeWithText("Payment status is incomplete").assertExists()
        compose.onNodeWithTag("studies-outstanding").onChildren().filter(hasText("€0.00")).assertCountEquals(0)
        compose.onAllNodesWithText("€0.00").assertCountEquals(0)
    }

    @Test fun loadedEmptyFeesAreDistinguishedFromUnavailableData() {
        show(StudentHubState(snapshot.copy(studies = listOf(current.copy(fees = emptyList())))))
        compose.onNodeWithTag("studies-tab-2").performClick()
        compose.onNodeWithText("No outstanding fees").assertExists()
        compose.onNodeWithText("No fee records").assertExists()
        compose.onAllNodesWithText("Payment status is incomplete").assertCountEquals(0)
    }

    @Test fun actualScholarshipPaymentHasPaidLabelAndAmount() {
        show()
        compose.onNodeWithTag("studies-tab-2").performClick()
        compose.onNodeWithText("Merit scholarship").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("€200.00").assertCountEquals(2)
        compose.onNodeWithText("Paid").assertExists()
    }

    @Test fun profileEditChangesDisplayOnlyAndKeepsUniversityName() {
        val value = StudentHubState(snapshot)
        var saved: String? = null
        compose.setContent {
            var state by remember { mutableStateOf(value) }
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme { StudiesContent(state, localCourses = listOf(live), onDisplayNameChanged = { name ->
                saved = name; state = state.copy(snapshot = state.snapshot!!.copy(displayName = name))
            }) }
        }
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasTestTag("studies-profile"))
        compose.onNodeWithTag("studies-profile").performClick()
        compose.onNodeWithTag("studies-edit-profile").performClick()
        compose.onNodeWithTag("studies-display-name").performTextReplacement("Preferred name")
        compose.onNodeWithTag("studies-save-name").performClick()
        compose.runOnIdle { assertEquals("Preferred name", saved) }
        compose.onNodeWithText("Preferred name").assertExists()
        compose.onNodeWithText("Synthetic Student").assertExists()
    }

    @Test fun failedProfileSaveKeepsTheTypedNameAvailableForRetry() {
        compose.setContent {
            var state by remember { mutableStateOf(StudentHubState(snapshot)) }
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme { StudiesContent(state, onDisplayNameChanged = {
                state = state.copy(problem = StudentHubProblem.SAVE_FAILED)
            }) }
        }
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasTestTag("studies-profile"))
        compose.onNodeWithTag("studies-profile").performClick()
        compose.onNodeWithTag("studies-edit-profile").performClick()
        compose.onNodeWithTag("studies-display-name").performTextReplacement("Unsaved preferred name")
        compose.onNodeWithTag("studies-save-name").performClick()
        compose.onNodeWithTag("studies-edit-profile").performScrollTo().performClick()
        compose.onNodeWithTag("studies-display-name").assertTextContains("Unsaved preferred name")
    }

    @Test fun archivedCourseWithSameIdNeverOpensLiveCourseOrStatus() {
        var liveOpens = 0
        var statusChanges = 0
        show(StudentHubState(snapshot.copy(viewedStudyId = "previous")), courseClick = { liveOpens++ }, courseStatus = { statusChanges++ }, gradeRepository = forbiddenGrades)
        compose.onNodeWithTag("studies-tab-1").performClick()
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasTestTag("studies-readonly-course-shared"))
        compose.onNodeWithTag("studies-readonly-course-shared").performClick()
        compose.onNodeWithText("Previous study course").assertExists()
        compose.onNodeWithText("Archived course").assertExists()
        compose.onAllNodesWithText("Current course").assertCountEquals(0)
        compose.onNodeWithTag("status-shared").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, liveOpens); assertEquals(0, statusChanges) }
    }

    @Test fun previousStudySelectionChangesViewedRecordAndPreservesCurrentStudy() {
        var selected: StudentHubSnapshot? = null
        compose.setContent {
            var state by remember { mutableStateOf(StudentHubState(snapshot)) }
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme { StudiesContent(state, localCourses = listOf(live), onSelectStudy = { id ->
                val updated = state.snapshot!!.withViewedStudy(id)
                selected = updated; state = state.copy(snapshot = updated)
            }) }
        }
        compose.onNodeWithTag("studies-study-selector").performClick()
        compose.onNodeWithTag("studies-select-previous").performClick()
        compose.onNodeWithTag("studies-selector-done").performClick()
        compose.onNodeWithText("Applied Informatics").assertExists()
        compose.runOnIdle { assertEquals("current", selected?.currentStudyId); assertEquals("previous", selected?.viewedStudyId) }
    }

    @Test fun unavailableAccountKeepsApprovedShellAndNeverInventsMoney() {
        show(StudentHubState(problem = StudentHubProblem.SIGN_IN_REQUIRED))
        compose.onNodeWithTag("studies-study-selector").assertExists()
        compose.onNodeWithTag("studies-tabs").assertExists()
        compose.onNodeWithText("Sign in to UIS to load study records").assertExists()
        compose.onAllNodes(hasText("€", substring = true)).assertCountEquals(0)
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasTestTag("studies-profile"))
        compose.onNodeWithTag("studies-profile").performClick()
        compose.onAllNodesWithText("Synthetic Student").assertCountEquals(0)
    }

    @Test fun ukrainianChromePreservesUniversityDataStrings() {
        show(language = AppLanguage.UKRAINIAN)
        compose.onNodeWithText("Навчання").assertExists()
        compose.onNodeWithText("Огляд").assertExists()
        compose.onNodeWithTag("studies-tab-2").assertTextEquals("Фінанси")
        compose.onNodeWithText("Automation & Control").assertExists()
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasTestTag("studies-profile"))
        compose.onNodeWithTag("studies-profile").performClick()
        compose.onNodeWithText("Профіль студента").assertExists()
        compose.onAllNodesWithText("Synthetic Student").assertCountEquals(2)
    }

    @Test fun archivedCourseTitlesFollowSelectedCourseLanguage() {
        val archived = snapshot.studies.last().copy(courses = listOf(previous.copy(titleEn = "Archived course", titleSk = "Archívny predmet")))
        show(StudentHubState(snapshot.copy(viewedStudyId = "previous", studies = listOf(current, archived))),
            settings = AppSettings(courseNameLanguage = CourseNameLanguage.SLOVAK))
        compose.onNodeWithTag("studies-tab-1").performClick()
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasTestTag("studies-readonly-course-shared"))
        compose.onNodeWithText("Archívny predmet").assertExists()
        compose.onAllNodesWithText("Archived course").assertCountEquals(0)
        compose.onNodeWithTag("studies-readonly-course-shared").performClick()
        compose.onNodeWithText("Archívny predmet").assertExists()
    }

    @Test fun darkElevatedCardsKeepNativeStudyAndFinanceContentReadable() {
        show(settings = AppSettings(themeMode = AppThemeMode.DARK, cardAppearance = CardAppearance.ELEVATED))
        compose.onNodeWithTag("studies-tab-2").performClick()
        compose.onNodeWithTag("studies-outstanding").assertIsDisplayed()
        compose.onNodeWithTag("studies-fee-isic").assertIsDisplayed()
        capture("studies-dark-elevated.png")
    }

    @Test fun lightFilledStudySelectorMatchesApprovedSelectionFlow() {
        show(settings = AppSettings(themeMode = AppThemeMode.LIGHT, cardAppearance = CardAppearance.TONAL_FILLED))
        compose.onNodeWithTag("studies-study-selector").performClick()
        compose.onNodeWithTag("studies-select-current").assertIsSelected()
        compose.onNodeWithTag("studies-select-previous").assertIsNotSelected()
        compose.onNodeWithTag("studies-selector-previous").assertIsOn()
        compose.onNodeWithTag("studies-selector-done").assertIsDisplayed()
        val current = compose.onNodeWithTag("studies-select-current").fetchSemanticsNode().boundsInRoot
        val checkbox = compose.onNodeWithTag("studies-selector-previous").fetchSemanticsNode().boundsInRoot
        val previous = compose.onNodeWithTag("studies-select-previous").fetchSemanticsNode().boundsInRoot
        assertTrue(current.bottom <= checkbox.top)
        assertTrue(checkbox.bottom <= previous.top)
        capture("studies-switcher.png")
        compose.onNodeWithTag("studies-selector-done").performClick()
        compose.onNodeWithTag("studies-study-selector").assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        captureStudiesScreenshot(name)
    }

    @Test fun slovakChromePreservesUniversityDataStrings() {
        show(language = AppLanguage.SLOVAK)
        compose.onNodeWithText("Štúdium").assertExists()
        compose.onNodeWithText("Prehľad").assertExists()
        compose.onNodeWithTag("studies-tab-2").assertTextEquals("Financie")
        compose.onNodeWithText("Automation & Control").assertExists()
    }
}
