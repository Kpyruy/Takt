# Takt

Takt is an offline-first Android study planner for university schedules, grades, deadlines, and study progress.

The project is currently private and under active development. It is designed as a modular Kotlin/Jetpack Compose application so individual areas such as timetable, grades, and study plan can evolve independently.

## Modules

- `app` — Android entry point and navigation shell
- `core:model` — Android-free domain models and calculations
- `core:database` — Room database, entities, and DAOs
- `core:data` — repositories and local data orchestration
- `core:ui` — shared Compose theme and reusable UI components
- `feature:home` — dashboard and today's timetable
- `feature:calendar` — timetable/calendar
- `feature:subjects` — subjects and grade tracking
- `feature:studyplan` — six-semester study plan and credit progress
- `feature:settings` — app preferences and future backup controls

## Current target

Android, Kotlin, Jetpack Compose, Room, offline-first.

## Build locally

Use JDK 17, Android SDK platform 35 and build tools 35.0.0. Set `ANDROID_HOME` to your SDK directory, or put `sdk.dir=/absolute/path/to/Android/Sdk` in the ignored `local.properties` file.

```sh
./gradlew :app:assembleDebug
./gradlew :core:model:test :core:data:testDebugUnitTest :app:lintDebug
```

The wrapper pins Gradle 8.11.1 and verifies the distribution SHA-256. Android Studio is optional. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

The opt-in `PulseFlowTest` writes review fixtures through the real repositories. Run it only on a disposable emulator (API 35 recommended), with `-e pulseReview true`; it is skipped without that flag. See `design-explorations/2026-09-22/NATIVE-QA.md` for the reviewed screens and verification details.
