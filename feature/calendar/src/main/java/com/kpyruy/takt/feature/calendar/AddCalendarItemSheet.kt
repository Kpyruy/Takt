package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarItemSheet(
    onDismiss: () -> Unit,
    onAddRecurring: () -> Unit,
    onAddOneOff: () -> Unit,
) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Що додати?")
            Button(
                onClick = onAddRecurring,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Repeat, contentDescription = null)
                Text("Повторювана пара")
            }
            OutlinedButton(
                onClick = onAddOneOff,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Event, contentDescription = null)
                Text("Разова подія / блокова акція")
            }
        }
    }
}
