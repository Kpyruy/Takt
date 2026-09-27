package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

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
import com.kpyruy.takt.core.model.AssessmentPhase
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.ExamEligibility
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeLetter
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
    phase: AssessmentPhase,
    gradeItems: List<GradeItem>,
    gradeScale: GradeScale,
    tasks: List<StudyTask>,
    eligibility: ExamEligibility,
    showFinalGrade: Boolean,
    manualGrade: GradeLetter?,
    onManualGradeChange: (GradeLetter?) -> Unit,
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
    val hasExam = course.gradingType != CourseGradingType.PASS_FAIL &&
        (phase == AssessmentPhase.EXAM || gradeItems.any { it.type == GradeItemType.EXAM })
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (hasExam) AdmissionProgressCard(eligibility)
        if (course.gradingType != CourseGradingType.PASS_FAIL) CourseworkProgressCard(allGradedWork, projection)
        else if (course.passFailResult != null) SubjectPanel {
            Text(t("Поточний результат"), style = MaterialTheme.typography.titleMedium)
            Text(when(course.passFailResult) { PassFailResult.PASSED -> t("Зараховано"); PassFailResult.FAILED -> t("Не зараховано"); null -> "" })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(t("Обов’язкові роботи"), style = MaterialTheme.typography.titleMedium)
            Text("${eligibility.completedCount} / ${eligibility.requiredCount}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        Column {
            HorizontalDivider()
            requiredTasks.forEach { task ->
                Row(Modifier.fillMaxWidth().clickable { onEditTask(task) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(task.completed, onCheckedChange = { onTaskCompleted(task, it) })
                    Column(Modifier.weight(1f)) { Text(task.title, fontSize = 13.sp, lineHeight = 17.sp); SmallText(if (task.meetsAdmissionRequirement) t("Зараховано") else if (task.completed) t("Поріг балів не виконано") else task.description?.takeIf { it.isNotBlank() } ?: t("Для допуску")) }
                    task.dueDate?.let { SmallText(it.format(DateTimeFormatter.ofPattern("dd.MM"))) }
                }
                HorizontalDivider()
            }
            requiredGrades.forEach { grade ->
                Row(Modifier.fillMaxWidth().clickable { onEditGrade(grade) }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (grade.meetsAdmissionRequirement) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Column { Text(grade.title, fontSize = 13.sp, lineHeight = 17.sp); SmallText(if (grade.meetsAdmissionRequirement) t("Зараховано") else if (grade.completed) t("Поріг балів не виконано") else t("Додати результат")) }
                }
                HorizontalDivider()
            }
            if (requiredTasks.isEmpty() && requiredGrades.isEmpty()) SmallText(t("Обов’язкових робіт не позначено"), Modifier.padding(vertical = 16.dp))
        }
        Text(t("Під рукою"), style = MaterialTheme.typography.titleMedium)
        ResourceRow(t("Формули й конспекти"), t("Нотатки та матеріали предмета"), onNotes)
        if (hasExam) ResourceRow(t("Підготовка до екзамену"), t("Дата, цільова оцінка й матеріали"), onExam)
        if (showFinalGrade && course.gradingType != CourseGradingType.PASS_FAIL) {
            ManualGradeSection(manualGrade, onManualGradeChange)
        }
        course.syllabusUrl?.let { url -> val uriHandler = LocalUriHandler.current; ResourceRow(t("Програма предмета"), course.code) { runCatching { uriHandler.openUri(url) } } }
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
