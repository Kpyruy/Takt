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
