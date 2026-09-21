package com.kpyruy.takt.core.data

interface BackupRepository {
    suspend fun exportJson(): String
    suspend fun importJson(raw: String)
}
