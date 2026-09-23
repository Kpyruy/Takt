# Pulse on Android

Approved concept: A / Pulse. Implementation branch: `feature/pulse-design`, based on `3153654`. No app version or database migration was changed.

## Implementation

- Bundled Inter, cobalt palette, neutral surfaces, 20dp cards, shared navigation and scrolling tab rails. Existing palettes, dark/system mode and card preferences remain available.
- Home uses repository data, a live next-class card, deadlines before schedule, visible task checkboxes and compact credit progress. Existing recent grades, course access and quick actions remain.
- Day calendar uses a 64dp/hour grid with overlap lanes. Events shorter than 45 minutes, more than two overlap lanes, invalid moved-event times or font scale above 1.15 use a chronological list. This avoids overlapping text and preserves full event details. Normal 45–60 minute classes keep proportional placement. Week and month modes remain.
- Course/exam screens separate coursework points, admission requirements, potential and manual grades. A potential A is explicitly not an earned grade. Existing editing, tasks, notes, materials, grading scales and manual overrides remain.
- Date cells are at least 48dp and scroll horizontally on narrow screens. Native system bars, 48dp controls and all five course tabs take more space than the HTML concept; layouts use scrolling rather than shrinking text.

## Build

JDK 17, Gradle 8.11.1, AGP 8.10.1, SDK 35. Wrapper and distribution checksum are included. Android Studio UI is optional.

The initial host Java 26 build was incompatible with this Gradle setup. A stale class from an independent build was also rebuilt with JDK 17. Final verification uses the same JDK throughout.

## Verification

Final verification on 2026-09-22:

- `:core:model:test`: 44 tests, 0 failures/errors, including four proportional timeline/lane tests.
- `:core:data:testDebugUnitTest`: 20 tests, 0 failures/errors.
- `:app:testDebugUnitTest`: no test sources; not counted as additional tests.
- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:lintDebug`: successful.
- `git diff --check`: clean.
- API 35 Pixel 6 emulator (1080×2400, 420dpi): one complete UI scenario passed at font scale 1.0 (11.08s) and again at 1.5 (10.87s). The scenario validates repository fixtures, Home/calendar/course/exam navigation, 24/30 coursework, 94/100 potential, admission change and dark mode. It also checks that the swipe completion hint is absent when the task row is idle.
- Inspected screenshots for default and 150% text sizes. Fixed transparent swipe-row backgrounds and dark-theme system icon contrast discovered in runtime inspection.
- Scope limit: this is a focused visual/navigation smoke test, not exhaustive device coverage or a full regression of every editing/backup action. Android 37 preview instrumentation remains incompatible with the current Espresso version; app startup itself worked there.

Final APK: `/home/kpyr/Projects/Takt/app/build/outputs/apk/debug/app-debug.apk`.

Run the opted-in device scenario after installing the debug and androidTest APKs:

```sh
adb -s emulator-5580 shell am instrument -w -r -e pulseReview true -e class com.kpyruy.takt.app.PulseFlowTest com.kpyruy.takt.test/androidx.test.runner.AndroidJUnitRunner
```

Screenshots use explicitly seeded review data; the production APK does not create these fixtures. The calendar screenshot is scrolled to the evening fixture, so the genuine empty hours remain visible. System controls and all scrollable content are native Compose UI.

The instrumentation flow is opt-in: it writes `pulse-test-*` fixtures and changes course grading/settings through real repositories. Run only on a disposable AVD; it is not a production demo-data feature. The test skips unless `pulseReview=true` is passed.

The installed Android 37 preview runs the application, but the current Espresso dependency fails before the UI test body (`InputManager.getInstance` removed). API 35 is the supported review target for this run.

## Screenshots

- [pulse-home](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-home.png) · [150%](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-home-large.png)
- [pulse-calendar](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-calendar.png) · [150%](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-calendar-large.png)
- [pulse-course](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-course.png) · [150%](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-course-large.png)
- [pulse-exam](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-exam.png) · [150%](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-exam-large.png)
- [pulse-home-dark](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-home-dark.png) · [150%](/home/kpyr/Projects/Takt/design-explorations/2026-09-22/native/pulse-home-dark-large.png)

APK SHA-256: `aa2ce67bce7dee67f65b43173b9b956e47d9d3ab8cba7445fb4bdfd7324aeb1e`.
