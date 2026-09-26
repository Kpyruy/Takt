package com.kpyruy.takt.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TaktMaterialReferenceTest {
    @Test fun reservedCharactersAndUnicodeSurviveRoundTrip() {
        val fileName = "a1b2c3d4-Лекція #1 + 50%?.pdf"
        val reference = TaktMaterialReference.encode("DIVR_6B", fileName)

        assertFalse(reference.contains("#"))
        assertFalse(reference.contains("?"))
        assertEquals(TaktMaterialReference.Path("DIVR_6B", fileName), TaktMaterialReference.decode(reference))
    }

    @Test fun onlyEncodedMaterialReferencesAreAccepted() {
        assertNull(TaktMaterialReference.decode("takt:///DIVR_6B/a1b2c3d4-Лекція #1?.pdf"))
        assertNull(TaktMaterialReference.decode("takt:///DIVR_6B"))
        assertNull(TaktMaterialReference.decode("takt://v2/DIVR_6B/file%2Fother.pdf"))
    }
}
