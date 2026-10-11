package com.kpyruy.takt.core.data

import android.content.Context
import com.kpyruy.takt.core.model.AppRelease
import com.kpyruy.takt.core.model.AppVersion

class SharedPreferencesAppReleaseStore(context: Context) : AppReleasePreferenceStore {
    private val preferences = context.applicationContext.getSharedPreferences("takt_app_release", Context.MODE_PRIVATE)

    override fun read(): AppReleasePreferences {
        val tag = preferences.getString("latest_tag", null)
        val page = preferences.getString("latest_page", null)
        val apk = preferences.getString("latest_apk", null)?.takeIf { GitHubReleaseClient.isTrustedReleaseUrl(it, apk = true, tag = tag) }
        val latest = if (tag != null && AppVersion.parse(tag) != null && page != null &&
            GitHubReleaseClient.isTrustedReleaseUrl(page, apk = false, tag = tag)) AppRelease(tag, page, apk) else null
        return AppReleasePreferences(
            automaticChecks = preferences.getBoolean("automatic", true), latest = latest,
            lastSuccessfulCheck = if (latest != null && preferences.contains("checked_at")) preferences.getLong("checked_at", 0) else null,
            dismissedVersion = preferences.getString("dismissed", null),
            retryAfter = if (preferences.contains("retry_after")) preferences.getLong("retry_after", 0L) else null,
        )
    }

    override fun write(value: AppReleasePreferences) {
        preferences.edit().putBoolean("automatic", value.automaticChecks)
            .putString("latest_tag", value.latest?.tag).putString("latest_page", value.latest?.releaseUrl)
            .putString("latest_apk", value.latest?.apkUrl).putString("dismissed", value.dismissedVersion)
            .apply { value.retryAfter?.let { putLong("retry_after", it) } ?: remove("retry_after") }
            .apply { value.lastSuccessfulCheck?.let { putLong("checked_at", it) } ?: remove("checked_at") }
            .apply()
    }
}
