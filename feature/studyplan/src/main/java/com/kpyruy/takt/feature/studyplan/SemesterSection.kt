package com.kpyruy.takt.feature.studyplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus

internal enum class SemesterState { COMPLETED, ACTIVE, FUTURE }

internal fun semesterState(courses: List<Course>): SemesterState = when {
    courses.isNotEmpty() && courses.all { it.status == CourseStatus.FULFILLED || it.status == CourseStatus.NOT_NEEDED } && courses.any { it.status == CourseStatus.FULFILLED } -> SemesterState.COMPLETED
    courses.any { it.status == CourseStatus.ENROLLED } -> SemesterState.ACTIVE
    else -> SemesterState.FUTURE
}

@Composable
fun SemesterSection(
    semester: Int,
    courses: List<Course>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCourseClick: (String) -> Unit,
    onCourseStatus: (Course) -> Unit,
    nextAction: (@Composable () -> Unit)? = null,
) {
    val state = semesterState(courses)
    val doneCount = courses.count { it.status == CourseStatus.FULFILLED }
    val relevantCount = courses.count { it.status != CourseStatus.NOT_NEEDED }
    val activeCount = courses.count { it.status == CourseStatus.ENROLLED }
    val stateLabel = when (state) {
        SemesterState.COMPLETED -> "закрито"
        SemesterState.ACTIVE -> "$activeCount активних предметів"
        SemesterState.FUTURE -> if (courses.any { it.status == CourseStatus.FULFILLED }) "частково виконано" else "заплановано"
    }
    val line = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val paper = MaterialTheme.colorScheme.surface
    Row(
        Modifier.fillMaxWidth().drawBehind {
            drawLine(line, Offset(10.dp.toPx(), 0f), Offset(10.dp.toPx(), size.height), 1.dp.toPx())
        }.padding(bottom = 17.dp),
    ) {
        Box(Modifier.width(36.dp).padding(top = 5.dp), contentAlignment = Alignment.TopStart) {
            Box(
                modifier = Modifier.padding(start = 2.dp).size(17.dp)
                    .background(if (state == SemesterState.FUTURE) paper else accent, CircleShape)
                    .border(if (state == SemesterState.ACTIVE) 2.dp else 1.5.dp, if (state == SemesterState.FUTURE) line else accent, CircleShape)
                    .testTag("semester-$semester-${state.name.lowercase()}")
                    .semantics { stateDescription = stateLabel },
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    SemesterState.COMPLETED -> Icon(Icons.Default.Check, "Завершений семестр", Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    SemesterState.ACTIVE -> Icon(Icons.Default.Schedule, "Активний семестр", Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    SemesterState.FUTURE -> Unit
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("semester-toggle-$semester")
                    .clickable(role = Role.Button, onClickLabel = if (expanded) "Згорнути семестр" else "Розгорнути семестр") { onExpandedChange(!expanded) }
                    .semantics { stateDescription = if (expanded) "Розгорнуто" else "Згорнуто" },
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("$semester семестр", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, lineHeight = 18.sp))
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state == SemesterState.ACTIVE) {
                        Text("зараз", Modifier.background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = accent)
                    }
                }
                Text(
                    "${if (semester % 2 == 1) "Зимовий" else "Літній"} · $stateLabel",
                    Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Здано $doneCount / $relevantCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text("${courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }} кр.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { if (relevantCount == 0) 0f else doneCount.toFloat() / relevantCount },
                modifier = Modifier.fillMaxWidth().height(3.dp), color = MaterialTheme.colorScheme.secondary,
                trackColor = line, gapSize = 0.dp, drawStopIndicator = {})
            nextAction?.invoke()
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    Text("${courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }} / ${courses.sumOf { it.credits }} кредитів", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    courses.forEach { course ->
                        HorizontalDivider(Modifier.padding(top = 8.dp), color = line)
                        ProgressCourseRow(course, onOpen = { onCourseClick(course.id) }, onStatus = { onCourseStatus(course) })
                    }
                }
            }
        }
    }
}
