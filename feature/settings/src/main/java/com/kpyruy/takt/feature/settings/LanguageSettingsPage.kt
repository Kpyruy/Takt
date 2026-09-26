package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.ui.i18n.t

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun LanguageSettingsPage(
    selected: AppLanguage,
    onBack: () -> Unit,
    onSelected: (AppLanguage) -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(
            title = t("Мова застосунку"),
            navigation = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = t("Назад"))
                }
            },
        )
        listOf(
            AppLanguage.UKRAINIAN to t("Українська"),
            AppLanguage.ENGLISH to "English",
            AppLanguage.SLOVAK to "Slovenčina",
        ).forEach { (language, label) ->
            val active = selected == language
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelected(language) }
                    .testTag("language-${language.name}"),
                shape = MaterialTheme.shapes.medium,
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp,
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (active) Icon(Icons.Outlined.Check, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
