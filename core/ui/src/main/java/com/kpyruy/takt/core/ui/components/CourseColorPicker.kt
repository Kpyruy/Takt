package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kpyruy.takt.core.model.CourseColor
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.i18n.tf

/** Deliberately bounded, portable palette with a distinct automatic reset. */
@Composable
fun CourseColorPicker(selectedKey: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t(UiKey.SUBJECT_COLOR), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, t(UiKey.SUBJECT_COLOR_CLOSE)) }
                }
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CourseColor.entries.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { option ->
                                val chosen = selectedKey == option.name
                                val color = Color(option.argb)
                                val label = tf(UiKey.SUBJECT_COLOR_OPTION, option.ordinal + 1)
                                Surface(Modifier.size(52.dp).testTag("course-color-${option.name}")
                                    .selectable(chosen, role = Role.RadioButton, onClick = { onSelect(option.name) })
                                    .semantics { contentDescription = label },
                                    shape = CircleShape, color = color,
                                    border = if (chosen) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (chosen) Icon(Icons.Outlined.Check, null, Modifier.size(24.dp),
                                            tint = if (color.luminance() > .45f) Color.Black else Color.White)
                                    }
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().testTag("course-color-automatic")
                        .selectable(selectedKey == null, role = Role.RadioButton, onClick = { onSelect(null) })
                        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selectedKey == null, onClick = null)
                        Text(t(UiKey.SUBJECT_COLOR_AUTOMATIC), Modifier.padding(start = 10.dp))
                    }
                }
            }
        }
    }
}
