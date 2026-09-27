package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.SemesterPeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jsoup.Jsoup

/** The examination interval spans every exam row, including holidays between them. */
internal object UisAcademicCalendarParser {
    private val date = Regex("""\b\d{1,2}/\d{1,2}/\d{4}\b""")
    private val format = DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US)
    private data class Row(val start: LocalDate, val end: LocalDate, val name: String)

    fun parse(html: String, today: LocalDate): SemesterPeriod {
        val rows = Jsoup.parse(html).select("tr").mapNotNull { tr ->
            val cells = tr.select("td")
            if (cells.size < 2) return@mapNotNull null
            val dates = date.findAll(cells[cells.size - 2].text()).map { LocalDate.parse(it.value, format) }.toList()
            if (dates.size != 2) return@mapNotNull null
            Row(dates[0], dates[1], cells[cells.size - 1].text().trim().lowercase(Locale.ROOT))
        }
        val teaching = rows.filter { it.name.startsWith("teaching part") || it.name.startsWith("výučba") }
            .sortedBy { it.start }
        val periods = teaching.mapIndexed { index, term ->
            val nextStart = teaching.getOrNull(index + 1)?.start
            val exams = rows.filter {
                (it.name.startsWith("exam period") || it.name.startsWith("skúškové obdobie")) &&
                    it.start > term.end && (nextStart == null || it.start < nextStart)
            }
            SemesterPeriod(term.start, term.end,
                exams.minOfOrNull { it.start }, exams.maxOfOrNull { it.end })
        }
        return periods.firstOrNull { today >= it.studyStart!! && today <= (it.examEnd ?: it.studyEnd!!) }
            ?: periods.firstOrNull { it.studyStart!! > today }
            ?: periods.lastOrNull()
            ?: error("UIS teaching period missing")
    }
}
