package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.ui.i18n.t

/** Compact disclosures with full accessible descriptions and a short, evenly spaced row. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkRequirementIcons(
    requiredForExam: Boolean,
    minimumPointsForExam: Double?,
    admissionWarning: Boolean,
    modifier: Modifier = Modifier,
    contributesToGrade: Boolean = true,
    tagPrefix: String = "work-requirements",
) {
    FlowRow(modifier) {
        if (!contributesToGrade) {
            RequirementIcon(Icons.Outlined.Block, t(UiKey.WORK_EXCLUDED_FROM_GRADE_DESCRIPTION),
                "$tagPrefix-excluded")
        }
        if (requiredForExam) {
            RequirementIcon(Icons.Outlined.School, t(UiKey.WORK_REQUIRED_FOR_EXAM_DESCRIPTION),
                "$tagPrefix-required")
            minimumPointsForExam?.let { minimum ->
                val points = if (minimum % 1.0 == 0.0) minimum.toInt().toString() else minimum.toString()
                RequirementIcon(Icons.Outlined.Flag, t(uiMessage(UiKey.WORK_MINIMUM_FOR_EXAM, points)),
                    "$tagPrefix-minimum")
            }
            if (admissionWarning) {
                RequirementIcon(Icons.Outlined.WarningAmber, t(UiKey.WORK_ADMISSION_WARNING_DESCRIPTION),
                    "$tagPrefix-warning", warning = true)
            }
        }
    }
}

@Composable
private fun RequirementIcon(icon: ImageVector, description: String, tag: String, warning: Boolean = false) {
    var expanded by remember(description) { mutableStateOf(false) }
    Box {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 24.dp) {
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(width = 32.dp, height = 24.dp).testTag(tag)) {
                Icon(icon, contentDescription = description, modifier = Modifier.size(15.dp),
                    tint = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        TaktChoiceMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(description, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("$tag-description"), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
