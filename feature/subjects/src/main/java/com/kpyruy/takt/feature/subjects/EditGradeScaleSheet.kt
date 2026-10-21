package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeBand
import com.kpyruy.takt.core.model.GradeLetter
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGradeScaleSheet(
    initialScale: GradeScale,
    onDismiss: () -> Unit,
    onSave: (GradeScale) -> Unit,
) {
    val initial = initialScale.bands.associate { it.grade to it.minimumPercentage }
    var a by remember(initialScale) { mutableStateOf(initial[GradeLetter.A]?.compact() ?: "92") }
    var b by remember(initialScale) { mutableStateOf(initial[GradeLetter.B]?.compact() ?: "83") }
    var c by remember(initialScale) { mutableStateOf(initial[GradeLetter.C]?.compact() ?: "74") }
    var d by remember(initialScale) { mutableStateOf(initial[GradeLetter.D]?.compact() ?: "65") }
    var e by remember(initialScale) { mutableStateOf(initial[GradeLetter.E]?.compact() ?: "56") }
    var error by remember { mutableStateOf<String?>(null) }

    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(t(UiKey.TEXT_ASSESSMENT_SCALE), style = MaterialTheme.typography.headlineMedium)
            Text(
                t(UiKey.TEXT_MINIMUM_PERCENTAGE),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SubjectPanel {
            listOf(
                Triple("A", a) { value: String -> a = value },
                Triple("B", b) { value: String -> b = value },
                Triple("C", c) { value: String -> c = value },
                Triple("D", d) { value: String -> d = value },
                Triple("E", e) { value: String -> e = value },
            ).forEach { (label, value, setter) ->
                GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)) {
                  Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GlassSurface(Modifier, shape = RoundedCornerShape(13.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
                        Text(label, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 21.sp)
                    }
                        Spacer(Modifier.weight(1f))
                        TaktTextField(
                            value = value,
                            onValueChange = {
                                setter(it)
                                error = null
                            },
                            modifier = Modifier.width(100.dp).semantics { contentDescription = t(UiKey.TEXT_MINIMUM_PERCENTAGE) + " · " + label },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            isError = error != null,
                        )
                  }
                }
            }
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("FX", style = MaterialTheme.typography.titleMedium)
                    Text(t(UiKey.TEXT_BELOW_THE_E_THRESHOLD), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SmallText(t(UiKey.TEXT_THRESHOLDS_APPLY_TO_POINTS_THAT_COUNT_TOWARD_THE_FINAL_GRADE))
            }

            if (error != null) {
                Text(
                    text = t(error.orEmpty()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              GlassSurface(Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
                TextButton(onClick = {
                    val defaults = GradeScale.default().bands.associate { it.grade to it.minimumPercentage }
                    a = defaults.getValue(GradeLetter.A).compact(); b = defaults.getValue(GradeLetter.B).compact()
                    c = defaults.getValue(GradeLetter.C).compact(); d = defaults.getValue(GradeLetter.D).compact()
                    e = defaults.getValue(GradeLetter.E).compact(); error = null
                }, modifier = Modifier.fillMaxWidth()) { Text(t(UiKey.TEXT_STANDARD_SCALE)) }
              }
              TaktPrimaryButton(
                onClick = {
                    val values = listOf(a, b, c, d, e).map {
                        it.replace(',', '.').toDoubleOrNull()
                    }
                    if (values.any { it == null }) {
                        error = uiMessage(UiKey.TEXT_ALL_LIMITS_MUST_BE_NUMBERS)
                        return@TaktPrimaryButton
                    }
                    val parsed = values.filterNotNull()
                    val valid = parsed[0] <= 100.0 &&
                        parsed[0] > parsed[1] &&
                        parsed[1] > parsed[2] &&
                        parsed[2] > parsed[3] &&
                        parsed[3] > parsed[4] &&
                        parsed[4] > 0.0

                    if (!valid) {
                        error = uiMessage(UiKey.TEXT_THE_LIMITS_SHOULD_DECREASE_A_B_C_D_E_0)
                        return@TaktPrimaryButton
                    }

                    onSave(
                        GradeScale(
                            listOf(
                                GradeBand(GradeLetter.A, parsed[0]),
                                GradeBand(GradeLetter.B, parsed[1]),
                                GradeBand(GradeLetter.C, parsed[2]),
                                GradeBand(GradeLetter.D, parsed[3]),
                                GradeBand(GradeLetter.E, parsed[4]),
                                GradeBand(GradeLetter.FX, 0.0),
                            )
                        )
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(t(UiKey.ACTION_SAVE))
            }
            }
        }
    }
}

private fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
