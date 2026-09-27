package com.kpyruy.takt.core.data

/** A local edit wins until the owner explicitly accepts the UIS value. */
object UisProgressAutoMerge {
    fun shouldApply(local: String, remote: String, lastAppliedUis: String?,
        localResult: String? = null, remoteResult: String? = null,
        lastAppliedResult: String? = null): Boolean =
        lastAppliedUis != null && local == lastAppliedUis &&
            (localResult == null || (lastAppliedResult != null && localResult == lastAppliedResult)) &&
            (local != remote || (localResult != null && localResult != remoteResult))

    fun canRemember(local: String, remote: String): Boolean = local == remote
}
