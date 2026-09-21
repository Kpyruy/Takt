# Takt Home, Calendar, and Add/Edit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved Home dashboard, day/month/dual-week calendar experience, and the five-part Add/Edit interaction system on top of the shared redesign foundation.

**Architecture:** Keep schedule rules and resolution in `:core:model`/`:core:data`; add only pure presentation helpers for “next event” and visible gaps. Reuse a single timeline component from `:core:ui`, split the current large Calendar screen into focused day/week/month composables, and let `:app` own the global FAB/add-routing shell while feature modules own their forms.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3 DatePicker/TimePicker, Navigation Compose, Coroutines/Flow.

---

### Task 1: Add pure schedule timeline helpers with tests

**Files:**
- Create: `core/model/src/main/kotlin/com/kpyruy/takt/core/model/ScheduleTimeline.kt`
- Create: `core/model/src/test/kotlin/com/kpyruy/takt/core/model/ScheduleTimelineTest.kt`

- [ ] **Step 1: Write failing tests**

Create `ScheduleTimelineTest.kt`:

```kotlin
package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScheduleTimelineTest {
    private val date = LocalDate.of(2026, 9, 21)

    private fun event(
        id: String,
        start: LocalTime,
        end: LocalTime,
    ) = ResolvedScheduleEvent(
        id = id,
        sourceRuleId = null,
        courseId = null,
        title = id,
        date = date,
        startTime = start,
        endTime = end,
        room = null,
        status = ScheduleEventStatus.NORMAL,
    )

    @Test
    fun nextEventReturnsFirstEventThatHasNotEnded() {
        val events = listOf(
            event("a", LocalTime.of(8, 0), LocalTime.of(9, 0)),
            event("b", LocalTime.of(10, 0), LocalTime.of(11, 0)),
        )

        assertEquals(
            "b",
            ScheduleTimeline.nextEvent(events, LocalTime.of(9, 30))?.id,
        )
    }

    @Test
    fun nextEventReturnsNullAfterLastEvent() {
        val events = listOf(event("a", LocalTime.of(8, 0), LocalTime.of(9, 0)))

        assertNull(ScheduleTimeline.nextEvent(events, LocalTime.of(10, 0)))
    }

    @Test
    fun gapsReturnsOnlyPositiveFreeTime() {
        val events = listOf(
            event("a", LocalTime.of(8, 0), LocalTime.of(9, 0)),
            event("b", LocalTime.of(9, 30), LocalTime.of(10, 30)),
            event("c", LocalTime.of(10, 15), LocalTime.of(11, 0)),
        )

        assertEquals(
            listOf(ScheduleGap(LocalTime.of(9, 0), LocalTime.of(9, 30))),
            ScheduleTimeline.gaps(events),
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
gradle :core:model:test --tests "*ScheduleTimelineTest" --stacktrace
```

Expected: compilation failure because `ScheduleTimeline` and `ScheduleGap` do not exist.

- [ ] **Step 3: Implement helper**

Create `ScheduleTimeline.kt`:

```kotlin
package com.kpyruy.takt.core.model

import java.time.LocalTime

data class ScheduleGap(
    val startTime: LocalTime,
    val endTime: LocalTime,
) {
    val minutes: Long
        get() = java.time.Duration.between(startTime, endTime).toMinutes()
}

object ScheduleTimeline {
    fun nextEvent(
        events: List<ResolvedScheduleEvent>,
        now: LocalTime,
    ): ResolvedScheduleEvent? =
        events
            .sortedBy { it.startTime }
            .firstOrNull { it.endTime > now }

    fun gaps(events: List<ResolvedScheduleEvent>): List<ScheduleGap> {
        val ordered = events.sortedBy { it.startTime }
        return ordered.zipWithNext().mapNotNull { (current, next) ->
            if (next.startTime > current.endTime) {
                ScheduleGap(current.endTime, next.startTime)
            } else {
                null
            }
        }
    }
}
```

- [ ] **Step 4: Run test**

