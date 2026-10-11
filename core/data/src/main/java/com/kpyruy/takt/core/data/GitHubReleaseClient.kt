package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppRelease
import com.kpyruy.takt.core.model.AppVersion
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A public, unauthenticated client, entirely separate from university sessions. */
class GitHubReleaseClient internal constructor(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
    .callTimeout(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
    .retryOnConnectionFailure(false).build(),
    private val endpoint: HttpUrl = "https://api.github.com/repos/Kpyruy/Takt/releases/latest".toHttpUrl(),
    private val clock: () -> Long = System::currentTimeMillis) {

    suspend fun latest(): AppRelease = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(endpoint)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Takt-Android-release-check").build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(AppReleaseException(AppReleaseError.NETWORK))
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!continuation.isActive) return
                    try {
                        if (response.code == 403 || response.code == 429) {
                            val now = clock()
                            val retrySeconds = response.header("Retry-After")?.toLongOrNull()?.coerceIn(60L, 86_400L)
                            val reset = response.header("X-RateLimit-Reset")?.toLongOrNull()
                                ?.takeIf { it in 0..Long.MAX_VALUE / 1000 }?.times(1000)
                            val retryAt = retrySeconds?.let { now + it * 1000 } ?: reset?.coerceIn(now + 60_000, now + 86_400_000)
                                ?: (now + 3_600_000)
                            throw AppReleaseException(AppReleaseError.RATE_LIMITED, retryAt)
                        }
                        if (!response.isSuccessful) throw AppReleaseException(AppReleaseError.UNAVAILABLE)
                        val body = response.body ?: throw AppReleaseException(AppReleaseError.INVALID_RESPONSE)
                        val source = body.source()
                        if (body.contentLength() > MAX_BODY_BYTES || source.request(MAX_BODY_BYTES + 1)) {
                            throw AppReleaseException(AppReleaseError.INVALID_RESPONSE)
                        }
                        val release = parseRelease(source.readUtf8())
                        if (continuation.isActive) continuation.resume(release)
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(when (error) {
                            is AppReleaseException -> error
                            is IOException -> AppReleaseException(AppReleaseError.NETWORK)
                            else -> AppReleaseException(AppReleaseError.INVALID_RESPONSE)
                        })
                    }
                }
            }
        })
    }

    companion object {
        fun forPublicReleases(): GitHubReleaseClient = GitHubReleaseClient()

        private const val MAX_BODY_BYTES = 524_288L
        fun parseRelease(raw: String): AppRelease {
            try {
                val json = Json.parseToJsonElement(raw).jsonObject
                val tag = json.getValue("tag_name").jsonPrimitive.content
                if (AppVersion.parse(tag) == null || json.getValue("draft").jsonPrimitive.boolean ||
                    json.getValue("prerelease").jsonPrimitive.boolean) invalid()
                val page = json.getValue("html_url").jsonPrimitive.content
                if (!isTrustedReleaseUrl(page, apk = false, tag = tag)) invalid()
                val apk = json["assets"]?.jsonArray?.firstNotNullOfOrNull { element ->
                    val asset = element.jsonObject
                    val name = asset["name"]?.jsonPrimitive?.content.orEmpty()
                    val url = asset["browser_download_url"]?.jsonPrimitive?.content.orEmpty()
                    url.takeIf { name.endsWith(".apk", ignoreCase = true) && isTrustedReleaseUrl(it, apk = true, tag = tag) }
                }
                return AppRelease(tag, page, apk)
            } catch (failure: AppReleaseException) { throw failure }
            catch (_: Exception) { invalid() }
        }

        fun isTrustedReleaseUrl(raw: String, apk: Boolean, tag: String? = null): Boolean {
            val url = raw.toHttpUrlOrNull() ?: return false
            if (url.scheme != "https" || url.host != "github.com" || url.port != 443 ||
                url.username.isNotEmpty() || url.password.isNotEmpty() || url.query != null || url.fragment != null) return false
            val segments = url.pathSegments
            if (segments.take(3) != listOf("Kpyruy", "Takt", "releases")) return false
            if (tag != null && segments.getOrNull(4) != tag) return false
            return if (apk) segments.size == 6 && segments[3] == "download" &&
                segments[4].isNotBlank() && segments[5].endsWith(".apk", ignoreCase = true)
            else segments.size == 5 && segments[3] == "tag" && segments[4].isNotBlank()
        }

        private fun invalid(): Nothing = throw AppReleaseException(AppReleaseError.INVALID_RESPONSE)
    }
}
