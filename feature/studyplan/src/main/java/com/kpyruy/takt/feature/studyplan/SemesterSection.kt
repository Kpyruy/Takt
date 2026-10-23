package com.kpyruy.takt.feature.studyplan

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.kpyruy.takt.core.ui.components.EntityIconTile
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
        count % 100 in 11..14 -> t(UiKey.TEXT_ACTIVE_SUBJECTS)
        count % 10 == 1 -> t(UiKey.TEXT_ACTIVE_SUBJECT_4D88CC7D)
        count % 10 in 2..4 -> t(UiKey.TEXT_CURRENTLY_ACTIVE_SUBJECTS)
        else -> t(UiKey.TEXT_ACTIVE_SUBJECTS)
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
    compactPreview: Boolean = false,
) {
    var showAllCourses by rememberSaveable(courses.map { it.id }) { mutableStateOf(false) }
    val displayedCourses = if (compactPreview && !showAllCourses) courses.take(4) else courses
    val state = semesterState(courses, isCurrent)
    val earnedCredits = courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }
    val relevantCredits = courses.filter { it.status != CourseStatus.NOT_NEEDED }.sumOf { it.credits }
    val activeCount = courses.count { it.status == CourseStatus.ENROLLED }
    val stateLabel = when (state) {
        SemesterState.COMPLETED -> t(UiKey.TEXT_CLOSED_C3EEFB58)
        SemesterState.ACTIVE -> t(UiKey.TEXT_CURRENT_SEMESTER_A9B3EA6D)
        SemesterState.FUTURE -> when {
            activeCount > 0 -> activeCoursesLabel(activeCount)
            courses.any { it.status == CourseStatus.FULFILLED } -> t(UiKey.TEXT_PARTIALLY_COMPLETED)
            else -> t(UiKey.TEXT_PLANNED)
        }
    }
    GlassSurface(Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(if (compactPreview) 23.dp else 20.dp)) {
        Column(Modifier.padding(horizontal = if (compactPreview) 17.dp else 12.dp, vertical = if (compactPreview) 13.dp else 9.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("semester-toggle-$semester")
                    .clickable(role = Role.Button, onClickLabel = if (expanded) t(UiKey.TEXT_COLLAPSE_THE_SEMESTER) else t(UiKey.TEXT_EXPAND_THE_SEMESTER)) { onExpandedChange(!expanded) }
                    .semantics { stateDescription = if (expanded) t(UiKey.TEXT_EXPANDED) else t(UiKey.TEXT_COLLAPSED) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val circleColor = when (state) {
                    SemesterState.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                    SemesterState.ACTIVE -> MaterialTheme.colorScheme.primary
                    SemesterState.FUTURE -> MaterialTheme.colorScheme.surfaceVariant
                }
                if (compactPreview) EntityIconTile(MaterialTheme.colorScheme.primary, Modifier.size(39.dp)) { tint ->
                    if (state == SemesterState.COMPLETED) Icon(Icons.Default.Check, t(UiKey.TEXT_COMPLETED_SEMESTER), Modifier.size(18.dp), tint = tint)
                    else Text("$semester", color = tint, fontWeight = FontWeight.SemiBold)
                }
                else Box(Modifier.size(30.dp).background(circleColor, CircleShape)
                    .testTag("semester-$semester-${state.name.lowercase()}")
                    .semantics { stateDescription = stateLabel }, contentAlignment = Alignment.Center) {
                    if (state == SemesterState.COMPLETED) Icon(Icons.Default.Check, t(UiKey.TEXT_COMPLETED_SEMESTER), Modifier.size(17.dp), tint = MaterialTheme.colorScheme.secondary)
                    else Text("$semester", color = if (state == SemesterState.ACTIVE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE, semester)) + if (state == SemesterState.ACTIVE) {
                        t(if (semester % 2 == 1) UiKey.TEXT_WINTER_11577647 else UiKey.TEXT_SUMMER_16945261)
                    } else "",
                        style = MaterialTheme.typography.titleMedium)
                    if (compactPreview && isCurrent) Text(t(UiKey.TEXT_CURRENT_SEMESTER_A9B3EA6D), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else if (activeCount > 0) Text(activeCoursesLabel(activeCount), modifier = Modifier.testTag("semester-active-count-$semester"), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (state == SemesterState.COMPLETED) t(uiMessage(UiKey.FORMAT_VALUE_VALUE_CREDITS, earnedCredits, relevantCredits))
                    else t(uiMessage(UiKey.FORMAT_VALUE_CREDITS_6F9D64BE, relevantCredits)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 9.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    displayedCourses.forEach { course ->
                        ProgressCourseRow(course, gradeRepository, onOpen = { onCourseClick(course.id) },
                            onStatus = { onCourseStatus(course) }, compactPresentation = compactPreview)
                        if (course != displayedCourses.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (compactPreview && !showAllCourses && courses.size > displayedCourses.size) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        androidx.compose.material3.TextButton(onClick = { showAllCourses = true }, modifier = Modifier.testTag("studies-more-courses-$semester")) {
                            Text(t(uiMessage(UiKey.STUDIES_MORE_COURSES, courses.size - displayedCourses.size)))
                        }
                    }
                }
            }
        }
    }
}
