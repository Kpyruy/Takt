package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.OneOffScheduleEvent
import com.kpyruy.takt.core.model.OneOffScheduleEventType
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.model.ScheduleRecurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object DefaultTimetable {
    val rules = listOf(
        ScheduleRule(
            id = "teve1-table-tennis",
            courseId = "TEVE1_6B",
            title = "Stolný tenis",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(13, 45),
            endTime = LocalTime.of(14, 30),
            recurrence = ScheduleRecurrence.WEEKLY,
            room = "T-068",
        ),
        ScheduleRule(
            id = "zast-monday",
            courseId = "ZAST_6B",
            title = "Základy štatistiky",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(15, 0),
            endTime = LocalTime.of(16, 50),
            recurrence = ScheduleRecurrence.WEEKLY,
        ),
        ScheduleRule(
            id = "tpar-tuesday-lecture",
            courseId = "TPAR_6B",
            title = "Technické prostriedky automatizovaného riadenia",
            dayOfWeek = DayOfWeek.TUESDAY,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(9, 50),
            recurrence = ScheduleRecurrence.WEEKLY,
        ),
        ScheduleRule(
            id = "fyzi-friday-lecture",
            courseId = "FYZI_6B",
            title = "Fyzika · prednáška",
            dayOfWeek = DayOfWeek.FRIDAY,
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(9, 50),
            recurrence = ScheduleRecurrence.WEEKLY,
        ),
        ScheduleRule(
            id = "fyzi-friday-exercise",
            courseId = "FYZI_6B",
            title = "Fyzika · cvičenie",
            dayOfWeek = DayOfWeek.FRIDAY,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 50),
            recurrence = ScheduleRecurrence.WEEKLY,
        ),
    )

    val oneOffEvents = listOf(
        OneOffScheduleEvent(
            id = "divr-block-2026-09-25",
            courseId = "DIVR_6B",
            title = "Digitalizácia a virtuálna realita",
            date = LocalDate.of(2026, 9, 25),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(12, 50),
            room = "T-aula",
            type = OneOffScheduleEventType.BLOCK_ACTION,
        )
    )
}
