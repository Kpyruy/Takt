package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import com.kpyruy.takt.core.model.CalendarDayMarkers

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun MonthCalendar(
    month: YearMonth,
    days: List<LocalDate>,
    selectedDate: LocalDate,
    today: LocalDate,
    markersForDate: (LocalDate) -> CalendarDayMarkers,
    lessonsAreMuted: (LocalDate) -> Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - 6.dp) / 7
        val adaptiveNarrow = cellWidth / LocalDensity.current.fontScale < 40.dp
        val numberStyle = if (adaptiveNarrow) MaterialTheme.typography.bodySmall
            else MaterialTheme.typography.bodyMedium
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                listOf(t(UiKey.TEXT_MON), t(UiKey.TEXT_TUE), t(UiKey.TEXT_WED), t(UiKey.TEXT_THU), t(UiKey.TEXT_FRI), t(UiKey.TEXT_SAT), t(UiKey.TEXT_SUN)).forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    week.forEach { date ->
                        val isSelected = date == selectedDate
                        val inMonth = YearMonth.from(date) == month
                        val isToday = date == today
                        val markers = markersForDate(date)

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .then(if (isToday && !isSelected) Modifier.border(1.dp,
                                    MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                                .testTag("month-day-${date.toEpochDay()}")
                                .semantics {
                                    selected = isSelected
                                    contentDescription = buildList {
                                        add(date.toString())
                                        if (markers.lessons) add(t(UiKey.TEXT_CLASSES_7B3BB6B0))
                                        if (markers.work) add(t(UiKey.TEXT_TASKS_08515408))
                                        if (markers.exams) add(t(UiKey.TEXT_EXAM_5E89E9D8))
                                    }.joinToString(", ")
                                }
                                .clickable { onSelect(date) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                // Keep the numeral separate from the marker row and readable over glass.
                                Box(
                                    Modifier.width((cellWidth - 6.dp).coerceAtMost(36.dp))
                                        .heightIn(min = 32.dp)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            CircleShape)
                                        .padding(horizontal = 3.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    BasicText(
                                        text = date.dayOfMonth.toString(),
                                        modifier = Modifier.fillMaxWidth(),
                                        style = numberStyle.copy(
                                            textAlign = TextAlign.Center,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday -> MaterialTheme.colorScheme.primary
                                                !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                        maxLines = 1,
                                        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp,
                                            maxFontSize = numberStyle.fontSize),
                                    )
                                }

                                Spacer(Modifier.size(4.dp))
                                Row(Modifier.height(7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    if (markers.lessons) MonthMarkerDot(if (lessonsAreMuted(date)) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                        "month-marker-lesson-${date.toEpochDay()}", isSelected)
                                    if (markers.work) MonthMarkerDot(MaterialTheme.colorScheme.tertiary,
                                        "month-marker-work-${date.toEpochDay()}", isSelected)
                                    if (markers.exams) MonthMarkerDot(MaterialTheme.colorScheme.error,
                                        "month-marker-exam-${date.toEpochDay()}", isSelected)
                                }
                            }
                        }
                    }
                }
            }
            FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MonthLegendItem(MaterialTheme.colorScheme.primary, t(UiKey.TEXT_CLASSES))
                MonthLegendItem(MaterialTheme.colorScheme.tertiary, t(UiKey.TEXT_TASKS))
                MonthLegendItem(MaterialTheme.colorScheme.error, t(UiKey.TEXT_EXAMS))
            }
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
