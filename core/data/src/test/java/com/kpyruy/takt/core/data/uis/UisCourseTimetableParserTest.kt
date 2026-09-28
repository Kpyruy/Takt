package com.kpyruy.takt.core.data.uis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UisCourseTimetableParserTest {
    private val directory = """
        <form><select name="predmet">
          <option value="0">-- all courses --</option>
          <option value="440870">TPAR_6B - Technical equipment</option>
          <option value="440871">TPAR_6BX - Another course</option>
        </select></form>
    """

    @Test fun findsOnlyTheExactShortCode() {
        assertEquals("440870", UisCourseTimetableParser.subjectId(directory, "tpar_6b"))
        assertNull(UisCourseTimetableParser.subjectId(directory, "TPAR_6"))
    }
}
