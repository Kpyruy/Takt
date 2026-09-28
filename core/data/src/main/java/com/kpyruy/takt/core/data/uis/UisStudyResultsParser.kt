package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.GradeLetter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.jsoup.Jsoup

internal data class UisStudyResult(val grade: GradeLetter?, val fulfilledOn: LocalDate?)

/** The detailed e-record has separate entered and fulfilled dates; only the latter is the completion date. */
internal object UisStudyResultsParser {
    private val codePattern = Regex("[A-Z0-9_.-]{2,32}")
    private val gradePattern = Regex("\\(([A-E]|FX)\\)", RegexOption.IGNORE_CASE)
    private val fulfilledPattern = Regex("fulfilled\\s*:\\s*(\\d{1,2}\\.\\d{1,2}\\.\\d{4})", RegexOption.IGNORE_CASE)
    private val dateFormat = DateTimeFormatter.ofPattern("d.M.uuuu", Locale.ROOT)

    fun parse(html: String): Map<String, UisStudyResult> {
        val doc = Jsoup.parse(html)
        val results = linkedMapOf<String, UisStudyResult>()
        var sawResultTable = false
        doc.select("table").forEach { table ->
            val headings = table.selectFirst("tr:has(th)")?.select("th")?.map { it.text().trim().lowercase(Locale.ROOT) }
                ?: return@forEach
            val codeIndex = headings.indexOf("code")
            val resultIndex = headings.indexOf("result")
            val enteredIndex = headings.indexOf("entered")
            if (codeIndex < 0 || resultIndex < 0 || enteredIndex < 0) return@forEach
            sawResultTable = true
            table.select("tr").forEach { row ->
                val cells = row.select("td")
                if (cells.size <= maxOf(codeIndex, resultIndex, enteredIndex)) return@forEach
                val code = cells[codeIndex].text().trim().uppercase(Locale.ROOT)
                if (!codePattern.matches(code)) return@forEach
                val rawGrade = gradePattern.find(cells[resultIndex].text())?.groupValues?.get(1)?.uppercase(Locale.ROOT)
                val grade = rawGrade?.let { runCatching { GradeLetter.valueOf(it) }.getOrNull() }
                val rawDate = fulfilledPattern.find(cells[enteredIndex].text())?.groupValues?.get(1)
                val date = rawDate?.let { runCatching { LocalDate.parse(it, dateFormat) }.getOrNull() }
                if (grade != null || date != null) results[code] = UisStudyResult(grade, date)
            }
        }
        check(sawResultTable) { "UIS study results table missing" }
        return results
    }
}
