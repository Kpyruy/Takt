package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val parsedCredits = credits.toIntOrNull()
    val parsedSemester = semester.toIntOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(title, { title = it; error = null }, label = { Text(t("Назва предмета")) },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("course-title"))
        OutlinedTextField(code, { code = it; error = null }, label = { Text(t("Код предмета")) },
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("course-code"))
        OutlinedTextField(credits, { credits = it.filter(Char::isDigit); error = null },
            label = { Text(t("Кредити")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("course-credits"))
        OutlinedTextField(semester, { semester = it.filter(Char::isDigit); error = null },
            label = { Text(t("Семестр")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("course-semester"))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Button(
            onClick = {
                saving = true
                scope.launch {
                    val result = runCatching {
                        repository.addCourse(title, code, parsedCredits!!, parsedSemester!!)
                    }
                    saving = false
                    if (result.isSuccess) withContext(Dispatchers.Main.immediate) { onSaved() }
                    else error = result.exceptionOrNull()?.message ?: t("Не вдалося додати предмет")
                }
            },
            enabled = !saving && title.isNotBlank() && code.isNotBlank() &&
                parsedCredits != null && parsedSemester != null,
            modifier = Modifier.fillMaxWidth().testTag("save-course"),
        ) { Text(t("Додати предмет")) }
    }
}
