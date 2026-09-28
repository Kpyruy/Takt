package com.kpyruy.takt.core.data.uis

import java.io.IOException
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.InterruptedIOException
import javax.net.ssl.SSLException
import kotlinx.serialization.SerializationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Call
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDate
import com.kpyruy.takt.core.model.SemesterPeriod

/** Blocking, single-session client. Used only on Dispatchers.IO; never logs request data. */
internal class UisClient(private val origin: HttpUrl = "https://is.stuba.sk/".toHttpUrl()) {
    var failure: UisFailure? = null
        private set
    private var stage = UisStage.LOGIN_FORM
    private val closed = AtomicBoolean(false)
    private val activeCall = AtomicReference<Call?>(null)
    private val cookies = mutableListOf<Cookie>()
    private var challenge: LoginForm? = null
    private val http = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(false)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, incoming: List<Cookie>) {
                incoming.forEach { cookie ->
                    cookies.removeAll { it.name == cookie.name && it.path == cookie.path && it.domain == cookie.domain }
                    cookies.add(cookie)
                }
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> = cookies.filter {
                it.expiresAt > System.currentTimeMillis() && it.matches(url)
            }
        }).build()

    fun login(login: String, password: String): UisResult = safely {
        stage = UisStage.LOGIN_FORM
        cookies.clear(); challenge = null
        val page = exchange(origin.resolve("system/login.pl?lang=sk")!!)
        val form = form(page) ?: throw ProtocolFailure(UisFailureReason.LOGIN_FORM_MISSING)
        form.fields["credential_0"] = login
        form.fields["credential_1"] = password
        // UIS's own login page performs this preflight before submitting the form.
        stage = UisStage.PREFLIGHT
        val preflight = exchange(origin.resolve("system/ajax_handler.pl")!!,
            mapOf("login" to login, "password" to password, "lang" to "sk"))
        val json = Json.parseToJsonElement(preflight.html).jsonObject
        if (json["error"]?.jsonPrimitive?.content?.let { it != "false" && it != "null" && it.isNotEmpty() } == true)
            return@safely rejectCredentials()
        when (json["need2FA"]?.jsonPrimitive?.content) {
            "true" -> {
                form.fields["auth_id_hidden"] = json["authID"]?.jsonPrimitive?.content ?: "0"
                form.fields["auth_2fa_type"] = json["type"]?.jsonPrimitive?.content
                    ?: throw ProtocolFailure(UisFailureReason.SECOND_FACTOR_CONFIGURATION)
                challenge = form
                UisResult.SECOND_FACTOR
            }
            "false" -> { stage = UisStage.SIGN_IN; classify(exchange(form.url, form.fields)) }
            else -> throw ProtocolFailure(UisFailureReason.RESPONSE_FORMAT)
        }
    }

    fun submitCode(code: String): UisResult = safely {
        stage = UisStage.SECOND_FACTOR
        val pending = challenge ?: return@safely UisResult.EXPIRED
        pending.fields["credential_k"] = code.trim().replace(" ", "")
        classify(exchange(pending.url, pending.fields))
    }

    fun checkSession(): UisResult = safely {
        stage = UisStage.SESSION_CHECK
        val page = exchange(origin.resolve("auth/?lang=sk")!!)
        if (isAuthenticated(page)) UisResult.CONNECTED else {
            cookies.clear(); challenge = null
            UisResult.EXPIRED
        }
    }

    fun readStudyPlan(): UisStudyPlan {
        val page = exchange(origin.resolve("auth/studijni/studijni_povinnosti.pl?lang=en")!!)
        check(isAuthenticated(page)) { "UIS session expired while reading the study plan" }
        val english = UisStudyPlanParser.parse(page.html)
        val slovakPage = exchange(origin.resolve("auth/studijni/studijni_povinnosti.pl?lang=sk")!!)
        check(isAuthenticated(slovakPage)) { "UIS session expired while reading Slovak course names" }
        val slovak = UisStudyPlanParser.slovakTitles(slovakPage.html)
        return english.copy(courses = english.courses.map { course ->
            course.copy(titleSk = slovak[course.code])
        })
    }

    fun readStudyLinks(studyId: String, periodId: String): UisStudyLinks {
        require(studyId.all(Char::isDigit) && periodId.all(Char::isDigit))
        val page = exchange(origin.resolve("auth/student/moje_studium.pl?_m=3110;studium=$studyId;obdobi=$periodId;lang=en")!!)
        check(isAuthenticated(page)) { "UIS session expired while reading study links" }
        val doc = Jsoup.parse(page.html, page.url.toString())
        fun find(path: String, criterion: String): HttpUrl? = doc.select("a[href]")
            .firstNotNullOfOrNull { link ->
                val url = page.url.resolve(link.attr("href"))
                url?.takeIf { sameOrigin(it) && it.encodedPath == path && it.toString().contains(criterion) }
            }
        val calendar = find("/auth/student/harmonogram.pl", "obdobi=$periodId")
        val timetable = find("/auth/katalog/rozvrhy_view.pl", "rozvrh_student=")
        return UisStudyLinks(calendar, timetable)
    }

    fun readAcademicCalendar(url: HttpUrl, today: LocalDate = LocalDate.now()): SemesterPeriod {
        val query = url.encodedQuery.orEmpty().split(';', '&')
            .filterNot { it.startsWith("lang=") }.filter { it.isNotEmpty() }
            .joinToString(";")
        val page = exchange(url.newBuilder().encodedQuery("$query;lang=en".trimStart(';')).build())
        check(isAuthenticated(page)) { "UIS session expired while reading academic calendar" }
        return UisAcademicCalendarParser.parse(page.html, today)
    }

    fun readTimetable(url: HttpUrl): List<UisTimetableItem> {
        val page = exchange(url)
        check(isAuthenticated(page)) { "UIS session expired while reading timetable" }
        val doc = Jsoup.parse(page.html, page.url.toString())
        val form = doc.select("form").firstOrNull { it.selectFirst("select[name=format]") != null }
            ?: error("UIS timetable format form missing")
        val target = page.url.resolve(form.attr("action")) ?: error("UIS timetable form destination missing")
        check(sameOrigin(target) && target.encodedPath == "/auth/katalog/rozvrhy_view.pl")
        val fields = linkedMapOf<String, String>()
        form.select("input[type=hidden][name]").forEach { fields[it.attr("name")] = it.attr("value") }
        form.select("input[type=checkbox][name][checked]").forEach {
            fields[it.attr("name")] = it.attr("value").ifBlank { "1" }
        }
        fields["typ_vypisu"] = "souhrn"
        fields["format"] = "list"
        fields["zobraz"] = "1"
        fields["zobraz2"] = "Display"
        val listPage = exchange(target, fields, referer = page.url)
        check(isAuthenticated(listPage)) { "UIS session expired while reading timetable list" }
        return UisTimetableParser.parse(listPage.html)
    }

    fun readCourseLessons(code: String): List<UisTimetableItem> {
        require(code.isNotBlank() && code.length <= 32)
        val base = origin.resolve("auth/katalog/rozvrhy_view.pl")!!
        val criteria = exchange(base.newBuilder().addQueryParameter("lang", "en").build())
        check(isAuthenticated(criteria)) { "UIS session expired while reading timetable criteria" }
        val criteriaDoc = Jsoup.parse(criteria.html)
        val available = criteriaDoc.select("select[name=rozvrh] option[value]")
            .filter { it.attr("value").isNotBlank() && it.attr("value") != "0" }
        val timetableId = (available.firstOrNull { it.hasAttr("selected") }
            ?: available.singleOrNull())?.attr("value")
            ?: criteriaDoc.selectFirst("input[name=rozvrh][value]")?.attr("value")
            ?: error("UIS timetable period is not selected")
        check(timetableId.all(Char::isDigit))
        val directory = exchange(base, mapOf(
            "z" to "1", "k" to "1", "f" to "0", "studijni_zpet" to "0",
            "rozvrh" to timetableId, "indiv_mistnosti_ne" to "Back to Simple selection",
            "garant" to "0", "ucitel" to "0", "predmet" to "0", "ustav" to "0",
            "den" to "0", "stupen" to "0", "program" to "0", "obor" to "0",
            "rocnik" to "0", "skupina" to "0", "format" to "html", "lang" to "en",
        ), referer = criteria.url)
        check(isAuthenticated(directory)) { "UIS session expired while reading course choices" }
        val subjectId = UisCourseTimetableParser.subjectId(directory.html, code) ?: return emptyList()
        val list = exchange(base, mapOf(
            "z" to "1", "k" to "1", "f" to "0", "studijni_zpet" to "0",
            "rozvrh" to timetableId, "mistnost" to "0", "garant" to "0",
            "ucitel" to "0", "predmet" to subjectId, "ustav" to "0", "den" to "0",
            "stupen" to "0", "program" to "0", "obor" to "0", "rocnik" to "0",
            "skupina" to "0", "format" to "list", "zobraz" to "Display", "lang" to "en",
        ), referer = directory.url)
        check(isAuthenticated(list)) { "UIS session expired while reading course lessons" }
        return UisTimetableParser.parse(list.html).filter { it.subjectId == subjectId }
    }

    fun close() {
        closed.set(true)
        activeCall.get()?.cancel()
        http.dispatcher.cancelAll()
        // State is discarded by the owner; do not mutate the jar concurrently with a request.
    }

    private fun classify(page: Page): UisResult {
        if (isAuthenticated(page)) {
            challenge = null
            return UisResult.CONNECTED
        }
        val doc = Jsoup.parse(page.html, page.url.toString())
        val code = doc.selectFirst("input[name=credential_k]:not([disabled])")
        if (code != null) {
            challenge = form(page) ?: throw ProtocolFailure(UisFailureReason.LOGIN_FORM_MISSING)
            return UisResult.SECOND_FACTOR
        }
        challenge = null
        return if (doc.selectFirst("input[name=credential_1]") != null)
            rejectCredentials() else throw ProtocolFailure(UisFailureReason.SESSION_NOT_CONFIRMED)
    }

    private fun isAuthenticated(page: Page): Boolean {
        if (!page.url.encodedPath.startsWith("/auth/")) return false
        if (cookies.none { it.name == "UISAuth" && it.expiresAt > System.currentTimeMillis() && it.matches(page.url) }) return false
        val doc = Jsoup.parse(page.html, page.url.toString())
        if (doc.selectFirst("input[name=credential_1]") != null) return false
        return doc.select("a[href]").any {
            page.url.resolve(it.attr("href"))?.let { url ->
                sameOrigin(url) && url.encodedPath in setOf("/auth/system/logout.pl", "/system/logout.pl")
            } == true
        }
    }

    private fun form(page: Page): LoginForm? {
        val doc = Jsoup.parse(page.html, page.url.toString())
        val input = doc.selectFirst("input[name=credential_1], input[name=credential_k]") ?: return null
        val form: Element = input.closest("form") ?: return null
        if (!form.attr("method").equals("post", true)) return null
        val target = page.url.resolve(form.attr("action")) ?: return null
        if (!sameOrigin(target) || target.encodedPath != "/system/login.pl") throw ProtocolFailure(UisFailureReason.UNSAFE_DESTINATION)
        val fields = linkedMapOf<String, String>()
        form.select("input[type=hidden][name]:not([disabled])").forEach { fields[it.attr("name")] = it.attr("value") }
        fields["lang"] = "sk"
        fields["destination"] = "/auth/?lang=sk"
        return LoginForm(target, fields)
    }

    private fun exchange(url: HttpUrl, fields: Map<String, String>? = null, referer: HttpUrl? = null): Page {
        if (closed.get()) throw IOException("Session closed")
        if (!sameOrigin(url)) throw ProtocolFailure(UisFailureReason.UNSAFE_DESTINATION)
        var request = Request.Builder().url(url).header("Accept-Language", "sk")
            .header("Cache-Control", "no-store")
            .apply { if (fields != null) {
                header("Origin", origin.toString().trimEnd('/'))
                header("Referer", (referer ?: origin.resolve("system/login.pl?lang=sk")!!).toString())
                post(FormBody.Builder().apply { fields.forEach { (key, value) -> add(key, value) } }.build())
            } }.build()
        repeat(6) {
            val call = http.newCall(request)
            activeCall.set(call)
            if (closed.get()) call.cancel()
            call.execute().use { response ->
                if (response.code in listOf(301, 302, 303)) {
                    val next = request.url.resolve(response.header("Location") ?: error("Missing redirect"))
                        ?: error("Invalid redirect")
                    if (!sameOrigin(next)) throw ProtocolFailure(UisFailureReason.UNSAFE_DESTINATION)
                    request = Request.Builder().url(next).header("Accept-Language", "sk").build()
                } else {
                    if (!response.isSuccessful) throw HttpFailure(response.code)
                    val body = response.body ?: error("Missing body")
                    // Bound memory even if the server returns an unexpected document.
                    val source = body.source()
                    source.request(2_000_001)
                    if (source.buffer.size > 2_000_000) throw ProtocolFailure(UisFailureReason.RESPONSE_TOO_LARGE)
                    return Page(request.url, source.readUtf8())
                }
            }
        }
        throw ProtocolFailure(UisFailureReason.TOO_MANY_REDIRECTS)
    }

    private fun sameOrigin(url: HttpUrl) = url.scheme == origin.scheme && url.host == origin.host && url.port == origin.port
    private fun rejectCredentials(): UisResult {
        failure = UisFailure(stage, UisFailureReason.CREDENTIALS_REJECTED)
        return UisResult.INVALID_CREDENTIALS
    }
    private fun safely(action: () -> UisResult): UisResult {
        failure = null
        return try { action() }
        catch (error: Exception) {
            challenge = null
            val reason = when (error) {
                is UnknownHostException -> UisFailureReason.DNS
                is SocketTimeoutException, is InterruptedIOException -> UisFailureReason.TIMEOUT
                is SSLException -> UisFailureReason.TLS
                is HttpFailure -> UisFailureReason.HTTP
                is IOException -> UisFailureReason.CONNECTION
                is ProtocolFailure -> error.reason
                is SerializationException, is IllegalArgumentException -> UisFailureReason.RESPONSE_FORMAT
                else -> UisFailureReason.UNKNOWN
            }
            failure = UisFailure(stage, reason, (error as? HttpFailure)?.status)
            if (error is IOException || error is HttpFailure) UisResult.UNAVAILABLE else UisResult.UNEXPECTED_RESPONSE
        }
    }
    private class HttpFailure(val status: Int) : Exception()
    private class ProtocolFailure(val reason: UisFailureReason) : Exception()
    private class LoginForm(val url: HttpUrl, val fields: MutableMap<String, String>)
    private class Page(val url: HttpUrl, val html: String)
}

internal data class UisStudyLinks(val calendar: HttpUrl?, val timetable: HttpUrl?)

enum class UisResult { DISCONNECTED, CONNECTING, CONNECTED, SECOND_FACTOR, INVALID_CREDENTIALS, EXPIRED, UNAVAILABLE, UNEXPECTED_RESPONSE }