```bash
gradle :core:model:test --tests "*ScheduleTimelineTest" --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add core/model
git commit -m "feat: add schedule timeline helpers"
```

---

### Task 2: Add reusable timeline and summary UI components

**Files:**
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/components/TaktTimeline.kt`
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/components/CompactSummaryStrip.kt`

- [ ] **Step 1: Create timeline component**

Create `TaktTimeline.kt` with this public API:

```kotlin
package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class TaktTimelineItem(
    val time: String,
    val title: String,
    val supporting: String?,
    val markerColor: Color,
    val emphasized: Boolean = false,
    val dimmed: Boolean = false,
)

@Composable
fun TaktTimeline(
    items: List<TaktTimelineItem>,
    gapLabels: Map<Int, String> = emptyMap(),
    modifier: Modifier = Modifier,
    onItemClick: ((Int) -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(if (item.emphasized) 10.dp else 8.dp)
                            .background(item.markerColor, CircleShape)
                    )
                    if (index != items.lastIndex) {
                        Spacer(
                            Modifier
                                .size(width = 2.dp, height = 42.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.dimmed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    item.supporting?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            gapLabels[index]?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier.padding(start = 76.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create compact summary strip**

Create `CompactSummaryStrip.kt`:

```kotlin
package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class CompactSummaryItem(
    val value: String,
    val label: String,
)

