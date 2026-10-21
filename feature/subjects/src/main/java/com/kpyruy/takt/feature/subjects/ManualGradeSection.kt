package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kpyruy.takt.core.model.GradeLetter
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun ManualGradeSection(
    manualGrade: GradeLetter?,
    onManualGradeChange: (GradeLetter?) -> Unit,
) {
    var autoEnabled by remember(manualGrade) { mutableStateOf(manualGrade == null) }
    var menuOpen by remember { mutableStateOf(false) }
    SectionCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(t("Final grade"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(t("Auto"), style = MaterialTheme.typography.bodyMedium)
            Switch(checked = autoEnabled, onCheckedChange = { enabled ->
                autoEnabled = enabled
                if (enabled) onManualGradeChange(null)
            })
        }
        if (!autoEnabled) {
            androidx.compose.foundation.layout.Box {
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                TextButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(manualGrade?.name ?: t("Choose a grade"), Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                }
                TaktChoiceMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    GradeLetter.entries.forEach { grade ->
                        TaktChoiceMenuItem(text = { Text(grade.name) }, onClick = {
                            menuOpen = false
                            onManualGradeChange(grade)
                        })
                    }
                }
            }
        }
    }
}
