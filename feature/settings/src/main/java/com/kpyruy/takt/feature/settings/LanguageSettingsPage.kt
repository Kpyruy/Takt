package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.ui.i18n.t

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import androidx.compose.material3.Text
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.CourseNameLanguage
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun LanguageSettingsPage(
    selected: AppLanguage,
    courseNameLanguage: CourseNameLanguage,
    ukrainianFallback: CourseNameLanguage,
    onBack: () -> Unit,
    onSelected: (AppLanguage) -> Unit,
    onCourseNameSelected: (CourseNameLanguage) -> Unit,
    onUkrainianFallbackSelected: (CourseNameLanguage) -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(
            title = t("Language"),
            navigation = {
                TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t("Back"), onBack)
            },
        )
        Text(t("Application language"), style = MaterialTheme.typography.titleLarge)
        LanguageRadioGroup(
            options = listOf(AppLanguage.UKRAINIAN to t("Ukrainian"), AppLanguage.ENGLISH to "English", AppLanguage.SLOVAK to "Slovenčina"),
            selected = selected, tagPrefix = "language", onSelect = onSelected,
        )
        Text(t("Course name language"), style = MaterialTheme.typography.titleLarge)
        LanguageRadioGroup(
            options = listOf(CourseNameLanguage.FOLLOW_APP to t("Follow app language"), CourseNameLanguage.SLOVAK to "Slovenčina", CourseNameLanguage.ENGLISH to "English"),
            selected = courseNameLanguage, tagPrefix = "course-name", onSelect = onCourseNameSelected,
        )
        if (selected == AppLanguage.UKRAINIAN && courseNameLanguage == CourseNameLanguage.FOLLOW_APP) {
            Text(t("When the app is in Ukrainian"), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LanguageRadioGroup(
                options = listOf(CourseNameLanguage.SLOVAK to "Slovenčina", CourseNameLanguage.ENGLISH to "English"),
                selected = ukrainianFallback, tagPrefix = "course-fallback", onSelect = onUkrainianFallbackSelected,
            )
        }
    }
}

@Composable
private fun <T : Enum<T>> LanguageRadioGroup(options: List<Pair<T, String>>, selected: T, tagPrefix: String, onSelect: (T) -> Unit) {
    com.kpyruy.takt.core.ui.components.SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
        Column {
            options.forEachIndexed { index, (value, label) ->
                Row(Modifier.fillMaxWidth().testTag("$tagPrefix-${value.name}")
                    .selectable(selected = value == selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = { onSelect(value) })
                    .padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(selected = value == selected, onClick = null)
                    Text(label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
                if (index != options.lastIndex) androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            }
        }
    }
}
