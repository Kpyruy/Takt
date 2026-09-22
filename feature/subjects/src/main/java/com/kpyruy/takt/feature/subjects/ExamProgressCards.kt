package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.ui.components.MetricCard
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill

@Composable
internal fun ExamProgressCards(
    gradeItems: List<GradeItem>,
    projection: GradeProjection,
) {
    val exam = gradeItems.firstOrNull { it.type == GradeItemType.EXAM }
    val coursework = gradeItems.filterNot { it.type == GradeItemType.EXAM }
    val courseworkEarned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val total = projection.totalPoints
    val minimumLetter = projection.minimumPossibleLetter
    val maximumLetter = projection.maximumPossibleLetter
    val rangeText = when {
        minimumLetter == null -> "—"
        minimumLetter == maximumLetter -> minimumLetter.name
        else -> minimumLetter.name + " – " + (maximumLetter?.name ?: "—")
    }
    val examPending = exam != null && !exam.completed

    StatusPill(text = "Екзаменаційний період")

    SectionCard {
        Text("Фінальний екзамен", style = MaterialTheme.typography.titleLarge)
        Text(
            text = when {
                exam == null -> "Екзамен ще не додано"
                exam.completed -> exam.earnedPoints.displayNumber() + " / " +
                    exam.maxPoints.displayNumber() + " балів · результат зафіксовано"
                else -> exam.maxPoints.displayNumber() + " балів · головна оцінка"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricCard(
            label = "Бали за курс",
            value = courseworkEarned.displayNumber() + " / " + total.displayNumber(),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "Ще на екзамені",
            value = projection.examRemainingPoints.displayNumber() + " / " + total.displayNumber(),
            modifier = Modifier.weight(1f),
        )
    }

    SectionCard {
        Text("Твоя можлива оцінка", style = MaterialTheme.typography.titleMedium)
        Text(rangeText, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (examPending) {
                "Залежить від результату екзамену"
            } else {
                "За поточними зафіксованими результатами"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (examPending) {
        SectionCard {
            Text(
                "Фокус на екзамені. Саме він зараз визначає підсумкову оцінку.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
