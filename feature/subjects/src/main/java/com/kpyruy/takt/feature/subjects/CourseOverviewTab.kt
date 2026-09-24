package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.ExamEligibility
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.GradeSummary
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.model.asScoredGradeItem
import com.kpyruy.takt.core.ui.components.SectionCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun CourseOverviewTab(
    course: Course,
    gradeItems: List<GradeItem>,
    gradeScale: GradeScale,
    tasks: List<StudyTask>,
    eligibility: ExamEligibility,
    onTaskCompleted: (StudyTask, Boolean) -> Unit,
    onEditTask: (StudyTask) -> Unit,
    onEditGrade: (GradeItem) -> Unit,
    onNotes: () -> Unit,
    onExam: () -> Unit,
) {
    val allGradedWork = gradeItems + tasks.mapNotNull { it.asScoredGradeItem() }
    val projection = GradeProjection.calculate(allGradedWork, gradeScale)
    val requiredTasks = tasks.filter { it.requiredForExam }.sortedBy { it.completed }
    val requiredGrades = gradeItems.filter { it.requiredForExam }.sortedBy { it.completed }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (course.gradingType == CourseGradingType.EXAM_LETTER) AdmissionProgressCard(eligibility)
        if (course.gradingType != CourseGradingType.PASS_FAIL) CourseworkProgressCard(allGradedWork, projection)
        else SubjectPanel {
            Text("Поточний результат", style = MaterialTheme.typography.titleMedium)
            Text(when(course.passFailResult) { PassFailResult.PASSED -> "Зараховано"; PassFailResult.FAILED -> "Не зараховано"; null -> "Результату ще немає" })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Обов’язкові роботи", style = MaterialTheme.typography.titleMedium)
            Text("${eligibility.completedCount} / ${eligibility.requiredCount}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        Column {
            HorizontalDivider()
            requiredTasks.forEach { task ->
                Row(Modifier.fillMaxWidth().clickable { onEditTask(task) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(task.completed, onCheckedChange = { onTaskCompleted(task, it) })
                    Column(Modifier.weight(1f)) { Text(task.title, fontSize = 13.sp, lineHeight = 17.sp); SmallText(if (task.meetsAdmissionRequirement) "Зараховано" else if (task.completed) "Поріг балів не виконано" else task.description?.takeIf { it.isNotBlank() } ?: "Для допуску") }
                    task.dueDate?.let { SmallText(it.format(DateTimeFormatter.ofPattern("dd.MM"))) }
                }
                HorizontalDivider()
            }
            requiredGrades.forEach { grade ->
                Row(Modifier.fillMaxWidth().clickable { onEditGrade(grade) }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (grade.meetsAdmissionRequirement) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Column { Text(grade.title, fontSize = 13.sp, lineHeight = 17.sp); SmallText(if (grade.meetsAdmissionRequirement) "Зараховано" else if (grade.completed) "Поріг балів не виконано" else "Додати результат") }
                }
                HorizontalDivider()
            }
            if (requiredTasks.isEmpty() && requiredGrades.isEmpty()) SmallText("Обов’язкових робіт не позначено", Modifier.padding(vertical = 16.dp))
        }
        Text("Під рукою", style = MaterialTheme.typography.titleMedium)
        ResourceRow("Формули й конспекти", "Нотатки та матеріали предмета", onNotes)
        if (course.gradingType == CourseGradingType.EXAM_LETTER) ResourceRow("Підготовка до екзамену", "Дата, цільова оцінка й матеріали", onExam)
        course.syllabusUrl?.let { url -> val uriHandler = LocalUriHandler.current; ResourceRow("Програма предмета", course.code) { runCatching { uriHandler.openUri(url) } } }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable private fun ResourceRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.Description, null, Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f)) { Text(title, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold); SmallText(subtitle) }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp))
    }
    HorizontalDivider()
}

internal fun CourseStatus.label(): String = when (this) {
    CourseStatus.FULFILLED -> "Закрито"
    CourseStatus.ENROLLED -> "Активний"
    CourseStatus.PLANNED -> "План"
    CourseStatus.NOT_ENROLLED -> "Не записаний"
    CourseStatus.NOT_NEEDED -> "Не потрібно"
}

internal fun CourseGradingType.label(): String = when (this) {
    CourseGradingType.EXAM_LETTER -> "Екзамен A–FX"
    CourseGradingType.CONTINUOUS_LETTER -> "Поточне A–FX"
    CourseGradingType.PASS_FAIL -> "Зараховано / ні"
}
