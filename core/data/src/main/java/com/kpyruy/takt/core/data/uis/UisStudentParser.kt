package com.kpyruy.takt.core.data.uis

import com.kpyruy.takt.core.model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.security.MessageDigest

/** Read-only, English UIS pages. Header matching includes visually hidden columns and legacy td headers. */
internal object UisStudentParser {
    private fun norm(s:String)=s.replace('\u00a0',' ').trim().lowercase(Locale.ROOT).replace(Regex("\\s+")," ")
    fun parameter(url:String,name:String):String?=Regex("(?:[?;&]|^)${Regex.escape(name)}=([1-9][0-9]*)(?=[;&]|$)").find(url)?.groupValues?.get(1)
    fun date(raw:String):LocalDate? {
        val s=raw.trim()
        return listOf("MM/dd/uuuu","d.M.uuuu","uuuu-MM-dd").firstNotNullOfOrNull { pattern ->
            runCatching { LocalDate.parse(s,DateTimeFormatter.ofPattern(pattern).withResolverStyle(java.time.format.ResolverStyle.STRICT)) }.getOrNull()
        }
    }
    fun money(raw:String):Pair<Long,String> {
        val currency=when { raw.contains('€') || raw.contains("EUR",true)->"EUR";raw.contains("CZK",true)||raw.contains("Kč")->"CZK";raw.contains("SKK",true)->"SKK";else->throw IllegalArgumentException("Unknown UIS currency") }
        var n=raw.replace(Regex("[\\s\\u00a0]"),"").replace(Regex("(?i)EUR|CZK|SKK|Kč|€"),"")
        require(n.matches(Regex("[+]?[0-9][0-9.,]*"))) { "Invalid UIS amount" }
        if(n.contains(',')&&n.contains('.')) n=if(n.lastIndexOf(',')>n.lastIndexOf('.'))n.replace(".","").replace(',','.') else n.replace(",","")
        else if(n.contains(',')) n=n.replace(',','.')
        val minor=runCatching { BigDecimal(n).movePointRight(2).longValueExact() }.getOrElse { throw IllegalArgumentException("Invalid UIS amount precision",it) }
        require(minor in 0..100_000_000_000L)
        return minor to currency
    }
    private fun cells(row:Element)=row.children().filter { it.tagName() in listOf("th","td") }
    private fun headers(table:Element,wanted:Set<String>):Pair<List<String>,Element>? = table.select("tr").firstNotNullOfOrNull { row ->
        val names=cells(row).map { norm(it.text()) }
        if(wanted.all { it in names }) names to row else null
    }
    private fun unique(base:String):String=MessageDigest.getInstance("SHA-256").digest(base.toByteArray()).take(12).joinToString(""){"%02x".format(it.toInt() and 255)}
    fun portal(html:String):StudentHubSnapshot {
        val d=Jsoup.parse(html,"https://is.stuba.sk/auth/student/moje_studium.pl")
        val table=d.select("table").firstOrNull { headers(it,setOf("faculty","study","progress of study"))!=null } ?: throw IllegalArgumentException("UIS study directory missing")
        val (names,header)=headers(table,setOf("faculty","study","progress of study"))!!
        val selectedIds=mutableSetOf<String>()
        val studies=table.select("tr").filter { it!==header }.mapNotNull { row ->
            val c=cells(row);if(c.size!=names.size)return@mapNotNull null
            val links=row.select("a[href]").map { it.attr("href") }
            val id=links.firstNotNullOfOrNull { parameter(it,"studium") } ?: return@mapNotNull null
            names.indexOf("choose").takeIf { it >= 0 }?.let { index ->
                val choice=c[index]
                val labels=listOf(choice.text()) + choice.select("[aria-label],[alt],[title]").flatMap {
                    listOf(it.attr("aria-label"),it.attr("alt"),it.attr("title"))
                }
                if (labels.any { norm(it) in setOf("active","selected") }) selectedIds += id
            }
            val study=c[names.indexOf("study")]
            val title=study.textNodes().firstOrNull { it.text().isNotBlank() }?.text()?.trim()
                ?: study.text().substringBefore("full-time").trim()
            val status=norm(c[names.indexOf("progress of study")].ownText())
            val faculty=c[names.indexOf("faculty")].text()
            StudentStudy(id,title,university="STU · $faculty",active=status=="enrolled" || status.startsWith("studying"),
                periodId=links.firstNotNullOfOrNull { if(parameter(it,"studium")==id)parameter(it,"obdobi") else null })
        }.distinctBy { it.id }
        require(studies.isNotEmpty())
        require(selectedIds.size <= 1) { "UIS selected study is ambiguous" }
        val selected=studies.firstOrNull { it.id in selectedIds && it.active }
            ?: studies.firstOrNull { it.active } ?: studies.first()
        val name=d.selectFirst("#log")?.ownText()?.substringAfter("Logged in:","")?.trim().orEmpty()
        return StudentHubSnapshot(identity=name.takeIf { it.isNotBlank() }?.let { StudentIdentity(name=it,university="STU") },studies=studies,currentStudyId=selected.id,viewedStudyId=selected.id)
    }
    fun programmeTitle(html: String): String? = Jsoup.parse(html).select("table tr").firstNotNullOfOrNull { row ->
        val c=cells(row)
        if (c.size == 2 && norm(c[0].text()).removeSuffix(":") in setOf("programme", "program", "študijný program"))
            c[1].text().trim().takeIf { it.isNotBlank() } else null
    }
    fun studyDetails(html:String,base:StudentStudy):Pair<StudentStudy,String?> {
        val d=Jsoup.parse(html)
        val fields=d.select("table tr").mapNotNull { row -> val c=cells(row);if(c.size==2)norm(c[0].text()).removeSuffix(":") to c[1].text().trim() else null }.toMap()
        require("programme" in fields && "beginning of study" in fields) { "UIS study details missing" }
        val start=fields["beginning of study"]?.let(::date);val end=fields["completion date"]?.let(::date)
        fun credits(key:String)=fields[key]?.replace(Regex("\\s"),"")?.toIntOrNull()
        val selected=d.selectFirst("select[name=studium] option[selected]")?.text().orEmpty()
        val semester=Regex("term\\s+(\\d+)\\s*,\\s*year\\s+(\\d+)",RegexOption.IGNORE_CASE).find(selected)?.let { (it.groupValues[2].toInt()-1)*2+if(it.groupValues[1].toInt()%2==1)1 else 2 }
        return base.copy(title=fields.getValue("programme"),titleEn=fields.getValue("programme"),degree=fields["your prospective academic degree"],
            periodLabel=start?.let { if(end!=null)"${it.year}–${end.year}" else "${it.year}–…" },currentSemester=semester?:base.currentSemester,
            earnedCredits=credits("number of credits obtained during your whole course of study"),
            requiredCredits=credits("number of credits required to complete your study")) to fields["user's identification number"]
    }
    fun fees(html:String,studyId:String):List<StudentFee> {
        val d=Jsoup.parse(html,"https://is.stuba.sk/auth/student/financovani.pl")
        val found=d.select("table").firstNotNullOfOrNull { t -> headers(t,setOf("status","type","sum","settled"))?.let { Triple(t,it.first,it.second) } }
            ?: throw IllegalArgumentException("UIS financing decisions table missing")
        val (table,names,header)=found
        val results=table.select("tr").filter { it!==header }.mapNotNull { row ->
            val c=cells(row);if(c.size!=names.size)return@mapNotNull null
            fun value(name:String)=names.indexOf(name).takeIf { it>=0 }?.let { c[it].text().trim() }.orEmpty()
            val title=value("type");if(title.isBlank())return@mapNotNull null
            val (amount,currency)=money(value("manually").ifBlank { value("sum") })
            val statusCell=c[names.indexOf("status")];val status=norm(statusCell.text()+" "+statusCell.select("[aria-label],[alt],[title]").joinToString(" "){it.attr("aria-label").ifBlank{it.attr("alt")}.ifBlank{it.attr("title")}})
            val settled=value("settled").takeIf { it.isNotBlank() }?.let { val m=money(it);require(m.second==currency);m.first }
            val state=when { status.contains("unpaid")||status.contains("not paid")->StudentFeeStatus.UNPAID;status.contains("partial")->StudentFeeStatus.PARTIAL;status.contains("paid")->StudentFeeStatus.PAID;status.contains("cancel")->StudentFeeStatus.CANCELLED;else->StudentFeeStatus.UNKNOWN }
            val url=row.select("a[href]").map { it.absUrl("href") }.firstOrNull { parameter(it,"fakturace")!=null }
                ?.takeIf { runCatching { val u=java.net.URI(it);u.scheme=="https"&&u.host=="is.stuba.sk"&&u.path=="/auth/student/financovani.pl"&&u.userInfo==null&&(u.port==-1||u.port==443) }.getOrDefault(false) }
            StudentFee(url?.let { parameter(it,"fakturace") }?:unique("$studyId:$title:${value("issue date of decision")}:$amount"),studyId,title,amount,currency,settled,state,
                dueDate=value("due date").takeIf { it.isNotBlank() }?.let(::date),issuedDate=date(value("issue date of decision")),recordUrl=url)
        }
        if(results.isEmpty())require(table.text().contains("No suitable data found",true)||table.select("tbody tr").isEmpty()) { "Unrecognized empty UIS fees" }
        require(results.map{it.id}.distinct().size==results.size)
        return results
    }
    fun scholarships(html:String,studyId:String):List<ScholarshipPayment> {
        val d=Jsoup.parse(html)
        if(d.text().contains("No scholarships have been paid to you for the selected study.",true))return emptyList()
        val found=d.select("table").firstNotNullOfOrNull { t -> headers(t,setOf("month","year","sum","date of payment"))?.let { Triple(t,it.first,it.second) } }
            ?:throw IllegalArgumentException("UIS scholarship table missing")
        val (table,names,header)=found;val occurrences=mutableMapOf<String,Int>()
        return table.select("tr").filter { it!==header }.mapNotNull { row ->
            val c=cells(row);if(c.size!=names.size)return@mapNotNull null
            fun value(name:String)=names.indexOf(name).takeIf{it>=0}?.let{c[it].text().trim()}.orEmpty()
            val rawMonth=value("month")
            val monthNumber=rawMonth.toIntOrNull() ?: java.time.Month.entries.firstOrNull {
                it.getDisplayName(java.time.format.TextStyle.FULL,Locale.ENGLISH).equals(rawMonth,true)
            }?.value
            val month=monthNumber?.toString().orEmpty();val year=value("year")
            if(month.toIntOrNull() !in 1..12||year.toIntOrNull() !in 1900..2200)return@mapNotNull null
            val (amount,currency)=money(value("sum"));val paid=date(value("date of payment"))?:throw IllegalArgumentException("UIS scholarship payment date missing")
            val key=unique("$studyId:${c.joinToString("|"){it.text()}}")
            val occurrence=(occurrences[key]?:0)+1;occurrences[key]=occurrence
            val title=value("type").ifBlank { value("classification") };require(title.isNotBlank())
            ScholarshipPayment("$key:$occurrence",studyId,title,amount,currency,paid,"$year-${month.padStart(2,'0')}")
        }.also { require(it.isNotEmpty()||table.text().contains("No suitable data found",true)) }
    }
}
