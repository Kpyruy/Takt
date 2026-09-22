# Takt Study Plan and Product Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the redesign by polishing study-plan navigation, subject-color diversity, settings organization, motion, swipe actions, haptics, accessibility, dark mode, and final end-to-end verification.

**Architecture:** Keep polish reusable: stable subject colors and motion constants belong in `:core:ui`; feature modules consume them without inventing local palettes. Settings remains the single persisted preference surface. No new business-domain behavior is introduced in this phase except UI state needed for collapsible semester sections and safe interaction feedback.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Compose animation APIs, Android haptics through Compose, Coroutines/Flow.

---

### Task 1: Add stable subject-color assignment for all five themes

**Files:**
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/theme/TaktSubjectColors.kt`
- Modify color usage in:
  - `feature/home/src/main/java/com/kpyruy/takt/feature/home/HomeScreen.kt`
  - `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/DayTimelineView.kt`
  - `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/WeekCompactList.kt`
  - `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/WeekTimetable.kt`
  - `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/SubjectCard.kt`

- [ ] **Step 1: Create stable color helper**

Create:

```kotlin
package com.kpyruy.takt.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun taktSubjectColor(subjectKey: String?): Color {
    val colors = LocalTaktSubjectColors.current
    if (colors.isEmpty()) {
        return androidx.compose.material3.MaterialTheme.colorScheme.primary
    }

    val raw = subjectKey.orEmpty().hashCode().toLong() and 0x7fffffffL
    return colors[(raw % colors.size).toInt()]
}
```

- [ ] **Step 2: Replace one-color-for-everything usage**

Where a course id exists, use:

```kotlin
val subjectColor = taktSubjectColor(event.courseId ?: event.title)
```

For course cards:

```kotlin
val subjectColor = taktSubjectColor(course.id)
```

Use the subject color for the small marker/accent only; keep text/background contrast controlled by Material color roles.

- [ ] **Step 3: Compile affected modules**

```bash
gradle :core:ui:assembleDebug :feature:home:assembleDebug :feature:calendar:assembleDebug :feature:subjects:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add core/ui feature/home feature/calendar feature/subjects
git commit -m "feat: diversify subject colors by theme"
```

---

### Task 2: Make Study Plan compact and collapsible

**Files:**
- Modify: `feature/studyplan/src/main/java/com/kpyruy/takt/feature/studyplan/StudyPlanScreen.kt`
- Modify: `feature/studyplan/src/main/java/com/kpyruy/takt/feature/studyplan/SemesterSection.kt`

- [ ] **Step 1: Keep the top summary compact**

Use one `SectionCard` with:
- `earned / 180`;
- linear progress;
- number of fulfilled / total courses.

Add:

```kotlin
val fulfilledCourses = courses.count { it.status == CourseStatus.FULFILLED }
val totalCourses = courses.size
```

Render:

```kotlin
Text(
    text = "$fulfilledCourses / $totalCourses предметів",
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

- [ ] **Step 2: Add collapsible semesters**

Change `SemesterSection` API:

```kotlin
@Composable
fun SemesterSection(
    semester: Int,
    courses: List<Course>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCourseClick: (String) -> Unit,
)
```

Header row:
- semester number;
- earned/available credits;
- chevron;
- minimum 48dp clickable height.

- [ ] **Step 3: Animate section expansion**

Use:

```kotlin
AnimatedVisibility(
    visible = expanded,
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut(),
) {
    Column {
        courses.forEach { course ->
            // existing course row content
        }
    }
}
```

- [ ] **Step 4: Store expansion state in StudyPlanScreen**

Use:

```kotlin
var expandedSemesters by rememberSaveable {
    mutableStateOf(setOf(semesters.keys.firstOrNull() ?: 1))
}
```

Toggle membership per semester.

- [ ] **Step 5: Compile**

```bash
gradle :feature:studyplan:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add feature/studyplan
git commit -m "feat: compact study plan semesters"
```

---

### Task 3: Reorganize Settings into clear groups

**Files:**
- Modify: `feature/settings/src/main/java/com/kpyruy/takt/feature/settings/SettingsScreen.kt`
- Create: `feature/settings/src/main/java/com/kpyruy/takt/feature/settings/SettingsSectionTitle.kt`

- [ ] **Step 1: Add section heading component**

Create:

```kotlin
package com.kpyruy.takt.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
    )
}
```

- [ ] **Step 2: Group existing/new settings**

Render in this order:
1. `Вигляд`: theme mode, theme family, card appearance.
2. `Календар`: cancellation style, show hidden, parity, week layout.
3. `Оцінювання`: current default grade scale explanation.
4. `Дані`: backup export/import.

Do not duplicate the same setting in multiple cards.

- [ ] **Step 3: Add compact theme preview**

For each theme option, render 3–4 small color dots from its palette next to the label rather than a full preview card.

- [ ] **Step 4: Keep backup messages inline and calm**

Use a single supporting-text area under backup actions. Success and failure should use semantic Material colors and must not block the screen with a dialog.

- [ ] **Step 5: Compile**

```bash
gradle :feature:settings:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add feature/settings
git commit -m "feat: reorganize redesign settings"
```

---

### Task 4: Add shared motion constants and haptic feedback helpers

**Files:**
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/motion/TaktMotion.kt`
- Create: `core/ui/src/main/java/com/kpyruy/takt/core/ui/motion/TaktHaptics.kt`

- [ ] **Step 1: Define restrained animation timing**

Create `TaktMotion.kt`:

```kotlin
package com.kpyruy.takt.core.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object TaktMotion {
    const val FastMs = 140
    const val StandardMs = 220
    const val EmphasizedMs = 300

    val StandardEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> fast() = tween<T>(durationMillis = FastMs, easing = StandardEasing)
    fun <T> standard() = tween<T>(durationMillis = StandardMs, easing = StandardEasing)
    fun <T> emphasized() = tween<T>(durationMillis = EmphasizedMs, easing = StandardEasing)
}
```

- [ ] **Step 2: Add haptic helper**

Create `TaktHaptics.kt`:

```kotlin
package com.kpyruy.takt.core.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class TaktHaptics internal constructor(
    private val confirmAction: () -> Unit,
    private val tickAction: () -> Unit,
) {
    fun confirm() = confirmAction()
    fun tick() = tickAction()
}

@Composable
fun rememberTaktHaptics(): TaktHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) {
        TaktHaptics(
            confirmAction = {
                feedback.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            tickAction = {
                feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
        )
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
git add core/ui/src/main/java/com/kpyruy/takt/core/ui/motion
git commit -m "feat: add Takt motion and haptic language"
```

---

### Task 5: Add swipe-to-complete and animated state feedback

**Files:**
- Modify: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/StudyTaskRow.kt`
- Modify: `feature/home/src/main/java/com/kpyruy/takt/feature/home/HomeTaskRow.kt`
- Modify: `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/CalendarDeadlineRow.kt`
- Modify: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/CourseTasksTab.kt`

- [ ] **Step 1: Wrap actionable task rows in SwipeToDismissBox**

Use `SwipeToDismissBox` with:
- end-to-start or start-to-end action for complete/restore;
- no destructive delete as an unguarded full swipe.

Background example:

```kotlin
val haptics = rememberTaktHaptics()

SwipeToDismissBox(
    state = dismissState,
    backgroundContent = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = if (task.completed) "Відновити" else "Виконано",
            )
        }
    },
    content = {
        taskRowContent()
    },
)
```

When the threshold is confirmed:
- call the repository completion update;
- call `haptics.confirm()`;
- reset the dismiss state so the row remains visible with its new completed style.

- [ ] **Step 2: Animate completed state**

Use `animateColorAsState`, `animateContentSize`, or `AnimatedContent` so:
- checkbox/status changes are visible;
- text changes do not jump;
- completed rows become calmer/dimmer rather than disappearing unexpectedly.

- [ ] **Step 3: Keep Home and Calendar consistent**

Home task and calendar deadline rows use the same completion language when an interactive completion action is available.

- [ ] **Step 4: Compile**

```bash
gradle :feature:subjects:assembleDebug :feature:home:assembleDebug :feature:calendar:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add feature/subjects feature/home feature/calendar
git commit -m "feat: add swipe completion feedback"
```

---

### Task 6: Apply motion to tabs, cards, sheets, and add/edit actions

**Files:**
- Modify:
  - `core/ui/src/main/java/com/kpyruy/takt/core/ui/components/TaktSegmentedTabs.kt`
  - `app/src/main/java/com/kpyruy/takt/app/GlobalAddSheet.kt`
  - `app/src/main/java/com/kpyruy/takt/app/QuickAddSheet.kt`
  - `feature/home/src/main/java/com/kpyruy/takt/feature/home/HomeScreen.kt`
  - `feature/calendar/src/main/java/com/kpyruy/takt/feature/calendar/CalendarScreen.kt`
  - `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/SubjectDetailScreen.kt`

- [ ] **Step 1: Animate tab content, not every pixel**

Wrap feature tab bodies in:

```kotlin
AnimatedContent(
    targetState = selectedTab,
    transitionSpec = {
        fadeIn(TaktMotion.fast()) togetherWith fadeOut(TaktMotion.fast())
    },
    label = "tab-content",
) { tab ->
    tabContent(tab)
}
```

- [ ] **Step 2: Animate insert/remove states**

Use `AnimatedVisibility` for:
- next-class block appearing/disappearing;
- empty-state replacement after adding an item;
- compact legend popup;
- exam material rows;
- semester sections.

- [ ] **Step 3: Give successful add/edit a restrained confirmation**

After save:
- haptic confirm;
- close sheet/screen;
- let the destination list animate the inserted/updated row.

Do not add confetti, large bounce, full-screen success pages, or long delays.

- [ ] **Step 4: Compile app**

```bash
gradle :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add core/ui app feature
git commit -m "feat: polish transitions and state changes"
```

---

### Task 7: Accessibility and large-font pass

**Files:**
- Modify all redesigned screen/component files touched in previous phases where checks fail.

- [ ] **Step 1: Enforce minimum interactive sizes**

For custom click targets, add:

```kotlin
Modifier
    .minimumInteractiveComponentSize()
    .clickable { /* action */ }
