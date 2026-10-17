package com.kpyruy.takt.core.data

/** A new optional photo marker or filename is not a data conflict; different pixels still are. */
internal fun sameDocumentBackupData(a: String, b: String): Boolean {
    fun normalized(raw: String): BackupPayload {
        val value = BackupPayloadCodec.decode(raw)
        return value.copy(version = 0, teaching = value.teaching?.takeIf { it.teams.isNotEmpty() || it.profiles.isNotEmpty() }, settings = value.settings.copy(
            wallpaperRecorded = true, wallpaper = value.settings.wallpaper?.copy(name = "")))
    }
    return normalized(a) == normalized(b)
}
