package com.kpyruy.takt.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.CourseNote
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt-in visual fixture for a disposable emulator. */
class NotesLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun notesAreSeparateCardsWithoutRepeatedHeading() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("notesReview") == "true")
        val data = (compose.activity.application as TaktApplication).dataContainer
        runBlocking {
            data.seedIfNeeded()
            data.studyContentRepository.upsertNote(CourseNote("review-note-one", "FYZI_6B",
                "Формули до семінару", "Закон Ома і приклади до наступної пари.", 1L))
            data.studyContentRepository.upsertNote(CourseNote("review-note-two", "FYZI_6B",
                "Матеріали лекції", "Конспект і файл із презентацією.", 2L))
        }
        compose.onAllNodes(hasText("Предмети") and hasClickAction()).onLast().performClick()
        compose.onNode(hasText("Fyzika") and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText("Нотатки") and hasClickAction()).performClick()
        compose.onAllNodesWithText("Нотатки").assertCountEquals(1)
        compose.onNodeWithText("Формули до семінару").assertExists()
        compose.onNodeWithText("Матеріали лекції").assertExists()

        compose.waitForIdle()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        InstrumentationRegistry.getInstrumentation().targetContext.openFileOutput("notes-cards-review.png", 0)
            .use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
