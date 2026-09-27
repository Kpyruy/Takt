package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.LessonType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.jsoup.Jsoup
import java.util.Locale

internal data class UisTimetableItem(
    val key: String,
    val subjectId: String?,
    val title: String,
    val day: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime,
    val room: String?,
    val lessonType: LessonType,
    val date: LocalDate? = null,
)

/** Parses UIS's list view; block-class footnotes provide the actual subject and date. */
internal object UisTimetableParser {
    private val datePattern = Regex("""\b(\d{1,2})\.\s*(\d{1,2})\.\s*(\d{4})\b""")
    private val idPattern = Regex("""(?:[?;&]|^)predmet=(\d+)""")
    private val dayNames = mapOf("mon" to DayOfWeek.MONDAY, "tue" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY, "thu" to DayOfWeek.THURSDAY,
        "fri" to DayOfWeek.FRIDAY, "sat" to DayOfWeek.SATURDAY, "sun" to DayOfWeek.SUNDAY)

    fun parse(html: String): List<UisTimetableItem> {
        val doc = Jsoup.parse(html)
        val table = doc.selectFirst("table#tmtab_1") ?: error("UIS timetable list missing")
        val notes = doc.select("table:not(#tmtab_1) tr").mapNotNull { tr ->
            val cells = tr.select("td")
            if (cells.size != 2) null else {
                val number = cells[0].text().trim().removeSurrounding("(", ")").toIntOrNull()
                number?.let { it to cells[1] }
            }
        }.toMap()
        val occurrence = mutableMapOf<String, Int>()
        return table.select("tbody tr").mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size != 9) return@mapNotNull null
            val day = dayNames[cells[0].text().trim().take(3).lowercase(Locale.ROOT)] ?: return@mapNotNull null
            val start = parseTime(cells[1].text()) ?: return@mapNotNull null
            val end = parseTime(cells[2].text()) ?: return@mapNotNull null
            if (end <= start) return@mapNotNull null
            val block = cells[3].text().startsWith("Block class", true)
            val noteNumber = cells[3].selectFirst("sup")?.text()?.trim()
                ?.removeSurrounding("(", ")")?.toIntOrNull()
            val note = if (block) notes[noteNumber] else null
            val courseLink = (note ?: cells[3]).selectFirst("a[href*=predmet=]")
            val subjectId = courseLink?.let { idPattern.find(it.attr("href"))?.groupValues?.get(1) }
            val title = courseLink?.text()?.trim() ?: cells[3].ownText().trim().ifBlank { cells[3].text().trim() }
            val date = if (block) datePattern.find(note?.text().orEmpty())?.let { match ->
                LocalDate.of(match.groupValues[3].toInt(), match.groupValues[2].toInt(), match.groupValues[1].toInt())
            } else null
            val type = when {
                block || cells[4].text().contains("Lecture", true) -> LessonType.LECTURE
                cells[4].text().contains("Seminar", true) -> LessonType.SEMINAR
                cells[4].text().contains("Lab", true) -> LessonType.LAB
                else -> LessonType.UNSPECIFIED
            }
            val room = cells[5].selectFirst("a")?.text()?.substringBefore(" (")?.trim()
                ?: cells[5].text().trim().ifBlank { null }
            val identity = listOf(day, start, end, subjectId, title, type, room, date,
                cells[6].text().trim(), cells[7].text().trim()).joinToString("|")
            val sequence = occurrence.merge(identity, 1, Int::plus) ?: 1
            UisTimetableItem("$identity|$sequence", subjectId, title, day, start, end, room, type, date)
        }
    }

    private fun parseTime(raw: String): LocalTime? {
        val parts = raw.trim().split('.')
        return if (parts.size == 2) runCatching { LocalTime.of(parts[0].toInt(), parts[1].toInt()) }.getOrNull()
        else null
    }
}
