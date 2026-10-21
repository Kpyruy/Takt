package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.ui.components.CourseAvatar
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseSettingsSheet(
    course: Course,
    uisManaged: Boolean = false,
    onDismiss: () -> Unit,
    onIcon: () -> Unit,
    onStatus: (CourseStatus) -> Unit,
    onGrading: (CourseGradingType) -> Unit,
    onResult: (PassFailResult?) -> Unit,
    onScale: () -> Unit,
) {
    var statusExpanded by remember { mutableStateOf(false) }
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("Subject settings"), style = MaterialTheme.typography.headlineSmall)
                    Text(course.title + " · " + course.code, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, t("Close subject settings")) }
            }
            SubjectPanel {
                Text(if (uisManaged) t("UIS data") else t("Subject"), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.weight(1f)) {
                        SmallText(t("Number of credits"))
                        Text(course.credits.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                    Column(Modifier.weight(1f)) {
                        SmallText(t("Semester"))
                        Text(course.semester.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            GlassSurface(Modifier.fillMaxWidth().clickable(onClick = onIcon), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CourseAvatar(course)
                    Text(t("Subject icon"), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsLabel(t("SUBJECT STATUS"))
                Surface(onClick = { statusExpanded = !statusExpanded },
                    enabled = !uisManaged,
                    modifier = Modifier.fillMaxWidth().testTag("settings-status-${course.status.name}"),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                    Row(Modifier.heightIn(min = 58.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        Icon(statusIcon(course.status), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(statusText(course.status), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (!uisManaged) Icon(if (statusExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            if (statusExpanded) t("Collapse statuses") else t("Change status"),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (uisManaged) Text(t("UIS determines this subject's status"), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                AnimatedVisibility(statusExpanded && !uisManaged) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CourseStatus.entries.filter { it != course.status }.forEach { status ->
                            Surface(onClick = { onStatus(status); statusExpanded = false },
                                modifier = Modifier.fillMaxWidth().testTag("settings-status-${status.name}"),
                                shape = RoundedCornerShape(13.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                Row(Modifier.heightIn(min = 50.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                                    Icon(statusIcon(status), null, Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(statusText(status), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsLabel(t("ASSESSMENT"))
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradingChoice("A–FX", course.gradingType != CourseGradingType.PASS_FAIL,
                        Modifier.weight(1f)) {
                        if (course.gradingType == CourseGradingType.PASS_FAIL) {
                            onGrading(CourseGradingType.CONTINUOUS_LETTER)
                        }
                    }
                    GradingChoice(t("Pass / fail"), course.gradingType == CourseGradingType.PASS_FAIL,
                        Modifier.weight(1f)) { onGrading(CourseGradingType.PASS_FAIL) }
                }
            }
            if (course.gradingType == CourseGradingType.PASS_FAIL) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsLabel(t("SUMMARY"))
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResultChoice(t("Passed"), course.passFailResult == PassFailResult.PASSED,
                            Modifier.weight(1f)) { onResult(PassFailResult.PASSED) }
                        ResultChoice(t("Failed"), course.passFailResult == PassFailResult.FAILED,
                            Modifier.weight(1f)) { onResult(PassFailResult.FAILED) }
                    }
                    if (course.passFailResult != null) {
                        TextButton(onClick = { onResult(null) }) { Text(t("Clear the result")) }
                    }
                }
            } else {
                GlassSurface(Modifier.fillMaxWidth().clickable(onClick = onScale), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Tune, null, Modifier.size(20.dp))
                        Text(t("Assessment scale"), Modifier.weight(1f).padding(start = 12.dp),
                            style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Outlined.ChevronRight, null)
                    }
                }
            }
            TaktPrimaryButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(t("Done")) }
        }
    }
}

@Composable
private fun GradingChoice(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick), shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.heightIn(min = 58.dp).padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ResultChoice(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick), shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Text(label, Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 13.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable private fun SettingsLabel(text: String) {
    Text(text, Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun statusText(status: CourseStatus) = when(status) {
    CourseStatus.FULFILLED -> t("Work completed")
    CourseStatus.ENROLLED -> t("Active subject")
    CourseStatus.PLANNED -> t("Scheduled")
    CourseStatus.NOT_ENROLLED -> t("Not recorded")
    CourseStatus.NOT_NEEDED -> t("Not needed")
}
private fun statusIcon(status: CourseStatus): ImageVector = when(status) {
    CourseStatus.FULFILLED -> Icons.Outlined.CheckCircle
    CourseStatus.ENROLLED -> Icons.Outlined.Schedule
    CourseStatus.PLANNED -> Icons.Outlined.Event
    CourseStatus.NOT_ENROLLED -> Icons.Outlined.RadioButtonUnchecked
    CourseStatus.NOT_NEEDED -> Icons.Outlined.RemoveCircleOutline
}
