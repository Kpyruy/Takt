package com.kpyruy.takt.feature.studyplan

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.data.GradeRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.kpyruy.takt.core.ui.components.CourseAvatar
import com.kpyruy.takt.core.ui.components.CourseGradeBadge
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface

internal fun CourseStatus.title() = when (this) {
    CourseStatus.FULFILLED -> t("Здано")
    CourseStatus.ENROLLED -> t("Активний")
    CourseStatus.PLANNED -> t("Заплановано")
    CourseStatus.NOT_ENROLLED -> t("Не записаний")
    CourseStatus.NOT_NEEDED -> t("Не потрібний")
}
internal fun CourseStatus.icon(): ImageVector = when (this) {
    CourseStatus.FULFILLED -> Icons.Outlined.CheckCircle
    CourseStatus.ENROLLED -> Icons.Outlined.Schedule
    CourseStatus.PLANNED -> Icons.Outlined.EventNote
    CourseStatus.NOT_ENROLLED -> Icons.Outlined.RemoveCircleOutline
    CourseStatus.NOT_NEEDED -> Icons.Outlined.Block
}
@Composable internal fun CourseStatus.tint(): Color = when (this) {
    CourseStatus.FULFILLED -> MaterialTheme.colorScheme.secondary
    CourseStatus.ENROLLED -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
internal fun ProgressCourseRow(course: Course, gradeRepository: GradeRepository, onOpen: () -> Unit, onStatus: () -> Unit) {
    val manualGrade = if (course.officialGrade == null) remember(gradeRepository, course.id) { gradeRepository.observeManualGrade(course.id) }
        .collectAsStateWithLifecycle(initialValue = null).value else null
    val grade = course.officialGrade ?: manualGrade
    // Reserve the same date-capable column for every status, including icon-only rows.
    val statusColumnWidth = 88.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 8.dp).testTag("progress-course-${course.id}"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.weight(1f).clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CourseAvatar(course, Modifier.size(36.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(course.title, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(t("${course.credits} кредитів"), fontSize = 12.sp, lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.width(statusColumnWidth).heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onStatus).testTag("status-${course.id}")
                .semantics { contentDescription = t("Змінити статус: ${course.title}"); stateDescription = course.status.title() },
            contentAlignment = Alignment.Center) {
            if (grade != null) CourseGradeBadge(grade, course.fulfilledOn.takeIf { course.officialGrade != null }, Modifier.fillMaxWidth())
            else GlassSurface(Modifier.size(42.dp), shape = RoundedCornerShape(11.dp),
                tint = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(course.status.icon(), null, Modifier.size(20.dp), tint = course.status.tint())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseStatusSheet(course: Course, uisManaged: Boolean = false, onStatus: (CourseStatus) -> Unit, onIcon: () -> Unit, onDismiss: () -> Unit) {
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(course.title, style = MaterialTheme.typography.headlineSmall)
            Text(t(if (uisManaged) "${course.semester} семестр" else "${course.semester} семестр · статус можна змінити будь-коли"),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            if (uisManaged) Text(t("Статус предмета визначає UIS"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!uisManaged) CourseStatus.entries.forEach { status ->
                Surface(onClick = { onStatus(status) }, modifier = Modifier.fillMaxWidth().testTag("choose-status-${status.name}"),
                    color = if (status == course.status) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(status.icon(), null, Modifier.size(22.dp), tint = status.tint())
                        Text(status.title(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        if (status == course.status) Icon(Icons.Outlined.Check, t("Обрано"), Modifier.size(20.dp))
                    }
                }
            }
            TextButton(onClick = onIcon, modifier = Modifier.fillMaxWidth()) { Text(t("Змінити іконку предмета")) }
        }
    }
}
