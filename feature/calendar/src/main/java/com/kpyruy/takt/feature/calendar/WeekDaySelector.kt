package com.kpyruy.takt.feature.calendar

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormatter = DateTimeFormatter.ofPattern("EE", Locale("uk"))

@Composable
fun WeekDaySelector(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedDate) {
        val index = dates.indexOf(selectedDate)
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            listState.scrollToItem(index)
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = LocalDensity.current.fontScale
        val cellWidth = if (scale > 1.15f) 48.dp * scale else maxOf(48.dp, (maxWidth - 6.dp) / 7)
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Попередній тиждень") { onPreviousWeek(); true },
                    CustomAccessibilityAction("Наступний тиждень") { onNextWeek(); true },
                )
            }.pointerInput(selectedDate, scale) {
                if (scale <= 1.15f) {
                    var drag = 0f
                    detectHorizontalDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
                        if (drag > 60.dp.toPx()) onPreviousWeek()
                        else if (drag < -60.dp.toPx()) onNextWeek()
                    }) { change, amount -> change.consume(); drag += amount }
                }
            },
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            items(dates, key = { it.toEpochDay() }) { date ->
                val selected = date == selectedDate
                Surface(
                    modifier = Modifier
                        .width(cellWidth)
                        .heightIn(min = 56.dp)
                        .selectable(selected = selected, role = Role.Tab) { onSelect(date) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            date.format(dayFormatter).replace(".", "").uppercase(),
                            fontSize = 10.sp, lineHeight = 13.sp,
                        )
                        Text(
                            date.dayOfMonth.toString(),
                            fontSize = 15.sp, lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
