package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.HomeWorkFilter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object HomeWorkFilterCodec {
    fun encode(filter: HomeWorkFilter): String = Json.encodeToString(filter.toBackup())

    fun decode(raw: String?): HomeWorkFilter = raw?.let {
        runCatching { Json.decodeFromString<BackupHomeWorkFilter>(it).toModel() }.getOrNull()
    } ?: HomeWorkFilter()
}
