package com.kpyruy.takt.feature.studyplan

import com.kpyruy.takt.core.ui.i18n.t

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
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight

internal enum class SemesterState { COMPLETED, ACTIVE, FUTURE }

internal fun activeCoursesLabel(count: Int): String {
    val word = when {
        count % 100 in 11..14 -> t("активних предметів")
        count % 10 == 1 -> t("активний предмет")
        count % 10 in 2..4 -> t("активні предмети")
        else -> t("активних предметів")
    }
    return "$count $word"
}

internal fun semesterState(courses: List<Course>, isCurrent: Boolean = false): SemesterState = when {
    isCurrent -> SemesterState.ACTIVE
    courses.isNotEmpty() && courses.all { it.status == CourseStatus.FULFILLED || it.status == CourseStatus.NOT_NEEDED } && courses.any { it.status == CourseStatus.FULFILLED } -> SemesterState.COMPLETED
    else -> SemesterState.FUTURE
}

@Composable
fun SemesterSection(
    semester: Int,
    courses: List<Course>,
    gradeRepository: GradeRepository,
    isCurrent: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCourseClick: (String) -> Unit,
    onCourseStatus: (Course) -> Unit,
) {
    val state = semesterState(courses, isCurrent)
    val earnedCredits = courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }
    val relevantCredits = courses.filter { it.status != CourseStatus.NOT_NEEDED }.sumOf { it.credits }
    val activeCount = courses.count { it.status == CourseStatus.ENROLLED }
    val stateLabel = when (state) {
        SemesterState.COMPLETED -> t("закрито")
        SemesterState.ACTIVE -> t("поточний семестр")
        SemesterState.FUTURE -> when {
            activeCount > 0 -> activeCoursesLabel(activeCount)
            courses.any { it.status == CourseStatus.FULFILLED } -> t("частково виконано")
            else -> t("заплановано")
        }
    }
    GlassSurface(Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("semester-toggle-$semester")
                    .clickable(role = Role.Button, onClickLabel = if (expanded) t("Згорнути семестр") else t("Розгорнути семестр")) { onExpandedChange(!expanded) }
                    .semantics { stateDescription = if (expanded) t("Розгорнуто") else t("Згорнуто") },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val circleColor = when (state) {
                    SemesterState.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                    SemesterState.ACTIVE -> MaterialTheme.colorScheme.primary
                    SemesterState.FUTURE -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(Modifier.size(30.dp).background(circleColor, CircleShape)
                    .testTag("semester-$semester-${state.name.lowercase()}")
                    .semantics { stateDescription = stateLabel }, contentAlignment = Alignment.Center) {
                    if (state == SemesterState.COMPLETED) Icon(Icons.Default.Check, t("Завершений семестр"), Modifier.size(17.dp), tint = MaterialTheme.colorScheme.secondary)
                    else Text("$semester", color = if (state == SemesterState.ACTIVE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(t("$semester семестр") + if (state == SemesterState.ACTIVE) t(if (semester % 2 == 1) " · зимовий" else " · літній") else "",
                        style = MaterialTheme.typography.titleMedium)
                    if (state == SemesterState.ACTIVE) Text(activeCoursesLabel(activeCount), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (state == SemesterState.COMPLETED) t("$earnedCredits/$relevantCredits кредитів")
                    else t("$relevantCredits кредитів"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 9.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    courses.forEach { course ->
                        ProgressCourseRow(course, gradeRepository, onOpen = { onCourseClick(course.id) },
                            onStatus = { onCourseStatus(course) })
                        if (course != courses.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
