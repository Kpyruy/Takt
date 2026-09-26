package com.kpyruy.takt.app

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.NoteAttachment
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in integration check after granting Documents on a disposable emulator. */
class DocumentMaterialReferenceTest {
    @Test fun copiedFileOpensThroughEncodedReference() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("documentReview") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val data = (context.applicationContext as TaktApplication).dataContainer
        val store = data.documentStore
        assumeTrue(store.isConnected)
        val bytes = "Takt attachment reference check".toByteArray()
        val source = File(context.cacheDir, "Takt lecture #1 + 50%?.pdf").apply { writeBytes(bytes) }

        val reference = store.copyMaterial("FYZI_6B", Uri.fromFile(source))
        val copied = store.resolve(reference)
        assertNotNull(copied)
        assertArrayEquals(bytes, context.contentResolver.openInputStream(copied!!)!!.use { it.readBytes() })

        data.studyContentRepository.upsertNote(CourseNote("document-reference-review", "FYZI_6B",
            "Attachment review", "", System.currentTimeMillis(),
            listOf(NoteAttachment(source.name, reference, "application/pdf"))))
        val stored = data.studyContentRepository.observeNotes("FYZI_6B").first()
            .first { it.id == "document-reference-review" }.attachments.single()
        assertEquals(reference, stored.uri)
        assertNotNull(store.resolve(stored.uri))

    }
}
