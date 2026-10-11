package com.kpyruy.takt.core.model

import java.math.BigInteger

/** Stable semantic versions. Build metadata does not affect precedence. */
data class AppVersion private constructor(val major: BigInteger, val minor: BigInteger, val patch: BigInteger) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int =
        major.compareTo(other.major).takeIf { it != 0 }
            ?: minor.compareTo(other.minor).takeIf { it != 0 }
            ?: patch.compareTo(other.patch)

    companion object {
        private val pattern = Regex("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?")
        fun parse(raw: String): AppVersion? {
            val match = pattern.matchEntire(raw) ?: return null
            return AppVersion(match.groupValues[1].toBigInteger(), match.groupValues[2].toBigInteger(), match.groupValues[3].toBigInteger())
        }
    }
}

data class AppRelease(val tag: String, val releaseUrl: String, val apkUrl: String?) {
    val downloadUrl: String get() = apkUrl ?: releaseUrl
    fun isNewerThan(installedVersion: String): Boolean {
        val installed = AppVersion.parse(installedVersion) ?: return false
        val latest = AppVersion.parse(tag) ?: return false
        return latest > installed
    }
}
