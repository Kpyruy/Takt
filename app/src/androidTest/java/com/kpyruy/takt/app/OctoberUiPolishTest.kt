package com.kpyruy.takt.app

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
            compose.onNodeWithContentDescription(t("Налаштування")).performClick()
            compose.onNode(hasText(t("Автооновлення UIS")) and hasClickAction()).performScrollTo().performClick()
            for (label in listOf("Прогрес", "Предмети", "Розклад")) {
                val node = compose.onNodeWithText(t(label))
                node.performScrollTo()
                val result = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(result) }
                assertEquals("Vertical label ${t(label)}", 1, result.single().lineCount)
            }
            capture("${language.name.lowercase()}-uis-sync")
            back()
            compose.onNode(hasText(t("Оновлення додатка")) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText(t("Раз на добу під час відкриття Takt. Перевіряється лише публічний реліз на GitHub.")).assertDoesNotExist()
            val time = compose.onAllNodes(hasText(":52", substring = true)).fetchSemanticsNodes()
            assertTrue("24-hour time missing", time.isNotEmpty())
            compose.onAllNodes(hasText(" PM", substring = true) or hasText(" AM", substring = true)).assertCountEquals(0)
            capture("${language.name.lowercase()}-updates")
            back()
            compose.onNode(hasText(t("Мова")) and hasClickAction()).performScrollTo().performClick()
            val first = compose.onNodeWithText(t("Мова застосунку"))
            val second = compose.onNodeWithText(t("Мова назв предметів"))
            val a = mutableListOf<TextLayoutResult>(); val b = mutableListOf<TextLayoutResult>()
            first.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(a) }
            second.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(b) }
            assertEquals(a.single().layoutInput.style.fontSize, b.single().layoutInput.style.fontSize)
            capture("${language.name.lowercase()}-language")
            back()
            compose.onNode(hasText("Documents/Takt") and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText(t("Експортувати JSON")).assertExists()
            compose.onNodeWithText(t("Вибрані дані застосовано й збережено.")).assertDoesNotExist()
            capture("${language.name.lowercase()}-backup")
            back(); back()
        }
    }

    @Test fun subjectsProgressAndCalendarRespectRequestedStructure() {
        seed()
        runBlocking { data.settingsRepository.setLanguage(AppLanguage.SLOVAK) }
        root(t("Предмети"))
        compose.onNodeWithText(t("Усі семестри")).assertDoesNotExist()
        compose.onNodeWithContentDescription(t("Обрати семестр")).assertDoesNotExist()
        compose.onNodeWithContentDescription(t("Знайти предмет")).assertDoesNotExist()
        compose.onNodeWithContentDescription(t("Налаштування")).performClick()
        compose.onNodeWithText(t("Мова")).assertExists()
        back()
        capture("subjects")
        root(t("Прогрес"))
        capture("progress-plan")
        compose.onNodeWithTag("progress-screen").performScrollToNode(hasTestTag("semester-toggle-4"))
        compose.onNodeWithText(t("1 активний предмет")).assertExists()
        compose.onNodeWithTag("progress-screen").performScrollToIndex(0)
        compose.onNode(hasText(t("Активні")) and hasClickAction()).performClick()
        capture("progress-active")
        compose.onNodeWithTag("progress-screen").performScrollToIndex(0)
        compose.onNode(hasText(t("Здані")) and hasClickAction()).performClick()
        capture("progress-completed")
        root(t("Календар"))
        compose.onNode(hasText(t("Місяць")) and hasClickAction()).performClick()
        val today = LocalDate.now()
        compose.onNodeWithTag("month-day-${today.toEpochDay()}").assertIsDisplayed()
        capture("calendar-month")
        compose.onNode(hasText(t("День")) and hasClickAction()).performClick()
        capture("calendar-day")
        compose.onNode(hasText(t("Тиждень")) and hasClickAction()).performClick()
        capture("calendar-week")
    }

    private fun root(label: String) = compose.onAllNodes(hasText(label) and hasClickAction()).onLast().performClick()
    private fun back() { compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; compose.waitForIdle() }
    private fun capture(name: String) {
        compose.waitForIdle(); instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(300)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        instrumentation.targetContext.openFileOutput("oct13-$name.png", 0).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
