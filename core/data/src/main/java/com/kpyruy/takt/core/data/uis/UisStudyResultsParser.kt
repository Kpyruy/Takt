package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.GradeLetter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import org.jsoup.Jsoup

internal enum class UisResultFieldState { ABSENT, UNRECOGNIZED, CONFIRMED }

/** Null values carry no clearing intent. A missing map entry means UIS did not return the course row. */
internal data class UisStudyResult(
    val grade: GradeLetter?,
    val fulfilledOn: LocalDate?,
    val gradeState: UisResultFieldState = if (grade == null) UisResultFieldState.ABSENT else UisResultFieldState.CONFIRMED,
    val fulfilledOnState: UisResultFieldState = if (fulfilledOn == null) UisResultFieldState.ABSENT else UisResultFieldState.CONFIRMED,
)

/** The detailed e-record has separate entered and fulfilled dates; only the latter is the completion date. */
internal object UisStudyResultsParser {
    private val codePattern = Regex("[A-Z0-9_.-]{2,32}")
    private val gradePattern = Regex("\\(([A-E]|FX)\\)", RegexOption.IGNORE_CASE)
    private val fulfilledPattern = Regex("fulfilled\\s*:\\s*([^)]*)", RegexOption.IGNORE_CASE)
    private val datePattern = Regex("\\d{1,2}\\.\\d{1,2}\\.\\d{4}")
    private val dateFormat = DateTimeFormatter.ofPattern("d.M.uuuu", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

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
                val code = cells.getOrNull(codeIndex)?.text()?.trim()?.uppercase(Locale.ROOT) ?: return@forEach
                if (!codePattern.matches(code)) return@forEach
                val gradeText = cells.getOrNull(resultIndex)?.text().orEmpty()
                val rawGrade = gradePattern.find(gradeText)?.groupValues?.get(1)?.uppercase(Locale.ROOT)
                val grade = rawGrade?.let { runCatching { GradeLetter.valueOf(it) }.getOrNull() }
                val enteredText = cells.getOrNull(enteredIndex)?.text().orEmpty()
                val rawDate = fulfilledPattern.find(enteredText)?.groupValues?.get(1)?.trim()
                val date = rawDate?.takeIf(datePattern::matches)?.let {
                    runCatching { LocalDate.parse(it, dateFormat) }.getOrNull()
                }
                val previous = results[code]
                // Several result tables can mention the same code. Only confirmed values replace earlier ones.
                results[code] = UisStudyResult(
                    grade = grade ?: previous?.grade,
                    fulfilledOn = date ?: previous?.fulfilledOn,
                    gradeState = if (grade == null && previous?.grade != null) previous.gradeState
                        else fieldState(gradeText, grade != null),
                    fulfilledOnState = if (date == null && previous?.fulfilledOn != null) previous.fulfilledOnState
                        else fieldState(rawDate, date != null),
                )
            }
        }
        check(sawResultTable) { "UIS study results table missing" }
        return results
    }

    private fun fieldState(raw: String?, confirmed: Boolean) = when {
        confirmed -> UisResultFieldState.CONFIRMED
        raw.isNullOrBlank() -> UisResultFieldState.ABSENT
        else -> UisResultFieldState.UNRECOGNIZED
    }
}
