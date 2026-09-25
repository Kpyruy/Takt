package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.SemesterPeriod
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object SemesterPeriodsCodec {
    fun encode(periods: Map<Int, SemesterPeriod>): String = Json.encodeToString(periods.toBackup())

    fun decode(raw: String?): Map<Int, SemesterPeriod> = raw?.let {
        runCatching { Json.decodeFromString<List<BackupSemesterPeriod>>(it).toModel() }.getOrNull()
    } ?: emptyMap()
}
