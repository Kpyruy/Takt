package com.kpyruy.takt.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.CustomColorTheme
import com.kpyruy.takt.core.model.normalizeThemeHex
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import com.kpyruy.takt.core.ui.components.TaktTextField
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktHsvColor
import kotlin.math.roundToInt

@Composable
internal fun PaletteColorPicker(
    role: PaletteRole,
    initialHex: String,
    previewSettings: AppSettings,
    previewTheme: CustomColorTheme,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    var hsv by remember { mutableStateOf(checkNotNull(TaktHsvColor.fromHex(initialHex))) }
    var hexInput by remember { mutableStateOf(initialHex) }
    val liveHex = hsv.toHex()
    val liveSettings = previewSettings.withPreview(previewTheme.withColor(role, liveHex))
    val roleLabel = role.label()
    val hueLabel = t("Hue")
    val saturationLabel = t("Saturation")
    val brightnessLabel = t("Brightness")
    fun change(color: TaktHsvColor) { hsv = color; hexInput = color.toHex() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(28.dp)
        GlassSurface(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 16.dp)
            .fillMaxHeight(.92f).clip(shape).background(MaterialTheme.colorScheme.surface.copy(alpha = .90f))
            .testTag("theme-color-picker"), shape = shape) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Text(roleLabel, Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleLarge)
                // The sample remains visible while the controls below it scroll or drag.
                AppearanceLivePreview(liveSettings, compact = true)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                    .testTag("theme-picker-controls"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Spacer(Modifier.height(8.dp))
                    val planeLabel = t("Saturation and brightness")
                    SaturationValuePlane(hsv, { change(hsv.copy(saturation = it.x, value = it.y)) },
                        Modifier.fillMaxWidth().height(112.dp).clip(MaterialTheme.shapes.medium)
                            .semantics { contentDescription = planeLabel }.testTag("theme-picker-sv"))
                    Text(hueLabel + " · ${hsv.hue.roundToInt()}°", style = MaterialTheme.typography.labelLarge)
                    val spectrum = remember { listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red) }
                    Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxWidth().height(10.dp).clip(MaterialTheme.shapes.small)
                            .background(Brush.horizontalGradient(spectrum)))
                        Slider(hsv.hue, { change(hsv.copy(hue = it)) }, valueRange = 0f..360f,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = hueLabel }.testTag("theme-picker-hue"),
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
                    }
                    Text(saturationLabel + " · ${(hsv.saturation * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                    Slider(hsv.saturation, { change(hsv.copy(saturation = it)) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = saturationLabel }.testTag("theme-picker-saturation"))
                    Text(brightnessLabel + " · ${(hsv.value * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                    Slider(hsv.value, { change(hsv.copy(value = it)) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = brightnessLabel }.testTag("theme-picker-brightness"))
                    TaktTextField(hexInput, { raw ->
                        hexInput = raw
                        TaktHsvColor.fromHex(raw)?.let { hsv = it }
                    }, Modifier.fillMaxWidth().testTag("theme-picker-hex"), label = { Text("HEX") }, singleLine = true,
                        isError = normalizeThemeHex(hexInput) == null,
                        leadingIcon = { Box(Modifier.size(22.dp).background(Color.hsv(hsv.hue.coerceIn(0f, 360f), hsv.saturation, hsv.value), MaterialTheme.shapes.small)) },
                        supportingText = { Text(t("HEX: #RRGGBB, for example #B52D67")) })
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("theme-picker-cancel")) { Text(t("Cancel")) }
                    TaktPrimaryButton(onClick = { onApply(liveHex) }, enabled = normalizeThemeHex(hexInput) != null,
                        modifier = Modifier.weight(1f).testTag("theme-picker-apply")) { Text(t("Choose color")) }
                }
            }
        }
    }
}

/** Two GPU gradients compose the entire SV plane. Pointer movement changes three scalar values. */
@Composable
private fun SaturationValuePlane(hsv: TaktHsvColor, onChange: (Offset) -> Unit, modifier: Modifier = Modifier) {
    val latestChange by rememberUpdatedState(onChange)
    val hue = Color.hsv(hsv.hue.coerceIn(0f, 360f), 1f, 1f)
    fun normalized(point: Offset, width: Int, height: Int) = Offset(
        (point.x / width.coerceAtLeast(1)).coerceIn(0f, 1f),
        (1f - point.y / height.coerceAtLeast(1)).coerceIn(0f, 1f))
    Canvas(modifier.pointerInput(Unit) {
        detectTapGestures { latestChange(normalized(it, size.width, size.height)) }
    }.pointerInput(Unit) {
        detectDragGestures(onDragStart = { latestChange(normalized(it, size.width, size.height)) }) { change, _ ->
            change.consume()
            latestChange(normalized(change.position, size.width, size.height))
        }
    }) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hue)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val marker = Offset(hsv.saturation * size.width, (1f - hsv.value) * size.height)
        drawCircle(Color.Black, radius = 8.dp.toPx(), center = marker, style = Stroke(3.dp.toPx()))
        drawCircle(Color.White, radius = 8.dp.toPx(), center = marker, style = Stroke(1.5.dp.toPx()))
    }
}
