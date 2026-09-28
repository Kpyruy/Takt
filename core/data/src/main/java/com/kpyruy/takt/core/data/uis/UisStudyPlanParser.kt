package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Locale

internal data class UisStudyPlan(
    val studyId: String,
    val periodId: String,
    val earnedCredits: Int?,
    val requiredCredits: Int?,
    val courses: List<UisCourse>,
    val currentSemester: Int?,
)

internal data class UisCourse(
    val subjectId: String,
    val code: String,
    val title: String,
    val credits: Int,
    val semester: Int,
    val status: CourseStatus,
    val requirementType: CourseRequirementType,
    val gradingType: CourseGradingType,
    val syllabusUrl: String,
    val titleSk: String? = null,
)

/** Reads only course rows from the selected study; UIS's other tables are not course data. */
internal object UisStudyPlanParser {
    private val semesterPattern = Regex("""(\d{1,2})(?:st|nd|rd|th|\.|\s)\s*semester""", RegexOption.IGNORE_CASE)
    private val subjectPattern = Regex("""(?:[?;&]|^)predmet=(\d+)""")
    private val studyTermPattern = Regex("""\[\s*term\s+(\d+)\s*,\s*year\s+(\d+)\s*]""",
        RegexOption.IGNORE_CASE)

    fun slovakTitles(html: String): Map<String, String> {
        val table = Jsoup.parse(html).selectFirst("table#tmtab_1") ?: return emptyMap()
        return table.select("tr").mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size != 6 || cells[0].selectFirst("a[href*=predmet=]") == null) return@mapNotNull null
            val code = cells[0].text().trim().uppercase(Locale.ROOT)
            val title = cells[1].text().trim()
            if (code.isEmpty() || title.isEmpty()) null else code to title
        }.toMap()
    }

    fun parse(html: String): UisStudyPlan {
        val doc = Jsoup.parse(html, "https://is.stuba.sk/auth/studijni/studijni_povinnosti.pl")
        val table = doc.selectFirst("table#tmtab_1") ?: error("UIS study plan table missing")
        val form = doc.selectFirst("form[name=formular]") ?: error("UIS study selector missing")
        val selectedStudy = form.selectFirst("select[name=studium] option[selected]")
            ?: form.selectFirst("select[name=studium] option")
            ?: error("UIS study ID missing")
        val studyId = selectedStudy.attr("value")
        val visiblePlanText = doc.body().clone().apply { select("select").remove() }.text()
        val currentSemester = (studyTermPattern.find(selectedStudy.text())
            ?: studyTermPattern.find(visiblePlanText))?.let { match ->
            val term = match.groupValues[1].toIntOrNull()
            val year = match.groupValues[2].toIntOrNull()
            if (term == null || year == null || term <= 0 || year !in 1..15) null
            else (year - 1) * 2 + (if (term % 2 == 1) 1 else 2)
        }
        val periodId = form.selectFirst("input[name=obdobi]")?.attr("value")
            ?: error("UIS period ID missing")
        require(studyId.matches(Regex("\\d+")) && periodId.matches(Regex("\\d+")))

        val creditRow = doc.select("table tr").firstOrNull {
            it.selectFirst("td")?.text()?.trim()?.lowercase(Locale.ROOT)?.let { label ->
                label.startsWith("credits:") || label.startsWith("kredity:")
            } == true
        }
        val creditNumbers = creditRow?.select("td")?.drop(1)?.joinToString(" ") { it.text() }
            ?.let { Regex("\\d+").findAll(it).take(2).map { match -> match.value.toInt() }.toList() }
            .orEmpty()

        var semester = 0
        var electiveSection = false
        val byCode = linkedMapOf<String, UisCourse>()
        table.select("tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size == 1 && cells[0].hasAttr("colspan")) {
                semesterPattern.find(cells[0].text())?.let {
                    semester = it.groupValues[1].toInt()
                    electiveSection = false
                }
                if (cells[0].text().contains("elective courses for the whole", ignoreCase = true) ||
                    cells[0].text().contains("voliteľné predmety", ignoreCase = true)) electiveSection = true
            }
            if (cells.size != 6 || semester == 0) return@forEach
            val link = cells[0].selectFirst("a[href*=predmet=]") ?: return@forEach
            val subjectId = subjectPattern.find(link.attr("href"))?.groupValues?.get(1) ?: return@forEach
            val code = link.text().trim().uppercase(Locale.ROOT)
            val credits = cells[3].text().trim().toIntOrNull() ?: return@forEach
            if (!code.matches(Regex("[A-Z0-9_.-]{2,32}")) || credits !in 0..60) return@forEach
            val title = cells[1].text().trim().takeIf { it.isNotEmpty() } ?: return@forEach
            val state = parseStatus(cells[5].text()) ?: error("Unknown UIS course status for $code")
            val className = row.className()
            val requirement = when {
                electiveSection || "predmety_vse_vyb-" in className -> CourseRequirementType.ELECTIVE
                "predmety_vse_pv-" in className -> CourseRequirementType.SEMI_COMPULSORY
                else -> CourseRequirementType.COMPULSORY
            }
            val mode = cells[2].text().trim().lowercase(Locale.ROOT)
            val grading = when {
                mode.contains("exm") || mode == "sk" || mode == "zk" -> CourseGradingType.EXAM_LETTER
                mode.contains("pass") || mode == "zap" || mode == "z" -> CourseGradingType.PASS_FAIL
                else -> CourseGradingType.CONTINUOUS_LETTER
            }
            val course = UisCourse(subjectId, code, title, credits, semester, state, requirement,
                grading, "https://is.stuba.sk/auth/katalog/syllabus.pl?predmet=$subjectId")
            val previous = byCode[code]
            if (previous == null || (previous.status != CourseStatus.FULFILLED && state == CourseStatus.FULFILLED))
                byCode[code] = course
        }
        check(byCode.isNotEmpty()) { "UIS study plan has no recognized courses" }
        return UisStudyPlan(studyId, periodId, creditNumbers.getOrNull(0), creditNumbers.getOrNull(1),
            byCode.values.toList(), currentSemester)
    }

    private fun parseStatus(raw: String): CourseStatus? {
        val value = raw.uppercase(Locale.ROOT).trim()
        return when {
            value.startsWith("FULFILLED") || value.startsWith("SPLNEN") || value.startsWith("SPLNĚN") -> CourseStatus.FULFILLED
            value.startsWith("=> OPTIONAL") || value.startsWith("⇒") -> CourseStatus.NOT_NEEDED
            value.startsWith("NOT ENROLLED") || value.startsWith("NEZAPÍS") || value.startsWith("NEZAPS") -> CourseStatus.NOT_ENROLLED
            value.startsWith("ENROLLED") || value.startsWith("ZAPÍS") || value.startsWith("ZAPS") -> CourseStatus.ENROLLED
            else -> null
        }
    }
}
