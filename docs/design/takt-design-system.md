# Takt Design System

Status: **Approved visual direction — ready for implementation planning**  
Date: 2026-09-21  
Repository: `Kpyruy/Takt`  
Target: Android, Kotlin, Jetpack Compose, Material 3

## 1. Purpose

This document is the source of truth for the Takt redesign before implementation.

It consolidates the approved visual concepts, interaction patterns, grading UX, calendar behavior, themes, dark mode, navigation, and form flows. The redesign must preserve the existing modular architecture and current functionality.

The visual reference pack is stored persistently in ChatGPT Library at:

`/Takt Design References`

Key rule: **do not silently reinterpret or replace an approved concept during implementation.** If code constraints create a conflict with this document or the reference images, surface that conflict before changing the design.

---

## 2. Product design principles

Takt should feel like a modern Android student productivity app rather than a generic task manager or a desktop dashboard compressed into a phone.

Core principles:

1. **Glanceable first.** Important information should be understandable in seconds.
2. **Student-specific hierarchy.** Classes, deadlines, exam eligibility, points, and grade potential matter more than generic productivity metrics.
3. **Card-based, but controlled.** Cards are useful for hierarchy; avoid box-in-box nesting and excessive elevation.
4. **Timeline where time matters.** Today and specific-day schedule views should communicate sequence, duration, gaps, and what is next.
5. **Do not fake certainty.** Before an exam, Takt should show what is possible and what has been secured, not pretend the student already has a final letter grade.
6. **Progress should reduce anxiety.** Required work, submitted work, eligibility, and completed tasks should be visibly acknowledged.
7. **Color has meaning.** Subject colors, status colors, and theme accents must improve scanning, not decorate randomly.
8. **Android-native behavior.** Use familiar Material 3 patterns, predictable back navigation, proper touch targets, system-aware edge-to-edge layout, native date/time selection, and restrained motion.
9. **Readable over dense.** Long Slovak course names, font scaling, and small phones must remain usable.
10. **Dark mode is first-class.** It is not a simple inversion of light mode.

---

## 3. Overall visual language

### Selected direction

Base: **Overall variant 4 — Card-heavy dashboard**

Additions:
- Quick actions from Overall variant 1.
- Compact tasks/deadlines summary from Overall variant 3.

Canonical direction:

**Overall = Card-heavy dashboard + compact quick actions + compact task/deadline summary.**

### Visual character

- Modern productivity UI.
- Rounded Material-like cards with moderate radius.
- Clear separation between primary and secondary information.
- Low-to-moderate elevation in light mode.
- Rich tonal surfaces in dark mode.
- Strong hierarchy without giant headers.
- Use color for subjects, status, deadlines, and grade state.
- Avoid glassmorphism, neon, excessive gradients, giant empty hero areas, and excessive pills.

### References

- `00_overall_home_calendar_concept_board.png`
- `01_overall_quick_actions.png`
- `02_overall_task_deadline_summary.png`
- `03_overall_card_heavy_dashboard.png`

---

## 4. Home

### Selected direction

Base: **Home variant 5 — Card-based overview**

Add:
- Timeline-style lesson sequence from Home variant 4.

Canonical direction:

**Home = fast card overview + timeline-style Today schedule.**

### Home hierarchy

Recommended information order:

1. Compact date / greeting / context.
2. Next class or next important academic event.
3. Compact task/deadline summary.
4. Today timeline.
5. Progress blocks such as study-plan progress / recent grade / active-subject summary.
6. Quick actions.
7. Motivational micro-message where space allows.

### Next class card

Should emphasize:
- subject,
- time,
- room,
- countdown or time until start,
- optional status such as moved/cancelled.

The visual treatment should be stronger than normal list items without becoming a giant hero card.

### Today timeline

Use a compact vertical timeline:
- chronological order,
- visible gaps/breaks,
- subject color marker,
- time,
- room,
- cancelled/moved state,
- current/next emphasis.

References:
- `04_home_card_based_overview.png`
- `05_home_timeline.png`

---

## 5. Calendar

Takt should support multiple useful representations rather than forcing one calendar layout to serve every task.

