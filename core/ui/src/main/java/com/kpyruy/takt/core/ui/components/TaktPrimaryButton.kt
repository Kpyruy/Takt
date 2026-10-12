package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun TaktPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(percent = 50)
    val scheme = MaterialTheme.colorScheme
    val primary = if (scheme.background.luminance() < .3f) lerp(scheme.primary, scheme.primaryContainer, .72f) else scheme.primary
    // The top edge reflects light while the lower body retains contrast for white labels.
    val colors = if (enabled) listOf(lerp(primary, Color.White, .10f), primary, lerp(primary, Color.Black, .08f))
        else listOf(primary.copy(alpha = .32f), primary.copy(alpha = .24f))
    Button(
        onClick = onClick, enabled = enabled, shape = shape,
        modifier = modifier.heightIn(min = 52.dp)
            .shadow(if (enabled) 8.dp else 0.dp, shape, spotColor = primary.copy(alpha = .35f))
            .clip(shape).background(Brush.verticalGradient(colors))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .72f), Color.White.copy(alpha = .18f))), shape),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White,
            disabledContainerColor = Color.Transparent),
        contentPadding = contentPadding, content = content,
    )
}
