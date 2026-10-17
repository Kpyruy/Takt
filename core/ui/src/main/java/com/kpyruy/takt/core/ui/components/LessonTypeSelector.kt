package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.ui.i18n.t

/** The same selected-value popup for event, lesson and Quick Add forms. */
@Composable
fun LessonTypeSelector(
    value: LessonType,
    modifier: Modifier = Modifier,
    tagPrefix: String = "lesson-type",
    onChange: (LessonType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("Тип заняття"), style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            TaktSurface(
                modifier = Modifier.fillMaxWidth().taktPressFeedback(interaction).clip(shape)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button,
                        onClick = { expanded = true })
                    .testTag("$tagPrefix-trigger"),
                shape = shape,
            ) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LessonChoiceIcon(value)
                    Text(t(value.label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.Outlined.ExpandMore, null)
                }
            }
            TaktChoiceMenu(expanded, onDismissRequest = { expanded = false }) {
                LessonType.entries.forEach { option ->
                    TaktChoiceMenuItem(
                        text = { Text(t(option.label)) },
                        modifier = Modifier.testTag("$tagPrefix-${option.name}")
                            .semantics { selected = value == option },
                        leadingIcon = { LessonChoiceIcon(option) },
                        trailingIcon = { if (value == option) Icon(Icons.Outlined.Check, null) },
                        onClick = { onChange(option); expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonChoiceIcon(type: LessonType) {
    if (type == LessonType.UNSPECIFIED) Icon(Icons.Outlined.HelpOutline, null)
    else LessonTypeIcon(type, Modifier.size(24.dp))
}

@Composable
fun LessonTypeIcon(type: LessonType, modifier: Modifier = Modifier.size(16.dp)) {
    val vector = when (type) {
        LessonType.UNSPECIFIED -> return
        LessonType.LECTURE -> Icons.Outlined.School
        LessonType.SEMINAR -> Icons.Outlined.Forum
        LessonType.PRACTICE -> Icons.Outlined.EditNote
        LessonType.LAB -> Icons.Outlined.Science
        LessonType.EVENT -> Icons.Outlined.Event
    }
    Icon(vector, t(type.label), modifier, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
