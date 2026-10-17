package com.kpyruy.takt.core.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaktChoiceMenuPositionTest {
    private val position = TaktChoiceMenuPositionProvider(margin = 8, gap = 4)
    private val window = IntSize(320, 640)

    @Test fun opensBelowTheAnchorWhenThereIsSpace() {
        assertEquals(IntOffset(20, 92), position.calculatePosition(
            IntRect(20, 40, 68, 88), window, LayoutDirection.Ltr, IntSize(196, 160)))
    }

    @Test fun rightEdgeActionMenuKeepsItsTrailingEdgeAndOpensAboveNearBottom() {
        assertEquals(IntOffset(116, 346), position.calculatePosition(
            IntRect(264, 510, 312, 558), window, LayoutDirection.Ltr, IntSize(196, 160)))
    }

    @Test fun rtlUsesTheAnchorTrailingEdgeFirst() {
        assertEquals(IntOffset(76, 92), position.calculatePosition(
            IntRect(200, 40, 272, 88), window, LayoutDirection.Rtl, IntSize(196, 160)))
    }

    @Test fun tallMenuAndPartlyScrolledAnchorStayInsideWindow() {
        assertEquals(IntOffset(8, 8), position.calculatePosition(
            IntRect(-12, -40, 36, 8), window, LayoutDirection.Ltr, IntSize(304, 624)))
    }

    @Test fun everyEdgeAndDirectionKeepsAMeasuredMenuWithinBounds() {
        for (direction in LayoutDirection.entries) {
            for (x in listOf(-40, 0, 156, 300, 350)) {
                for (y in listOf(-40, 0, 320, 600, 700)) {
                    val size = IntSize(240, 480)
                    val result = position.calculatePosition(IntRect(x, y, x + 48, y + 48), window, direction, size)
                    assertTrue(result.x >= 8 && result.x + size.width <= window.width - 8)
                    assertTrue(result.y >= 8 && result.y + size.height <= window.height - 8)
                }
            }
        }
    }

    @Test fun tinyWindowDoesNotProduceAnInvalidClampRange() {
        assertEquals(IntOffset.Zero, position.calculatePosition(
            IntRect(0, 0, 48, 48), IntSize(6, 6), LayoutDirection.Ltr, IntSize(6, 6)))
    }
}
