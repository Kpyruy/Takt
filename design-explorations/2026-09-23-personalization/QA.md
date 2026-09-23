# Personalization and progress — 2026-09-23

## Delivered

- Lesson type: unspecified, lecture, seminar, practice or lab. Editable in recurring, one-off and quick-add flows. Resolved normal, cancelled and moved events preserve the type. Home/calendar present type text and a distinct icon; compact week cells use an abbreviation.
- Optional subject link in lesson forms ensures the selected subject icon is used for the right class.
- 109 bundled Material outlined icons with Ukrainian/English search, categories and reset to initials. Select from the subject header/settings or the progress status sheet. Icons are stored per course and appear in subjects, details, progress and linked schedule events.
- Root menu gestures: right goes to the next menu destination, left to the previous; stops at either end. Child-consumed gestures, vertical drags, short drags, multitouch and the outer 24dp system-back area are excluded. Root detection is disabled for detail routes and global add sheets.
- Progress shows passed counts/bars by semester, expandable subject rows with explicit status icons, and Active/Passed lists across all semesters. Every course status can be changed in place. Marking a course passed updates earned credits; a fifth-semester course can be active alongside third-semester courses.

## Data compatibility

Room schema 8→9 adds nullable courses.iconKey and non-null lessonType with UNSPECIFIED defaults to recurring and one-off event tables. No destructive migration or data reset. Existing class types stay unspecified until the user chooses one; no guessed classification.

Installed over the previous APK on the disposable API35 emulator. Compared all previous table columns/rows: 53 courses, 3 schedule rules, 7 grade records, 7 tasks and 1 exam record were preserved exactly. The existing startup seeder added its default one-off event to the previously empty table; the migration itself is additive. See evidence/takt-personalization-migration.json.

Backup mappings retain icon/type fields; older version-2 backups without these optional fields still load. Unit tests cover course icon round-trip and recurring/one-off metadata round-trip. No app version change, commit, push or release.

## Verification

- Model: 47 tests, 0 failures/errors. Data: 22 tests, 0 failures/errors.
- Final app and instrumentation APK build + lint: BUILD SUCCESSFUL (24s). Lint: 0 errors, 14 warnings.
- Final light interaction flow: PASS, 27.309s.
- Font scale 1.5 flow: PASS, 37.819s.
- Dark flow after adding a selected-chip assertion: PASS, 35.965s; confirmation PASS, 31.277s.
- Original six-screen/exam/admission regression flow: PASS, 27s.
- Native screenshots reviewed for normal, large-font and dark presentation.

The interaction flow checks menu swipes in both directions and at boundaries, a short drag, a consumed day-strip swipe, and actual task-completion swiping without changing the root menu. It selects and persists a course icon through the UI, activates a fifth-semester course, finds it in the active list, marks it passed and verifies the credit increment, restores its active status, edits a lesson type and checks the result on Home, then recreates the activity and rechecks saved values.

One earlier dark test timed out waiting for the edit-save operation; its database retained both the old title and old type. It did not reproduce in two subsequent runs with an explicit assertion that the requested type was selected before editing the title. No production change was made in response to that intermittent test failure; the initial failure log is retained. The precise cause of that one missed save was not established.

Validation uses one headless Android15/API35 emulator (1008×2160, 420dpi), not a physical phone or every device configuration. The pictures contain disposable fixture data, not the user's academic record. They capture scroll positions used by the test, so large-font active-list/header positions differ. Existing lint warnings remain.

## APK

[app-debug.apk](/home/kpyr/Projects/Takt/app/build/outputs/apk/debug/app-debug.apk)

SHA-256: `6f36f964519f9af320dcac4e6ee74dd86148f2a17f970f9ebda55f790dc35829`

## Use

Тип пари: календар → пара → редагування → «Тип заняття».
Іконка: предмет → значок справа від назви або меню → «Іконка предмета».
Статус: прогрес → план → розгорнути семестр → натиснути статус біля предмета.
Свайпи: вільне місце, вправо — наступний розділ, вліво — попередній.
