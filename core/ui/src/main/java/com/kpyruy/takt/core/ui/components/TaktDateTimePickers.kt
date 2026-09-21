package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val taktDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val taktTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaktDatePickerField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val initialMillis = value
        ?.atStartOfDay()
        ?.toInstant(ZoneOffset.UTC)
        ?.toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    OutlinedTextField(
        value = value?.format(taktDateFormatter).orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("Оберіть дату") },
        modifier = modifier.clickable { showPicker = true },
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onValueChange(LocalDate.ofEpochDay(millis / 86_400_000L))
                        }
                        showPicker = false
                    },
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Скасувати")
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaktTimePickerField(
    label: String,
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val state = rememberTimePickerState(
        initialHour = value.hour,
        initialMinute = value.minute,
        is24Hour = true,
    )

    OutlinedTextField(
        value = value.format(taktTimeFormatter),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = modifier.clickable { showPicker = true },
    )

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(LocalTime.of(state.hour, state.minute))
                        showPicker = false
                    },
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Скасувати")
                }
            },
            text = {
                Column {
                    TimePicker(state = state)
                }
            },
        )
    }
}
