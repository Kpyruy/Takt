package com.kpyruy.takt.app

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun PlanningLoadingScreen() {
    val pulse = rememberInfiniteTransition(label = "planning-loading")
    val opacity = pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "planning-loading-opacity",
    ).value
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = opacity)
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(t("Завантаження розкладу"), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        LoadingBar(174.dp, 28.dp, placeholder)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(7) {
                Box(Modifier.weight(1f).height(56.dp).background(placeholder, RoundedCornerShape(8.dp)))
            }
        }
        Spacer(Modifier.height(8.dp))
        LoadingBar(120.dp, 18.dp, placeholder)
        repeat(3) { index ->
            Column(Modifier.fillMaxWidth().background(placeholder, RoundedCornerShape(16.dp))
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LoadingBar(if (index == 1) 210.dp else 164.dp, 15.dp,
                    MaterialTheme.colorScheme.surface.copy(alpha = opacity))
                LoadingBar(120.dp, 10.dp, MaterialTheme.colorScheme.surface.copy(alpha = opacity))
            }
        }
    }
}

@Composable
private fun LoadingBar(width: Dp, height: Dp, color: Color) {
    Box(Modifier.width(width).height(height).background(color, RoundedCornerShape(8.dp)))
}
