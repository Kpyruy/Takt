package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.CancellationException

@Composable
internal fun CourseTeachersPanel(
    course: Course,
    repository: CourseTeachingRepository?,
    language: AppLanguage,
    connected: Boolean,
    authenticate: (String, (DeviceAuthenticationResult) -> Unit) -> Unit,
    onSettings: () -> Unit,
) {
    var teachers by remember(course.id, connected) { mutableStateOf(repository?.savedTeachers(course)) }
    var error by remember(course.id, connected) { mutableStateOf<Throwable?>(null) }
    var loading by remember { mutableStateOf(false) }
    var reload by remember(course.id) { mutableIntStateOf(0) }
    var selected by remember(course.id, connected) { mutableStateOf<CourseTeacher?>(null) }
    val hasSyllabus = course.syllabusUrl != null || course.id.startsWith("uis:")
    LaunchedEffect(course.id, repository, connected) {
        repository?.changes?.collect { teachers = repository.savedTeachers(course) }
    }
    LaunchedEffect(course.id, course.syllabusUrl, repository, connected, reload) {
        if (repository == null || !connected || !hasSyllabus) return@LaunchedEffect
        loading = true; error = null
        try { teachers = repository.teachers(course, refresh = true) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure }
        finally { loading = false }
    }
    SubjectPanel(modifier = Modifier.testTag("course-teachers")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(t("Teaching team"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (repository != null && connected && hasSyllabus) TaktIconButton(Icons.Outlined.Refresh,
                t("Refresh teaching team"), { if (!loading) reload++ }, Modifier.testTag("teachers-refresh"))
        }
        when {
            !hasSyllabus -> SmallText(t("Teaching staff will appear after importing this subject from UIS."))
            (!connected || repository == null) && teachers == null -> {
                SmallText(t("Connect UIS to view teaching staff and contacts."))
                TextButton(onClick = onSettings) { Text(t("Open settings")) }
            }
            loading && teachers == null -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 16.dp))
            teachers?.isEmpty() == true && error == null -> SmallText(t("No teaching staff are listed in the syllabus."))
        }
        teachers.orEmpty().forEachIndexed { index, teacher ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().testTag("course-teacher-${teacher.personId}")
                .clickable { selected = teacher }.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TeacherAvatar(teacher.name, Modifier.size(46.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(teacher.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(teacher.roles.joinToString(" · ", transform = ::teacherRoleLabel),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Outlined.ChevronRight, t("Lecturer profile"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        error?.let { failure ->
            TeacherLoadProblem(failure, authenticate, { reload++ }, onSettings)
        }
    }
    selected?.let { teacher ->
        if (repository != null) TeacherProfileSheet(teacher, repository, language, authenticate,
            onDismiss = { selected = null }, onSettings = { selected = null; onSettings() })
    }
}

internal fun teacherRoleLabel(role: String): String = t(when (role.lowercase()) {
    "examiner", "skúšajúci", "zkoušející" -> "Examiner"
    "instructor", "cvičiaci", "cvičící" -> "Tutorials"
    "lecturer", "prednášajúci", "přednášející" -> "Lectures"
    "person responsible for course", "garant predmetu", "garant předmětu", "supervisor" -> "Course supervisor"
    else -> role
})

@Composable
internal fun TeacherLoadProblem(
    failure: Throwable,
    authenticate: (String, (DeviceAuthenticationResult) -> Unit) -> Unit,
    retry: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(t(when (failure) {
            is TeachingAuthenticationRequired -> "Confirm UIS access to load contacts."
            is TeachingSessionUnavailable -> "Sign in to UIS or enter the verification code in settings."
            else -> "Could not load contacts. Check your connection and try again."
        }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = {
            when (failure) {
                is TeachingAuthenticationRequired -> authenticate(t("UIS teaching contacts")) { if (it == DeviceAuthenticationResult.SUCCESS) retry() }
                is TeachingSessionUnavailable -> onSettings()
                else -> retry()
            }
        }, modifier = Modifier.testTag("teacher-retry")) {
            Text(t(when (failure) {
                is TeachingAuthenticationRequired -> "Confirm access"
                is TeachingSessionUnavailable -> "Open settings"
                else -> "Try again"
            }))
        }
    }
}

@Composable
internal fun TeacherAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = name.split(' ').map { it.trim('.', ',') }
        .filter { it.isNotBlank() && it.lowercase() !in setOf("ing", "prof", "mgr", "phd", "doc", "rndr", "csc") }
        .take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
    GlassSurface(modifier, shape = RoundedCornerShape(16.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(initials, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold)
        }
    }
}
