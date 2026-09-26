package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import kotlinx.coroutines.flow.drop

private val dayFormatter get() = DateTimeFormatter.ofPattern("EE", TaktI18n.locale)
private val baseWeek = LocalDate.of(2020, 1, 6)
private const val centerPage = Int.MAX_VALUE / 2

/** The Home-style week pager, shared by Calendar's day and week views. */
@Composable
fun WeekDaySelector(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onWeekChange: (LocalDate) -> Unit,
) {
    val visibleWeek = dates.first()
    fun pageFor(week: LocalDate) = centerPage + ChronoUnit.WEEKS.between(baseWeek, week).toInt()
    fun weekFor(page: Int) = baseWeek.plusWeeks((page - centerPage).toLong())
    val pagerState = rememberPagerState(initialPage = pageFor(visibleWeek), pageCount = { Int.MAX_VALUE })
    val currentWeek by rememberUpdatedState(visibleWeek)
    val currentWeekday by rememberUpdatedState(selectedDate.dayOfWeek.value)
    val weekChange by rememberUpdatedState(onWeekChange)

    LaunchedEffect(visibleWeek) {
        val target = pageFor(visibleWeek)
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            val week = weekFor(page)
            if (week != currentWeek) weekChange(week.plusDays((currentWeekday - 1).toLong()))
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().testTag("calendar-week-strip").semantics {
            customActions = listOf(
                CustomAccessibilityAction(t("Попередній тиждень")) {
                    onWeekChange(currentWeek.minusWeeks(1).plusDays((currentWeekday - 1).toLong())); true
                },
                CustomAccessibilityAction(t("Наступний тиждень")) {
                    onWeekChange(currentWeek.plusWeeks(1).plusDays((currentWeekday - 1).toLong())); true
                },
            )
        },
    ) { page ->
        val week = weekFor(page)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            repeat(7) { index ->
                val date = week.plusDays(index.toLong())
                val selected = date == selectedDate
                Column(Modifier.weight(1f).testTag("calendar-day-${date.toEpochDay()}")
                    .background(if (selected) MaterialTheme.colorScheme.primary else
                        MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .selectable(selected, role = Role.Tab) { onSelect(date) }
                    .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(date.format(dayFormatter).replace(".", "").uppercase(),
                        fontSize = 10.sp, lineHeight = 13.sp,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else
                            MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(date.dayOfMonth.toString(), fontSize = 15.sp, lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else
                            MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
