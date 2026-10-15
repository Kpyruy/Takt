package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.components.TaktTourActions
import com.kpyruy.takt.core.ui.components.TaktTourTargets
import kotlinx.coroutines.delay
import java.time.LocalDate

internal class TourSceneSession {
    val actions = TaktTourActions()
    val targets = TaktTourTargets()
}

internal val GuidedTourStep.hasDemonstration: Boolean
    get() = this == GuidedTourStep.CALENDAR_LAYOUT || this == GuidedTourStep.CALENDAR_TODAY

/** Finite, cancellable actions on the real example controls. No live account or repository writes. */
internal suspend fun runTourDemonstration(
    step: GuidedTourStep,
    scene: TourSceneSession,
    visit: Int,
    onPhase: (String) -> Unit,
) {
    onPhase("PREPARING")
    delay(400) // Let the incoming page and its control bindings settle.
    when (step) {
        GuidedTourStep.CALENDAR_LAYOUT -> {
            scene.actions.perform("calendar-week-timetable")
            onPhase("TIMETABLE")
            delay(350)
            scene.targets.showTap("calendar-week-layout")
            delay(120)
            scene.actions.perform("calendar-week-menu")
            onPhase("MENU_OPEN")
            delay(1_200)
            scene.targets.showTap("calendar-week-list")
            delay(180)
            scene.actions.perform("calendar-week-list")
            onPhase("LIST_SELECTED")
            delay(500)
        }
        GuidedTourStep.CALENDAR_TODAY -> {
            // Revisiting or replaying this step always starts with a different non-today date.
            val delta = (6 + visit % 11).toLong() * if (visit % 2 == 0) 1 else -1
            scene.actions.perform("calendar-select-date", LocalDate.now().plusDays(delta).toEpochDay())
            onPhase("AWAY_FROM_TODAY")
            delay(850)
            scene.targets.showTap("calendar-mode-month")
            scene.actions.perform("calendar-tap-month")
            onPhase("FIRST_TAP")
            delay(150)
            scene.targets.showTap("calendar-mode-month")
            scene.actions.perform("calendar-tap-month")
            onPhase("RETURNED_TODAY")
            delay(650)
        }
        else -> Unit
    }
    onPhase("COMPLETE")
}
