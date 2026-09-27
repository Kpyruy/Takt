package com.kpyruy.takt.core.data

interface BackupRepository {
    suspend fun exportJson(): String
    suspend fun importJson(raw: String)

    suspend fun importSelectedJson(raw: String, expectedLocalRaw: String, fromBackup: Set<BackupSection>) {
        require(fromBackup.isNotEmpty()) { "Select at least one backup section" }
        val expected = BackupPayloadCodec.decode(expectedLocalRaw).copy(version = 0)
        val current = BackupPayloadCodec.decode(exportJson()).copy(version = 0)
        check(current == expected) { "Local data changed; review the backup again" }
        val incoming = BackupPayloadCodec.decode(raw)
        importJson(BackupPayloadCodec.encode(BackupMerger.merge(current, incoming, fromBackup)))
    }
}
