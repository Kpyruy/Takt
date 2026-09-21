package com.kpyruy.takt.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupPayloadCodecTest {
    @Test
    fun backupPayload_roundTripsThroughJson() {
        val payload = BackupPayload(
            version = 1,
            courses = listOf(
                BackupCourse(
                    id = "FYZI_6B",
                    code = "FYZI_6B",
                    title = "Fyzika",
                    credits = 5,
                    semester = 3,
                    status = "enrolled",
                    requirementType = "COMPULSORY",
                    syllabusUrl = null,
                )
            ),
            settings = BackupSettings(
                cancellationStyle = "HIDDEN",
                showHiddenLessons = true,
                parityOverride = "ODD",
            ),
        )

        val restored = BackupPayloadCodec.decode(BackupPayloadCodec.encode(payload))

        assertEquals(payload, restored)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedBackupVersion_isRejected() {
        BackupPayloadCodec.decode("""{"version":999}""")
    }
}
