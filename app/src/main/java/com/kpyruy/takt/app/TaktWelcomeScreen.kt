package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.UiKey
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun TaktWelcomeScreen(onConnect: () -> Unit, onLocal: () -> Unit) {
    TaktGlassBackground {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.size(96.dp).glassSurface(), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.MenuBook, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(t(UiKey.TEXT_TAKT), style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp), fontWeight = FontWeight.Bold)
            Text(t(UiKey.TEXT_STUDY_AT_YOUR_OWN_PACE), style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            WelcomeFeature(Icons.Outlined.CalendarMonth, t(UiKey.TEXT_YOUR_DAILY_TIMETABLE),
                t(UiKey.TEXT_CLASSES_ROOMS_AND_CHANGES_IN_ONE_PLACE), Color(0xFF2671CC))
            WelcomeFeature(Icons.Outlined.Checklist, t(UiKey.TEXT_TASKS_AND_EXAM_ELIGIBILITY),
                t(UiKey.TEXT_TRACK_TASKS_DEADLINES_AND_EXAM_ELIGIBILITY), Color(0xFFAD721F))
            WelcomeFeature(Icons.Outlined.BarChart, t(UiKey.TEXT_GRADES_AND_PROGRESS),
                t(UiKey.TEXT_YOUR_GRADES_AND_ACADEMIC_PROGRESS), Color(0xFF21816B))
            Spacer(Modifier.height(6.dp))
            TaktPrimaryButton(onClick = onConnect, modifier = Modifier.fillMaxWidth().testTag("welcome-uis")) {
                Icon(Icons.Outlined.School, null)
                Spacer(Modifier.width(10.dp))
                Text(t(UiKey.TEXT_CONNECT_UIS_STU))
            }
            GlassSurface(Modifier.fillMaxWidth()) {
                TextButton(onClick = onLocal, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("welcome-local")) {
                    Icon(Icons.Outlined.Laptop, null)
                    Spacer(Modifier.width(10.dp))
                    Text(t(UiKey.TEXT_START_LOCALLY), style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Shield, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(t(UiKey.TEXT_YOUR_STUDY_DATA_STAYS_ON_THIS_DEVICE), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WelcomeFeature(icon: ImageVector, title: String, detail: String, tint: Color) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(54.dp).glassSurface(tint = tint.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(32.dp), tint = tint)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
