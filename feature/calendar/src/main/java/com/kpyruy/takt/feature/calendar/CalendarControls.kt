package com.kpyruy.takt.feature.calendar

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.components.tourTarget
import com.kpyruy.takt.core.ui.components.BindTaktTourAction
import com.kpyruy.takt.core.ui.components.LocalTaktTourActions
import com.kpyruy.takt.core.ui.i18n.t

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import com.kpyruy.takt.core.ui.components.TaktChoiceMenu as DropdownMenu
import com.kpyruy.takt.core.ui.components.TaktChoiceMenuItem as DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.kpyruy.takt.core.ui.components.glassSurface
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.WeekLayout
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics

private val viewKeys = listOf("day", "week", "month")

/** A double tap returns to today; holding Week or tapping its arrow opens the layout menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CalendarViewTabs(
    selectedIndex: Int,
    weekLayout: WeekLayout,
    onSelected: (Int) -> Unit,
    onDoubleTap: (Int) -> Unit,
    onWeekLayoutSelected: (WeekLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberTaktHaptics()
    val switchTab = com.kpyruy.takt.core.ui.motion.rememberTabSwitchGuard()
    var weekMenuExpanded by remember { mutableStateOf(false) }
    var lastTapIndex by remember { mutableIntStateOf(-1) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    fun openWeekMenu() {
        if (selectedIndex == 1) weekMenuExpanded = true else switchTab {
            onSelected(1)
            weekMenuExpanded = true
        }
    }
    BindTaktTourAction("calendar-week-menu") { openWeekMenu() }
    BindTaktTourAction("calendar-close-menu") { weekMenuExpanded = false }
    BindTaktTourAction("calendar-week-list") { weekMenuExpanded = false; onWeekLayoutSelected(WeekLayout.COMPACT_LIST) }
    BindTaktTourAction("calendar-week-timetable") { weekMenuExpanded = false; onWeekLayoutSelected(WeekLayout.TIMETABLE) }
    val density = LocalDensity.current
    val labels = listOf(t(UiKey.TEXT_DAY), t(UiKey.TEXT_WEEK), t(UiKey.TEXT_MONTH))
    val textStyle = MaterialTheme.typography.titleSmall
    val measurer = rememberTextMeasurer()
    val neededWidth = remember(labels, textStyle, density) {
        with(density) { labels.mapIndexed { index, label ->
            measurer.measure(label, textStyle).size.width.toDp() + 12.dp + if (index == 1) 28.dp else 0.dp
        }.max() }
    }
    BoxWithConstraints(modifier.height(48.dp).glassSurface(RoundedCornerShape(24.dp))) {
    val scrollable = density.fontScale > 1.15f || neededWidth > (maxWidth - 10.dp) / 3
    Row(
        modifier = Modifier
            .height(48.dp)
            .then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .selectableGroup()
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bringIntoView = remember { BringIntoViewRequester() }
            LaunchedEffect(selected, scrollable) { if (selected && scrollable) bringIntoView.bringIntoView() }
            fun tapView() {
                val now = SystemClock.uptimeMillis()
                if (selected && lastTapIndex == index && now - lastTapAt <= ViewConfiguration.getDoubleTapTimeout()) {
                    haptics.tick()
                    onDoubleTap(index)
                    lastTapAt = 0L
                } else if (!selected) {
                    switchTab {
                        haptics.tick()
                        onSelected(index)
                        lastTapIndex = index
                        lastTapAt = now
                    }
                } else {
                    lastTapIndex = index
                    lastTapAt = now
                }
            }
            BindTaktTourAction("calendar-tap-${viewKeys[index]}") { tapView() }
            Box(
                modifier = (if (scrollable) Modifier.widthIn(min = neededWidth.coerceAtLeast(88.dp)) else Modifier.weight(1f))
                    .height(42.dp)
                    .bringIntoViewRequester(bringIntoView)
                    .then(if (selected) Modifier.glassSurface(RoundedCornerShape(21.dp), MaterialTheme.colorScheme.primaryContainer) else Modifier)
                    .testTag("calendar-view-${viewKeys[index]}").tourTarget("calendar-mode-${viewKeys[index]}")
                    .semantics { this.selected = selected }
                    .combinedClickable(
                        role = Role.Tab,
                        onClick = ::tapView,
                        onLongClick = if (index == 1) {
                            {
                                lastTapAt = 0L
                                haptics.tick()
                                if (selected) weekMenuExpanded = true else switchTab {
                                    onSelected(index)
                                    weekMenuExpanded = true
                                }
                            }
                        } else null,
                        onLongClickLabel = if (index == 1) t(UiKey.TEXT_CHOOSE_A_VIEW_OF_THE_WEEK) else null,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (index == 1) {
                        IconButton(
                            onClick = ::openWeekMenu,
                            modifier = Modifier.size(28.dp).tourTarget("calendar-week-layout"),
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, t(UiKey.TEXT_CHOOSE_A_VIEW_OF_THE_WEEK), Modifier.size(19.dp))
                        }
                    }
                }
                if (index == 1) {
                    DropdownMenu(expanded = weekMenuExpanded, onDismissRequest = { weekMenuExpanded = false },
                        focusable = LocalTaktTourActions.current == null,
                        modifier = Modifier.tourTarget("calendar-week-menu-popup")) {
                        listOf(
                            WeekLayout.TIMETABLE to t(UiKey.TEXT_SCHEDULE_TIMETABLE),
                            WeekLayout.COMPACT_LIST to t(UiKey.TEXT_LIST),
                        ).forEach { (layout, title) ->
                            DropdownMenuItem(
                                text = { Text(title) },
                                modifier = Modifier.tourTarget(if (layout == WeekLayout.TIMETABLE) "calendar-week-timetable" else "calendar-week-list"),
                                onClick = {
                                    weekMenuExpanded = false
                                    onWeekLayoutSelected(layout)
                                },
                                trailingIcon = if (weekLayout == layout) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
internal fun CalendarPeriodArrows(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        TaktIconButton(Icons.Default.ChevronLeft, t(UiKey.TEXT_PREVIOUS_PERIOD), onPrevious, Modifier.size(40.dp))
        TaktIconButton(Icons.Default.ChevronRight, t(UiKey.TEXT_THE_NEXT_PERIOD), onNext, Modifier.size(40.dp))
    }
}
