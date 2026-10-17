package com.kpyruy.takt.core.ui.i18n

import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Localizes platform/Material strings while retaining the original Activity wrapper chain. */
@Composable
internal fun TaktNativeLocale(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val original = LocalConfiguration.current
    val locale = TaktI18n.locale
    val configuration = remember(original, locale) {
        Configuration(original).apply { setLocales(LocaleList(locale)) }
    }
    val localized = remember(context, configuration) {
        val localizedResources = context.createConfigurationContext(configuration).resources
        object : ContextWrapper(context) {
            override fun getResources() = localizedResources
        }
    }
    CompositionLocalProvider(LocalConfiguration provides configuration, LocalContext provides localized, content = content)
}
