package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeLetter
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.model.CourseWork
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.glassSurface
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import java.time.format.DateTimeFormatter
import com.kpyruy.takt.core.ui.i18n.TaktI18n

@Composable
internal fun CourseExamTab(
    courseId: String,
    gradeItems: List<GradeItem>,
    gradeScale: GradeScale,
    tasks: List<StudyTask>,
    eligibility: ExamEligibility,
    examInfo: ExamInfo?,
    onSaveExamInfo: (ExamInfo) -> Unit,
    onAdmission: () -> Unit,
) {
    var showEditor by remember { mutableStateOf(false) }
    var target by rememberSaveable { mutableStateOf(GradeLetter.A) }
    val allGradedWork = remember(gradeItems, tasks) { CourseWork.scoredItems(tasks, gradeItems) }
    val projection = remember(allGradedWork, gradeScale) { GradeProjection.calculate(allGradedWork, gradeScale) }
    val coursework = allGradedWork.filterNot { it.type == GradeItemType.EXAM }
    val earned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val maximum = coursework.sumOf { it.maxPoints }
    val needed = projection.examPointsNeeded[target]
    val threshold = gradeScale.bands.first { it.grade == target }.minimumPercentage
    val pendingOther = coursework.filterNot { it.completed }.sumOf { it.maxPoints }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        GlassSurface(Modifier.fillMaxWidth().clickable { showEditor = true }, shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(examInfo?.date?.dayOfMonth?.toString() ?: "—", fontSize = 49.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(examInfo?.date?.format(DateTimeFormatter.ofPattern("MMM yyyy", TaktI18n.locale))?.uppercase(TaktI18n.locale) ?: t("Екзамен"),
                        style = MaterialTheme.typography.titleMedium)
                    Text(listOfNotNull(examInfo?.startTime?.toString(), examInfo?.room).joinToString(" · ").ifBlank { t("Додати дату й аудиторію") }, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
                    SmallText(t("Підсумковий екзамен · спроба ${examInfo?.attemptNumber ?: 1}/${examInfo?.maxAttempts ?: 3}"))
                }
                Icon(Icons.Default.CalendarMonth, t("Редагувати дані екзамену"), Modifier.size(20.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExamMetric(t("За семестр"), earned.displayNumber(), "/ ${maximum.displayNumber()}", Icons.Default.Assignment, Modifier.weight(1f))
            ExamMetric(t("Доступно на екзамені"), if (allGradedWork.any { it.type == GradeItemType.EXAM }) projection.examRemainingPoints.displayNumber() else "—", t("балів"), Icons.Default.School, Modifier.weight(1f))
        }
        SubjectPanel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(t("Цільова оцінка"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    SmallText(t("${target.name} · від ${threshold.displayNumber()}%"))
                }
                Text(needed?.displayNumber() ?: "—", fontSize = 39.sp, lineHeight = 47.sp, fontWeight = FontWeight.Bold)
            }
            if (needed == null && projection.examRemainingPoints > 0) {
                Text(t("${target.name} поки недосяжна"), Modifier.padding(vertical = 8.dp),
                    fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
                SmallText(t("Навіть максимальних балів недостатньо."))
            } else {
                SmallText(if (needed != null) t("із ${projection.examRemainingPoints.displayNumber()} балів на екзамені") else t("Немає незавершеного екзамену"))
            }
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), tint = MaterialTheme.colorScheme.primaryContainer) {
                Text(if (needed == null) t("Розрахунок за поточною шкалою оцінювання.")
                    else t("${projection.securedPoints.displayNumber()} набрано") + (if (pendingOther > 0) t(" + до ${pendingOther.displayNumber()} за інші роботи") else t(" за семестр")) + t(" + ${needed.displayNumber()} на екзамені = ${(projection.securedPoints + pendingOther + needed).displayNumber()}"),
                    Modifier.padding(11.dp), style = MaterialTheme.typography.titleSmall)
            }
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(GradeLetter.A, GradeLetter.B, GradeLetter.C, GradeLetter.D, GradeLetter.E).forEach { grade ->
                    GlassSurface(modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .selectable(selected = target == grade, role = Role.RadioButton, onClick = { target = grade }),
                        shape = RoundedCornerShape(11.dp), tint = if (target == grade) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Unspecified) {
                        Box(contentAlignment = Alignment.Center) { Text(grade.name, Modifier.padding(vertical = 12.dp),
                            fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            color = if (target == grade) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            PotentialSummary(projection, allGradedWork.any { !it.completed })
        }
        CompactAdmission(eligibility, tasks.firstOrNull { it.requiredForExam && !it.meetsAdmissionRequirement }?.title
            ?: gradeItems.firstOrNull { it.requiredForExam && !it.meetsAdmissionRequirement }?.title, onAdmission)
        if (!examInfo?.notes.isNullOrBlank()) SmallText(examInfo!!.notes)
        Spacer(Modifier.height(12.dp))
    }

    if (showEditor) {
        EditExamInfoSheet(
            courseId = courseId,
            initial = examInfo,
            gradeItems = gradeItems,
            onDismiss = { showEditor = false },
            onSave = {
                onSaveExamInfo(it)
                showEditor = false
            },
        )
    }
}

@Composable private fun ExamMetric(label: String, value: String, suffix: String, icon: ImageVector, modifier: Modifier) {
    SubjectPanel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            SmallText(label, Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold)
            SmallText(suffix, Modifier.padding(bottom = 4.dp))
        }
    }
}
