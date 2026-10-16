package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.CourseTeacher
import com.kpyruy.takt.core.model.TeacherProfile
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.Normalizer
import java.util.Locale

/** Parse data sections only: page navigation, logged-in users and modification authors are not lecturers. */
internal object UisTeachingParser {
    private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(':', ' ').replace(Regex("\\s+"), " ").trim()

    fun teachers(html: String): List<CourseTeacher> {
        val doc = Jsoup.parse(html)
        check(doc.selectFirst("input[name=credential_1]") == null) { "UIS sign-in required" }
        val labels = setOf("name of lecturer(s)", "name of lecturers", "vyucujuci", "vyucujuci predmetu", "vyucujici", "vyucujici predmetu")
        val label = doc.select("td,th").firstOrNull { normalized(it.text()) in labels }
            ?: error("UIS lecturer section missing")
        val row = requireNotNull(label.parent())
        val ownCells = row.children().filter { it.tagName() in setOf("td", "th") }
        var section = ownCells.drop(1).firstOrNull { it.select("a[href]").isNotEmpty() }
        if (section == null) {
            var next = row.nextElementSibling()
            repeat(3) {
                if (section == null && next != null) {
                    if (next!!.select("a[href]").isNotEmpty()) section = next else if (next!!.text().isNotBlank()) return@repeat
                    next = next!!.nextElementSibling()
                }
            }
        }
        val teachers = linkedMapOf<String, CourseTeacher>()
        section?.select("a[href]")?.forEach { anchor ->
            val id = UisTeachingLinks.personId(anchor.attr("href")) ?: return@forEach
            val name = anchor.text().trim().takeIf { it.isNotEmpty() } ?: return@forEach
            val tail = StringBuilder()
            var node = anchor.nextSibling()
            while (node != null && node.nodeName() !in setOf("br", "a")) {
                tail.append(node.outerHtml()); node = node.nextSibling()
            }
            val details = Jsoup.parseBodyFragment(tail.toString()).text()
            val roles = Regex("\\(([^)]*)\\)").find(details)?.groupValues?.get(1)
                ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()
            val languages = details.substringAfter(')', "").trim().removePrefix("-").trim()
                .split(',', ';').map(String::trim).filter(String::isNotEmpty)
            val old = teachers[id]
            teachers[id] = CourseTeacher(id, old?.name ?: name,
                (old?.roles.orEmpty() + roles).distinct(), (old?.languages.orEmpty() + languages).distinct())
        }
        // The course supervisor may be listed separately; never include the modification author.
        doc.select("tr").forEach { candidate ->
            val cells = candidate.children().filter { it.tagName() in setOf("td", "th") }
            if (cells.size < 2 || normalized(cells[0].text()) !in setOf("supervisor", "garant", "garant predmetu")) return@forEach
            cells[1].select("a[href]").forEach { anchor ->
                val id = UisTeachingLinks.personId(anchor.attr("href")) ?: return@forEach
                if (id !in teachers) teachers[id] = CourseTeacher(id, anchor.text(), listOf("person responsible for course"))
            }
        }
        return teachers.values.toList()
    }

    fun profile(html: String, expectedId: String): TeacherProfile {
        require(UisTeachingLinks.validId(expectedId))
        val doc = Jsoup.parse(html)
        check(doc.selectFirst("input[name=credential_1]") == null) { "UIS sign-in required" }
        val identity = Regex("(?:identification number|identifikacne cislo|identifikacni cislo)\\s+(\\d+)")
            .find(normalized(doc.body().text()))?.groupValues?.get(1)
        check(identity == expectedId) { "UIS person identity mismatch" }
        val name = (doc.selectFirst("font[size=+1]") ?: doc.select("h1").lastOrNull())?.text()?.trim()
        check(!name.isNullOrBlank()) { "UIS person name missing" }
        val fields = linkedMapOf<String, String>()
        var position: String? = null
        var mailbox: String? = null
        doc.select("tr").forEach { row ->
            val cells = row.children().filter { it.tagName() in setOf("td", "th") }
            if (cells.isEmpty()) return@forEach
            if (cells.size == 1) {
                val text = cells[0].text()
                val colon = text.indexOf(':')
                if (colon > 0) fields[normalized(text.take(colon))] = text.substring(colon + 1).trim()
                if (cells[0].select("a[href*=pracoviste.pl]").isNotEmpty()) position = text
                val key = normalized(text)
                if (key.contains("university e-mail box last checked") || (key.contains("schranka") && key.contains("naposled"))) {
                    mailbox = Regex("\\d{1,4}[./]\\s*\\d{1,2}[./]\\s*\\d{1,4}\\s+\\d{1,2}:\\d{2}").find(text)?.value
                }
            } else {
                fields[normalized(cells[0].text())] = cells.drop(1).joinToString("\n") { multiline(it) }
            }
        }
        fun field(vararg labels: String) = labels.firstNotNullOfOrNull { fields[normalized(it)]?.takeIf(String::isNotBlank) }
        return TeacherProfile(expectedId, name,
            position = position,
            universityEmail = field("University e-mail", "Univerzitný e-mail", "Univerzitní e-mail")?.let(::email),
            systemEmail = field("E-mail in the information system", "E-mail v informačnom systéme", "E-mail v informačním systému", "E-mail v IS")?.let(::email),
            officeAddress = field("Office address", "Adresa pracoviska", "Adresa pracoviště", "Adresa kancelárie"),
            officeNumber = field("Office number", "Číslo kancelárie", "Číslo kanceláře", "Miestnosť", "Místnost"),
            officePhone = field("Office phone number", "Telefón do kancelárie", "Telefón na pracovisko", "Telefon do kanceláře", "Telefón"),
            consultationHours = field("Office hours", "Consultation hours", "Konzultačné hodiny", "Konzultační hodiny"),
            mailboxLastChecked = mailbox)
    }

    private fun multiline(element: Element) = element.wholeText().lines().map(String::trim).filter(String::isNotEmpty).joinToString("\n")
    private fun email(value: String): String? {
        val address = value.replace(Regex("\\s*\\[at]\\s*", RegexOption.IGNORE_CASE), "@").trim()
        return address.takeIf { it.matches(Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}")) }
    }
}