### 5.1 Day / specific date

Selected base: **Structured-style timeline**

Requirements:
- vertical time progression,
- current-time line when relevant,
- chronological events,
- clear gaps/free time,
- duration is visually understandable,
- moved/cancelled state,
- deadlines can appear as all-day / untimed items rather than fake timed blocks,
- overlapping timed events must remain readable.

Reference:
- `06_calendar_day_timeline.png`

### 5.2 Month

Selected base: **clean Google-style month grid**

Requirements:
- familiar month layout,
- selected day,
- today state,
- event/deadline indicators,
- selected-day agenda below or after selection,
- compact enough for small phones,
- do not rely on color alone for state.

Reference:
- `07_calendar_month_google_style.png`

### 5.3 Week

Week view should support **more than one display mode**.

Approved weekly option:
- **Compact list, variant 3**

The user wants a selectable weekly display mode rather than being locked into one week layout.

Week mode should therefore support at least:
1. **Compact list** — days grouped vertically with essential lesson details.
2. **Visual timetable/grid alternative** — the previously discussed weekly timetable representation.

The selected weekly mode should persist as a user preference.

Reference:
- `27_week_compact_list.png`
- `26_week_forms_colors_dark_motion_board.png`

---

## 6. Cards and list items

### Default style

Selected base: **variant 3 — Elevated cards**

Use for:
- important course/task rows,
- Home summary cards,
- major status blocks,
- exam-period progress cards.

### User-selectable appearance

Settings should allow card appearance switching between:
- **Elevated cards** — default.
- **Tonal filled cards** — inspired by variant 2.

This is a visual preference only; information hierarchy and behavior stay consistent.

### Additional patterns

From variant 5:
- short motivational / progress message.

From variant 4:
- **Courses** section with an affordance to open a semester overview.

The Courses entry should lead to a higher-level overview such as:
- this semester,
- subjects,
- status,
- grades/progress,
- relevant semester summary.

References:
- `08_cards_list_concepts_board.png`
- `12_cards_elevated.png`
- `13_cards_tonal_filled.png`
- `14_cards_motivational_message.png`
- `15_cards_courses_section.png`
- `16_cards_this_semester.png`

---

## 7. Subject / Course Detail

Selected base: **variant 2 — Segmented tabs**

Do not turn course detail into one extremely long page.

### Primary tabs

- Overview
- Assessments
- Homework
- Notes

Additional exam-specific content may appear when applicable, but the segmented/tabbed organization is the approved structural pattern.

### Header

Keep the header compact:
- course name,
- code / credits if useful,
- status,
- normal Android back navigation on the left.

Long Slovak names must wrap gracefully without consuming excessive vertical space.

Reference:
- `17_subject_course_detail_segmented_tabs.png`
- `09_subject_course_detail_concepts_board.png`

---

## 8. Grades and progress domain model

The grading UI must support multiple course types.

### 8.1 Exam-based course

The final A/B/C/D/E/FX result is determined through the exam or final assessment.

Before the exam is taken, Takt must not present a letter grade as a settled result.

Instead show:
- points already secured,
- points still available,
- current maximum possible total,
- maximum possible letter grade,
- requirements needed to reach each grade,
- exam eligibility,
- impact of unfinished work.

Example:
- final total = 100 points,
- pre-exam tasks = 30 points,
- student earns 0/30,
- remaining maximum = 70,
- higher grades may become mathematically impossible.

The UI should make this consequence obvious.

### 8.2 Required tasks that do not award grade points

Some tasks:
- must be submitted,
- do not add points,
- are required for exam admission.

Track:
- submitted / not submitted,
- required / optional,
- eligibility state,
- completion count.

This must be visually rewarding enough that students can see their preparation progress even when no grade points are attached.

### 8.3 Pass / fail course

Some subjects do not use A–FX.

Support:
- passed / not passed,
- required work completion,
- no forced letter-grade visualization.

---

## 9. Grades / Progress — before exam period

Selected base: **custom pre-exam variant 2 — Task-focused list**

### Main focus

Before exam period, the interface should emphasize:
- tasks,
- tests,
- assignments,
- completion,
- points,
- how each item affects final exam potential.

