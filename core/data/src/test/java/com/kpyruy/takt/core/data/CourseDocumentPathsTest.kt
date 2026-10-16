package com.kpyruy.takt.core.data

import org.junit.Assert.*
import org.junit.Test

class CourseDocumentPathsTest {
    @Test fun courseCodesStayInOneFolderAndDocumentNamesCannotTraversePaths() {
        assertEquals("FYZI_6B", CourseDocumentPaths.courseFolder("FYZI_6B"))
        assertEquals("Download/Takt/FYZI_6B/", CourseDocumentPaths.downloadsPath("FYZI_6B"))
        val file = CourseDocumentPaths.fileName("../../lecture\r\n.pdf")
        assertFalse(file.contains('/')); assertFalse(file.contains('\\'))
        assertFalse(file.startsWith('.')); assertFalse(file.contains('\r')); assertFalse(file.contains('\n'))
        assertEquals("prednáška.pdf", CourseDocumentPaths.fileName("prednáška.pdf"))
    }
    @Test fun blankAndOversizedNamesHaveUsableBoundedFallbacks() {
        assertEquals("document", CourseDocumentPaths.fileName("..."))
        assertTrue(CourseDocumentPaths.fileName("a".repeat(500)+".pdf").endsWith(".pdf"))
        assertTrue(CourseDocumentPaths.fileName("a".repeat(500)+".pdf").length <= 150)
        assertEquals("course", CourseDocumentPaths.courseFolder("..."))
    }
}
