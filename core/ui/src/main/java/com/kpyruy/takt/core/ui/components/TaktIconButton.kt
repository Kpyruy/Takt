package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun TaktIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp), enabled = enabled) {
        Box(Modifier.size(40.dp).glassSurface(CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .38f))
        }
    }
}
