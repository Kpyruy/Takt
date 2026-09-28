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
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.data.GradeRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import java.time.format.DateTimeFormatter
import com.kpyruy.takt.core.ui.components.CourseAvatar

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
    val manualGrade by remember(gradeRepository, course.id) { gradeRepository.observeManualGrade(course.id) }
        .collectAsStateWithLifecycle(initialValue = null)
    val grade = course.officialGrade ?: manualGrade
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp).testTag("progress-course-${course.id}"), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(onClick = onOpen).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CourseAvatar(course)
            Column(Modifier.weight(1f)) {
                Text(course.title, style = MaterialTheme.typography.titleSmall)
                Text(t("${course.semester} семестр · ${course.credits} кр."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (grade != null) Column(horizontalAlignment = Alignment.End) {
            Text(grade.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            course.fulfilledOn?.takeIf { course.officialGrade != null }?.let { Text(it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        val color = course.status.tint()
        TextButton(onClick = onStatus, modifier = Modifier.widthIn(max = if (grade == null) 110.dp else 48.dp).heightIn(min = 48.dp).testTag("status-${course.id}")
            .semantics { contentDescription = t("Змінити статус: ${course.title}"); stateDescription = course.status.title() }, contentPadding = PaddingValues(horizontal = 6.dp)) {
            Icon(course.status.icon(), null, Modifier.size(16.dp), tint = color)
            if (grade == null) {
                Spacer(Modifier.width(5.dp))
                Text(course.status.title(), style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CourseStatusSheet(course: Course, uisManaged: Boolean = false, onStatus: (CourseStatus) -> Unit, onIcon: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(course.title, style = MaterialTheme.typography.headlineSmall)
            Text(t(if (uisManaged) "${course.semester} семестр" else "${course.semester} семестр · статус можна змінити будь-коли"),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            if (uisManaged) Text(t("Статус предмета визначає UIS"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!uisManaged) CourseStatus.entries.forEach { status ->
                Surface(onClick = { onStatus(status) }, Modifier.fillMaxWidth().testTag("choose-status-${status.name}"),
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
