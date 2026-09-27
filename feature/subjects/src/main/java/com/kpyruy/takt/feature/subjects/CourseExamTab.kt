package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ExamEligibility
import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.ExamMaterial
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeLetter
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.model.asScoredGradeItem
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import java.time.format.DateTimeFormatter
import com.kpyruy.takt.core.ui.i18n.TaktI18n

@Composable
internal fun CourseExamTab(
    courseId: String,
    courseCode: String,
    documentStore: TaktDocumentStore,
    gradeItems: List<GradeItem>,
    gradeScale: GradeScale,
    tasks: List<StudyTask>,
    eligibility: ExamEligibility,
    examInfo: ExamInfo?,
    materials: List<ExamMaterial>,
    notes: List<CourseNote>,
    onNotes: () -> Unit,
    onAddNote: () -> Unit,
    onSaveExamInfo: (ExamInfo) -> Unit,
    onAddMaterial: (ExamMaterial) -> Unit,
    onDeleteMaterial: (ExamMaterial) -> Unit,
    onAdmission: () -> Unit,
) {
    var showEditor by remember { mutableStateOf(false) }
    var target by rememberSaveable { mutableStateOf(GradeLetter.A) }
    val allGradedWork = remember(gradeItems, tasks) { gradeItems + tasks.mapNotNull { it.asScoredGradeItem() } }
    val projection = remember(allGradedWork, gradeScale) { GradeProjection.calculate(allGradedWork, gradeScale) }
    val coursework = allGradedWork.filterNot { it.type == GradeItemType.EXAM }
    val earned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val maximum = coursework.sumOf { it.maxPoints }
    val needed = projection.examPointsNeeded[target]
    val threshold = gradeScale.bands.first { it.grade == target }.minimumPercentage
    val pendingOther = coursework.filterNot { it.completed }.sumOf { it.maxPoints }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Surface(Modifier.fillMaxWidth().clickable { showEditor = true }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onSurface) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(examInfo?.date?.dayOfMonth?.toString() ?: "—", fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold)
                    SmallText(examInfo?.date?.format(DateTimeFormatter.ofPattern("MMM yyyy", TaktI18n.locale)) ?: t("ДАТА"))
                }
                VerticalDivider(Modifier.height(42.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(listOfNotNull(examInfo?.startTime?.toString(), examInfo?.room).joinToString(" · ").ifBlank { t("Додати дату й аудиторію") }, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
                    SmallText(t("Підсумковий екзамен · спроба ${examInfo?.attemptNumber ?: 1}/${examInfo?.maxAttempts ?: 3}"))
                }
                Icon(Icons.Default.CalendarMonth, t("Редагувати дані екзамену"), Modifier.size(20.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExamMetric(t("За семестр"), earned.displayNumber(), "/ ${maximum.displayNumber()}", Modifier.weight(1f))
            ExamMetric(t("Доступно на екзамені"), if (gradeItems.any { it.type == GradeItemType.EXAM }) projection.examRemainingPoints.displayNumber() else "—", t("балів"), Modifier.weight(1f))
        }
        SubjectPanel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(t("Цільова оцінка"), style = MaterialTheme.typography.titleMedium)
                Surface(shape = RoundedCornerShape(7.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(t("${target.name} · від ${threshold.displayNumber()}%"), Modifier.padding(horizontal = 8.dp, vertical = 5.dp), fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(needed?.displayNumber() ?: "—", fontSize = 37.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold)
                SmallText(if (needed != null) t("із ${projection.examRemainingPoints.displayNumber()} балів на екзамені") else if (projection.examRemainingPoints > 0) t("Ціль недосяжна") else t("Немає незавершеного екзамену"), Modifier.padding(bottom = 7.dp).weight(1f))
            }
            SmallText(if (needed == null) t("Розрахунок за поточною шкалою оцінювання.")
                else t("${projection.securedPoints.displayNumber()} набрано") + (if (pendingOther > 0) t(" + до ${pendingOther.displayNumber()} за інші роботи") else t(" за семестр")) + t(" + ${needed.displayNumber()} на екзамені = ${(projection.securedPoints + pendingOther + needed).displayNumber()}"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(GradeLetter.A, GradeLetter.B, GradeLetter.C, GradeLetter.D, GradeLetter.E).forEach { grade ->
                    Surface(onClick = { target = grade }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { selected = target == grade; role = Role.RadioButton }, shape = RoundedCornerShape(9.dp),
                        color = if (target == grade) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (target == grade) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        border = BorderStroke(1.dp, if (target == grade) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                        Box(contentAlignment = Alignment.Center) { Text(grade.name, fontSize = 12.sp, lineHeight = 16.sp) }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            PotentialSummary(projection, allGradedWork.any { !it.completed })
        }
        CompactAdmission(eligibility, tasks.firstOrNull { it.requiredForExam && !it.meetsAdmissionRequirement }?.title, onAdmission)
        if (!examInfo?.notes.isNullOrBlank()) SmallText(examInfo!!.notes)
        ExamMaterialsSection(courseId, courseCode, documentStore, materials, notes,
            onNotes, onAddNote, onAddMaterial, onDeleteMaterial)
        Spacer(Modifier.height(12.dp))
    }

    if (showEditor) {
        EditExamInfoSheet(
            courseId = courseId,
            initial = examInfo,
            gradeItems = allGradedWork,
            onDismiss = { showEditor = false },
            onSave = {
                onSaveExamInfo(it)
                showEditor = false
            },
        )
    }
}

@Composable private fun ExamMetric(label: String, value: String, suffix: String, modifier: Modifier) {
    SubjectPanel(modifier) {
        SmallText(label)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold)
            SmallText(suffix, Modifier.padding(bottom = 4.dp))
        }
    }
}