### Recommended tab structure

- Overview
- Tasks
- Exam
- Notes / Materials

Exact labels may be slightly adjusted for space, but the information architecture should remain.

### Tasks

Each scored task should show:
- name,
- due date,
- points available,
- points earned,
- completion state,
- contribution to exam/final total when relevant.

Required non-scored tasks should show:
- required status,
- submitted state,
- exam-eligibility impact.

### Compact summary blocks

Borrow the visual language from pre-exam variant 4:
- required tasks completed,
- scored points secured,
- tests/assessments progress,
- eligibility.

These blocks must distinguish **completion** from **grade points**.

### Legend

The category legend from variant 3 is useful but too space-hungry.

Use a smaller treatment:
- info button,
- popup,
- bottom sheet,
- compact expandable legend.

### Upcoming

Include an Upcoming section for:
- approaching tasks,
- required work,
- tests,
- exam milestones.

### Exam tab

Practical exam information:
- date,
- time,
- room/location,
- attempt number,
- maximum attempts: 3,
- attached study materials,
- notes,
- useful preparation details.

References:
- `18_grades_progress_custom_concept_board.png`
- `19_grades_preexam_task_focused.png`
- `20_grades_preexam_summary_blocks.png`
- `21_grades_preexam_compact_legend_reference.png`
- `22_grades_preexam_upcoming_reference.png`

---

## 10. Grades / Progress — exam period

Selected base: **exam-period variant 4 — Card-based modern**

This specific concept is the approved visual base.

### Primary blocks

- Final exam
- Course points
- Still on exam
- Your possible grade

### Behavior

During exam period:
- the exam becomes the dominant visual focus,
- show secured points,
- show points still decided by the exam,
- show the possible grade range,
- show what exam score is required for each grade,
- do not imply a final grade before the exam result is known.

Example wording:
- Possible grade: C–A
- Depends on exam result

### Navigation inside this area

Provide:
- tabs/segments,
- obvious access back to the pre-exam/task-oriented view,
- exam materials,
- notes,
- location/time,
- attempt tracking.

Reference:
- `23_grades_exam_period_card_based.png`
- `18_grades_progress_custom_concept_board.png`

---

## 11. Bottom navigation and quick add

Selected base: **Navigation variant 3 — FAB + tabs**

### Bottom navigation

Keep the existing core destinations:
- Home
- Calendar
- Subjects
- Plan

Do not add unnecessary permanent tabs.

Settings remains secondary rather than a core bottom destination.

### FAB

Use a central or prominent `+` FAB for quick creation.

The FAB opens the Add flow rather than immediately assuming one entity type.

Variant 2 is not the base because it felt visually overloaded.

References:
- `11_navigation_concepts_board.png`
- `24_navigation_variant3_fab_tabs.png`
- `25_navigation_variant2_reference.png`

---

## 12. Add / Edit flow

All five concepts are approved **in the exact interaction hierarchy shown**.

### 1. Bottom sheet — entry point

The FAB opens a bottom sheet to choose what to add:
- Class
- Task / Assignment
- Exam
- Note
- Event
- Reminder

This is the main gateway.

### 2. Full screen form

Use for complete creation when the item needs:
- full details,
- recurrence,
- points,
- subject,
- location,
- date/time,
- notes,
- advanced options.

### 3. Quick add

Offer a compact path for fast entry with only the essential fields.

Quick add should not replace the full form.

### 4. Edit existing

Editing should use the same vocabulary and field structure as creation.

### 5. Date & time picker

Use a modern native-feeling Material date/time picker rather than raw ISO text entry.

Reference:
- `28_add_edit_all_five.png`

---

## 13. Color themes

All five theme families are approved:

1. Blue
2. Green
3. Purple
4. Warm
5. Monochrome

### Important requirement: internal color diversity

A selected theme must not make every subject/event/task the same color.

Each theme needs:
- one global brand/accent direction,
- multiple subject/event colors within that direction,
- sufficient separation between subjects,
- controlled saturation,
- dark-mode equivalents.

Use color consistently by semantic role.

Reference:
- `29_color_themes_all_five_more_variety.png`

