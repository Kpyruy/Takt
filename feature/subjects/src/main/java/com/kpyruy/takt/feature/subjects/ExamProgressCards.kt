package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.GradeProjection
import com.kpyruy.takt.core.ui.components.MetricCard

@Composable
internal fun ExamProgressCards(
    gradeItems: List<GradeItem>,
    projection: GradeProjection,
) {
    val exam = gradeItems.firstOrNull { it.type == GradeItemType.EXAM }
    val coursework = gradeItems.filterNot { it.type == GradeItemType.EXAM }
    val courseworkEarned = coursework.filter { it.completed }.sumOf { it.earnedPoints }
    val courseworkMax = coursework.sumOf { it.maxPoints }
    val minimumLetter = projection.minimumPossibleLetter
    val maximumLetter = projection.maximumPossibleLetter
    val rangeText = when {
        minimumLetter == null -> "—"
        minimumLetter == maximumLetter -> minimumLetter.name
        else -> minimumLetter.name + " – " + (maximumLetter?.name ?: "—")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricCard(
                label = "Фінальний екзамен",
                value = when {
                    exam == null -> "Не додано"
                    exam.completed -> exam.earnedPoints.displayNumber() + " / " + exam.maxPoints.displayNumber()
                    else -> "до " + exam.maxPoints.displayNumber() + " б."
                },
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Бали за курс",
                value = courseworkEarned.displayNumber() + " / " + courseworkMax.displayNumber(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricCard(
                label = "Ще на екзамені",
                value = projection.examRemainingPoints.displayNumber() + " б.",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Можлива оцінка",
                value = rangeText,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
