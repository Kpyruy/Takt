# Selected UI — verification, 2026-09-23

Implemented Г2 · К1 · С1 · П2 · Е1 · Р2 in Compose. Semester achievement is prominent below admission in П2. Completed semester markers contain checks; active markers contain clocks; future markers remain open circles.

## Validation

- JDK 17 / Gradle 8.11.1: debug APK and instrumentation APK built successfully.
- Model: 44 tests, 0 failures/errors. Data: 20 tests, 0 failures/errors. Existing test results were up-to-date on the resumed build.
- Final build after Home large-font wrapping/copy refinement: successful in 39 seconds.
- Android lint: 0 errors, 14 warnings. Warnings remain; this is not a warning-free build.
- Opt-in fixture instrumentation on headless Android 15 / API 35: final light run PASS (15.812s), font scale 1.5 PASS (18.541s), dark PASS (16.344s).
- Runtime assertions: all six screens, A→B→A goal calculation (68→59→68 exam points), back from exam, completed/active semester markers, task deep link, actual task completion changes admission but preserves earned points, earlier assessment wins over a future task, grade-only admission opens Grades, all-creation sheet reachable.
- All 18 screen PNGs and overview contact sheet visually inspected. Calendar adapts to an agenda at large font; tabs and days scroll horizontally. Home deadline date moves below the title at large font.
- Source review's two P2 findings are fixed and exercised by instrumentation. Root rechecked the deadline comparison and task/assessment destination dispatch.

## Fidelity and scope

Matched approved composition, type scale, cobalt palette, thin separators, rounded surfaces and four-item navigation. Kept native touch targets at least 48dp for shared tab/icon controls. No claim of pixel-identical rendering across every Android device.

Native data differs from mock data: tasks store a date without a time, so the UI does not invent a 14:00 deadline; schedule entries do not store lecture/practice type; academic years are not fabricated. The calendar includes the real 08:00 fixture class, which shifts the visible range compared with the 09:00 mock. Full course names can wrap. Long content scrolls.

All displayed points, credits, statuses and deadlines in these captures are disposable test fixtures, not personal academic records. Screenshots show the initial viewport. The fixture uses repository writes through the application; it is opt-in and is not bundled as production seed behavior.

Runtime validation covers this fixture on one API 35 emulator (1008 × 2160, 420dpi), not every dialog, device, theme family, locale or accessibility setting. No physical phone tested. No commit, push, release, version or database schema change.

## Artifact

APK: [app-debug.apk](/home/kpyr/Projects/Takt/app/build/outputs/apk/debug/app-debug.apk)

SHA-256: `f930efaaffa030e5375220493d314a0ea8e8faf987a8ebdd84629f6e91e7568a`

Reproducible test source: `/home/kpyr/Projects/Takt/app/src/androidTest/java/com/kpyruy/takt/app/PulseFlowTest.kt`. Pass `-e pulseReview true` only on a disposable emulator because it replaces fixture data. Full command output is in `evidence/` beside this report.
