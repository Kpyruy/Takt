package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.ExamEligibility
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeLetter
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.ui.components.MetricCard
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.material.icons.filled.School

@Composable
internal fun AdmissionProgressCard(eligibility: ExamEligibility, compact: Boolean = false) {
    if (compact) {
        GlassSurface(modifier = Modifier.fillMaxWidth().testTag("course-admission-progress"),
            shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Default.School, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t(UiKey.TEXT_EXAM_ELIGIBILITY), style = MaterialTheme.typography.titleMedium)
                if (eligibility.requiredCount > 0) {
                    LinearProgressIndicator(
                        progress = { (eligibility.completedCount.toFloat() / eligibility.requiredCount).coerceIn(0f, 1f) },
                        gapSize = 0.dp, drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(7.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                } else {
                    Text(t(UiKey.TEXT_REQUIREMENTS_NOT_MARKED), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                    if (eligibility.requiredCount > 0) SmallText(t(UiKey.TEXT_COMPLETE_ALL_REQUIREMENTS_FOR_EXAM_ELIGIBILITY))
                }
                Text(if (eligibility.requiredCount > 0) "${eligibility.completedCount} / ${eligibility.requiredCount}" else "—",
                    style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }
    val hasRequirements = eligibility.requiredCount > 0
    val complete = hasRequirements && eligibility.eligible
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val warningSurface = if (dark) Color(0xFF463A2A) else Color(0xFFFCF1DF)
    val warningText = if (dark) Color(0xFFEFC78F) else Color(0xFF986023)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = when { !hasRequirements -> MaterialTheme.colorScheme.surfaceVariant; complete -> MaterialTheme.colorScheme.primaryContainer; else -> warningSurface },
        contentColor = when { !hasRequirements -> MaterialTheme.colorScheme.onSurfaceVariant; complete -> MaterialTheme.colorScheme.onPrimaryContainer; else -> warningText },
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t(UiKey.TEXT_EXAM_ELIGIBILITY_72CC89D4), fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(when { !hasRequirements -> t(UiKey.TEXT_REQUIREMENTS_NOT_MARKED); complete -> t(UiKey.TEXT_EXAM_REQUIREMENTS_MET); else -> if (eligibility.requiredCount - eligibility.completedCount == 1) t(UiKey.TEXT_1_TASK_LEFT) else t(uiMessage(UiKey.FORMAT_REMAINING_WORK_VALUE, eligibility.requiredCount - eligibility.completedCount)) }, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
            Text(if (hasRequirements) t(uiMessage(UiKey.FORMAT_COMPLETED_VALUE_OF_VALUE_REQUIRED_TASKS, eligibility.completedCount, eligibility.requiredCount)) else t(UiKey.TEXT_ADD_REQUIRED_WORK_IN_TASKS), fontSize = 12.sp, lineHeight = 16.sp)
            if (hasRequirements) LinearProgressIndicator(gapSize = 0.dp, drawStopIndicator = {}, progress = { (eligibility.completedCount.toFloat() / eligibility.requiredCount).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = if (complete) MaterialTheme.colorScheme.primary else warningText, trackColor = warningText.copy(alpha = .2f))
        }
    }
}

@Composable
internal fun CourseworkProgressCard(gradeItems: List<GradeItem>, projection: GradeProjection, currentGrade: GradeLetter? = null) {
    val coursework = gradeItems.filter { it.contributesToGrade && it.type != GradeItemType.EXAM }
    val earned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val maximum = coursework.sumOf { it.maxPoints }
    SubjectPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SmallText(t(UiKey.TEXT_EARNED_THIS_SEMESTER))
            if (maximum > 0) Text("${(earned / maximum * 100).toInt()}%", fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(earned.displayNumber(), fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold)
            Text(t(uiMessage(UiKey.FORMAT_VALUE_POINTS, maximum.displayNumber())), Modifier.padding(bottom = 7.dp), fontSize = 17.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (maximum > 0) LinearProgressIndicator(gapSize = 0.dp, drawStopIndicator = {}, progress = { (earned / maximum).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp))
        currentGrade?.let { SmallText(t(UiKey.TEXT_CURRENT_GRADE) + ": " + it.name) }
        SmallText(when { gradeItems.none { it.type == GradeItemType.EXAM } -> t(UiKey.TEXT_CURRENT_RESULTS_FROM_COMPLETED_WORK); projection.examRemainingPoints > 0 -> t(uiMessage(UiKey.FORMAT_VALUE_POINTS_ARE_STILL_AVAILABLE_ON_THE_EXAM, projection.examRemainingPoints.displayNumber())); else -> t(UiKey.TEXT_THE_RESULT_OF_THE_EXAM_IS_RECORDED_SEPARATELY) })
    }
}

@Composable
internal fun ExamProgressCards(gradeItems: List<GradeItem>, projection: GradeProjection, manualGrade: GradeLetter?) {
    val exams = gradeItems.filter { it.contributesToGrade && it.type == GradeItemType.EXAM }
    val coursework = gradeItems.filter { it.contributesToGrade && it.type != GradeItemType.EXAM }
    val courseworkEarned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val courseworkMaximum = coursework.sumOf { it.maxPoints }
    val pending = gradeItems.any { it.contributesToGrade && !it.completed }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard(label = t(UiKey.TEXT_EARNED_THIS_SEMESTER), value = courseworkEarned.displayNumber() + " / " + courseworkMaximum.displayNumber(), modifier = Modifier.weight(1f))
        MetricCard(label = t(UiKey.TEXT_STILL_ON_THE_EXAM), value = if (exams.isEmpty()) "—" else projection.examRemainingPoints.displayNumber(), modifier = Modifier.weight(1f))
    }
    if (exams.isEmpty()) {
        SectionCard {
            Text(t(UiKey.TEXT_THE_EXAM_HAS_NOT_BEEN_ADDED_YET), style = MaterialTheme.typography.titleMedium)
            Text(t(UiKey.TEXT_ADD_AN_EXAM_IN_TASKS_TO_SEE_A_POSSIBLE_RESULT), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    SectionCard {
        Text(if (pending) t(UiKey.TEXT_ACHIEVABLE_MAXIMUM) else t(UiKey.TEXT_RESULT_BY_POINTS), style = MaterialTheme.typography.titleMedium)
        Text(
            projection.maximumPossiblePoints.displayNumber() + " / " + projection.totalPoints.displayNumber() +
                if (projection.totalPoints > 0) " · " + (projection.maximumPossibleLetter?.name ?: "—") else "",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (manualGrade != null) {
            Text(t(uiMessage(UiKey.FORMAT_MANUAL_GRADE_VALUE, manualGrade.name)), style = MaterialTheme.typography.titleMedium)
        }
        val secured = projection.securedPoints
        val available = (projection.maximumPossiblePoints - secured).coerceAtLeast(0.0)
        val unavailable = (projection.totalPoints - projection.maximumPossiblePoints).coerceAtLeast(0.0)
        if (projection.totalPoints > 0) {
            Row(
                Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)
                    .semantics { contentDescription = t(uiMessage(UiKey.FORMAT_VALUE_POINTS_EARNED_UP_TO_VALUE_STILL_AVAILABLE, secured.displayNumber(), available.displayNumber())) },
            ) {
                if (secured > 0) Box(Modifier.weight(secured.toFloat()).height(8.dp).background(MaterialTheme.colorScheme.primary))
                if (available > 0) Box(Modifier.weight(available.toFloat()).height(8.dp).background(MaterialTheme.colorScheme.primaryContainer))
                if (unavailable > 0) Box(Modifier.weight(unavailable.toFloat()).height(8.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Text(
            if (pending) t(uiMessage(UiKey.FORMAT_VALUE_EARNED_UP_TO_VALUE_FROM_UNFINISHED_WORK_THIS_IS_POTENTIA_F2AADE7A, secured.displayNumber(), available.displayNumber()))
            else t(UiKey.TEXT_THE_GRADE_IS_CALCULATED_FROM_POINTS_FOR_COMPLETED_WORK),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable internal fun PotentialSummary(projection: GradeProjection, pending: Boolean) {
    val secured = projection.securedPoints
    val available = (projection.maximumPossiblePoints - secured).coerceAtLeast(0.0)
    val unavailable = (projection.totalPoints - projection.maximumPossiblePoints).coerceAtLeast(0.0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(if (pending) t(UiKey.TEXT_ACHIEVABLE_MAXIMUM) else t(UiKey.TEXT_RESULT_BY_POINTS), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("${projection.maximumPossiblePoints.displayNumber()} / ${projection.totalPoints.displayNumber()}", fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    }
    if (projection.totalPoints > 0) Row(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(9.dp))) {
        if (secured > 0) Box(Modifier.weight(secured.toFloat()).height(9.dp).background(MaterialTheme.colorScheme.primary))
        if (available > 0) Box(Modifier.weight(available.toFloat()).height(9.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .35f)))
        if (unavailable > 0) Box(Modifier.weight(unavailable.toFloat()).height(9.dp).background(MaterialTheme.colorScheme.outlineVariant))
    }
    Text(t(uiMessage(UiKey.FORMAT_VALUE_EARNED_VALUE_AVAILABLE_VALUE_LOST, secured.displayNumber(), available.displayNumber(), unavailable.displayNumber())), fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    SmallText(if (pending) t(UiKey.TEXT_THIS_IS_A_POTENTIAL_RESULT_NOT_AN_EARNED_GRADE) else t(UiKey.TEXT_THE_GRADE_IS_CALCULATED_FROM_COMPLETED_WORKS))
}

@Composable internal fun CompactAdmission(eligibility: ExamEligibility, next: String?, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val complete = eligibility.eligible
    val color = if (complete) MaterialTheme.colorScheme.primary else if (dark) Color(0xFFEFC78F) else Color(0xFF986023)
    GlassSurface(Modifier.fillMaxWidth().testTag("exam-admission").clickable(onClick = onClick), shape = RoundedCornerShape(20.dp),
        tint = if (complete) MaterialTheme.colorScheme.primaryContainer else if (dark) Color(0xFF77582B) else Color(0xFFFFDCA9)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${eligibility.completedCount}/${eligibility.requiredCount}", fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f)) {
                Text(if(eligibility.requiredCount == 0) t(UiKey.TEXT_REQUIREMENTS_NOT_MARKED) else if(complete) t(UiKey.TEXT_EXAM_REQUIREMENTS_MET) else if (eligibility.requiredCount - eligibility.completedCount == 1) t(UiKey.TEXT_ONE_MORE_TASK_FOR_EXAM_ELIGIBILITY) else t(uiMessage(UiKey.FORMAT_REMAINING_WORK_FOR_EXAM_ELIGIBILITY_VALUE, eligibility.requiredCount - eligibility.completedCount)), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                if(next != null) Text(next, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp))
        }
    }
}
