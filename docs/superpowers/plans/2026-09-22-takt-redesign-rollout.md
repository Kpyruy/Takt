# Takt Redesign Rollout Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the approved Takt redesign without breaking existing data, modular boundaries, backup compatibility, or the verified Android build pipeline.

**Architecture:** Keep the existing `:app`, `:core:*`, and `:feature:*` module boundaries. Roll the redesign out in four independently testable phases: shared design foundation, Home/Calendar/Add-Edit UX, academic progress/grading domain, then study-plan/settings/motion/accessibility polish. Each phase ends with the repository's full verification commands before the next phase starts.

**Tech Stack:** Kotlin 2.1.21, Jetpack Compose + Material 3, Room 2.7.1, Coroutines/Flow, Kotlin Serialization, Navigation Compose, Java 17, compile/target SDK 35.

---

## Source of truth

Read before implementation:

- `docs/design/takt-design-system.md`
- ChatGPT Library: `/Takt Design References`
- Reference images named in the design spec

Do not substitute a different visual pattern without surfacing the conflict first.

## Branching

Implementation must not happen on `main`.

Start from the approved docs branch so the design spec and plans travel with the implementation:

```bash
git fetch origin
git switch docs/takt-design-system
git pull --ff-only
git switch -c feature/design-redesign
```

Expected: current branch is `feature/design-redesign` and includes `docs/design/takt-design-system.md`.

## Phase order

1. **Foundation and preferences**  
   Plan: `docs/superpowers/plans/2026-09-22-takt-foundation.md`  
   Outcome: theme families, typography, shapes, card style, persisted preferences, edge-to-edge, reusable UI primitives, FAB navigation shell.

2. **Home, Calendar, and Add/Edit**  
   Plan: `docs/superpowers/plans/2026-09-22-takt-home-calendar.md`  
   Outcome: approved Home dashboard, day timeline, month grid, dual week modes, global add entry point, quick add/full forms, Material date/time pickers.

3. **Subjects, grading, and exam progress**  
   Plan: `docs/superpowers/plans/2026-09-22-takt-academic-progress.md`  
   Outcome: segmented course detail, task-focused pre-exam UX, exam-period cards, projection math, pass/fail support, Room migration, backward-compatible backup v2.

4. **Study plan and product polish**  
   Plan: `docs/superpowers/plans/2026-09-22-takt-polish.md`  
   Outcome: semester overview polish, settings completion, motion/haptics, accessibility/large-font checks, dark-mode QA, final integration verification.

## Verification gate after every phase

Run all four commands exactly:

```bash
gradle :core:model:test --stacktrace
gradle :core:data:testDebugUnitTest :app:testDebugUnitTest --stacktrace
gradle :app:lintDebug --stacktrace
gradle :app:assembleDebug --stacktrace
```

Expected:
- all test tasks finish with `BUILD SUCCESSFUL`;
- lint finishes with no fatal errors;
- assemble finishes with `BUILD SUCCESSFUL`;
- APK exists at `app/build/outputs/apk/debug/app-debug.apk`.

Do not claim a phase is complete from a partial command or an old CI run.

## Commit discipline

Keep commits small enough to revert independently. Preferred sequence:

```text
feat: add persisted visual preferences
feat: establish Takt design tokens
feat: add FAB navigation shell
feat: redesign home dashboard
feat: add calendar timeline and week modes
feat: modernize add and edit flows
feat: model exam progress and eligibility
feat: migrate academic progress storage
feat: redesign course progress views
feat: polish study plan and settings
feat: add motion haptics and accessibility polish
test: cover redesign regressions
```

## Final acceptance

Before requesting merge:
- re-read `docs/design/takt-design-system.md`;
- compare every implemented screen with the saved reference pack;
- run the four verification commands fresh;
- inspect light and dark mode on a small-phone preview and a larger-phone preview;
- verify long Slovak course names and increased font scaling;
- export a JSON backup, import it into a fresh app database, and verify courses/schedule/grades/tasks/notes/settings/exam metadata survive;
- verify a pre-redesign v1 backup still imports;
- verify no destructive Room migration path was introduced.
