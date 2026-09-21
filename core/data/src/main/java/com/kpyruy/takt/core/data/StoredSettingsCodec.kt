package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CancellationDisplayStyle
import com.kpyruy.takt.core.model.ParityOverride

object StoredSettingsCodec {
    fun decode(
        cancellationStyle: String?,
        showHiddenLessons: Boolean,
        parityOverride: String?,
    ): AppSettings {
        val default = AppSettings()
        return AppSettings(
            cancellationStyle = enumValueOrNull<CancellationDisplayStyle>(cancellationStyle)
                ?: default.cancellationStyle,
            showHiddenLessons = showHiddenLessons,
            parityOverride = enumValueOrNull<ParityOverride>(parityOverride)
                ?: default.parityOverride,
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } }
}
