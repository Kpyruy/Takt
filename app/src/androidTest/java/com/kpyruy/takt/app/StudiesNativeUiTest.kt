package com.kpyruy.takt.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
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

    @Test fun paymentsShowNumericPlaceholderUntilTheFetchedBalanceArrives() {
        var state by mutableStateOf(StudentHubState(snapshot.copy(studies = listOf(current.copy(feesLoaded = false)))))
        compose.setContent {
            TaktI18n.use(AppLanguage.SLOVAK)
            TaktTheme { StudiesContent(state, settings = AppSettings(language = AppLanguage.SLOVAK)) }
        }
        compose.onNodeWithTag("studies-screen", useUnmergedTree = true).performScrollToNode(hasTestTag("studies-payments-value"))
        compose.onNodeWithTag("studies-payments-value", useUnmergedTree = true).assertTextEquals("0,00 EUR")
        compose.runOnIdle { state = StudentHubState(snapshot) }
        compose.onNodeWithTag("studies-payments-value", useUnmergedTree = true).assertTextEquals("22,00 EUR")
    }

    private fun show(state: StudentHubState = StudentHubState(snapshot), language: AppLanguage = AppLanguage.ENGLISH,
                     settings: AppSettings = AppSettings(), uisManaged: Boolean = false, courseClick: (String) -> Unit = {}, courseStatus: (Course) -> Unit = {}, gradeRepository: GradeRepository? = null, openRecord: (String) -> Unit = {}, detailChanged: (Boolean) -> Unit = {}) {
        compose.setContent {
            TaktI18n.use(language)
            TaktTheme(settings) { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
                StudiesContent(state, settings = settings.copy(language = language), uisManaged = uisManaged, localCourses = listOf(live), onCourseClick = courseClick, onCourseStatus = courseStatus, gradeRepository = gradeRepository,
                    onOpenPaymentRecord = openRecord, onDetailChanged = detailChanged)
            } }
        }
    }

    @Test fun progressUsesALeftSearchAndAPopupWithExclusiveStatusChoices() {
        show()
        compose.onNodeWithTag("studies-tab-1").performClick()
        compose.onNodeWithTag("studies-progress-filter-0").assertDoesNotExist()
        val search = compose.onNodeWithTag("progress-search-open").fetchSemanticsNode().boundsInRoot
        val tools = compose.onNodeWithTag("studies-progress-tools").fetchSemanticsNode().boundsInRoot
        val filter = compose.onNodeWithTag("studies-progress-filter-open").fetchSemanticsNode().boundsInRoot
        assertEquals(tools.left, search.left, 1f)
        assertTrue(search.right <= filter.left)
        compose.onNodeWithTag("studies-progress-filter-open").performClick()
        compose.onNodeWithTag("studies-progress-filter-0").assertIsSelected().assertTextContains("All")
        compose.onNodeWithTag("studies-progress-filter-2").performClick()
        compose.onNodeWithTag("studies-readonly-course-shared").assertDoesNotExist()
        compose.onNodeWithTag("studies-progress-filter-open").performClick()
        compose.onNodeWithTag("studies-progress-filter-2").assertIsSelected()
        compose.onNodeWithTag("studies-progress-filter-0").assertIsNotSelected()
        compose.onNodeWithTag("studies-progress-filter-1").performClick()
        compose.onNodeWithTag("studies-readonly-course-shared").assertIsDisplayed()
        compose.onNodeWithTag("studies-progress-filter-open").performClick()
        capture("studies-status-filter.png")
    }

    @Test fun studySelectorUsesWallpaperMaterialAcrossTheWholeSheet() {
        val wallpaper = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF945CA0.toInt()) }
        val settings = AppSettings(themeMode = AppThemeMode.DARK, themeFamily = ThemeFamily.PURPLE)
        compose.setContent {
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme(settings) {
                com.kpyruy.takt.core.ui.components.TaktGlassBackground(photoPreview = wallpaper) {
                    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                        StudiesContent(StudentHubState(snapshot), settings = settings)
                    }
                }
            }
        }
        compose.onNodeWithTag("studies-study-selector").performClick()
        val bounds = compose.onNodeWithTag("studies-selector-body").fetchSemanticsNode().boundsInWindow
        compose.waitUntil(5_000) {
            val shot = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val pixel = shot.getPixel((bounds.left + 6).toInt(), (bounds.top + 10).toInt())
            shot.recycle()
            android.graphics.Color.red(pixel) > 70
        }
        capture("studies-themed-selector.png")
    }

    @Test fun refreshFailuresStaySilentAndEmptyScholarshipIsNumeric() {
        show(StudentHubState(snapshot.copy(studies = listOf(current.copy(scholarships = emptyList(), scholarshipsLoaded = false))), problem = StudentHubProblem.UNAVAILABLE))
        compose.onNodeWithTag("studies-status").assertDoesNotExist()
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasText("0.00 EUR"))
        compose.onNodeWithTag("studies-scholarship-value", useUnmergedTree = true).assertTextEquals("0.00 EUR")
    }

    @Test fun creditCardIsCompactAndCollapsesUnderTabsInProgress() {
        show()
        compose.onNodeWithText("Study progress").assertDoesNotExist()
        val original = compose.onNodeWithTag("studies-credit-card").getUnclippedBoundsInRoot()
        assertTrue(original.bottom - original.top < 150.dp)
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("studies-tab-1").performClick()
            val tops = mutableListOf<Float>()
            repeat(20) { frame ->
                compose.mainClock.advanceTimeByFrame()
                compose.waitForIdle()
                val tabs = compose.onNodeWithTag("studies-tabs").fetchSemanticsNode().boundsInRoot
                val slot = compose.onNodeWithTag("studies-credit-slot").fetchSemanticsNode().boundsInRoot
                val tools = compose.onNodeWithTag("studies-progress-tools").fetchSemanticsNode().boundsInRoot
                if (slot.height > 0f) {
                    assertEquals("Mask must begin at the buttons, with no invisible gap", tabs.bottom, slot.top, 1f)
                    assertEquals("Constant list spacing throughout every frame", slot.bottom + with(compose.density) { 14.dp.toPx() }, tools.top, 2f)
                } else {
                    // Semantics represents a fully clipped zero-height slot with Rect.Zero.
                    assertEquals("Final spacing has no hidden disappearing lazy item", tabs.bottom + with(compose.density) { 14.dp.toPx() }, tools.top, 2f)
                }
                tops += tools.top
                if (frame in listOf(2, 6, 10, 16)) capture("credits-frame-$frame.png")
            }
            assertTrue("Every frame moves continuously upward", tops.zipWithNext().all { (a, b) -> b <= a + 1f })
            val finalTail = tops.zipWithNext().takeLast(8)
            assertTrue("No terminal 14dp jump", finalTail.all { (a, b) -> kotlin.math.abs(a - b) < with(compose.density) { 4.dp.toPx() } })
            compose.onNodeWithTag("studies-credit-card").assertDoesNotExist()
            compose.onNodeWithTag("studies-tab-0").performClick()
            compose.mainClock.advanceTimeBy(600)
            compose.onNodeWithTag("studies-credit-card").assertIsDisplayed()
        } finally { compose.mainClock.autoAdvance = true }
    }

    @Test fun financeLabelsStayBesideTheirIcons() {
        show(language = AppLanguage.SLOVAK)
        listOf("studies-payments-value", "studies-scholarship-value").forEach { tag ->
            compose.onNodeWithTag("studies-screen", useUnmergedTree = true).performScrollToNode(hasTestTag("$tag-label"))
            val icon = compose.onNodeWithTag("$tag-icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val label = compose.onNodeWithTag("$tag-label", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue(label.left >= icon.right)
            assertEquals(icon.center.y, label.center.y, 1f)
            compose.onNodeWithTag("$tag-label", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                    assertTrue(action(layouts))
                    assertEquals("Finance label must stay on one line", 1, layouts.single().lineCount)
                    assertFalse("Finance label must fit in full", layouts.single().hasVisualOverflow)
                }
        }
        capture("studies-finance-labels.png")
    }

    @Test fun financeLabelsFitAcrossLanguagesAndTextSizes() {
        var language by mutableStateOf(AppLanguage.ENGLISH)
        var fontScale by mutableFloatStateOf(1f)
        var width by mutableIntStateOf(340)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TaktI18n.use(language)
                TaktTheme { Box(Modifier.width(width.dp).fillMaxHeight()) {
                    StudiesContent(StudentHubState(snapshot), settings = AppSettings(language = language), uisManaged = true)
                } }
            }
        }
        AppLanguage.entries.forEach { locale ->
            listOf(1f, 1.5f).forEach { scale ->
                listOf(340, 360).forEach { screenWidth ->
                    compose.runOnIdle { language = locale; fontScale = scale; width = screenWidth }
                    listOf("studies-payments-value", "studies-scholarship-value").forEach { tag ->
                        compose.onNodeWithTag("studies-screen", useUnmergedTree = true).performScrollToNode(hasTestTag("$tag-label"))
                        compose.onNodeWithTag("$tag-label", useUnmergedTree = true)
                            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                                assertTrue(action(layouts))
                                assertEquals("$locale, scale $scale", 1, layouts.single().lineCount)
                                assertFalse("$locale, scale $scale, width $screenWidth", layouts.single().hasVisualOverflow)
                            }
                    }
                }
            }
        }
    }

    @Test fun uisStudySemesterWinsOverRestoredCourseAndManualFallbacks() {
        val restored = live.copy(semester = 5)
        compose.setContent {
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme {
                StudiesContent(StudentHubState(snapshot), localCourses = listOf(restored),
                    settings = AppSettings(currentSemester = 5), uisManaged = true)
            }
        }
        compose.onNodeWithText("Semester 3 · winter").assertIsDisplayed()
        compose.onNodeWithTag("studies-tab-1").performClick()
        compose.onNodeWithTag("semester-5-active").assertDoesNotExist()
    }

    @Test fun cachedRefreshNeverAddsALoadingPane() {
        show(StudentHubState(snapshot, loading = true))
        compose.onNodeWithTag("studies-credit-card").assertIsDisplayed()
        compose.onNodeWithTag("studies-status").assertDoesNotExist()
    }

    @Test fun allStudiesAreAvailableWithoutAnOptIn() {
        show(StudentHubState(snapshot.copy(showPreviousStudies = false)))
        compose.onNodeWithTag("studies-study-selector").performClick()
        compose.onNodeWithTag("studies-select-previous").assertExists()
        compose.onNodeWithTag("studies-selector-previous").assertDoesNotExist()
    }

    @Test fun planShowsEverySemesterAndEveryCourseWithoutMoreButtons() {
        val full = (1..7).map { live.copy(id = "course-$it", title = "Course $it") } + live.copy(id = "future", semester = 6, status = CourseStatus.PLANNED)
        compose.setContent {
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme { StudiesContent(StudentHubState(snapshot), localCourses = full, gradeRepository = object : GradeRepository by forbiddenGrades {
                override fun observeManualGrade(courseId: String): Flow<GradeLetter?> = kotlinx.coroutines.flow.flowOf(null)
            }) }
        }
        compose.onNodeWithTag("studies-tab-1").performClick()
        compose.onNodeWithTag("studies-more-courses-3").assertDoesNotExist()
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasTestTag("progress-course-course-7"))
        compose.onNodeWithTag("progress-course-course-7").assertIsDisplayed()
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasTestTag("semester-toggle-6"))
        compose.onNodeWithTag("semester-toggle-6").assertIsDisplayed()
        compose.onNodeWithTag("studies-future-semesters").assertDoesNotExist()
    }

    @Test fun mixedGradesAndStatusesHaveTheSameHorizontalCenter() {
        val rows = listOf(live.copy(id = "grade", officialGrade = GradeLetter.E, fulfilledOn = LocalDate.of(2026, 5, 10)),
            live.copy(id = "clock"), live.copy(id = "minus", status = CourseStatus.NOT_ENROLLED))
        compose.setContent {
            TaktI18n.use(AppLanguage.ENGLISH)
            TaktTheme { com.kpyruy.takt.feature.studyplan.SemesterSection(3, rows, object : GradeRepository by forbiddenGrades {
                override fun observeManualGrade(courseId: String): Flow<GradeLetter?> = kotlinx.coroutines.flow.flowOf(null)
            }, true, true, {}, {}, {}, compactPreview = true) }
        }
        val centers = listOf("grade", "clock", "minus").map { compose.onNodeWithTag("status-$it").fetchSemanticsNode().boundsInRoot.center.x }
        assertEquals(centers[0], centers[1], 1f)
        assertEquals(centers[0], centers[2], 1f)
    }

    @Test fun uisSemesterIsReadOnlyButLocalSemesterKeepsItsAction() {
        var picks = 0
        compose.setContent {
            var managed by remember { mutableStateOf(true) }
            TaktTheme { Column {
                androidx.compose.material3.TextButton(onClick = { managed = false }, modifier = Modifier.testTag("local-mode")) { androidx.compose.material3.Text("Local") }
                StudiesContent(StudentHubState(snapshot), localCourses = listOf(live), uisManaged = managed, onChooseSemester = { picks++ })
            } }
        }
        compose.onNodeWithTag("studies-current-semester").assertHasNoClickAction()
        compose.onNodeWithTag("local-mode").performClick()
        compose.onNodeWithTag("studies-current-semester").performClick()
        compose.runOnIdle { assertEquals(1, picks) }
    }

    @Test fun zeroScholarshipFitsNarrowSlovakLayoutWithLargeFonts() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                TaktI18n.use(AppLanguage.SLOVAK)
                TaktTheme { Box(Modifier.width(320.dp).fillMaxHeight()) {
                    StudiesContent(StudentHubState(snapshot.copy(studies = listOf(current.copy(scholarships = emptyList(), titleSk = "Priemyselná informatika, automatizácia a robotika")))),
                        settings = AppSettings(language = AppLanguage.SLOVAK), uisManaged = true)
                } }
            }
        }
        compose.onNodeWithTag("studies-screen").performScrollToNode(hasText("0,00 EUR"))
        compose.onNodeWithTag("studies-scholarship-value", useUnmergedTree = true).assertTextEquals("0,00 EUR")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                assertTrue(action(layouts)); assertEquals(1, layouts.single().lineCount); assertFalse(layouts.single().hasVisualOverflow)
            }
        capture("studies-scholarship-narrow-sk.png")
    }

    @Test fun overviewUsesApprovedOrderAndAllThreeStudyTabs() {
        show()
        compose.onNodeWithText("Studies").assertExists()
        compose.onNodeWithTag("studies-study-selector").assertIsDisplayed()
        compose.onNodeWithTag("studies-tabs").assertIsDisplayed()
        compose.onNodeWithTag("studies-credit-card").assertIsDisplayed()
        val selector = compose.onNodeWithTag("studies-study-selector").fetchSemanticsNode().boundsInRoot
        val settings = compose.onNodeWithTag("studies-settings").fetchSemanticsNode().boundsInRoot
        val tabs = compose.onNodeWithTag("studies-tabs").fetchSemanticsNode().boundsInRoot
        val credits = compose.onNodeWithTag("studies-credit-card").fetchSemanticsNode().boundsInRoot
        assertTrue(selector.right <= settings.left)
        assertEquals(selector.top, settings.top, 1f)
        assertEquals(selector.width, settings.width, 1f)
        assertEquals(selector.height, settings.height, 1f)
        assertTrue(selector.bottom <= tabs.top)
        assertTrue(tabs.bottom <= credits.top)
        compose.onNodeWithText("Automation & Control").assertDoesNotExist()
        compose.onNodeWithTag("studies-study-selector").performClick()
        compose.onNodeWithText("Automation & Control").assertExists()
        compose.onNodeWithText("STU").assertExists()
        compose.onNodeWithTag("studies-selector-done").performClick()
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
        compose.onNodeWithText("Applied Informatics").assertDoesNotExist()
        compose.onNodeWithTag("studies-study-selector").performClick()
        compose.onNodeWithTag("studies-select-previous").assertIsSelected()
        compose.onNodeWithTag("studies-selector-done").performClick()
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
        compose.onNodeWithTag("studies-study-selector").performClick()
        compose.onNodeWithText("Automation & Control").assertExists()
        compose.onNodeWithTag("studies-selector-done").performClick()
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
        compose.onNodeWithTag("studies-study-selector").performClick()
        compose.onNodeWithTag("studies-select-current").assertIsSelected()
        capture("studies-switcher-dark.png")
        compose.onNodeWithTag("studies-selector-done").performClick()
    }

    @Test fun lightFilledStudySelectorMatchesApprovedSelectionFlow() {
        show(settings = AppSettings(themeMode = AppThemeMode.LIGHT, cardAppearance = CardAppearance.TONAL_FILLED))
        compose.onNodeWithTag("studies-study-selector").performClick()
        compose.onNodeWithTag("studies-select-current").assertIsSelected()
        compose.onNodeWithTag("studies-select-previous").assertIsNotSelected()
        compose.onNodeWithTag("studies-selector-previous").assertDoesNotExist()
        compose.onNodeWithTag("studies-selector-done").assertIsDisplayed()
        capture("studies-switcher.png")
        compose.onNodeWithTag("studies-selector-done").performClick()
        compose.onNodeWithTag("studies-study-selector").assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        captureStudiesScreenshot(name)
    }

    @Test fun slovakChromeUsesOfficialSlovakProgrammeTitle() {
        show(StudentHubState(snapshot.copy(studies = listOf(current.copy(titleEn = current.title, titleSk = "Automatizácia a robotika")))), language = AppLanguage.SLOVAK)
        compose.onNodeWithText("Štúdium").assertExists()
        compose.onNodeWithText("Prehľad").assertExists()
        compose.onNodeWithTag("studies-tab-2").assertTextEquals("Financie")
        compose.onNodeWithTag("studies-study-selector").performClick()
        compose.onNodeWithText("Automatizácia a robotika").assertExists()
        compose.onNodeWithText("Automation & Control").assertDoesNotExist()
    }
}
