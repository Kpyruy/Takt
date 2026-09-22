package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Шкала предмета", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Вкажи мінімальний відсоток для кожної оцінки.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            listOf(
                Triple("A", a) { value: String -> a = value },
                Triple("B", b) { value: String -> b = value },
                Triple("C", c) { value: String -> c = value },
                Triple("D", d) { value: String -> d = value },
                Triple("E", e) { value: String -> e = value },
            ).chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { (label, value, setter) ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = {
                                setter(it)
                                error = null
                            },
                            label = { Text(label) },
                            suffix = { Text("%") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = error != null,
                        )
                    }
                    if (rowItems.size == 1) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }

            Text("FX · 0%–нижче межі E")

            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    val values = listOf(a, b, c, d, e).map {
                        it.replace(',', '.').toDoubleOrNull()
                    }
                    if (values.any { it == null }) {
                        error = "Усі межі мають бути числами."
                        return@Button
                    }
                    val parsed = values.filterNotNull()
                    val valid = parsed[0] <= 100.0 &&
                        parsed[0] > parsed[1] &&
                        parsed[1] > parsed[2] &&
                        parsed[2] > parsed[3] &&
                        parsed[3] > parsed[4] &&
                        parsed[4] > 0.0

                    if (!valid) {
                        error = "Межі мають спадати: A > B > C > D > E > 0."
                        return@Button
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Зберегти шкалу")
            }
        }
    }
}

private fun Double.compact(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
