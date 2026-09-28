package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.clickable
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

@Composable
internal fun AdmissionProgressCard(eligibility: ExamEligibility) {
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
            Text(t("ДОПУСК ДО ЕКЗАМЕНУ"), fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(when { !hasRequirements -> t("Вимоги не позначено"); complete -> t("Умови допуску виконано"); else -> if (eligibility.requiredCount - eligibility.completedCount == 1) t("Залишилась 1 робота") else t("Залишилось ${eligibility.requiredCount - eligibility.completedCount} робіт") }, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
            Text(if (hasRequirements) t("${eligibility.completedCount} з ${eligibility.requiredCount} обов’язкових уже виконано.") else t("Додай обов’язкові роботи в «Задачах»."), fontSize = 12.sp, lineHeight = 16.sp)
            if (hasRequirements) LinearProgressIndicator(gapSize = 0.dp, drawStopIndicator = {}, progress = { (eligibility.completedCount.toFloat() / eligibility.requiredCount).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = if (complete) MaterialTheme.colorScheme.primary else warningText, trackColor = warningText.copy(alpha = .2f))
        }
    }
}

@Composable
internal fun CourseworkProgressCard(gradeItems: List<GradeItem>, projection: GradeProjection, currentGrade: GradeLetter? = null) {
    val coursework = gradeItems.filterNot { it.type == GradeItemType.EXAM }
    val earned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val maximum = coursework.sumOf { it.maxPoints }
    SubjectPanel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SmallText(t("Набрано за семестр"))
            if (maximum > 0) Text("${(earned / maximum * 100).toInt()}%", fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(earned.displayNumber(), fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.SemiBold)
            Text(t("/ ${maximum.displayNumber()} балів"), Modifier.padding(bottom = 7.dp), fontSize = 17.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (maximum > 0) LinearProgressIndicator(gapSize = 0.dp, drawStopIndicator = {}, progress = { (earned / maximum).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp))
        currentGrade?.let { SmallText(t("Поточна оцінка") + ": " + it.name) }
        SmallText(when { gradeItems.none { it.type == GradeItemType.EXAM } -> t("Поточні результати за завершені роботи."); projection.examRemainingPoints > 0 -> t("На екзамені доступно ще ${projection.examRemainingPoints.displayNumber()} балів."); else -> t("Результат екзамену зафіксовано окремо.") })
    }
}

@Composable
internal fun ExamProgressCards(gradeItems: List<GradeItem>, projection: GradeProjection, manualGrade: GradeLetter?) {
    val exams = gradeItems.filter { it.type == GradeItemType.EXAM }
    val coursework = gradeItems.filterNot { it.type == GradeItemType.EXAM }
    val courseworkEarned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val courseworkMaximum = coursework.sumOf { it.maxPoints }
    val pending = gradeItems.any { !it.completed }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCard(label = t("Набрано за семестр"), value = courseworkEarned.displayNumber() + " / " + courseworkMaximum.displayNumber(), modifier = Modifier.weight(1f))
        MetricCard(label = t("Ще на екзамені"), value = if (exams.isEmpty()) "—" else projection.examRemainingPoints.displayNumber(), modifier = Modifier.weight(1f))
    }
    if (exams.isEmpty()) {
        SectionCard {
            Text(t("Екзамен ще не додано"), style = MaterialTheme.typography.titleMedium)
            Text(t("Додай екзамен у вкладці «Задачі», щоб побачити можливий результат."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    SectionCard {
        Text(if (pending) t("Досяжний максимум") else t("Результат за балами"), style = MaterialTheme.typography.titleMedium)
        Text(
            projection.maximumPossiblePoints.displayNumber() + " / " + projection.totalPoints.displayNumber() +
                if (projection.totalPoints > 0) " · " + (projection.maximumPossibleLetter?.name ?: "—") else "",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (manualGrade != null) {
            Text(t("Оцінка, виставлена вручну · ${manualGrade.name}"), style = MaterialTheme.typography.titleMedium)
        }
        val secured = projection.securedPoints
        val available = (projection.maximumPossiblePoints - secured).coerceAtLeast(0.0)
        val unavailable = (projection.totalPoints - projection.maximumPossiblePoints).coerceAtLeast(0.0)
        if (projection.totalPoints > 0) {
            Row(
                Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)
                    .semantics { contentDescription = t("${secured.displayNumber()} балів набрано, до ${available.displayNumber()} ще доступно") },
            ) {
                if (secured > 0) Box(Modifier.weight(secured.toFloat()).height(8.dp).background(MaterialTheme.colorScheme.primary))
                if (available > 0) Box(Modifier.weight(available.toFloat()).height(8.dp).background(MaterialTheme.colorScheme.primaryContainer))
                if (unavailable > 0) Box(Modifier.weight(unavailable.toFloat()).height(8.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Text(
            if (pending) t("${secured.displayNumber()} набрано + до ${available.displayNumber()} за незавершені роботи. Це потенціал, а не отримана оцінка.")
            else t("Оцінку розраховано з балів за завершені роботи."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable internal fun PotentialSummary(projection: GradeProjection, pending: Boolean) {
    val secured = projection.securedPoints
    val available = (projection.maximumPossiblePoints - secured).coerceAtLeast(0.0)
    val unavailable = (projection.totalPoints - projection.maximumPossiblePoints).coerceAtLeast(0.0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(if (pending) t("Досяжний максимум") else t("Результат за балами"), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("${projection.maximumPossiblePoints.displayNumber()} / ${projection.totalPoints.displayNumber()}", fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    }
    if (projection.totalPoints > 0) Row(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(9.dp))) {
        if (secured > 0) Box(Modifier.weight(secured.toFloat()).height(9.dp).background(MaterialTheme.colorScheme.primary))
        if (available > 0) Box(Modifier.weight(available.toFloat()).height(9.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .35f)))
        if (unavailable > 0) Box(Modifier.weight(unavailable.toFloat()).height(9.dp).background(MaterialTheme.colorScheme.outlineVariant))
    }
    Text(t("${secured.displayNumber()} набрано   ·   ${available.displayNumber()} доступно   ·   ${unavailable.displayNumber()} втрачено"), fontSize = 10.sp, lineHeight = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    SmallText(if (pending) t("Це потенціал, а не отримана оцінка.") else t("Оцінку розраховано з завершених робіт."))
}

@Composable internal fun CompactAdmission(eligibility: ExamEligibility, next: String?, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val complete = eligibility.eligible
    val color = if (complete) MaterialTheme.colorScheme.primary else if (dark) Color(0xFFEFC78F) else Color(0xFF986023)
    Surface(Modifier.fillMaxWidth().testTag("exam-admission").clickable(onClick = onClick), shape = RoundedCornerShape(13.dp), color = if(complete) MaterialTheme.colorScheme.primaryContainer else if(dark) Color(0xFF463A2A) else Color(0xFFFFF2DF), contentColor = color) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${eligibility.completedCount}/${eligibility.requiredCount}", fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f)) {
                Text(if(eligibility.requiredCount == 0) t("Вимоги не позначено") else if(complete) t("Умови допуску виконано") else if (eligibility.requiredCount - eligibility.completedCount == 1) t("Ще одна робота до допуску") else t("Ще ${eligibility.requiredCount - eligibility.completedCount} робіт до допуску"), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                if(next != null) Text(next, fontSize = 11.sp, lineHeight = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp))
        }
    }
}
