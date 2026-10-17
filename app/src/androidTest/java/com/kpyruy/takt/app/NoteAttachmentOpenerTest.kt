package com.kpyruy.takt.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.feature.subjects.NoteAttachmentOpenResult
import com.kpyruy.takt.feature.subjects.NoteAttachmentOpener
import org.junit.Assert.*
import org.junit.Test

/** Captures the launch boundary: no browser, external activity, or network is opened. */
class NoteAttachmentOpenerTest {
    @Test fun driveLinkIsTrimmedAndSchemeNormalizedWithoutChangingResourceOrQuery() {
        val intent = NoteAttachmentOpener.webLinkIntent(" HTTPS://drive.google.com/file/d/ABC/view?usp=sharing ")
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://drive.google.com/file/d/ABC/view?usp=sharing", intent.dataString)
        assertNull(intent.type)
        assertNull(intent.`package`)
        assertNull(intent.component)
        assertEquals(0, intent.flags)
    }

    @Test fun invalidOrNonWebLinksCannotReachAnActivity() {
        listOf("https://drive.google.com/a b", "https://user:pass@drive.google.com", "file:///tmp/note", "javascript:alert(1)")
            .forEach { url -> assertTrue(runCatching { NoteAttachmentOpener.webLinkIntent(url) }.isFailure) }
    }

    @Test fun activityIsRecoveredThroughContextWrappersWithoutNewTask() {
        lateinit var activity: RecordingActivity
        InstrumentationRegistry.getInstrumentation().runOnMainSync { activity = RecordingActivity() }
        val wrapped = object : ContextWrapper(ContextWrapper(activity)) {
            override fun startActivity(intent: Intent) = error("The wrapper must not be the launch context")
        }
        val intent = NoteAttachmentOpener.webLinkIntent("https://drive.google.com")
        assertEquals(NoteAttachmentOpenResult.Opened, NoteAttachmentOpener.launch(wrapped, intent))
        assertEquals("https://drive.google.com", activity.launched?.dataString)
        assertEquals(0, activity.launched!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test fun configurationContextWithoutActivityGetsNewTaskAndDoesNotMutateOriginalIntent() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val context = RecordingContext(target.createConfigurationContext(target.resources.configuration))
        val intent = NoteAttachmentOpener.webLinkIntent("https://drive.google.com")
        assertEquals(NoteAttachmentOpenResult.Opened, NoteAttachmentOpener.launch(context, intent))
        assertTrue(context.launched!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertEquals(0, intent.flags)
    }

    @Test fun fileLaunchRetainsMimeTypeAndReadGrant() {
        val uri = Uri.parse("content://com.example.documents/document/lecture.pdf")
        val intent = NoteAttachmentOpener.fileIntent(uri, "application/pdf")
        val context = RecordingContext(InstrumentationRegistry.getInstrumentation().targetContext)
        assertEquals(NoteAttachmentOpenResult.Opened, NoteAttachmentOpener.launch(context, intent))
        val launched = context.launched!!
        assertEquals(Intent.ACTION_VIEW, launched.action)
        assertEquals(uri, launched.data)
        assertEquals("application/pdf", launched.type)
        assertTrue(launched.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertTrue(launched.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun onlyActivityNotFoundIsReportedAsNoApplication() {
        val context = RecordingContext(InstrumentationRegistry.getInstrumentation().targetContext)
        val intent = NoteAttachmentOpener.webLinkIntent("https://drive.google.com")
        context.failure = ActivityNotFoundException("Fixture has no handler")
        assertEquals(NoteAttachmentOpenResult.NoApplication, NoteAttachmentOpener.launch(context, intent))
        for (failure in listOf(SecurityException("Denied"), android.util.AndroidRuntimeException("Context launch failed"))) {
            context.failure = failure
            val result = NoteAttachmentOpener.launch(context, intent)
            assertTrue(result is NoteAttachmentOpenResult.Failed)
            assertSame(failure, (result as NoteAttachmentOpenResult.Failed).cause)
        }
    }

    private class RecordingActivity : Activity() {
        var launched: Intent? = null
        override fun startActivity(intent: Intent) { launched = intent }
    }

    private class RecordingContext(base: Context) : ContextWrapper(base) {
        var launched: Intent? = null
        var failure: Exception? = null
        override fun startActivity(intent: Intent) {
            failure?.let { throw it }
            launched = intent
        }
    }
}
