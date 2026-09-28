package com.kpyruy.takt.core.data.uis

import org.jsoup.Jsoup

/** Finds a course by its exact short code in the selected UIS timetable. */
internal object UisCourseTimetableParser {
    fun subjectId(html: String, code: String): String? = Jsoup.parse(html)
        .select("select[name=predmet] option[value]")
        .firstOrNull { option ->
            option.attr("value").isNotEmpty() && option.attr("value").all(Char::isDigit) &&
                option.attr("value") != "0" &&
                option.text().substringBefore(" - ").trim().equals(code, ignoreCase = true)
        }?.attr("value")
}