```

Use Material interactive components wherever possible, since they already enforce minimum targets.

- [ ] **Step 2: Remove color-only status communication**

Every state must include at least one non-color signal:
- selected: selection indicator/shape;
- cancelled: text/status icon/strikethrough according to preference;
- completed: checkbox/icon;
- deadline warning: label/icon;
- moved: `Перенесено` text;
- exam eligibility: explicit text.

- [ ] **Step 3: Add meaningful content descriptions**

Icons that perform actions receive descriptions such as:
- `Назад`
- `Налаштування`
- `Додати`
- `Попередній тиждень`
- `Наступний тиждень`
- `Редагувати`

Purely decorative icons use `contentDescription = null`.

- [ ] **Step 4: Remove rigid one-line assumptions**

For course titles and primary task names:
- allow up to 2 lines;
- use `TextOverflow.Ellipsis` only after 2 lines;
- avoid fixed heights around text.

For compact navigation labels keep one line.

- [ ] **Step 5: Verify font scaling in previews/manual device**

Check at least:
- default font scale;
- 1.3×;
- 1.5×.

Any row that clips must be changed to wrap/stack rather than shrinking text.

- [ ] **Step 6: Run lint**

```bash
gradle :app:lintDebug --stacktrace
```

Expected: no fatal accessibility/content-description errors.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: improve redesign accessibility"
```

