package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.components.TaktChoiceMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem
import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import com.kpyruy.takt.core.ui.components.CourseIcons
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.glassSurface
import com.kpyruy.takt.core.model.CourseGradingType
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.StudyPlanRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AddCourseForm(repository: StudyPlanRepository, onSaved: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("1") }
    var grading by remember { mutableStateOf(CourseGradingType.CONTINUOUS_LETTER) }
    var iconKey by remember { mutableStateOf<String?>(null) }
    var gradingMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val parsedCredits = credits.toIntOrNull()
    val parsedSemester = semester.toIntOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TaktTextField(title, { title = it; error = null }, label = { Text(t("Subject name")) },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("course-title"))
        TaktTextField(code, { code = it; error = null }, label = { Text(t("Subject code")) },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("course-code"))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaktTextField(credits, { credits = it.filter(Char::isDigit); error = null },
                label = { Text(t("Credits")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.weight(1f).testTag("course-credits"))
            TaktTextField(semester, { semester = it.filter(Char::isDigit); error = null },
                label = { Text(t("Semester")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.weight(1f).testTag("course-semester"))
        }
        Box {
            GlassSurface(Modifier.fillMaxWidth().clickable { gradingMenu = true }) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("Assessment"), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(gradingLabel(grading), style = MaterialTheme.typography.bodyLarge)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TaktChoiceMenu(expanded = gradingMenu, onDismissRequest = { gradingMenu = false }) {
                CourseGradingType.entries.forEach { value ->
                    TaktChoiceMenuItem(text = { Text(gradingLabel(value)) }, onClick = { grading = value; gradingMenu = false })
                }
            }
        }
        Text(t("Subject icon"), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Atom", "MenuBook", "Code", "Calculate", "Translate").forEach { key ->
                val icon = CourseIcons.find(key)
                val selected = iconKey == key
                Box(Modifier.weight(1f).size(52.dp)
                    .glassSurface(RoundedCornerShape(16.dp), if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .18f) else androidx.compose.ui.graphics.Color.Unspecified)
                    .clickable { iconKey = key }, contentAlignment = Alignment.Center) {
                    icon?.let { Icon(it.vector, t(it.label), Modifier.size(28.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        TaktPrimaryButton(
            onClick = {
                saving = true
                scope.launch {
                    val result = runCatching {
                        val createdId = repository.addCourse(title, code, parsedCredits!!, parsedSemester!!)
                        if (grading != CourseGradingType.CONTINUOUS_LETTER) repository.setGradingType(createdId, grading)
                        iconKey?.let { repository.setIcon(createdId, it) }
                    }
                    saving = false
                    if (result.isSuccess) withContext(Dispatchers.Main.immediate) { onSaved() }
                    else error = result.exceptionOrNull()?.message?.let(::t) ?: t("Failed to add subject")
                }
            },
            enabled = !saving && title.isNotBlank() && code.isNotBlank() &&
                parsedCredits != null && parsedSemester != null,
            modifier = Modifier.fillMaxWidth().testTag("save-course"),
        ) { Text(t("Add course")) }
    }
}

@Composable
private fun gradingLabel(type: CourseGradingType): String = t(when (type) {
    CourseGradingType.EXAM_LETTER -> "Exam · A–FX"
    CourseGradingType.CONTINUOUS_LETTER -> "Grade · A–FX"
    CourseGradingType.PASS_FAIL -> "Credit"
})
