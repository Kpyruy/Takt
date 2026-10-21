package com.kpyruy.takt.app

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.NoteAttachment
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.subjects.CourseNoteCard
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CourseNoteReaderTest {
    @get:Rule val compose = createComposeRule()
    private val note = CourseNote(
        "reader-fixture", "UIS-linked-course-id", "A very long lecture title that must fit on one preview line",
        (1..80).joinToString("\n") { "Lecture paragraph $it: definitions and examples." }, 1L,
        listOf(NoteAttachment.webLink("https://drive.google.com/file/d/ABC/view", "Drive lecture")),
    )
    private var edits = 0
    private var deletes = 0
    private val previousLanguage = TaktI18n.language

    @After fun restoreLanguage() = TaktI18n.use(previousLanguage)

    private fun fixture() {
        val data = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TaktApplication).dataContainer
        TaktI18n.use(AppLanguage.ENGLISH)
        compose.setContent {
            TaktTheme {
                Box(Modifier.fillMaxSize()) {
                    SectionCard(Modifier.width(320.dp), contentPadding = PaddingValues(0.dp)) {
                        CourseNoteCard(note, data.documentStore, onEdit = { edits++ }, onDelete = { deletes++ })
                    }
                }
            }
        }
    }

    @Test fun previewEllipsizesTitleAndBodyAndAlignsHeader() {
        fixture()
        val title = textLayout("note-preview-title-${note.id}")
        assertEquals(1, title.lineCount)
        assertTrue(title.isLineEllipsized(0))
        val body = textLayout("note-preview-content-${note.id}")
        assertTrue(body.lineCount <= 3)
        assertTrue(body.isLineEllipsized(body.lineCount - 1))
        val iconBounds = compose.onNodeWithTag("note-preview-icon-${note.id}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val titleBounds = compose.onNodeWithTag("note-preview-title-${note.id}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val dateBounds = compose.onNodeWithTag("note-preview-date-${note.id}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(iconBounds.center.y, titleBounds.center.y, 1f)
        assertEquals(titleBounds.center.y, dateBounds.center.y, 1f)
    }

    @Test fun wholeCardOpensReaderWithFullTextAndAttachmentsAndCloseReturnsToPreview() {
        fixture()
        // Edge of the card, outside the title and content, must also open the reader.
        compose.onNodeWithTag("note-preview-${note.id}").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(8f, center.y))
        }
        compose.onNodeWithTag("note-reader").assertIsDisplayed()
        compose.onNodeWithTag("note-reader-title", useUnmergedTree = true).assertTextEquals(note.title)
        compose.onNodeWithTag("note-reader-content", useUnmergedTree = true).assertTextEquals(note.content)
        val content = textLayout("note-reader-content")
        assertFalse((0 until content.lineCount).any { content.isLineEllipsized(it) })
        compose.onNodeWithTag("note-reader-attachment-0").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("note-reader-close").performClick()
        compose.onNodeWithTag("note-reader").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, edits); assertEquals(0, deletes) }
    }

    @Test fun nativeBackClosesReaderAndOverflowStillEditsInsteadOfOpeningReader() {
        fixture()
        compose.onNodeWithTag("note-preview-${note.id}").performClick()
        compose.onNodeWithTag("note-reader").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("note-reader").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("note-reader").assertDoesNotExist()
        compose.onNodeWithContentDescription(t("Note actions")).performClick()
        compose.onNodeWithText(t("Edit")).performClick()
        compose.onNodeWithTag("note-reader").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, edits); assertEquals(0, deletes) }
    }

    @Test fun readerOverflowEditsThroughExistingCallbackAndDismissesReader() {
        fixture()
        compose.onNodeWithTag("note-preview-${note.id}").performClick()
        compose.onNodeWithTag("note-reader-actions").performClick()
        compose.onNodeWithText(t("Edit")).performClick()
        compose.onNodeWithTag("note-reader").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, edits); assertEquals(0, deletes) }
    }

    private fun textLayout(tag: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single()
    }
}