---

### Task 8: Dark-mode and small/large phone visual QA

**Files:** modify theme/components only where visual checks reveal a specific defect.

- [ ] **Step 1: Check approved dark surfaces**

Verify:
- background is rich dark blue/charcoal, not pure black everywhere;
- cards remain visually separable;
- subject colors stay distinct;
- exam red/status colors retain readable contrast;
- primary blue/green/purple/warm/monochrome themes all have dark equivalents.

- [ ] **Step 2: Check screen sizes**

At minimum inspect:
- compact phone around 360dp width;
- standard phone around 412dp width;
- large/landscape calendar.

Week timetable should use horizontal scrolling on narrow portrait rather than compressing seven columns into unreadable widths.

- [ ] **Step 3: Fix only evidence-backed visual defects**

Examples of acceptable fixes:
- replace a fixed Row with adaptive Column;
- reduce header top/bottom padding;
- adjust dark surface role;
- increase contrast;
- widen timetable column;
- allow long title wrapping.

Do not redesign approved layouts during QA.

- [ ] **Step 4: Compile**

```bash
gradle :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit if changes exist**

```bash
git add -A
git commit -m "fix: tune redesign responsive dark mode"
```

Skip if no changes.

---

### Task 9: Final specification coverage review

**Files:**
- Read: `docs/design/takt-design-system.md`
- Read: `docs/superpowers/plans/2026-09-22-takt-redesign-rollout.md`
- Compare against implemented source.

- [ ] **Step 1: Verify every accepted visual decision**

Confirm:
- Overall card-heavy direction.
- Quick actions.
- Compact tasks/deadlines summary.
- Home card overview + timeline.
- Day timeline.
- Google-like month.
- Week timetable + compact list.
- Elevated/tonal card setting.
- Motivational micro-message.
- Courses overview entry.
- Segmented Subject detail.
- Pre-exam task focus.
- Exam tab/logistics/materials/attempts.
- Exam-period four-card view.
- FAB + four root tabs.
- all five theme families with internal color diversity.
- approved typography.
- approved dark mode.
- controlled animation, swipes, and haptics.

- [ ] **Step 2: Verify architecture constraints**

Confirm:
- feature modules remain separate;
- shared UI is in `:core:ui`;
- pure projection logic is in `:core:model`;
- repositories remain in `:core:data`;
- Room remains in `:core:database`;
- no feature module directly owns another feature's persistence.

- [ ] **Step 3: Verify data safety**

Confirm:
- database version is 8;
- migration 7→8 is registered;
- no destructive migration is configured;
- v1 backup imports;
- v2 backup exports/imports new metadata;
- old rows receive safe defaults.

---

### Task 10: Final build verification

**Files:** no source changes unless verification exposes defects.

- [ ] **Step 1: Fresh domain tests**

```bash
gradle :core:model:test --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Fresh Android unit tests**

```bash
gradle :core:data:testDebugUnitTest :app:testDebugUnitTest --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Fresh lint**

```bash
gradle :app:lintDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Fresh APK build**

```bash
gradle :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Verify APK path**

```bash
test -f app/build/outputs/apk/debug/app-debug.apk
```

Expected: exit status 0.

- [ ] **Step 6: Inspect git diff/status**

```bash
git status --short
git diff --stat origin/main...HEAD
```

Expected:
- no accidental generated/local files;
- only planned source/docs/schema changes.

- [ ] **Step 7: Commit final verification fixes only**

If fixes were required:

```bash
git add -A
git commit -m "fix: finalize Takt redesign"
```

Skip when no files changed.

- [ ] **Step 8: Hand off for code review**

Invoke the code-review workflow before merge/PR. Do not merge to `main` without the user's approval.
