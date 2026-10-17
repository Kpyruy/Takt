package com.kpyruy.takt.core.ui.theme

import org.junit.Assert.*
import org.junit.Test

class TaktHsvColorTest {
    @Test fun fullSpectrumIncludesEveryPrimaryAndSecondaryHue() {
        listOf("#FF0000", "#FFFF00", "#00FF00", "#00FFFF", "#0000FF", "#FF00FF", "#FF0000")
            .forEachIndexed { index, hex -> assertEquals(hex, TaktHsvColor(index * 60f, 1f, 1f).toHex()) }
    }

    @Test fun brightnessAndSaturationCanReachBlackWhiteAndNeutralGray() {
        assertEquals("#000000", TaktHsvColor(170f, .8f, 0f).toHex())
        assertEquals("#FFFFFF", TaktHsvColor(170f, 0f, 1f).toHex())
        assertEquals("#808080", TaktHsvColor(170f, 0f, .5f).toHex())
        assertEquals("#800000", TaktHsvColor(0f, 1f, .5f).toHex())
    }

    @Test fun hexEditingRoundTripsWithoutDroppingAChannel() {
        listOf("#000000", "#FFFFFF", "#123456", "#B52D67", "#147D64", "#7048C8", "#808080")
            .forEach { hex -> assertEquals(hex, TaktHsvColor.fromHex(hex)!!.toHex()) }
        assertEquals("#0099AA", TaktHsvColor.fromHex("  0099aa ")!!.toHex())
        assertNull(TaktHsvColor.fromHex("#oops"))
    }
}
