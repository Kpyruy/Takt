package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.model.Course

@Composable
fun SubjectCard(course: Course, progressLine: String, onClick: () -> Unit, earned: Double = 0.0, maximum: Double = 0.0, scoreLabel: String = "За семестр") {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SubjectMonogram(course)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(course.title, Modifier.weight(1f), fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp))
                }
                SmallText(progressLine)
                if (maximum > 0) {
                    LinearProgressIndicator(gapSize = 0.dp, drawStopIndicator = {}, progress = { (earned / maximum).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SmallText(scoreLabel)
                        Text("${earned.displayNumber()} / ${maximum.displayNumber()}", fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
