# Selected Takt UI implementation

**Goal:** Implement approved Г2 · К1 · С1 · П2 · Е1 · Р2 in Compose with close visual fidelity.
**Architecture:** Keep repositories and domain calculations; replace presentation. Shared typography, palette and controls stay in core/ui. Native screens use real data and accessible actions.
**Spec:** /home/kpyr/Projects/Takt/design-explorations/2026-09-22-v2/app.js and style.css, plus user adjustments: prominent earned semester points in П2, check icons in completed Р2 nodes and clock in active nodes.

## Constraints and decisions
- Existing feature/pulse-design checkout contains authorized previous work; preserve it. No commit/push/release/version/schema changes.
- HTML is a visual specification, not dummy production data. Maintain editing, all five course tabs, existing themes, accessibility and large-font fallback.
- Native dp correspond to CSS px. 20dp screen inset; Inter; 29sp bold primary titles; 15sp section titles; 12sp supporting text; thin separators and 13–18dp corners. Avoid default oversized Material cards/chips.
- White paper background for Г2/П2/Р2, pale #F6F8FC for К1/С1/Е1. Theme-aware dark equivalents.
- Use one consistent four-destination bottom bar as Г2/Р2; add actions in top app bars. No central FAB. Detail and exam have no bottom bar.
- П2: admission first, prominent earned-points card directly beneath it, then required work. This preserves action priority while giving achievement first-screen weight.
- All image handoffs must be rendered with view_image and linked by absolute path. Background/headless runtime only.

## Task 1: Shared visual system and app integration (root)
- [x] Refine ScreenHeader, palette, typography, navigation and compact underline tab helper.
- [x] Wire add/settings and repository callbacks without dropping features.
- [x] Expand opt-in instrumentation to cover six selected screens and accessibility modes.

## Task 2: Home Г2 and calendar К1
- [x] Implement date header, working week strip and unified chronological schedule/deadlines; preserve untimed tasks and completion.
- [x] Match calendar compact header/segments/date strip/deadline strip and proportional event geometry.
- [x] Keep week/month modes, event editing, live clock and font scaling fallback.
Files: feature/home and feature/calendar only. Shared API requests go to root.

## Task 3: Subjects С1, course П2 plus score emphasis, exam Е1
- [x] Flat searchable subject rows, compact icon/progress/next action, active/all/closed filters.
- [x] Compact course toolbar/header/five underline tabs; administrative controls in menu/dialog.
- [x] Admission callout, earned-points hero, required task rows, useful resources.
- [x] Exam standalone-looking header, date ticket, metric pair, target-grade card, potential bar, admission and resources; preserve result editing and grade math.
Files: feature/subjects only. Shared API requests go to root.

## Task 4: Progress Р2
- [x] Credits summary and vertical semester journey, check/clock/open-circle nodes based on real state.
- [x] Active next action and drill-down to existing plan management. Preserve semester operations.
Files: feature/studyplan only. Tell root required navigation callbacks.

## Task 5: Runtime and visual verification
- [x] Build, existing model/data tests and lint with JDK17/Gradle8.11.1.
- [x] API35 opt-in fixture flow covering home/calendar/subjects/course/exam/progress; screenshot each.
- [x] Inspect actual raster screens against selected HTML reference; fix material differences.
- [x] Test font scale1.5 and dark mode, grade goal switching, completion and key navigation.
- [x] Review implementation and report actual validation limits and deliver APK/screens.

Completion evidence: `/home/kpyr/Projects/Takt/design-explorations/2026-09-22-v2/native/QA.md`. All selected screens implemented; review fixes validated; final APK and native screenshots delivered.