@Composable
fun CompactSummaryStrip(
    items: List<CompactSummaryItem>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                androidx.compose.foundation.layout.Column(Modifier.padding(10.dp)) {
                    Text(item.value, style = MaterialTheme.typography.titleMedium)
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Compile UI**

```bash
gradle :core:ui:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add core/ui
git commit -m "feat: add timeline and summary primitives"
```

---

### Task 3: Redesign Home around next class, summary cards, and timeline

**Files:**
- Modify: `feature/home/src/main/java/com/kpyruy/takt/feature/home/HomeScreen.kt`
- Modify: `feature/home/src/main/java/com/kpyruy/takt/feature/home/HomeScheduleCard.kt`
- Modify: `feature/home/src/main/java/com/kpyruy/takt/feature/home/HomeTaskRow.kt`
- Modify: `feature/home/src/main/java/com/kpyruy/takt/feature/home/HomeGradeRow.kt`

- [ ] **Step 1: Derive next event and compact counts**

In `HomeScreen`, derive:

```kotlin
val now = java.time.LocalTime.now()
val nextEvent = ScheduleTimeline.nextEvent(todayEvents, now)
val incompleteToday = allTasks.count { !it.completed && it.dueDate == today }
val overdueCount = allTasks.count {
    !it.completed && it.dueDate != null && it.dueDate.isBefore(today)
}
val gapByEventIndex = buildMap {
    todayEvents.sortedBy { it.startTime }.zipWithNext().forEachIndexed { index, pair ->
        val minutes = java.time.Duration.between(pair.first.endTime, pair.second.startTime).toMinutes()
        if (minutes > 0) put(index, "Перерва $minutes хв")
    }
}
```

- [ ] **Step 2: Build approved Home order**

Replace the current content order with:
1. header;
2. next-class card;
3. compact tasks/deadlines summary;
4. today timeline;
5. study progress / active subjects;
6. deadlines;
7. recent grades;
8. quick actions/motivational micro-message.

Use:

```kotlin
nextEvent?.let { event ->
    SectionCard {
        Text("Наступна пара", style = MaterialTheme.typography.labelLarge)
        Text(event.title, style = MaterialTheme.typography.titleLarge)
        Text(
            listOfNotNull(
                "${event.startTime}–${event.endTime}",
                event.room,
            ).joinToString(" · "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val minutesUntil = java.time.Duration.between(now, event.startTime).toMinutes()
        if (minutesUntil > 0) {
            Text(
                "Через $minutesUntil хв",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
```

Then:

```kotlin
CompactSummaryStrip(
    items = listOf(
        CompactSummaryItem(incompleteToday.toString(), "завдань сьогодні"),
        CompactSummaryItem(overdueCount.toString(), "прострочено"),
    )
)
```

Map schedule entries:

```kotlin
val orderedToday = todayEvents.sortedBy { it.startTime }
TaktTimeline(
    items = orderedToday.map { event ->
        TaktTimelineItem(
            time = event.startTime.toString(),
            title = event.title,
            supporting = event.room,
            markerColor = MaterialTheme.colorScheme.primary,
            emphasized = event.id == nextEvent?.id,
            dimmed = event.endTime < now,
        )
    },
    gapLabels = gapByEventIndex,
)
```

- [ ] **Step 3: Remove rigid two-column metrics on narrow screens**

Replace the fixed two-card row with `CompactSummaryStrip` or vertically stacked `MetricCard` items so font scaling cannot force horizontal overflow.

- [ ] **Step 4: Keep settings action and factual motivational micro-copy**

Add a short secondary message based only on real progress:

```kotlin
if (upcomingTasks.isEmpty() && todayEvents.isNotEmpty()) {
    Text(
        "На найближчі дні немає активних дедлайнів.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
```

- [ ] **Step 5: Compile Home**

```bash
gradle :feature:home:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add feature/home
git commit -m "feat: redesign home dashboard"
```

---

### Task 4: Split Calendar into focused Day, Week, and Month views

**Files:**
- Create: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/DayTimelineView.kt`
- Create: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/WeekCompactList.kt`
- Create: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/WeekTimetable.kt`
- Modify: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/MonthCalendar.kt`
- Modify: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/CalendarScreen.kt`
- Retire from main path: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/WeekDaySelector.kt`

- [ ] **Step 1: Create DayTimelineView**

Expose:

```kotlin
@Composable
fun DayTimelineView(
    events: List<ResolvedScheduleEvent>,
    selectedDate: LocalDate,
    today: LocalDate,
    cancellationStyle: CancellationDisplayStyle,
    onEventClick: (ResolvedScheduleEvent) -> Unit,
)
```

Build event rows sorted by start time. For today, compare with `LocalTime.now()`; emphasize current/next event and dim past events. Insert gap labels from adjacent event times.

- [ ] **Step 2: Create compact week list**

Create a `LazyColumn` grouped by Monday–Sunday:

```kotlin
@Composable
fun WeekCompactList(
    dates: List<LocalDate>,
    eventsForDate: (LocalDate) -> List<ResolvedScheduleEvent>,
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (ResolvedScheduleEvent) -> Unit,
)
```

Each day header must be at least 48dp high and each lesson row must show:
- start/end,
- subject/title,
- room,
- status marker.

- [ ] **Step 3: Create visual timetable week**

Create:

```kotlin
@Composable
fun WeekTimetable(
    dates: List<LocalDate>,
    eventsForDate: (LocalDate) -> List<ResolvedScheduleEvent>,
    onEventClick: (ResolvedScheduleEvent) -> Unit,
)
```

Use a horizontally scrollable timetable, day-column width `132.dp`, a vertical hour rail, and event block height based on duration. For overlapping blocks in the same day, divide available day-column width by the overlap group size and place events side-by-side.

- [ ] **Step 4: Refine MonthCalendar touch targets**

Use:

```kotlin
Modifier
    .weight(1f)
    .heightIn(min = 48.dp)
    .clickable { onSelect(date) }
```

Keep selected date fill, today marker, content dot, and out-of-month dimming.

- [ ] **Step 5: Replace chips with segmented view selector**

At the top of `CalendarScreen`, use `TaktSegmentedTabs` with `День`, `Тиждень`, `Місяць`. Render `Сьогодні` as a separate text/icon action.

- [ ] **Step 6: Respect persisted week layout and allow switching**

When `viewMode == WEEK`, show:
- `Таймтейбл`
- `Список`

Persist through `settingsRepository.setWeekLayout(...)`.

- [ ] **Step 7: Route view content**

Use:

```kotlin
when (viewMode) {
    CalendarViewMode.DAY -> DayTimelineView(...)
    CalendarViewMode.WEEK -> when (settings.weekLayout) {
        WeekLayout.TIMETABLE -> WeekTimetable(...)
        WeekLayout.COMPACT_LIST -> WeekCompactList(...)
    }
    CalendarViewMode.MONTH -> MonthCalendar(...)
}
```

Keep a selected-day agenda under Month after date selection.

- [ ] **Step 8: Compile Calendar**

```bash
gradle :feature:calendar:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add feature/calendar
git commit -m "feat: add calendar timeline and week modes"
```

---

### Task 5: Add reusable Material date/time picker fields

**Files:**
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/components/TaktDateTimePickers.kt`
- Modify: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/AddLessonSheet.kt`
- Modify: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/MoveLessonSheet.kt`
- Modify: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/OneOffEventSheet.kt`
- Modify: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/AddTaskSheet.kt`

- [ ] **Step 1: Create date picker field**

Expose:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaktDatePickerField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
)
```

Implement with a read-only `OutlinedTextField`, `DatePickerDialog`, and `DatePicker`. Convert selected UTC millis through epoch days to avoid local-zone date drift.

- [ ] **Step 2: Create time picker field**

Expose:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaktTimePickerField(
    label: String,
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
)
```

Implement with a Material 3 `TimePicker` in a dialog and preserve 24-hour time.

- [ ] **Step 3: Replace AddLesson time strings**

Use:

```kotlin
var startTime by remember(initialRule) {
    mutableStateOf(initialRule?.startTime ?: LocalTime.of(8, 0))
}
var endTime by remember(initialRule) {
    mutableStateOf(initialRule?.endTime ?: LocalTime.of(9, 50))
}
```

Use two `TaktTimePickerField` controls. Validation is `endTime > startTime`.

- [ ] **Step 4: Replace OneOffEvent raw date/time strings**

Hold `LocalDate` and `LocalTime` directly. Use `TaktDatePickerField` and `TaktTimePickerField`; create the event after validating title and end > start.

- [ ] **Step 5: Apply same pattern to move and task due date**

Use Material pickers in `MoveLessonSheet` and `AddTaskSheet` while preserving existing repository save behavior.

- [ ] **Step 6: Compile affected modules**

```bash
gradle :core:ui:assembleDebug :feature:calendar:assembleDebug :feature:subjects:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add core/ui feature/calendar feature/subjects
git commit -m "feat: replace raw date time entry with pickers"
```

---

### Task 6: Implement the global Add/Edit hierarchy

**Files:**
- Create: `app/src/main/java/com/kpyruy/takt/app/GlobalAddSheet.kt`
- Create: `app/src/main/java/com/kpyruy/takt/app/QuickAddSheet.kt`
- Create: `app/src/main/java/com/kpyruy/takt/app/CreateItemType.kt`
- Modify: `app/src/main/java/com/kpyruy/takt/app/TaktApp.kt`
- Modify: `core/model/src/main/kotlin/com/kpyruy/takt/core/model/ScheduleModels.kt`
- Modify: `core/model/src/test/kotlin/com/kpyruy/takt/core/model/ScheduleResolverTest.kt`
- Modify:
  - `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/AddLessonSheet.kt`
  - `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/OneOffEventSheet.kt`
  - `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/AddTaskSheet.kt`
  - `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/AddNoteSheet.kt`
  - `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/AddGradeItemSheet.kt`

- [ ] **Step 1: Add the in-app reminder schedule type with a failing resolver test**

Append to `ScheduleResolverTest.kt`:

```kotlin
@Test
fun reminder_isResolvedAsAOneOffCalendarEntry() {
    val reminder = OneOffScheduleEvent(
        id = "reminder-1",
        courseId = null,
        title = "Взяти калькулятор",
        date = date,
        startTime = LocalTime.of(7, 45),
        endTime = LocalTime.of(7, 50),
        room = null,
        type = OneOffScheduleEventType.REMINDER,
    )

    val result = ScheduleResolver.eventsForDate(
        rules = emptyList(),
        exceptions = emptyList(),
        oneOffEvents = listOf(reminder),
        date = date,
    )

    assertEquals(1, result.size)
    assertEquals("Взяти калькулятор", result.single().title)
    assertEquals(ScheduleEventStatus.ONE_OFF, result.single().status)
}
```

Run:

```bash
gradle :core:model:test --tests "*ScheduleResolverTest" --stacktrace
```

Expected: compilation fails because `OneOffScheduleEventType.REMINDER` does not exist.

Then add:

```kotlin
enum class OneOffScheduleEventType {
    BLOCK_ACTION,
    EXTRA,
    REMINDER,
}
```

Run the same test again.

Expected: `BUILD SUCCESSFUL`.

For this redesign, `REMINDER` is an **in-app calendar reminder entry**. Android notification scheduling is deliberately outside this redesign scope; do not imply that a notification will fire.

- [ ] **Step 2: Define add types**

Create:

```kotlin
package com.kpyruy.takt.app

enum class CreateItemType(val label: String) {
    CLASS("Пара"),
    TASK("Завдання"),
    EXAM("Екзамен"),
    NOTE("Нотатка"),
    EVENT("Подія"),
    REMINDER("Нагадування"),
}
```

- [ ] **Step 3: Wire the approved center FAB to real type selection**

In `TaktApp.kt`, pass the real center action into the navigation shell:

```kotlin
var showGlobalAdd by rememberSaveable { mutableStateOf(false) }

TaktBottomNavigation(
    items = navItems,
    centerContent = {
        TaktAddFab(onClick = { showGlobalAdd = true })
    },
)
```

Then render the sheet only while `showGlobalAdd` is true.

`GlobalAddSheet` API:

```kotlin
@Composable
fun GlobalAddSheet(
    onDismiss: () -> Unit,
    onCreate: (CreateItemType) -> Unit,
    onQuickAdd: () -> Unit,
)
```

Show all six types in the approved order. A normal row click opens the complete creation flow; the separate compact action opens `QuickAddSheet`.

- [ ] **Step 4: Implement compact quick add**

`QuickAddSheet` supports Task, Class, and Note. It keeps only essential fields and includes `Більше параметрів`, which opens the corresponding full form while retaining the entered draft values.

- [ ] **Step 5: Add creation routes**

Add:

```kotlin
private const val CREATE_ROUTE = "create/{type}"
```

Navigate with:

```kotlin
navController.navigate("create/${type.name}")
```

The app-level route host delegates to feature-owned forms and repositories.

Map the six types explicitly:
- `CLASS` -> recurring lesson form;
- `TASK` -> task form with course selection;
- `EXAM` -> exam grade-item form with course selection; Phase 3 extends this with exam logistics/materials;
- `NOTE` -> note form with course selection;
- `EVENT` -> one-off event form;
- `REMINDER` -> one-off event form preselected to `REMINDER`, using a short time range and no room field.

- [ ] **Step 6: Keep edit consistent**

Calendar and Subject edit actions use the same field order, date/time pickers, labels, and validation as creation. Destructive actions stay in contextual sheets rather than permanently visible beside every row.

- [ ] **Step 7: Compile app**

```bash
gradle :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add app feature/calendar feature/subjects
git commit -m "feat: modernize add and edit flows"
```

---

### Task 7: Home/Calendar verification

**Files:** verification only unless failures require fixes.

- [ ] **Step 1: Run domain tests**

```bash
gradle :core:model:test --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run Android unit tests**

```bash
gradle :core:data:testDebugUnitTest :app:testDebugUnitTest --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Lint**

```bash
gradle :app:lintDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL` with no fatal lint errors.

- [ ] **Step 4: Assemble**

```bash
gradle :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manual visual checklist**

Check:
- Home next-class card does not dominate the viewport.
- Timeline gaps are readable.
- Day view clearly distinguishes current/next/past.
- Month cells remain touchable on a small phone.
- Week switches between timetable and compact list.
- long Slovak names do not break rows.
- FAB opens the full add hub.
- date/time entry never requires ISO text.

- [ ] **Step 6: Commit verification-driven fixes only**

```bash
git add -A
git commit -m "fix: stabilize home calendar redesign"
```

Skip the commit when no files changed.
