package com.kpyruy.takt.core.data

import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.util.Base64

@Serializable
data class BackupWallpaper(val name: String, val jpegBase64: String, val sha256: String) {
    fun decodedBytes(): ByteArray {
        require(jpegBase64.length <= 32 * 1024 * 1024) { "Wallpaper is too large" }
        val bytes = Base64.getDecoder().decode(jpegBase64)
        require(bytes.size in 3..(24 * 1024 * 1024) && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) { "Invalid wallpaper image" }
        require(digest(bytes) == sha256) { "Wallpaper checksum mismatch" }
        return bytes
    }
    companion object {
        fun fromBytes(bytes: ByteArray, name: String = "wallpaper.jpg") = BackupWallpaper(name,
            Base64.getEncoder().encodeToString(bytes), digest(bytes))
        private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