---

## 14. Typography

The presented typography direction is approved.

### Hierarchy

Use a restrained Android-friendly scale:
- Large title
- Section title
- Card title
- Body text
- Secondary text
- Caption

Do not oversize screen headers.

Requirements:
- readable on small devices,
- resilient to font scaling,
- long Slovak labels must wrap or truncate intentionally,
- no tiny decorative labels.

Reference:
- `30_typography_approved.png`

---

## 15. Dark mode

The approved dark direction applies to:
- Home,
- Calendar,
- Course,
- Exam.

### Requirements

- Use rich dark surfaces rather than pure black for every layer.
- Maintain clear surface hierarchy.
- Keep accent colors strong but controlled.
- Preserve subject differentiation.
- Ensure cancelled, moved, deadline, success, warning, and exam states remain distinguishable.
- Preserve the same information hierarchy as light mode.
- Avoid glowing/neon visual treatment.

Reference:
- `31_dark_mode_approved.png`

---

## 16. Motion, gestures, and haptics

Motion should feel polished and frequent enough to make the app responsive, but never flashy.

### Approved interaction language

Use:
- smooth screen transitions,
- tab transitions,
- bottom-sheet rise/dismiss,
- card/list insertion and removal,
- completion animations,
- move/reschedule feedback,
- expand/collapse animation,
- swipe actions,
- state-change animation,
- subtle FAB response.

### Haptics

Use restrained vibration where it confirms a meaningful action, for example:
- successful completion,
- destructive confirmation,
- drag/drop or move confirmation,
- selected snap point,
- important state toggle.

Do not vibrate for every tap.

### Swipe actions

Appropriate examples:
- complete,
- restore,
- contextual secondary action.

Destructive swipe behavior should require clear feedback and safe confirmation/undo where appropriate.

Reference:
- `32_interactions_motion_reference.png`

---

## 17. Settings additions

The redesign should expose the following user preferences where appropriate:

- cancellation display style:
  - strikethrough,
  - hidden,
  - marked;
- show hidden lessons;
- week parity mode;
- card appearance:
  - elevated,
  - tonal filled;
- theme family:
  - Blue,
  - Green,
  - Purple,
  - Warm,
  - Monochrome;
- light / dark / system theme;
- preferred week-view layout;
- existing grading-scale settings;
- existing backup/restore.

Preferences should not fragment the design system. They alter appearance or display mode, not core information architecture.

---

## 18. Accessibility and responsive behavior

### Touch targets

Interactive targets should be approximately 48dp minimum where possible.

### Font scaling

The UI must remain functional with increased font size:
- do not depend on one-line labels everywhere,
- avoid rigid rows that overflow,
- cards should expand vertically,
- segmented controls may need scrollable/adaptive behavior.

### Small phones

Avoid:
- fixed two-column layouts for long content,
- oversized headers,
- too many controls in one horizontal row.

### Large phones / landscape

Use extra space where useful:
- richer week timetable,
- wider calendar grids,
- more breathing room,
- do not simply stretch cards to excessive width without structure.

### State must not rely on color alone

Selected, cancelled, completed, warning, and exam status require icon/text/shape support in addition to color.

---

## 19. Edge-to-edge and system UI

Because the project targets SDK 35, the redesigned UI should be intentionally edge-to-edge aware.

Requirements:
- safe insets,
- status/navigation bar contrast,
- bottom navigation/FAB must not conflict with gesture regions,
- sheets and dialogs respect system areas,
- consistent behavior on supported Android versions.

---

## 20. Reusable design-system component families

The redesign should consolidate repeated visual patterns into reusable components in `:core:ui`.

Conceptual families:

- App screen header
- Section title / section action
- Elevated card
- Tonal filled card
- Metric / summary card
- Compact status block
- Subject color marker
- Timeline event row
- Deadline row
- Course row
- Grade/progress row
- Status pill
- Empty state
- Quick action item
- Segmented tabs
- Bottom navigation/FAB shell
- Bottom-sheet menu item
- Form field wrappers
- Progress / eligibility indicator

Exact implementation details belong in the implementation plan.

---

## 21. Existing architecture constraints

