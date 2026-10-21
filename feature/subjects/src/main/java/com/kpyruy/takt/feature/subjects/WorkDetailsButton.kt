package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.model.UiKey
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.TaktSurface
import com.kpyruy.takt.core.ui.components.taktPressFeedback
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun WorkDetailsButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    TaktSurface(modifier = Modifier.fillMaxWidth().taktPressFeedback(interaction).clip(shape)
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
        .testTag("grade-details-trigger"), shape = shape) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Tune, null)
            Column(Modifier.weight(1f)) {
                Text(t(UiKey.WORK_DETAILS), style = MaterialTheme.typography.titleSmall)
                Text(t(UiKey.TEXT_DATE_RESULT_AND_ASSESSMENT_RULES), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null)
        }
    }
}
