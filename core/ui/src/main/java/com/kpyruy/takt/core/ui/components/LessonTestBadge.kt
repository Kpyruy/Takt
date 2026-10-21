package com.kpyruy.takt.core.ui.components

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LessonTestBadge(compact: Boolean = false, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.glassSurface(MaterialTheme.shapes.extraSmall, MaterialTheme.colorScheme.primaryContainer).testTag("lesson-test-badge"),
        shape = MaterialTheme.shapes.extraSmall,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(Modifier.padding(horizontal = if (compact) 5.dp else 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.Quiz, contentDescription = t(UiKey.TEXT_TEST_IN_THIS_CLASS), modifier = Modifier.size(16.dp))
            if (!compact) Text(t(UiKey.TEXT_TEST_532EAABD), style = MaterialTheme.typography.labelSmall)
        }
    }
}
