package com.kpyruy.takt.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteAttachmentTest {
    @Test fun webLinkUsesHostAsDefaultName() {
        val link = NoteAttachment.webLink(" https://example.com/path ")
        assertEquals("example.com", link.name)
        assertEquals("https://example.com/path", link.uri)
        assertTrue(link.isWebLink)
    }

    @Test fun webLinkRejectsUnsupportedSchemesAndCredentials() {
        listOf("javascript:alert(1)", "file:///tmp/a", "https://user:pass@example.com", "https://example.com/a b")
            .forEach { value ->
                assertTrue(runCatching { NoteAttachment.webLink(value) }.isFailure)
            }
    }
}
