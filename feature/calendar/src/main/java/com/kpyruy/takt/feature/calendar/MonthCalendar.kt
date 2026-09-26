package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import com.kpyruy.takt.core.model.CalendarDayMarkers

@Composable
fun MonthCalendar(
    month: YearMonth,
    days: List<LocalDate>,
    selectedDate: LocalDate,
    today: LocalDate,
    markersForDate: (LocalDate) -> CalendarDayMarkers,
    lessonsAreMuted: (LocalDate) -> Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(t("Пн"), t("Вт"), t("Ср"), t("Чт"), t("Пт"), t("Сб"), t("Нд")).forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                week.forEach { date ->
                    val selected = date == selectedDate
                    val inMonth = YearMonth.from(date) == month
                    val isToday = date == today
                    val markers = markersForDate(date)

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .minimumInteractiveComponentSize()
                            .then(if (isToday && !selected) Modifier.border(1.dp,
                                MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                            .testTag("month-day-${date.toEpochDay()}")
                            .semantics {
                                contentDescription = buildList {
                                    add(date.toString())
                                    if (markers.lessons) add(t("пари"))
                                    if (markers.work) add(t("задачі"))
                                    if (markers.exams) add(t("іспит"))
                                }.joinToString(", ")
                            }
                            .clickable { onSelect(date) },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = when {
                                    selected -> MaterialTheme.colorScheme.onPrimary
                                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal,
                            )

                            Spacer(Modifier.size(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                if (markers.lessons) MonthMarkerDot(if (lessonsAreMuted(date)) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                    "month-marker-lesson-${date.toEpochDay()}", selected)
                                if (markers.work) MonthMarkerDot(MaterialTheme.colorScheme.tertiary,
                                    "month-marker-work-${date.toEpochDay()}", selected)
                                if (markers.exams) MonthMarkerDot(MaterialTheme.colorScheme.error,
                                    "month-marker-exam-${date.toEpochDay()}", selected)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            MonthLegendItem(MaterialTheme.colorScheme.primary, t("Пари"))
            Spacer(Modifier.width(14.dp))
            MonthLegendItem(MaterialTheme.colorScheme.tertiary, t("Задачі"))
            Spacer(Modifier.width(14.dp))
            MonthLegendItem(MaterialTheme.colorScheme.error, t("Іспити"))
        }
    }
}

@Composable
private fun MonthMarkerDot(color: Color, tag: String, selected: Boolean) {
    Box(Modifier.size(7.dp).testTag(tag)
        .background(color, CircleShape)
        .then(if (selected) Modifier.border(1.dp, MaterialTheme.colorScheme.onPrimary, CircleShape) else Modifier))
}

@Composable
private fun MonthLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
