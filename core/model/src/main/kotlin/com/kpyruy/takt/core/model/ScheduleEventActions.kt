package com.kpyruy.takt.core.model

data class ScheduleEventActions(
    val canCancelOccurrence: Boolean,
    val canMoveOccurrence: Boolean,
    val canRestoreOccurrence: Boolean,
    val canEditRecurringRule: Boolean,
    val canDeleteRecurringRule: Boolean,
    val canEditOneOff: Boolean,
    val canDeleteOneOff: Boolean,
) {
    companion object {
        fun forEvent(
            event: ResolvedScheduleEvent,
            isRecurringRule: Boolean,
        ): ScheduleEventActions = when (event.status) {
            ScheduleEventStatus.NORMAL -> ScheduleEventActions(
                canCancelOccurrence = isRecurringRule,
                canMoveOccurrence = isRecurringRule,
                canRestoreOccurrence = false,
                canEditRecurringRule = isRecurringRule,
                canDeleteRecurringRule = isRecurringRule,
                canEditOneOff = false,
                canDeleteOneOff = false,
            )

            ScheduleEventStatus.CANCELLED -> ScheduleEventActions(
                canCancelOccurrence = false,
                canMoveOccurrence = false,
                canRestoreOccurrence = true,
                canEditRecurringRule = false,
                canDeleteRecurringRule = false,
                canEditOneOff = false,
                canDeleteOneOff = false,
            )

            ScheduleEventStatus.MOVED -> ScheduleEventActions(
                canCancelOccurrence = false,
                canMoveOccurrence = true,
                canRestoreOccurrence = true,
                canEditRecurringRule = false,
                canDeleteRecurringRule = false,
                canEditOneOff = false,
                canDeleteOneOff = false,
            )

            ScheduleEventStatus.ONE_OFF -> ScheduleEventActions(
                canCancelOccurrence = false,
                canMoveOccurrence = false,
                canRestoreOccurrence = false,
                canEditRecurringRule = false,
                canDeleteRecurringRule = false,
                canEditOneOff = true,
                canDeleteOneOff = true,
            )
        }
    }
}
