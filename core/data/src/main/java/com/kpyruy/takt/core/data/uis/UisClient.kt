package com.kpyruy.takt.core.data.uis

import java.io.IOException
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

/** Blocking, single-session client. Used only on Dispatchers.IO; never logs request data. */
internal class UisClient(private val origin: HttpUrl = "https://is.stuba.sk/".toHttpUrl()) {
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
        cookies.clear(); challenge = null
        val page = exchange(origin.resolve("system/login.pl?lang=sk")!!)
        val form = form(page) ?: return@safely UisResult.UNEXPECTED_RESPONSE
        form.fields["credential_0"] = login
        form.fields["credential_1"] = password
        // UIS's own login page performs this preflight before submitting the form.
        val preflight = exchange(origin.resolve("system/ajax_handler.pl")!!,
            mapOf("login" to login, "password" to password, "lang" to "sk"))
        val json = Json.parseToJsonElement(preflight.html).jsonObject
        if (json["error"]?.jsonPrimitive?.content?.let { it != "false" && it != "null" && it.isNotEmpty() } == true)
            return@safely UisResult.INVALID_CREDENTIALS
        when (json["need2FA"]?.jsonPrimitive?.content) {
            "true" -> {
                form.fields["auth_id_hidden"] = json["authID"]?.jsonPrimitive?.content ?: "0"
                form.fields["auth_2fa_type"] = json["type"]?.jsonPrimitive?.content
                    ?: return@safely UisResult.UNEXPECTED_RESPONSE
                challenge = form
                UisResult.SECOND_FACTOR
            }
            "false" -> classify(exchange(form.url, form.fields))
            else -> UisResult.UNEXPECTED_RESPONSE
        }
    }

    fun submitCode(code: String): UisResult = safely {
        val pending = challenge ?: return@safely UisResult.EXPIRED
        pending.fields["credential_k"] = code.trim().replace(" ", "")
        classify(exchange(pending.url, pending.fields))
    }

    fun checkSession(): UisResult = safely {
        val page = exchange(origin.resolve("auth/?lang=sk")!!)
        if (isAuthenticated(page)) UisResult.CONNECTED else {
            cookies.clear(); challenge = null
            UisResult.EXPIRED
        }
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
            challenge = form(page) ?: return UisResult.UNEXPECTED_RESPONSE
            return UisResult.SECOND_FACTOR
        }
        challenge = null
        return if (doc.selectFirst("input[name=credential_1]") != null)
            UisResult.INVALID_CREDENTIALS else UisResult.UNEXPECTED_RESPONSE
    }

    private fun isAuthenticated(page: Page): Boolean {
        if (!page.url.encodedPath.startsWith("/auth/")) return false
        if (cookies.none { it.name == "UISAuth" && it.expiresAt > System.currentTimeMillis() && it.matches(page.url) }) return false
        val doc = Jsoup.parse(page.html, page.url.toString())
        if (doc.selectFirst("input[name=credential_1]") != null) return false
        return doc.select("a[href]").any {
            page.url.resolve(it.attr("href"))?.let { url ->
                sameOrigin(url) && url.encodedPath == "/system/logout.pl"
            } == true
        }
    }

    private fun form(page: Page): LoginForm? {
        val doc = Jsoup.parse(page.html, page.url.toString())
        val input = doc.selectFirst("input[name=credential_1], input[name=credential_k]") ?: return null
        val form: Element = input.closest("form") ?: return null
        if (!form.attr("method").equals("post", true)) return null
        val target = page.url.resolve(form.attr("action")) ?: return null
        require(sameOrigin(target) && target.encodedPath == "/system/login.pl")
        val fields = linkedMapOf<String, String>()
        form.select("input[type=hidden][name]:not([disabled])").forEach { fields[it.attr("name")] = it.attr("value") }
        fields["lang"] = "sk"
        fields["destination"] = "/auth/?lang=sk"
        return LoginForm(target, fields)
    }

    private fun exchange(url: HttpUrl, fields: Map<String, String>? = null): Page {
        if (closed.get()) throw IOException("Session closed")
        require(sameOrigin(url))
        var request = Request.Builder().url(url).header("Accept-Language", "sk")
            .header("Cache-Control", "no-store")
            .apply { if (fields != null) {
                header("Origin", origin.toString().trimEnd('/'))
                header("Referer", origin.resolve("system/login.pl?lang=sk").toString())
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
                    require(sameOrigin(next))
                    request = Request.Builder().url(next).header("Accept-Language", "sk").build()
                } else {
                    if (response.code == 429 || response.code >= 500) throw IOException("UIS unavailable")
                    require(response.isSuccessful)
                    val body = response.body ?: error("Missing body")
                    // Bound memory even if the server returns an unexpected document.
                    val source = body.source()
                    source.request(2_000_001)
                    require(source.buffer.size <= 2_000_000)
                    return Page(request.url, source.readUtf8())
                }
            }
        }
        error("Redirect limit")
    }

    private fun sameOrigin(url: HttpUrl) = url.scheme == origin.scheme && url.host == origin.host && url.port == origin.port
    private fun safely(action: () -> UisResult): UisResult = try { action() }
        catch (_: IOException) { UisResult.UNAVAILABLE }
        catch (_: Exception) { challenge = null; UisResult.UNEXPECTED_RESPONSE }
    private class LoginForm(val url: HttpUrl, val fields: MutableMap<String, String>)
    private class Page(val url: HttpUrl, val html: String)
}

enum class UisResult { DISCONNECTED, CONNECTING, CONNECTED, SECOND_FACTOR, INVALID_CREDENTIALS, EXPIRED, UNAVAILABLE, UNEXPECTED_RESPONSE }
