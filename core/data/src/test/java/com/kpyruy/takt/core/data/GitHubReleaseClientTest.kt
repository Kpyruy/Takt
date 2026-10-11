package com.kpyruy.takt.core.data

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class GitHubReleaseClientTest {
    private fun releaseJson(extra: String = "", page: String = "https://github.com/Kpyruy/Takt/releases/tag/v0.3.0", apk: String = "https://github.com/Kpyruy/Takt/releases/download/v0.3.0/Takt.apk") =
        """{"tag_name":"v0.3.0","html_url":"$page","draft":false,"prerelease":false,"assets":[{"name":"Takt.apk","browser_download_url":"$apk"}]$extra}"""
    @Test fun fetchesPublicReleaseWithoutCredentialsAndSelectsVerifiedApk() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(releaseJson()))
            val release = GitHubReleaseClient(OkHttpClient(), server.url("/releases/latest")).latest()
            assertEquals("v0.3.0", release.tag)
            assertEquals("https://github.com/Kpyruy/Takt/releases/download/v0.3.0/Takt.apk", release.apkUrl)
            val request = server.takeRequest()
            assertEquals("application/vnd.github+json", request.getHeader("Accept"))
            assertNull(request.getHeader("Authorization"))
            assertNull(request.getHeader("Cookie"))
        }
    }
    @Test fun rejectsPrereleaseMalformedVersionAndUntrustedPage() = runBlocking {
        listOf(releaseJson().replace("\"prerelease\":false", "\"prerelease\":true"),
            releaseJson().replace("v0.3.0", "garbage"), releaseJson(page = "https://evil.example/releases/tag/v0.3.0")).forEach { body ->
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setBody(body))
                try { GitHubReleaseClient(OkHttpClient(), server.url("/")).latest(); fail("Accepted unsafe release") }
                catch (e: AppReleaseException) { assertEquals(AppReleaseError.INVALID_RESPONSE, e.reason) }
            }
        }
    }
    @Test fun untrustedApkFallsBackToVerifiedReleasePage() = runBlocking {
        listOf("http://github.com/Kpyruy/Takt/releases/download/v0.3.0/Takt.apk",
            "https://github.com.evil.example/Kpyruy/Takt/releases/download/v0.3.0/Takt.apk",
            "https://github.com/Other/Takt/releases/download/v0.3.0/Takt.apk",
            "https://github.com/Kpyruy/Takt/releases/download/v0.3.0/Takt.apk?redirect=evil").forEach { url ->
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setBody(releaseJson(apk = url)))
                assertNull(GitHubReleaseClient(OkHttpClient(), server.url("/")).latest().apkUrl)
            }
        }
    }
    @Test fun releaseLinksMustBelongToTheReportedTag() {
        try { GitHubReleaseClient.parseRelease(releaseJson(page = "https://github.com/Kpyruy/Takt/releases/tag/v0.2.0")); fail("Page for another version") }
        catch (e: AppReleaseException) { assertEquals(AppReleaseError.INVALID_RESPONSE, e.reason) }
        val release = GitHubReleaseClient.parseRelease(releaseJson(apk = "https://github.com/Kpyruy/Takt/releases/download/v0.2.0/Takt.apk"))
        assertNull(release.apkUrl)
    }
    @Test fun rateLimitsReturnRetryTimeAndDoNotLoop() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "120"))
            try { GitHubReleaseClient(OkHttpClient(), server.url("/"), { 1000L }).latest(); fail() }
            catch (e: AppReleaseException) {
                assertEquals(AppReleaseError.RATE_LIMITED, e.reason)
                assertEquals(121000L, e.retryAt)
            }
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun cancellationClosesPendingCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(releaseJson()).setBodyDelay(2, TimeUnit.SECONDS))
            val canceledCall = java.util.concurrent.CountDownLatch(1)
            val client = OkHttpClient.Builder().eventListener(object : okhttp3.EventListener() {
                override fun callFailed(call: okhttp3.Call, ioe: java.io.IOException) {
                    if (call.isCanceled()) canceledCall.countDown()
                }
            }).build()
            val job = launch { GitHubReleaseClient(client, server.url("/")).latest() }
            assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(1, TimeUnit.SECONDS) })
            withTimeout(1000) { job.cancelAndJoin() }
            assertTrue(job.isCancelled)
            assertTrue(withContext(Dispatchers.IO) { canceledCall.await(1, TimeUnit.SECONDS) })
        }
    }
}
