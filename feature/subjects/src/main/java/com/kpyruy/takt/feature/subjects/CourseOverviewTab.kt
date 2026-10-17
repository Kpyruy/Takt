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
import com.kpyruy.takt.core.model.CourseWork
import com.kpyruy.takt.core.model.CourseNote
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
    onExam: () -> Unit,
    nextLesson: com.kpyruy.takt.core.model.ResolvedScheduleEvent? = null,
    onSettings: (() -> Unit)? = null,
    teachersContent: @Composable () -> Unit = {},
    documentsContent: @Composable () -> Unit = {},
) {
    val allGradedWork = CourseWork.scoredItems(tasks, gradeItems)
    val projection = GradeProjection.calculate(allGradedWork, gradeScale)
    val currentGrade = if (course.officialGrade == null && course.gradingType == CourseGradingType.CONTINUOUS_LETTER &&
        (manualGrade != null || allGradedWork.any { it.completed })) {
        GradeSummary.calculate(allGradedWork, gradeScale, manualGrade).letter
    } else null
    val requiredTasks = tasks.filter { it.requiredForExam }.sortedBy { it.completed }
    val requiredGrades = gradeItems.filter { it.requiredForExam }.sortedBy { it.completed }
    val hasExam = course.gradingType != CourseGradingType.PASS_FAIL &&
        (phase == AssessmentPhase.EXAM || gradeItems.any { it.type == GradeItemType.EXAM })
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        course.officialGrade?.let { grade ->
            SectionCard {
                Text(t("Офіційна оцінка UIS"), style = MaterialTheme.typography.titleMedium)
                Text(grade.name, style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary)
                course.fulfilledOn?.let { date ->
                    Text(t("Дата складання") + ": " + date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (course.gradingType != CourseGradingType.PASS_FAIL) {
            SubjectPanel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OverviewMetric(t("Набрано"), projection.securedPoints, projection.totalPoints, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(74.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    OverviewMetric(t("Потенціал"), projection.maximumPossiblePoints, projection.totalPoints, Modifier.weight(1f))
                }
                currentGrade?.let { SmallText(t("Поточна оцінка") + ": " + it.name) }
            }
        }
        else if (course.passFailResult != null) SubjectPanel {
            Text(t("Поточний результат"), style = MaterialTheme.typography.titleMedium)
            Text(when(course.passFailResult) { PassFailResult.PASSED -> t("Зараховано"); PassFailResult.FAILED -> t("Не зараховано"); null -> "" })
        }
        if (hasExam || eligibility.requiredCount > 0) SubjectPanel {
            Text(t("Допуск до екзамену"), style = MaterialTheme.typography.titleMedium)
            Text("${eligibility.completedCount}/${eligibility.requiredCount}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { if (eligibility.requiredCount > 0) eligibility.completedCount.toFloat() / eligibility.requiredCount else 0f },
                modifier = Modifier.fillMaxWidth().height(7.dp), gapSize = 0.dp, drawStopIndicator = {})
            requiredTasks.forEach { task ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().clickable { onEditTask(task) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(task.completed, onCheckedChange = { onTaskCompleted(task, it) })
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(task.title, fontSize = 13.sp, lineHeight = 17.sp)
                        SmallText(if (task.meetsAdmissionRequirement) t("Зараховано") else if (task.completed) t("Поріг балів не виконано") else task.description?.takeIf { it.isNotBlank() } ?: t("Для допуску"))
                        task.minimumPointsForExam?.let { SmallText(t("Мінімум ") + it.displayNumber() + t(" б.")) }
                    }
                    task.dueDate?.let { SmallText(it.format(DateTimeFormatter.ofPattern("dd.MM"))) }
                }
            }
            requiredGrades.forEach { grade ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().clickable { onEditGrade(grade) }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (grade.meetsAdmissionRequirement) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(grade.title, fontSize = 13.sp, lineHeight = 17.sp)
                        SmallText(if (grade.meetsAdmissionRequirement) t("Зараховано") else if (grade.completed) t("Поріг балів не виконано") else if (grade.isAwaitingResult) t("Очікую оцінку") else t("Додати результат"))
                        grade.minimumPointsForExam?.let { SmallText(t("Мінімум ") + it.displayNumber() + t(" б.")) }
                    }
                }
            }
            if (requiredTasks.isEmpty() && requiredGrades.isEmpty()) SmallText(t("Обов’язкових робіт не позначено"), Modifier.padding(vertical = 16.dp))
        }
        nextLesson?.let { lesson ->
            SubjectPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SmallText(t("Найближча пара"))
                        Text(lesson.date.format(DateTimeFormatter.ofPattern("EEEE, dd.MM", com.kpyruy.takt.core.ui.i18n.TaktI18n.locale)) +
                            " · ${lesson.startTime}–${lesson.endTime}", style = MaterialTheme.typography.titleSmall)
                        SmallText(t(lesson.lessonType.label) + lesson.room?.let { " · " + t("ауд.") + " $it" }.orEmpty())
                    }
                }
            }
        }
        teachersContent()
        if (hasExam) SubjectPanel { ResourceRow(t("Підготовка до екзамену"), t("Дата й цільова оцінка"), onExam) }
        if (showFinalGrade && course.gradingType != CourseGradingType.PASS_FAIL && course.officialGrade == null) {
            ManualGradeSection(manualGrade, onManualGradeChange)
        }
        course.syllabusUrl?.let { url -> val uriHandler = LocalUriHandler.current; SubjectPanel {
            ResourceRow(t("Програма предмета"), course.code) { runCatching { uriHandler.openUri(url) } }
        } }
        documentsContent()
        onSettings?.let { open ->
            SubjectPanel {
                Row(Modifier.fillMaxWidth().clickable(onClick = open).heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Settings, null)
                    Text(t("Налаштування предмета"), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable private fun OverviewMetric(label: String, value: Double, maximum: Double, modifier: Modifier) {
    val progress = if (maximum > 0) (value / maximum).toFloat().coerceIn(0f, 1f) else 0f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        SmallText(label)
        Text("${value.displayNumber()}/${maximum.displayNumber()}", fontSize = 24.sp, lineHeight = 30.sp,
            fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            LinearProgressIndicator(progress = { progress },
                modifier = Modifier.weight(1f).height(6.dp), gapSize = 0.dp, drawStopIndicator = {})
            SmallText("${(progress * 100).toInt()}%")
        }
    }
}

@Composable private fun ResourceRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.Description, null, Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f)) { Text(title, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold); SmallText(subtitle) }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp))
    }
}
