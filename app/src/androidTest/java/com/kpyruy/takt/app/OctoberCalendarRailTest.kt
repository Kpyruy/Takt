package com.kpyruy.takt.app

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.calendar.DayTimelineView
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/** Check the rail's actual rendered extent, including the unused lower part of a timed card. */
class OctoberCalendarRailTest {
    @get:Rule val compose = createComposeRule()

    @Test fun timedEventRailReachesBottomBeyondItsText() {
        val day = LocalDate.now().plusDays(1)
        val event = ResolvedScheduleEvent("rail", null, "Full height rail", day,
            LocalTime.of(8, 0), LocalTime.of(9, 30), "302", ScheduleEventStatus.NORMAL)
        compose.setContent {
            TaktTheme(AppSettings(themeMode = AppThemeMode.LIGHT)) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    DayTimelineView(listOf(event), day, day.minusDays(1), CancellationDisplayStyle.STRIKETHROUGH,
                        onEventClick = {}, onEventLongClick = {})
                }
            }
        }
        val card = compose.onNode(hasText("Full height rail") and hasClickAction())
        val image = card.captureToImage()
        val pixels = image.toPixelMap()
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val x = density.roundToInt()
        val inset = (18 * density).roundToInt()
        assertTrue("Timed card must leave room beyond its text", image.height > 80 * density)
        val reference = pixels[x, inset]
        for (y in inset until image.height - inset) {
            assertEquals("Accent rail ends early at row $y", reference, pixels[x, y])
        }
        assertNotEquals("Rail must differ from the unused card body", reference,
            pixels[(10 * density).roundToInt(), image.height - inset])
    }
}
