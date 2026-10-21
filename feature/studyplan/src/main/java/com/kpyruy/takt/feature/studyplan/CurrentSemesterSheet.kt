package com.kpyruy.takt.feature.studyplan

import com.kpyruy.takt.core.model.uiArg
import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrentSemesterSheet(
    semesters: Map<Int, Int>,
    currentSemester: Int?,
    manualSemester: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(manualSemester) { mutableStateOf(manualSemester) }
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 28.dp).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(t(UiKey.TEXT_CURRENT_SEMESTER), style = MaterialTheme.typography.headlineSmall)
            GlassSurface(
                modifier = Modifier.fillMaxWidth().testTag("select-current-semester-auto").selectable(selected == null, role = Role.RadioButton, onClick = { selected = null }),
                shape = RoundedCornerShape(18.dp),
                tint = if (selected == null) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Unspecified,
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected == null, onClick = null)
                    Column(Modifier.weight(1f)) {
                        Text(t(UiKey.TEXT_AUTOMATICALLY), style = MaterialTheme.typography.titleSmall)
                        currentSemester?.let { Text(t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE, it)), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    if (selected == null) Icon(Icons.Outlined.Check, contentDescription = t(UiKey.TEXT_CURRENT),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            semesters.toSortedMap().forEach { (semester, activeCourses) ->
                val isSelected = semester == selected
                GlassSurface(
                    modifier = Modifier.fillMaxWidth().testTag("select-current-semester-$semester").selectable(isSelected, role = Role.RadioButton, onClick = { selected = semester }),
                    shape = RoundedCornerShape(18.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Unspecified,
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isSelected, onClick = null)
                        Column(Modifier.weight(1f)) {
                            Text(t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE_VALUE, semester, uiArg(if (semester % 2 == 1) UiKey.TEXT_WINTER_30C5461F else UiKey.TEXT_SUMMER_E8366425))), style = MaterialTheme.typography.titleSmall)
                            if (activeCourses > 0) Text(activeCoursesLabel(activeCourses),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) Icon(Icons.Outlined.Check, contentDescription = t(UiKey.TEXT_CURRENT),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            TaktPrimaryButton(onClick = { onSelect(selected) }, modifier = Modifier.fillMaxWidth()) {
                Text(t(UiKey.TEXT_APPLY))
            }
        }
    }
}
