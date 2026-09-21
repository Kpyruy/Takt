package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime

data class OneOffEventDraft(
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val blockAction: Boolean,
) {
    fun toEvent(
        id: String,
        courseId: String?,
    ): Result<OneOffScheduleEvent> = runCatching {
        val parsedDate = LocalDate.parse(date.trim())
        val start = LocalTime.parse(startTime.trim())
        val end = LocalTime.parse(endTime.trim())

        require(title.isNotBlank()) { "Title is required" }
        require(end > start) { "End time must be after start time" }

        OneOffScheduleEvent(
            id = id,
            courseId = courseId,
            title = title.trim(),
            date = parsedDate,
            startTime = start,
            endTime = end,
            room = room.trim().ifBlank { null },
            type = if (blockAction) {
                OneOffScheduleEventType.BLOCK_ACTION
            } else {
                OneOffScheduleEventType.EXTRA
            },
        )
    }
}
