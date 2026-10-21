package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.UiKey
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs

/** Real routes at phone-like width; fixture changes are explicitly emulator-only. */
class OctoberUiPolishTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer

    private fun seed() = runBlocking {
        data.seedLiquidGlassReview()
        val courses = data.database.courseDao().observeAll().first()
        data.database.courseDao().insertAll(courses.map { row ->
            when (row.id) {
                "glass-physics" -> row.copy(title = "Základy štatistiky", titleEn = "Basics of statistics", titleSk = "Základy štatistiky")
                "glass-programming" -> row.copy(title = "Technické prostriedky automatizovaného riadenia",
                    titleEn = "Technical means of automated control", titleSk = "Technické prostriedky automatizovaného riadenia")
                "glass-4-1", "glass-5-1" -> row.copy(status = "enrolled")
                else -> row
            }
        })
        data.settingsRepository.setCourseNameLanguage(CourseNameLanguage.FOLLOW_APP)
        data.awaitRestoredPlanning()
    }

    @Test fun localizedSettingsHaveReadableLabelsAndOneDataRoute() {
        seed()
        val store = object : AppReleasePreferenceStore {
            var value = AppReleasePreferences(automaticChecks = false,
                latest = AppRelease("v0.2.0", "https://github.com/Kpyruy/Takt/releases/tag/v0.2.0", null),
                lastSuccessfulCheck = Instant.parse("2026-10-12T23:52:00Z").toEpochMilli())
            override fun read() = value
            override fun write(value: AppReleasePreferences) { this.value = value }
        }
        val app = compose.activity.application as TaktApplication
        app.javaClass.getDeclaredField("appReleaseRepository").apply { isAccessible = true }
            .set(app, AppReleaseRepository("0.2.0+glass.preview", store, { checkNotNull(store.value.latest) }))
        compose.activityRule.scenario.recreate()
        for (language in listOf(AppLanguage.UKRAINIAN, AppLanguage.ENGLISH, AppLanguage.SLOVAK)) {
            runBlocking { data.settingsRepository.setLanguage(language) }
            compose.waitForIdle()
            compose.onNodeWithContentDescription(t(UiKey.TEXT_SETTINGS)).performClick()
            compose.onNode(hasText(t(UiKey.TEXT_AUTOMATIC_UIS_UPDATES)) and hasClickAction()).performScrollTo().performClick()
            for (label in listOf("Прогрес", "Предмети", "Розклад")) {
                val node = compose.onNodeWithText(t(label))
                node.performScrollTo()
                val result = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(result) }
                assertEquals("Vertical label ${t(label)}", 1, result.single().lineCount)
            }
            capture("${language.name.lowercase()}-uis-sync")
            back()
            compose.onNode(hasText(t(UiKey.TEXT_APP_UPDATES)) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText(t(UiKey.TEXT_ONCE_A_DAY_WHEN_OPENING_TAKT_CHECKS_THE_PUBLIC_GITHUB_RELEASE)).assertDoesNotExist()
            val time = compose.onAllNodes(hasText(":52", substring = true)).fetchSemanticsNodes()
            assertTrue("24-hour time missing", time.isNotEmpty())
            compose.onAllNodes(hasText(" PM", substring = true) or hasText(" AM", substring = true)).assertCountEquals(0)
            capture("${language.name.lowercase()}-updates")
            back()
            compose.onNode(hasText(t(UiKey.TEXT_LANGUAGE)) and hasClickAction()).performScrollTo().performClick()
            val first = compose.onNodeWithText(t(UiKey.TEXT_APPLICATION_LANGUAGE))
            val second = compose.onNodeWithText(t(UiKey.TEXT_COURSE_NAME_LANGUAGE))
            val a = mutableListOf<TextLayoutResult>(); val b = mutableListOf<TextLayoutResult>()
            first.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(a) }
            second.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(b) }
            assertEquals(a.single().layoutInput.style.fontSize, b.single().layoutInput.style.fontSize)
            capture("${language.name.lowercase()}-language")
            back()
            compose.onNode(hasText("Documents/Takt") and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText(t(UiKey.TEXT_EXPORT_JSON)).assertExists()
            compose.onNodeWithText(t(UiKey.TEXT_SELECTED_DATA_APPLIED_AND_SAVED)).assertDoesNotExist()
            capture("${language.name.lowercase()}-backup")
            back(); back()
        }
    }

    @Test fun subjectsProgressAndCalendarRespectRequestedStructure() {
        seed()
        runBlocking { data.settingsRepository.setLanguage(AppLanguage.SLOVAK) }
        compose.waitForIdle()
        root(t(UiKey.TEXT_SUBJECTS))
        compose.onNodeWithText(t(UiKey.TEXT_ALL_SEMESTERS)).assertDoesNotExist()
        compose.onNodeWithContentDescription(t(UiKey.TEXT_CHOOSE_A_SEMESTER)).assertDoesNotExist()
        compose.onNodeWithContentDescription(t(UiKey.TEXT_FIND_A_SUBJECT)).assertDoesNotExist()
        compose.onNodeWithContentDescription(t(UiKey.TEXT_SETTINGS)).performClick()
        compose.onNodeWithText(t(UiKey.TEXT_LANGUAGE)).assertExists()
        back()
        capture("subjects")
        root(t(UiKey.TEXT_PROGRESS))
        capture("progress-plan")
        compose.onNodeWithTag("progress-screen").performScrollToNode(hasTestTag("semester-toggle-4"))
        compose.onNodeWithTag("semester-active-count-4", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("progress-screen").performScrollToIndex(0)
        compose.onNode(hasText(t(UiKey.TEXT_ACTIVE)) and hasClickAction()).performClick()
        assertStatusAlignment()
        capture("progress-active")
        compose.onNodeWithTag("progress-screen").performScrollToIndex(0)
        compose.onNode(hasText(t(UiKey.TEXT_COMPLETED)) and hasClickAction()).performClick()
        assertStatusAlignment()
        capture("progress-completed")
        compose.onNodeWithTag("progress-screen").performScrollToIndex(0)
        compose.onNodeWithTag("progress-menu").performClick()
        compose.onNodeWithTag("progress-search-open").performClick()
        compose.onNodeWithTag("progress-search-query").performTextInput("FYZI")
        compose.onNodeWithTag("progress-search-result-glass-physics").assertExists().performClick()
        compose.onNodeWithText("Základy štatistiky").assertExists()
        val titleBounds = compose.onNodeWithTag("subject-header-title").fetchSemanticsNode().boundsInRoot
        for (tag in listOf("subject-header-back", "subject-header-menu", "subject-header-avatar")) {
            val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue("$tag must align with the title", abs(bounds.center.y - titleBounds.center.y) < 1f)
        }
        val avatar = compose.onNodeWithTag("subject-header-avatar").fetchSemanticsNode().boundsInRoot
        val metadata = compose.onNodeWithTag("subject-header-metadata").fetchSemanticsNode().boundsInRoot
        assertEquals(avatar.left, metadata.left, 1f)
        for (label in listOf("Огляд", "Задачі", "Екзамен", "Нотатки")) {
            val tab = compose.onNode(hasText(t(label)) and SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab))
            tab.assertIsDisplayed()
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNode(hasText(t(label)) and hasAnyAncestor(SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab)),
                useUnmergedTree = true).performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals("A subject tab must keep its word together", 1, layouts.single().lineCount)
        }
        capture("subject-header")
        compose.onNode(hasText(t(UiKey.TEXT_EXAM)) and hasClickAction()).performClick()
        compose.onNodeWithTag("exam-target-A").performScrollTo()
        for (letter in listOf("A", "B", "C", "D", "E")) {
            val button = compose.onNodeWithTag("exam-target-$letter").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val text = compose.onNodeWithText(letter, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(button.center.x, text.center.x, 1f)
            assertEquals(button.center.y, text.center.y, 1f)
        }
        capture("exam-targets")
        back(); back()
        root(t(UiKey.TEXT_CALENDAR))
        compose.onNode(hasText(t(UiKey.TEXT_MONTH)) and hasClickAction()).performClick()
        val today = LocalDate.now()
        compose.onNodeWithTag("month-day-${today.toEpochDay()}").assertIsDisplayed()
        val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val cells = (0L..6L).map {
            compose.onNodeWithTag("month-day-${weekStart.plusDays(it).toEpochDay()}").fetchSemanticsNode().boundsInRoot
        }
        // A seven-way split of physical pixels may distribute a one-pixel remainder.
        cells.forEach { assertEquals(cells.first().width, it.width, 1f) }
        for (date in (0L..6L).map(weekStart::plusDays)) {
            val cell = compose.onNodeWithTag("month-day-${date.toEpochDay()}")
            val numeral = compose.onNode(hasText(date.dayOfMonth.toString()) and
                hasAnyAncestor(hasTestTag("month-day-${date.toEpochDay()}")), useUnmergedTree = true)
            assertEquals(cell.fetchSemanticsNode().boundsInRoot.center.x,
                numeral.fetchSemanticsNode().boundsInRoot.center.x, 1f)
        }
        capture("calendar-month")
        compose.onNode(hasText(t(UiKey.TEXT_DAY)) and hasClickAction()).performClick()
        capture("calendar-day")
        compose.onNode(hasText(t(UiKey.TEXT_WEEK)) and hasClickAction()).performClick()
        capture("calendar-week")
    }

    @Test fun customThemeCreateEditDeleteApplyAndBackupAreDurable() {
        seed()
        runBlocking {
            data.settingsRepository.setLanguage(AppLanguage.ENGLISH)
            data.settingsRepository.setAppearance(data.settingsRepository.settings.first().copy(
                customThemes = emptyList(), selectedCustomThemeId = null))
        }
        compose.waitForIdle()
        openAppearance()
        capture("appearance-lighting")
        compose.onNodeWithTag("appearance-color-open").performScrollTo().performClick()
        compose.onNodeWithTag("theme-create").performScrollTo().performClick()
        compose.onNodeWithTag("theme-name").performScrollTo().performTextInput("Ocean review")
        compose.onNodeWithTag("theme-hex").performScrollTo().performTextReplacement("#007E91")
        compose.onNodeWithTag("theme-save").performClick()
        compose.onNodeWithTag("palette-popup-apply").performClick()
        compose.onNodeWithText(t(UiKey.TEXT_CARD_STYLE)).performScrollTo()
        capture("appearance-card-styles")
        compose.onNodeWithText(t(UiKey.TEXT_APPLY_APPEARANCE)).performClick()
        compose.waitForIdle()
        val created = settings().customThemes.single()
        assertEquals("Ocean review", created.name)
        assertEquals("#007E91", created.primaryHex)
        assertEquals(created.id, settings().selectedCustomThemeId)
        val durable = SharedPreferencesAppSettingsRepository(instrumentation.targetContext)
        assertEquals(created, runBlocking { durable.settings.first() }.selectedCustomTheme)
        back()
        compose.activityRule.scenario.recreate()
        openAppearance()
        compose.onNodeWithTag("appearance-color-open").performScrollTo().performClick()
        compose.onNodeWithTag("appearance-color-custom-${created.id}").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("theme-edit-${created.id}").performScrollTo().performClick()
        compose.onNodeWithTag("theme-name").performScrollTo().performTextReplacement("Lagoon review")
        compose.onNodeWithTag("theme-hex").performScrollTo().performTextReplacement("#147D64")
        compose.onNodeWithTag("theme-save").performClick()
        capture("appearance-custom-theme")
        compose.onNodeWithTag("palette-popup-apply").performClick()
        compose.onNodeWithText(t(UiKey.TEXT_APPLY_APPEARANCE)).performClick()
        compose.waitForIdle()
        val edited = settings().customThemes.single()
        assertEquals(created.id, edited.id)
        assertEquals("Lagoon review", edited.name)
        assertEquals("#147D64", edited.primaryHex)
        val backup = runBlocking { data.backupRepository.exportJson() }
        back()
        openAppearance()
        compose.onNodeWithTag("appearance-color-open").performScrollTo().performClick()
        compose.onNodeWithTag("theme-delete-${created.id}").performScrollTo().performClick()
        compose.onNodeWithTag("palette-popup-apply").performClick()
        compose.onNodeWithText(t(UiKey.TEXT_APPLY_APPEARANCE)).performClick()
        compose.waitForIdle()
        assertTrue(settings().customThemes.isEmpty())
        assertNull(settings().selectedCustomThemeId)
        val before = settings()
        runBlocking {
            data.backupRepository.importSelectedJson(backup, data.backupRepository.exportJson(),
                setOf(BackupSection.APPEARANCE))
        }
        compose.waitForIdle()
        val restored = settings()
        assertEquals(edited, restored.selectedCustomTheme)
        assertEquals(before.language, restored.language)
        assertEquals(before.uisAutoSync, restored.uisAutoSync)
        assertEquals(before.currentSemester, restored.currentSemester)
        assertEquals(edited, runBlocking {
            SharedPreferencesAppSettingsRepository(instrumentation.targetContext).settings.first()
        }.selectedCustomTheme)
    }

    private fun assertStatusAlignment() {
        val nodes = compose.onAllNodes(hasTestTagPrefix("status-"), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
        assertTrue("Expected several progress status columns", nodes.size >= 2)
        nodes.forEach { assertEquals(nodes.first().boundsInRoot.center.x, it.boundsInRoot.center.x, 1f) }
    }

    private fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("Tag begins with $prefix") {
        it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith(prefix)
    }

    private fun settings() = runBlocking { data.settingsRepository.settings.first() }
    private fun openAppearance() {
        compose.waitForIdle()
        compose.onNodeWithContentDescription(t(UiKey.TEXT_SETTINGS)).performClick()
        compose.onNode(hasText(t(UiKey.TEXT_APPEARANCE)) and hasClickAction()).performScrollTo().performClick()
    }

    private fun root(label: String) = compose.onAllNodes(hasText(label) and hasClickAction()).onLast().performClick()
    private fun back() { compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle(); instrumentation.waitForIdleSync()
        // The Material dismiss label follows the Activity's native locale, not Takt's dictionary.
        // Dismiss the emulator fixture's reminder through its actual control before capturing.
        val dismissId = compose.activity.resources.getIdentifier("m3c_snackbar_dismiss", "string", compose.activity.packageName)
        if (dismissId != 0) {
            val labels = listOf(java.util.Locale.ENGLISH, java.util.Locale("uk"), java.util.Locale("sk")).map { locale ->
                val config = android.content.res.Configuration(compose.activity.resources.configuration).apply { setLocale(locale) }
                compose.activity.createConfigurationContext(config).getString(dismissId)
            }
            val matcher = labels.map { hasContentDescription(it) }.reduce { a, b -> a or b }
            val dismiss = compose.onAllNodes(matcher)
            if (dismiss.fetchSemanticsNodes().size == 1) dismiss.onFirst().performClick()
        }
        compose.waitForIdle()
        android.os.SystemClock.sleep(300)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        instrumentation.targetContext.openFileOutput("oct13-$name.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
