package com.kpyruy.takt.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.NoteAttachment
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.motion.TaktMotion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.After
import androidx.lifecycle.Lifecycle
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Real Activity recreation checks; opt in only on a disposable emulator with -e draftReview true. */
class DraftRestorationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val data get() = (compose.activity.application as TaktApplication).dataContainer
    private val courseId = "FYZI_6B"
    private val prefix = "Draft review "
    private var testActivity: MainActivity? = null
    private var restoreDocumentsTree: (() -> Unit)? = null
    private val date get() = LocalDate.now()
    private var lastGradeTabTap = 0L

    @Before fun prepareDisposableFixture() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("draftReview") == "true")
        assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone") || Build.HARDWARE == "ranchu")
        runBlocking {
            // seedIfNeeded deliberately preserves populated data; reset this opt-in emulator fixture
            // so a preceding Glass review (with different course IDs) cannot change the test routes.
            data.database.clearAllTables()
            data.seedIfNeeded()
            data.settingsRepository.setCurrentSemester(3)
            data.settingsRepository.setLanguage(AppLanguage.UKRAINIAN)
        }
        compose.waitForIdle()
    }

    @Test fun taskScreenRestoresFieldsWithoutSavingUntilExplicitSave() {
        openGlobal("Завдання")
        fillTask()
        compose.activityRule.scenario.recreate()
        assertTaskDraft()
        assertNoTaskSaved()
        save()
        compose.waitUntil(5_000) { tasks().count { it.title == prefix + "task" } == 1 }
        val saved = tasks().single { it.title == prefix + "task" }
        assertEquals("Keep this description", saved.description)
        assertEquals(date, saved.dueDate)
        assertTrue(saved.requiredForExam)
    }

    @Test fun gradedTestScreenRestoresDateDurationPointsAndAdmission() {
        openGlobal("Тест")
        fillGrade()
        compose.activityRule.scenario.recreate()
        assertGradeDraft()
        assertNoGradeSaved()
        saveGrade()
        compose.waitUntil(5_000) { grades().count { it.title == prefix + "test" } == 1 }
        val saved = grades().single { it.title == prefix + "test" }
        assertEquals(date, saved.dueDate)
        assertEquals(45, saved.durationMinutes)
        assertEquals(7.0, saved.earnedPoints, 0.0)
        assertEquals(20.0, saved.maxPoints, 0.0)
        assertEquals(6.0, saved.minimumPointsForExam!!, 0.0)
        assertTrue(saved.completed)
        assertTrue(saved.requiredForExam)
    }

    @Test fun noteScreenRestoresContentAndAddedLinkWithoutDuplicatingIt() {
        openGlobal("Нотатка")
        fillNote()
        addLink()
        compose.activityRule.scenario.recreate()
        assertNoteDraft()
        compose.onNodeWithText("Lecture reference").assertExists()
        assertNoNoteSaved()
        save()
        compose.waitUntil(5_000) { notes().count { it.title == prefix + "note" } == 1 }
        val saved = notes().single { it.title == prefix + "note" }
        assertEquals("Keep the lecture notes", saved.content)
        assertEquals(listOf(NoteAttachment.webLink("https://example.org/lecture", "Lecture reference")), saved.attachments)
    }

    @Test fun unfinishedLinkDialogRestoresItsInputsAndRemainsOpen() {
        openGlobal("Нотатка")
        fillNote()
        openLink()
        compose.activityRule.scenario.recreate()
        field("Адреса посилання").assertTextContains("https://example.org/lecture")
        field("Назва (необов’язково)").assertTextContains("Lecture reference")
        compose.onNodeWithText("Додати", useUnmergedTree = true).performClick()
        assertNoteDraft()
        compose.onNodeWithText("Lecture reference").assertExists()
        assertNoNoteSaved()
    }

    @Test fun subjectTaskSheetRemainsOpenWithItsDraftAfterRecreation() {
        openSubject("Задачі")
        openSubjectWork("Завдання без балів")
        fillTask()
        compose.activityRule.scenario.recreate()
        compose.onNode(hasText("Додати завдання") and !hasClickAction()).assertExists()
        assertTaskDraft()
        assertNoTaskSaved()
        save()
        compose.waitUntil(5_000) { tasks().count { it.title == prefix + "task" } == 1 }
    }

    @Test fun subjectGradeSheetRemainsOpenWithItsDraftAfterRecreation() {
        openSubject("Задачі")
        openSubjectWork("Оцінювана робота з балами")
        fillGrade()
        compose.activityRule.scenario.recreate()
        assertGradeDraft()
        compose.onNode(hasText("Додати тест") and !hasClickAction()).assertExists()
        assertNoGradeSaved()
        saveGrade()
        compose.waitUntil(5_000) { grades().count { it.title == prefix + "test" } == 1 }
    }

    @Test fun subjectNoteSheetRemainsOpenWithItsDraftAndLinkAfterRecreation() {
        openSubject("Нотатки")
        compose.onNodeWithText("Додати нотатку").performScrollTo().performClick()
        fillNote()
        addLink()
        compose.activityRule.scenario.recreate()
        compose.onNode(hasText("Додати нотатку") and !hasClickAction()).assertExists()
        assertNoteDraft()
        compose.onNodeWithText("Lecture reference").assertExists()
        assertNoNoteSaved()
        save()
        compose.waitUntil(5_000) { notes().count { it.title == prefix + "note" } == 1 }
        assertEquals(1, notes().single { it.title == prefix + "note" }.attachments.size)
    }

    @Test fun editingNoteRestoresTargetAndExistingAttachmentThenUpdatesOneRecord() {
        val original = CourseNote("draft-review-existing-note", courseId, prefix + "existing note", "Original", 1L,
            listOf(NoteAttachment("Draft attachment.txt", "takt-fixture-reference", "text/plain")))
        runBlocking { data.studyContentRepository.upsertNote(original) }
        openSubject("Нотатки")
        compose.onNodeWithContentDescription("Дії нотатки")
            .performScrollTo().performClick()
        compose.onNodeWithText("Редагувати").performClick()
        fillNote()
        addLink()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Редагувати нотатку").assertExists()
        assertNoteDraft()
        // The original card behind the modal also contains this filename.
        compose.onNode(hasText("Draft attachment.txt") and !hasClickAction()).assertExists()
        compose.onNodeWithText("Lecture reference").assertExists()
        assertEquals(original, notes().single { it.id == original.id })
        save("Оновити")
        compose.waitUntil(5_000) { notes().any { it.id == original.id && it.title == prefix + "note" } }
        val saved = notes().single { it.title == prefix + "note" }
        assertEquals(original.id, saved.id)
        assertEquals(2, saved.attachments.size)
        assertEquals(original.attachments.single(), saved.attachments.first())
    }

    @Test fun editingScoredTaskRestoresTargetAndPointsThenUpdatesOneRecord() {
        val original = StudyTask("draft-review-existing-task", courseId, prefix + "existing task", "Original",
            null, true, earnedPoints = 4.0, maxPoints = 20.0)
        runBlocking { data.studyContentRepository.upsertTask(original) }
        openSubject("Задачі")
        compose.onNodeWithContentDescription("Дії завдання").performScrollTo().performClick()
        compose.onNodeWithText("Редагувати").performClick()
        fillTask()
        fill("Отримано", "7")
        fill("Максимум", "30")
        fill("Мінімум балів для допуску", "6")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Редагувати завдання").assertExists()
        assertTaskDraft()
        field("Отримано").assertTextContains("7")
        field("Максимум").assertTextContains("30")
        field("Мінімум балів для допуску").assertTextContains("6")
        assertEquals(original, tasks().single { it.id == original.id })
        save("Оновити")
        compose.waitUntil(5_000) { tasks().any { it.id == original.id && it.title == prefix + "task" } }
        val saved = tasks().single { it.title == prefix + "task" }
        assertEquals(original.id, saved.id)
        assertEquals(7.0, saved.earnedPoints!!, 0.0)
        assertEquals(30.0, saved.maxPoints!!, 0.0)
        assertEquals(6.0, saved.minimumPointsForExam!!, 0.0)
    }

    @Test fun editingGradedTestRestoresTargetThenUpdatesOneRecord() {
        val original = GradeItem("draft-review-existing-grade", courseId, prefix + "existing test",
            GradeItemType.TEST, 0.0, 20.0, completed = false)
        runBlocking { data.gradeRepository.upsertItem(original) }
        openSubject("Задачі")
        compose.onNodeWithContentDescription("Дії оцінювання").performScrollTo().performClick()
        compose.onNodeWithText("Редагувати").performClick()
        fillGrade()
        compose.activityRule.scenario.recreate()
        assertGradeDraft()
        compose.onNodeWithText("Редагувати оцінювання").assertExists()
        assertEquals(original, grades().single { it.id == original.id })
        saveGrade()
        compose.waitUntil(5_000) { grades().any { it.id == original.id && it.title == prefix + "test" } }
        val saved = grades().single { it.title == prefix + "test" }
        assertEquals(original.id, saved.id)
        assertEquals(7.0, saved.earnedPoints, 0.0)
        assertEquals(6.0, saved.minimumPointsForExam!!, 0.0)
    }

    @Test fun returningFromDocumentPickerAndShortBackgroundKeepsUnsavedNote() {
        openGlobal("Нотатка")
        fillNote()
        addLink()
        // Exercise unlock lifetime without configuring an account, credentials, or a phone lock.
        val application = compose.activity.application as TaktApplication
        compose.runOnIdle { application.deviceUnlocked = true }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = compose.activity
        testActivity = activity
        // Exercise the fresh-install folder-picker branch regardless of a preceding suite's grant.
        val preferences = instrumentation.targetContext.getSharedPreferences("takt_documents", 0)
        val originalTree = preferences.getString("tree_uri", null)
        assertTrue(preferences.edit().remove("tree_uri").commit())
        restoreDocumentsTree = {
            assertTrue(preferences.edit().apply {
                if (originalTree == null) remove("tree_uri") else putString("tree_uri", originalTree)
            }.commit())
            runBlocking { application.dataContainer.documentStore.resume() }
        }
        val fileFilter = android.content.IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); addDataType("*/*") }
        val treeFilter = android.content.IntentFilter(Intent.ACTION_OPEN_DOCUMENT_TREE)
        val monitors = listOf(instrumentation.addMonitor(fileFilter, null, false), instrumentation.addMonitor(treeFilter, null, false))
        try {
            compose.onNodeWithText("Додати файл").performScrollTo().performClick()
            // A fresh device opens the tree picker; an already-connected one opens the file picker.
            // Accessibility root package can remain cached/null while the system picker owns focus.
            compose.waitUntil(5_000) { monitors.any { it.hits > 0 } && !activity.hasWindowFocus() }
            returnToApp(activity)
        } finally { monitors.forEach(instrumentation::removeMonitor) }
        waitForNoteForm()
        assertNoteDraft()
        compose.onNodeWithText("Lecture reference").assertExists()
        assertTrue(application.deviceUnlocked)
        assertTrue(instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME))
        compose.waitUntil(5_000) { !activity.hasWindowFocus() }
        instrumentation.targetContext.startActivity(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        compose.waitUntil(5_000) { foreground(activity) }
        waitForNoteForm()
        assertNoteDraft()
        compose.onNodeWithText("Lecture reference").assertExists()
        assertTrue(application.deviceUnlocked)
        assertNoNoteSaved()
    }

    private fun foreground(activity: MainActivity) =
        activity.hasWindowFocus() && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    private fun returnToApp(activity: MainActivity) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        repeat(10) {
            if (foreground(activity)) return
            assertTrue(device.pressBack())
            val until = android.os.SystemClock.elapsedRealtime() + 1_000
            while (!foreground(activity) && android.os.SystemClock.elapsedRealtime() < until) android.os.SystemClock.sleep(50)
        }
        assertTrue("System picker did not return focus to the app", foreground(activity))
    }

    @After fun restoreForegroundAfterExternalUi() {
        // Cleanup also runs on assertion failure; a picker must never poison the next Activity.
        try {
            val activity = testActivity ?: return
            if (activity.isDestroyed || foreground(activity)) return
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            try { returnToApp(activity) }
            finally {
                if (!foreground(activity)) {
                    instrumentation.targetContext.startActivity(Intent(instrumentation.targetContext, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    UiDevice.getInstance(instrumentation).waitForIdle()
                }
            }
        } finally { restoreDocumentsTree?.invoke() }
    }

    private fun openGlobal(label: String) {
        compose.onNodeWithTag("root-add").performClick()
        compose.onNode(hasText(label) and hasClickAction()).performClick()
        compose.onNode(hasText("Fyzika") and hasClickAction()).performScrollTo().performClick()
    }

    private fun openSubject(tab: String) {
        compose.onAllNodes(hasText("Предмети") and hasClickAction()).onLast().performClick()
        compose.onNode(hasText("Fyzika") and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText(tab) and hasClickAction()).performClick()
    }

    private fun openSubjectWork(label: String) {
        compose.onNodeWithText("Додати роботу").performScrollTo().performClick()
        compose.onNodeWithText(label).performClick()
    }

    private fun field(label: String) = compose.onNode(hasText(label) and hasSetTextAction())
    private fun fill(label: String, value: String) = field(label).performScrollTo().performTextReplacement(value)
    private fun selectDate(label: String) {
        compose.onNodeWithText(label).performScrollTo().performTouchInput { click() }
        // Material's calendar exposes full dates through semantics, not day-number Text.
        val accessibleDate = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(TaktI18n.locale))
        compose.onNode(hasText(accessibleDate, substring = true) and hasClickAction()).performClick()
        compose.onNode(hasText("Готово") and hasClickAction() and !hasTestTag("grade-details-done")).performClick()
    }
    private fun assertDate(label: String) = compose.onNodeWithText(label).assertTextContains(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
    private fun fillTask() {
        fill("Назва", prefix + "task")
        fill("Опис", "Keep this description")
        selectDate("Дедлайн")
        compose.onNodeWithText("Потрібно для допуску до екзамену").performScrollTo().performClick()
    }
    private fun assertTaskDraft() {
        field("Назва").assertTextContains(prefix + "task")
        field("Опис").assertTextContains("Keep this description")
        assertDate("Дедлайн")
        compose.onNode(hasText("Потрібно для допуску до екзамену") and
            SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On)).assertExists()
    }
    private fun fillGrade() {
        fill("Назва", prefix + "test")
        compose.onNodeWithTag("grade-maximum").performScrollTo().performTextReplacement("20")
        openGradeDetails()
        gradeTab("WHEN")
        compose.onNodeWithTag("grade-date-mode-DATE").performClick()
        selectDate("Дата тесту")
        compose.onNodeWithTag("grade-duration").performScrollTo().performTextReplacement("45")
        gradeTab("RESULT")
        compose.onNodeWithTag("grade-result-KNOWN").performClick()
        compose.onNodeWithTag("grade-earned").performScrollTo().performTextReplacement("7")
        gradeTab("RULES")
        compose.onNodeWithTag("grade-required").performClick()
        compose.onNodeWithTag("grade-minimum").performScrollTo().performTextReplacement("6")
        // Recreate with Details open, preserving the nested sheet and its selected category.
    }
    private fun assertGradeDraft() {
        compose.onNodeWithTag("grade-details").assertExists()
        compose.onNodeWithTag("grade-details-tab-RULES").assertIsSelected()
        compose.onNodeWithTag("grade-required").assertIsOn()
        compose.onNodeWithTag("grade-minimum").assertTextContains("6")
        gradeTab("WHEN")
        compose.onNodeWithTag("grade-date-mode-DATE").assertIsSelected()
        compose.onNodeWithTag("grade-explicit-date").assertTextContains(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
        compose.onNodeWithTag("grade-duration").assertTextContains("45")
        gradeTab("RESULT")
        compose.onNodeWithTag("grade-result-KNOWN").assertIsSelected()
        compose.onNodeWithTag("grade-earned").assertTextContains("7").assertIsEnabled()
        doneGradeDetails()
        field("Назва").assertTextContains(prefix + "test")
        compose.onNodeWithTag("grade-maximum").assertTextContains("20")
    }
    private fun openGradeDetails() {
        compose.onNodeWithTag("grade-details-trigger").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
    private fun gradeTab(name: String) {
        // Compose's virtual clock does not advance the navigation guard's elapsedRealtime clock.
        // Keep successive semantic clicks outside the same real tap-cooldown window.
        compose.waitUntil(5_000) {
            SystemClock.elapsedRealtime() - lastGradeTabTap >= TaktMotion.NavigationTapCooldownMs
        }
        compose.onNodeWithTag("grade-details-tab-$name").performClick().assertIsSelected()
        lastGradeTabTap = SystemClock.elapsedRealtime()
    }
    private fun doneGradeDetails() {
        compose.onNodeWithTag("grade-details-done").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("grade-details").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
    }
    private fun saveGrade() = compose.onNodeWithTag("grade-save").performScrollTo().performClick()
    private fun fillNote() {
        fill("Заголовок", prefix + "note")
        fill("Нотатка", "Keep the lecture notes")
    }
    private fun waitForNoteForm() {
        compose.waitUntil(5_000) {
            runCatching { field("Заголовок").fetchSemanticsNode() }.isSuccess
        }
    }
    private fun assertNoteDraft() {
        field("Заголовок").assertTextContains(prefix + "note")
        field("Нотатка").assertTextContains("Keep the lecture notes")
    }
    private fun openLink() {
        compose.onNodeWithText("Додати посилання").performScrollTo().performClick()
        field("Адреса посилання").performTextReplacement("https://example.org/lecture")
        field("Назва (необов’язково)").performTextReplacement("Lecture reference")
    }
    private fun addLink() {
        openLink()
        compose.onNodeWithText("Додати", useUnmergedTree = true).performClick()
    }
    private fun save(label: String = "Зберегти") = compose.onNodeWithText(label).performScrollTo().performClick()
    private fun tasks() = runBlocking { data.studyContentRepository.observeTasks(courseId).first() }
    private fun grades() = runBlocking { data.gradeRepository.observeItems(courseId).first() }
    private fun notes() = runBlocking { data.studyContentRepository.observeNotes(courseId).first() }
    private fun assertNoTaskSaved() = assertFalse(tasks().any { it.title == prefix + "task" })
    private fun assertNoGradeSaved() = assertFalse(grades().any { it.title == prefix + "test" })
    private fun assertNoNoteSaved() = assertFalse(notes().any { it.title == prefix + "note" })
}
