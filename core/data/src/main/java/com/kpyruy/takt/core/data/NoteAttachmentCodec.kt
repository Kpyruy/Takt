package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.NoteAttachment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object NoteAttachmentCodec {
    fun encode(items: List<NoteAttachment>): String = Json.encodeToString(items.map { it.toBackup() })

    fun decode(raw: String): List<NoteAttachment> =
        Json.decodeFromString<List<BackupNoteAttachment>>(raw).map { it.toModel() }
}

internal fun NoteAttachment.toBackup() = BackupNoteAttachment(name, uri, mimeType)
internal fun BackupNoteAttachment.toModel() = NoteAttachment(name, uri, mimeType)