Preserve the current modular structure:

- `:app`
- `:core:model`
- `:core:database`
- `:core:data`
- `:core:ui`
- `:feature:home`
- `:feature:calendar`
- `:feature:subjects`
- `:feature:studyplan`
- `:feature:settings`

Do not rearchitect the project into a monolith.

Preserve:
- Room migrations,
- backup compatibility,
- current user data,
- existing functional behavior unless explicitly redesigned in this spec.

---

## 22. Data-model implications for implementation planning

The implementation plan must assess whether the existing model already represents the following cleanly:

- course grading type:
  - exam/letter grade,
  - pass/fail,
  - custom grading;
- scored assessment contribution;
- required-but-unscored task;
- exam eligibility requirement;
- exam date/time/location;
- exam attempt number;
- maximum attempts;
- exam materials/attachments metadata;
- exam notes;
- maximum possible score/grade calculation;
- secured points vs remaining exam points;
- possible final grade range;
- preferred week layout;
- preferred card style;
- theme selection.

If new fields/entities are needed:
- add migrations safely,
- preserve existing backups where possible,
- version backup changes explicitly.

---

## 23. Error, empty, and edge states

Design must include reasonable states for:

- no classes today,
- no upcoming deadlines,
- no grades yet,
- exam date unknown,
- room unknown,
- no materials attached,
- exam not applicable,
- pass/fail course,
- eligibility not met,
- all required work complete,
- cancelled class,
- moved class,
- offline/local-only state,
- long subject names,
- zero-point or optional assessments.

Do not use fake placeholder metrics when data is unavailable.

---

## 24. Non-goals

The redesign does **not** require:
- a desktop-style dashboard,
- AI-generated recommendations,
- social/community features,
- cloud-first architecture,
- glassmorphism,
- decorative analytics charts without a real student decision use case,
- replacing existing module boundaries,
- redesigning functionality that was not discussed unless necessary for consistency.

---

## 25. Acceptance criteria for the redesign

The visual redesign is successful when:

1. Home communicates the next class, today schedule, tasks/deadlines, and progress quickly.
2. Day calendar uses a readable timeline with gaps.
3. Month calendar remains familiar and compact.
4. Week has at least the approved compact-list mode plus an alternative timetable mode.
5. Course detail uses segmented tabs.
6. Before exam period, the UI emphasizes tasks, completion, eligibility, and maximum achievable outcome rather than a fake final grade.
7. During exam period, the exam and possible grade range become the visual focus.
8. Pass/fail courses are represented without forcing A–FX.
9. Add/Edit follows the approved five-part hierarchy.
10. Navigation uses the FAB + core tabs pattern.
11. Card style, theme, and week layout preferences are supported.
12. Dark mode preserves hierarchy and subject differentiation.
13. Motion is smooth, restrained, and useful.
14. Font scaling, small screens, and touch targets remain usable.
15. Existing functionality, Room migrations, and backup compatibility are preserved.
16. The implemented screens visibly match the approved reference pack rather than merely approximating it.

---

## 26. Reference index

Persistent reference folder:

`/Takt Design References`

Most important files:

- `03_overall_card_heavy_dashboard.png`
- `04_home_card_based_overview.png`
- `05_home_timeline.png`
- `06_calendar_day_timeline.png`
- `07_calendar_month_google_style.png`
- `12_cards_elevated.png`
- `13_cards_tonal_filled.png`
- `17_subject_course_detail_segmented_tabs.png`
- `19_grades_preexam_task_focused.png`
- `20_grades_preexam_summary_blocks.png`
- `23_grades_exam_period_card_based.png`
- `24_navigation_variant3_fab_tabs.png`
- `27_week_compact_list.png`
- `28_add_edit_all_five.png`
- `29_color_themes_all_five_more_variety.png`
- `30_typography_approved.png`
- `31_dark_mode_approved.png`
- `32_interactions_motion_reference.png`

Supporting written records:
- `README.md`
- `33_final_visual_decisions.md`

---

## 27. Next step after review

After the user reviews and approves this document, create a detailed implementation plan before changing application code.

Implementation must happen on a feature branch, not on `main`.

