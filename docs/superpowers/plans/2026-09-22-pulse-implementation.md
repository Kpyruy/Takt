# Pulse Android implementation plan

> Execute with subagent-driven-development. User approved variant A and implementation on 2026-09-22.

**Goal:** Implement the approved Pulse visual language in the existing Android app, with real data and preserved functionality.
**Architecture:** Keep the modular Compose architecture and existing repositories. Shared theme/components define Pulse; Home, calendar and course screens use them. Calendar time placement is pure model logic with tests.
**Tech stack:** Kotlin 2.1.21, Compose BOM 2025.05.01, AGP 8.10.1, Gradle 8.11.1, Android SDK 35.
**Spec:** `/home/kpyr/Projects/Takt/design-explorations/2026-09-22/index.html`, `styles.css`, `app.js`, `previews/comparison.png`, `previews/calendar.png`, `previews/course.png`, `previews/exam.png`; choose Pulse only. Original product constraints remain in `/home/kpyr/Projects/Takt-docs/product/`.

## Global constraints

- Do not change database schemas, backups, persisted settings, grade arithmetic, app version, or repository interfaces for styling.
- Keep all five color families, dark/system theme, card appearances, calendar modes, required/optional work, pass/fail courses, assessment editing, notes and exam materials.
- Use real repository data. Never embed the HTML sample schedule or scores in production.
- Android typography uses scalable sp and minimum 48dp interactive targets. Preserve long course names through wrapping/scrolling.
- No push, merge, release, or automatic commit. Work in the current checkout on `feature/pulse-design`; preserve the untracked approved concepts.
- HTML is the visual authority, with recorded native adaptations for accessibility, real data, and features omitted from the prototype.

## Task 1: Theme, home and app shell — root

Files: `core/ui/.../theme/*`, `core/ui/.../components/*`, `feature/home/.../*`, `app/.../TaktApp.kt`, build support as needed.

- [x] Run baseline model/data/app tests and debug build with Gradle 8.11.1 / JDK 17 and installed SDK35.
- [x] Apply cobalt `#355BCE`, background `#F4F6FA`, white surface, text `#19273C`, muted `#607087`, line `#E3E8F1`, soft primary `#E9EFFF`; dark tokens from approved Pulse mockup. Complete paired foreground/background roles for all retained palettes.
- [x] Bundle Inter regular/medium/semibold/bold with license. Use 28sp headline, 16sp section, 14sp body, readable labels, 20dp card radius and low elevation.
- [x] Replace independent filter chips with a single adaptive/scrollable selected tab rail. Keep existing public signatures.
- [x] Align four bottom destinations around the central 48dp FAB, keep system insets and semantic selection.
- [x] Home: date before title, colored next-event card, concise counters, deadlines before schedule, highlighted next row and compact credit progress. Preserve recent grades, courses and quick actions below.
- [x] Wire next class and section links to real navigation. Refresh current time while visible so countdowns do not become stale.

## Task 2: Calendar — calendar implementer

Owned files: `feature/calendar/src/main/java/...`, new `core/model/.../DayTimelineLayout.kt` and its tests. Do not edit shared UI, other existing model files or build configuration.

- [x] Inspect existing calendar selection, event actions, week/month paths and approved Pulse calendar.
- [x] Add a pure chronological time-layout calculation: minutes from range start, duration, and collision lane allocation. Test proportional duration/gap, overlap chains, boundary-touching intervals and empty input.
- [x] Replace the equal-row day layout with a proportional time grid (64dp/hour nominal; use a chronological fallback for very short events or increased font scale), tinted subject blocks, room/time labels, current-time line, textual moved/cancelled indicators and existing click callbacks.
- [x] Keep untimed deadlines outside the timed grid. Preserve all actions and alternate weekly/month views. Style the date strip and month selection to Pulse.
- [x] Verify model tests; root runs integrated build and emulator. Document any layout deviation needed for tiny/overlapping events.

## Task 3: Subjects and exams — subjects implementer

Owned files: `feature/subjects/src/main/java/...`. Do not edit shared UI, model, repositories or build files.

- [x] Inspect course/task/assessment/exam behavior and approved Pulse course/exam screenshots.
- [x] Compact course header with code/title/credits, retain all five real tabs and navigation.
- [x] Overview: separate earned coursework points from exam potential, highlight admission progress, put upcoming work before administrative course settings. Keep controls available.
- [x] Exam: logistics in a tinted leading card, coursework and remaining exam metrics, explicit maximum/possible result with secured versus available progress. Never show a final letter grade before result exists. Handle absent exam/empty data/pass-fail accurately.
- [x] Admission uses completed/required progress and text, without conflating completion with points. Preserve task edit/delete/swipe, materials, notes, grading scale and manual grade overrides.
- [x] Align remaining subject lists/forms with shared tokens without deleting functionality. Root handles compilation and integrated validation.

## Task 4: Integration, review, Android verification — root + reviewer

- [x] Review each scope and fix compile/regression findings.
- [x] Run model, data and app unit tests, lintDebug and assembleDebug.
- [x] Create an isolated test AVD if needed. Install only into that emulator, inspect real Home/calendar/course/exam plus dark theme and increased font scale.
- [x] Capture screenshots through adb, render each final PNG with view_image and link it in the handoff.
- [x] Compare real screens to approved Pulse (hierarchy, palette, type, cards, navigation, timeline, progress), record unavoidable differences and limits.
- [x] Deliver code, debug APK, test evidence and screenshot paths. Do not claim production fidelity from static checks alone.

## Execution ledger

- Baseline: `3153654`. Feature branch created; approved concepts preserved.
- Ownership scan: Task 1 owns shared APIs without changing signatures. Task 2 consumes theme/tabs and owns a new standalone model helper. Task 3 consumes theme/tabs and keeps all domain APIs unchanged. Task 4 integrates all three; no shared file mutations between implementers.
- Ruling: use the already-cloned checkout on a feature branch because user asked to change code here; a second worktree adds no useful isolation for this fresh checkout.

- Final integration: 44 model tests + 20 data tests pass; no app JVM tests existed. Debug app/test APK and lintDebug pass on JDK 17. The explicit Kotlin JVM target is 17; official Gradle wrapper 8.11.1 and SHA-256 are included.
- Runtime: isolated API 35 Pixel 6 emulator, 1080×2400 at 420dpi. Home → day calendar → course → exam → admission update → dark mode passes at font scales 1.0 and 1.5. Review fixtures are opt-in and emulator-only.
- Review fixes: bounded timeline content keeps 45/60-minute grids; invalid effective moved times use agenda; 48dp scrolling date cells; manual grade labeled separately; swipe background no longer bleeds through task rows; system status/navigation icon contrast follows the chosen app theme.
- Native deviations and screenshots: `design-explorations/2026-09-22/NATIVE-QA.md`. Large font/dense or tiny-event calendars use a readable agenda. Controls and extra real app features use scrolling, rather than matching the prototype's fixed dimensions.
- No commits, push, release, app version change or schema migration. The build-generated unchanged Room schema snapshot is omitted from the redesign diff.
