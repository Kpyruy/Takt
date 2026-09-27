package com.kpyruy.takt.core.data

interface BackupRepository {
    suspend fun exportJson(): String
    suspend fun importJson(raw: String)

    suspend fun importSelectedJson(raw: String, expectedLocalRaw: String, fromBackup: Set<BackupSection>) {
        require(fromBackup.isNotEmpty()) { "Select at least one backup section" }
        val expected = BackupPayloadCodec.decode(expectedLocalRaw).copy(version = 0)
        val current = BackupPayloadCodec.decode(exportJson()).copy(version = 0)
        val incoming = BackupPayloadCodec.decode(raw)
        val merged = BackupMerger.merge(expected, incoming, fromBackup)
        if (current == merged.copy(version = 0)) return
        check(current == expected) { "Local data changed; review the backup again" }
        importJson(BackupPayloadCodec.encode(merged))
    }
}
