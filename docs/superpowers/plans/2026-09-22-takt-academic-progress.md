# Takt Academic Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current generic percentage-first grading UX with the approved student-specific model: task importance before exams, exam-focused projection during exam period, explicit eligibility, pass/fail courses, exam logistics/materials, and truthful A–FX possibilities.

**Architecture:** Extend existing course/grade/task models rather than replacing them. Keep scored components in `GradeItem`, unscored work in `StudyTask`, add explicit exam metadata/resources through a new `ExamRepository`, and calculate projections with pure functions in `:core:model`. Persist additions through a Room 7→8 migration and export them in backup v2 while continuing to accept v1 JSON.

**Tech Stack:** Kotlin, Room, Coroutines/Flow, Kotlin Serialization, Jetpack Compose, Material 3.

---

### Task 1: Define grading semantics with failing domain tests

**Files:**
- Create: `core/model/src/test/kotlin/com/kpyruy/takt/core/model/GradeProjectionTest.kt`
- Modify: `core/model/src/test/kotlin/com/kpyruy/takt/core/model/GradeBookTest.kt`

- [ ] **Step 1: Write projection tests**

Create `GradeProjectionTest.kt`:

```kotlin
package com.kpyruy.takt.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GradeProjectionTest {
    private val scale = GradeScale.default()

    private fun item(
        id: String,
        earned: Double,
        max: Double,
        completed: Boolean,
        type: GradeItemType = GradeItemType.HOMEWORK,
    ) = GradeItem(
        id = id,
        courseId = "course",
        title = id,
        type = type,
        earnedPoints = earned,
        maxPoints = max,
        recordedAtEpochMillis = 0L,
        dueDate = LocalDate.of(2026, 10, 1),
        completed = completed,
        requiredForExam = false,
    )

    @Test
    fun completedZeroPointTasksReduceMaximumPossibleGrade() {
        val items = listOf(
            item("task-1", 0.0, 10.0, completed = true),
            item("task-2", 0.0, 10.0, completed = true),
            item("task-3", 0.0, 10.0, completed = true),
            item("exam", 0.0, 70.0, completed = false, type = GradeItemType.EXAM),
        )

        val projection = GradeProjection.calculate(items, scale)

        assertEquals(70.0, projection.maximumPossiblePoints, 0.001)
        assertEquals(GradeLetter.FX, projection.minimumPossibleLetter)
        // With Takt's default scale, 70% maps to D (C starts at 74%).
        // The key behavior is that lost coursework points permanently lower the ceiling.
        assertEquals(GradeLetter.D, projection.maximumPossibleLetter)
        assertNull(projection.examPointsNeeded[GradeLetter.A])
        assertNull(projection.examPointsNeeded[GradeLetter.B])
        assertNull(projection.examPointsNeeded[GradeLetter.C])
        assertEquals(65.0, projection.examPointsNeeded[GradeLetter.D]!!, 0.001)
    }

    @Test
    fun securedCoursePointsLowerRequiredExamScore() {
        val items = listOf(
            item("coursework", 30.0, 30.0, completed = true),
            item("exam", 0.0, 70.0, completed = false, type = GradeItemType.EXAM),
        )

        val projection = GradeProjection.calculate(items, scale)

        assertEquals(30.0, projection.securedPoints, 0.001)
        assertEquals(62.0, projection.examPointsNeeded[GradeLetter.A]!!, 0.001)
        assertEquals(53.0, projection.examPointsNeeded[GradeLetter.B]!!, 0.001)
    }

    @Test
    fun completedExamCollapsesPossibleRangeToActualLetter() {
        val items = listOf(
            item("coursework", 30.0, 30.0, completed = true),
            item("exam", 60.0, 70.0, completed = true, type = GradeItemType.EXAM),
        )

        val projection = GradeProjection.calculate(items, scale)

        assertEquals(GradeLetter.B, projection.minimumPossibleLetter)
        assertEquals(GradeLetter.B, projection.maximumPossibleLetter)
        assertEquals(0.0, projection.examRemainingPoints, 0.001)
    }
}
```

- [ ] **Step 2: Add pass/fail model test**

Append to `AcademicRulesTest.kt`:

```kotlin
@Test
fun passFailCourseCanRepresentExplicitFailure() {
    val course = Course(
        id = "pf",
        code = "PF",
        title = "Practice",
        credits = 1,
        semester = 3,
        status = CourseStatus.ENROLLED,
        gradingType = CourseGradingType.PASS_FAIL,
        passFailResult = PassFailResult.FAILED,
    )

    assertEquals(PassFailResult.FAILED, course.passFailResult)
}
```

- [ ] **Step 3: Run tests to verify failure**

```bash
gradle :core:model:test --stacktrace
```

Expected: compilation failure because the new fields/types/calculator do not exist.

---

### Task 2: Implement course, assessment, task, and exam domain models

**Files:**
- Modify: `core/model/src/main/kotlin/com/kpyruy/takt/core/model/AcademicModels.kt`
- Modify: `core/model/src/main/kotlin/com/kpyruy/takt/core/model/GradeModels.kt`
- Modify: `core/model/src/main/kotlin/com/kpyruy/takt/core/model/StudyModels.kt`
- Create: `core/model/src/main/kotlin/com/kpyruy/takt/core/model/ExamModels.kt`

- [ ] **Step 1: Add course grading type and pass/fail result**

In `AcademicModels.kt` add:

```kotlin
enum class CourseGradingType {
    EXAM_LETTER,
    CONTINUOUS_LETTER,
    PASS_FAIL,
}

enum class PassFailResult {
    PASSED,
    FAILED,
}
```

Extend `Course` with backward-compatible defaults:

```kotlin
val gradingType: CourseGradingType = CourseGradingType.CONTINUOUS_LETTER,
val passFailResult: PassFailResult? = null,
```

- [ ] **Step 2: Extend GradeItem to represent pending assessments**

Add to `GradeItem`:

```kotlin
val dueDate: java.time.LocalDate? = null,
val completed: Boolean = true,
val requiredForExam: Boolean = false,
```

Keep `earnedPoints >= 0` and `maxPoints > 0`. A pending scored assessment uses `completed = false` and normally `earnedPoints = 0.0`; a completed zero is `completed = true, earnedPoints = 0.0`.

- [ ] **Step 3: Extend StudyTask for exam eligibility**

Add:

```kotlin
val requiredForExam: Boolean = false,
```

to `StudyTask`.

- [ ] **Step 4: Add exam models**

Create `ExamModels.kt`:

```kotlin
package com.kpyruy.takt.core.model

import java.time.LocalDate
import java.time.LocalTime

data class ExamInfo(
    val courseId: String,
    val gradeItemId: String?,
    val date: LocalDate?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val room: String?,
    val attemptNumber: Int = 1,
    val maxAttempts: Int = 3,
    val notes: String = "",
) {
    init {
        require(attemptNumber >= 1)
        require(maxAttempts in 1..3)
        require(attemptNumber <= maxAttempts)
        require(endTime == null || startTime == null || endTime > startTime)
    }
}

data class ExamMaterial(
    val id: String,
    val courseId: String,
    val title: String,
    val uri: String,
)

data class ExamEligibility(
    val requiredCount: Int,
    val completedCount: Int,
) {
    val eligible: Boolean get() = completedCount >= requiredCount
}

object ExamEligibilityCalculator {
    fun calculate(
        tasks: List<StudyTask>,
        assessments: List<GradeItem>,
    ): ExamEligibility {
        val requiredTasks = tasks.filter { it.requiredForExam }
        val requiredAssessments = assessments.filter { it.requiredForExam }
        val requiredCount = requiredTasks.size + requiredAssessments.size
        val completedCount =
            requiredTasks.count { it.completed } +
                requiredAssessments.count { it.completed }
        return ExamEligibility(requiredCount, completedCount)
    }
}
```

- [ ] **Step 5: Implement GradeProjection**

Add to `GradeModels.kt`:

```kotlin
data class GradeProjection(
    val securedPoints: Double,
    val totalPoints: Double,
    val maximumPossiblePoints: Double,
    val examRemainingPoints: Double,
    val minimumPossibleLetter: GradeLetter?,
    val maximumPossibleLetter: GradeLetter?,
    val examPointsNeeded: Map<GradeLetter, Double?>,
) {
    companion object {
        fun calculate(
            items: List<GradeItem>,
            scale: GradeScale,
        ): GradeProjection {
            if (items.isEmpty()) {
                return GradeProjection(
                    securedPoints = 0.0,
                    totalPoints = 0.0,
                    maximumPossiblePoints = 0.0,
                    examRemainingPoints = 0.0,
                    minimumPossibleLetter = null,
                    maximumPossibleLetter = null,
                    examPointsNeeded = GradeLetter.entries.associateWith { null },
                )
            }

            val total = items.sumOf { it.maxPoints }
            val secured = items.filter { it.completed }.sumOf { it.earnedPoints }
            val unfinished = items.filterNot { it.completed }
            val maximum = secured + unfinished.sumOf { it.maxPoints }
            val exam = items.firstOrNull { it.type == GradeItemType.EXAM && !it.completed }
            val examRemaining = exam?.maxPoints ?: 0.0

            fun letter(points: Double): GradeLetter =
                scale.gradeFor((points / total * 100.0).coerceIn(0.0, 100.0))

            val pendingNonExamMax = unfinished
                .filterNot { it.type == GradeItemType.EXAM }
                .sumOf { it.maxPoints }

            // For pre-exam planning, assume all still-open non-exam work is completed
            // at its maximum; during exam period that value is normally zero.
            val pointsBeforeExamPotential = secured + pendingNonExamMax

            val examNeeded = GradeLetter.entries.associateWith { grade ->
                if (grade == GradeLetter.FX || exam == null) {
                    null
                } else {
                    val threshold = scale.bands.first { it.grade == grade }.minimumPercentage
                    val thresholdPoints = total * threshold / 100.0
                    val needed = (thresholdPoints - pointsBeforeExamPotential).coerceAtLeast(0.0)
                    needed.takeIf { it <= exam.maxPoints }
                }
            }

            return GradeProjection(
                securedPoints = secured,
                totalPoints = total,
                maximumPossiblePoints = maximum,
                examRemainingPoints = examRemaining,
                minimumPossibleLetter = letter(secured),
                maximumPossibleLetter = letter(maximum),
                examPointsNeeded = examNeeded,
            )
        }
    }
}
```

- [ ] **Step 6: Run model tests**

```bash
gradle :core:model:test --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add core/model
git commit -m "feat: model exam progress and eligibility"
```

---

### Task 3: Add Room schema v8 and exam storage

**Files:**
- Modify: `core/database/src/main/java/com/kpyruy/takt/core/database/CourseEntity.kt`
- Modify: `core/database/src/main/java/com/kpyruy/takt/core/database/GradeItemEntity.kt`
- Modify: `core/database/src/main/java/com/kpyruy/takt/core/database/StudyTaskEntity.kt`
- Create: `core/database/src/main/java/com/kpyruy/takt/core/database/ExamInfoEntity.kt`
- Create: `core/database/src/main/java/com/kpyruy/takt/core/database/ExamMaterialEntity.kt`
- Create: `core/database/src/main/java/com/kpyruy/takt/core/database/ExamDao.kt`
- Modify: `core/database/src/main/java/com/kpyruy/takt/core/database/TaktDatabase.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/TaktDataContainer.kt`

- [ ] **Step 1: Add columns to entities**

Add to `CourseEntity`:

```kotlin
val gradingType: String = "CONTINUOUS_LETTER",
val passFailResult: String? = null,
```

Add to `GradeItemEntity`:

```kotlin
val dueDateEpochDay: Long? = null,
val completed: Boolean = true,
val requiredForExam: Boolean = false,
```

Add to `StudyTaskEntity`:

```kotlin
val requiredForExam: Boolean = false,
```

- [ ] **Step 2: Create exam entities**

`ExamInfoEntity.kt`:

```kotlin
package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_info")
data class ExamInfoEntity(
    @PrimaryKey val courseId: String,
    val gradeItemId: String?,
    val dateEpochDay: Long?,
    val startMinute: Int?,
    val endMinute: Int?,
    val room: String?,
    val attemptNumber: Int,
    val maxAttempts: Int,
    val notes: String,
)
```

`ExamMaterialEntity.kt`:

```kotlin
package com.kpyruy.takt.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exam_materials")
data class ExamMaterialEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    val uri: String,
)
```

- [ ] **Step 3: Create ExamDao**

```kotlin
@Dao
interface ExamDao {
    @Query("SELECT * FROM exam_info WHERE courseId = :courseId LIMIT 1")
    fun observeExamInfo(courseId: String): Flow<ExamInfoEntity?>

    @Query("SELECT * FROM exam_materials WHERE courseId = :courseId ORDER BY title")
    fun observeMaterials(courseId: String): Flow<List<ExamMaterialEntity>>

    @Query("SELECT * FROM exam_info ORDER BY courseId")
    suspend fun getExamInfoSnapshot(): List<ExamInfoEntity>

    @Query("SELECT * FROM exam_materials ORDER BY courseId, title")
    suspend fun getMaterialsSnapshot(): List<ExamMaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExamInfo(item: ExamInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExamInfo(items: List<ExamInfoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterial(item: ExamMaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMaterials(items: List<ExamMaterialEntity>)

    @Query("DELETE FROM exam_materials WHERE id = :id")
    suspend fun deleteMaterial(id: String)

    @Query("DELETE FROM exam_info")
    suspend fun deleteAllExamInfo()

    @Query("DELETE FROM exam_materials")
    suspend fun deleteAllMaterials()
}
```

- [ ] **Step 4: Add migration 7→8**

Change database version to `8`, include both exam entities, add `abstract fun examDao(): ExamDao`, and add:

```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courses ADD COLUMN gradingType TEXT NOT NULL DEFAULT 'CONTINUOUS_LETTER'")
        db.execSQL("ALTER TABLE courses ADD COLUMN passFailResult TEXT")
        db.execSQL("ALTER TABLE grade_items ADD COLUMN dueDateEpochDay INTEGER")
        db.execSQL("ALTER TABLE grade_items ADD COLUMN completed INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE grade_items ADD COLUMN requiredForExam INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE study_tasks ADD COLUMN requiredForExam INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exam_info (
                courseId TEXT NOT NULL PRIMARY KEY,
                gradeItemId TEXT,
                dateEpochDay INTEGER,
                startMinute INTEGER,
                endMinute INTEGER,
                room TEXT,
                attemptNumber INTEGER NOT NULL,
                maxAttempts INTEGER NOT NULL,
                notes TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exam_materials (
                id TEXT NOT NULL PRIMARY KEY,
                courseId TEXT NOT NULL,
                title TEXT NOT NULL,
                uri TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}
```

- [ ] **Step 5: Register migration**

Add `TaktDatabase.MIGRATION_7_8` to `TaktDataContainer`.

- [ ] **Step 6: Compile database/data**

```bash
gradle :core:database:assembleDebug :core:data:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL` and Room schema export for version 8 is generated by the configured build.

- [ ] **Step 7: Commit**

```bash
git add core/database core/data/src/main/java/com/kpyruy/takt/core/data/TaktDataContainer.kt
git commit -m "feat: migrate academic progress storage"
```

---

### Task 4: Map new fields and add ExamRepository

**Files:**
- Modify: `core/database/src/main/java/com/kpyruy/takt/core/database/CourseDao.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/StudyPlanRepository.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/RoomStudyPlanRepository.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/RoomGradeRepository.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/RoomStudyContentRepository.kt`
- Create: `core/data/src/main/java/com/kpyruy/takt/core/data/ExamRepository.kt`
- Create: `core/data/src/main/java/com/kpyruy/takt/core/data/RoomExamRepository.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/TaktDataContainer.kt`
- Modify tests:
  - `core/data/src/test/java/com/kpyruy/takt/core/data/GradeItemMappingTest.kt`
  - `core/data/src/test/java/com/kpyruy/takt/core/data/StudyContentMappingTest.kt`

- [ ] **Step 1: Map course fields**

In `CourseEntity.toDomain()` add:

```kotlin
gradingType = runCatching { CourseGradingType.valueOf(gradingType) }
    .getOrDefault(CourseGradingType.CONTINUOUS_LETTER),
passFailResult = passFailResult?.let {
    runCatching { PassFailResult.valueOf(it) }.getOrNull()
},
```

- [ ] **Step 2: Add course grading configuration writes**

Add to `CourseDao`:

```kotlin
@Query("UPDATE courses SET gradingType = :gradingType WHERE id = :courseId")
suspend fun updateGradingType(courseId: String, gradingType: String)

@Query("UPDATE courses SET passFailResult = :result WHERE id = :courseId")
suspend fun updatePassFailResult(courseId: String, result: String?)
```

Add to `StudyPlanRepository`:

```kotlin
suspend fun setGradingType(courseId: String, gradingType: CourseGradingType)
suspend fun setPassFailResult(courseId: String, result: PassFailResult?)
```

Implement in `RoomStudyPlanRepository` by storing enum names. When switching away from `PASS_FAIL`, clear `passFailResult` so a stale pass/fail result never appears on a letter-graded course.

- [ ] **Step 3: Map GradeItem fields**

Update entity/domain conversions:

```kotlin
dueDate = dueDateEpochDay?.let(LocalDate::ofEpochDay),
completed = completed,
requiredForExam = requiredForExam,
```

and reverse:

```kotlin
dueDateEpochDay = dueDate?.toEpochDay(),
completed = completed,
requiredForExam = requiredForExam,
```

- [ ] **Step 4: Map StudyTask field**

Add `requiredForExam` both directions.

- [ ] **Step 5: Define ExamRepository**

```kotlin
package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.ExamMaterial
import kotlinx.coroutines.flow.Flow

interface ExamRepository {
    fun observeExamInfo(courseId: String): Flow<ExamInfo?>
    fun observeMaterials(courseId: String): Flow<List<ExamMaterial>>
    suspend fun upsertExamInfo(info: ExamInfo)
    suspend fun upsertMaterial(material: ExamMaterial)
    suspend fun deleteMaterial(id: String)
}
```

- [ ] **Step 6: Implement RoomExamRepository**

Map date with `LocalDate.ofEpochDay`, times with `LocalTime.ofSecondOfDay(minute * 60L)`, and store times as minute-of-day.

- [ ] **Step 7: Register repository**

In `TaktDataContainer`:

```kotlin
val examRepository: ExamRepository = RoomExamRepository(database.examDao())
```

- [ ] **Step 8: Run data tests**

```bash
gradle :core:data:testDebugUnitTest --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add core/data
git commit -m "feat: add academic progress repositories"
```

---

### Task 5: Upgrade JSON backup to v2 while importing v1

**Files:**
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/BackupPayload.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/BackupMappings.kt`
- Modify: `core/data/src/main/java/com/kpyruy/takt/core/data/RoomBackupRepository.kt`
- Modify: `core/data/src/test/java/com/kpyruy/takt/core/data/BackupPayloadCodecTest.kt`
- Modify: `core/data/src/test/java/com/kpyruy/takt/core/data/BackupMappingsTest.kt`

- [ ] **Step 1: Write v1 compatibility test first**

Add:

```kotlin
@Test
fun v1BackupStillDecodesWithSafeDefaults() {
    val raw = """
        {
          "version": 1,
          "courses": [{
            "id":"c1","code":"C1","title":"Course","credits":5,
            "semester":3,"status":"enrolled","requirementType":"COMPULSORY"
          }]
        }
    """.trimIndent()

    val restored = BackupPayloadCodec.decode(raw)

    assertEquals(1, restored.version)
    assertEquals("CONTINUOUS_LETTER", restored.courses.single().gradingType)
    assertEquals(false, restored.gradeItems.any { it.requiredForExam })
}
```

- [ ] **Step 2: Bump current version and add defaulted fields**

Set:

```kotlin
const val CURRENT_VERSION = 2
```

Add to backup DTOs:
- `BackupCourse.gradingType: String = "CONTINUOUS_LETTER"`
- `BackupCourse.passFailResult: String? = null`
- `BackupGradeItem.dueDateEpochDay: Long? = null`
- `BackupGradeItem.completed: Boolean = true`
- `BackupGradeItem.requiredForExam: Boolean = false`
- `BackupStudyTask.requiredForExam: Boolean = false`

Keep the visual-preference fields already added to `BackupSettings` in the foundation phase; do not redefine or remove them when bumping the payload version.

Add:

```kotlin
@Serializable
data class BackupExamInfo(
    val courseId: String,
    val gradeItemId: String? = null,
    val dateEpochDay: Long? = null,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val room: String? = null,
    val attemptNumber: Int = 1,
    val maxAttempts: Int = 3,
    val notes: String = "",
)

@Serializable
data class BackupExamMaterial(
    val id: String,
    val courseId: String,
    val title: String,
    val uri: String,
)
```

Add lists to `BackupPayload`:

```kotlin
val examInfo: List<BackupExamInfo> = emptyList(),
val examMaterials: List<BackupExamMaterial> = emptyList(),
```

- [ ] **Step 3: Accept v1 and v2**

Change decoder version check to:

```kotlin
require(payload.version in 1..BackupPayload.CURRENT_VERSION) {
    "Unsupported Takt backup version: " + payload.version
}
```

Keep the existing non-empty study-plan validation.

- [ ] **Step 4: Add mappings**

Extend all entity/DTO mappings with the new fields and add exam-info/material mappings.

- [ ] **Step 5: Export/import exam tables**

During export include snapshots from `database.examDao()`.

During import delete:
1. exam materials,
2. exam info,
then existing dependent data;

then restore courses and existing data, followed by exam info/materials.

Keep the existing visual-settings restore path unchanged so a v2 academic backup still preserves appearance preferences.

- [ ] **Step 6: Run backup tests**

```bash
gradle :core:data:testDebugUnitTest --stacktrace
```

Expected:
- v1 compatibility passes;
- v2 round-trip passes;
- version 999 still fails;
- empty backup still fails.

- [ ] **Step 7: Commit**

```bash
git add core/data
git commit -m "feat: preserve exam progress in backup v2"
```

---

### Task 6: Replace generic course detail with segmented academic views

**Files:**
- Modify: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/SubjectsScreen.kt`
- Modify: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/SubjectCard.kt`
- Modify: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/SubjectDetailScreen.kt`
- Create: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/CourseOverviewTab.kt`
- Create: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/CourseAssessmentsTab.kt`
- Create: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/CourseTasksTab.kt`
- Create: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/CourseExamTab.kt`
- Create: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/CourseNotesTab.kt`
- Modify: `app/src/main/java/com/kpyruy/takt/app/TaktApp.kt`

- [ ] **Step 1: Pass ExamRepository into subject detail**

Add `examRepository` parameter to `TaktApp` and `SubjectDetailScreen`, sourced from `TaktDataContainer.examRepository`.

- [ ] **Step 2: Turn Subjects into the semester overview opened from Home**

Update `SubjectsScreen` to accept `gradeRepository` and `studyContentRepository` in addition to the study-plan repository.

Render:
1. `Цей семестр` section containing enrolled courses first;
2. other planned/fulfilled courses below in compact grouped sections;
3. each `SubjectCard` with course title, code/credits/status, and one truthful progress line.

Progress-line rules:
- `EXAM_LETTER`: show secured points / maximum possible grade ceiling or exam-readiness text; never show a predicted final letter as owned.
- `CONTINUOUS_LETTER`: show normal percentage/letter summary.
- `PASS_FAIL`: show pass/fail result or required-work completion.

Keep the whole card tappable to open `SubjectDetailScreen`.

Pass the two additional repositories from `TaktApp`.

- [ ] **Step 3: Use left-side back navigation**

Call:

```kotlin
ScreenHeader(
    title = item.title,
    subtitle = item.code,
    navigation = {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
        }
    },
)
```

- [ ] **Step 4: Add tab state**

Use five tabs for exam-capable courses:

```kotlin
val tabs = if (item.gradingType == CourseGradingType.EXAM_LETTER) {
    listOf("Огляд", "Оцінювання", "Завдання", "Екзамен", "Нотатки")
} else {
    listOf("Огляд", "Оцінювання", "Завдання", "Нотатки")
}
```

Render through `TaktSegmentedTabs`.

- [ ] **Step 5: Build Overview tab**

Show:
- credits/semester/status;
- grading type;
- eligibility summary;
- compact progress numbers;
- upcoming deadlines;
- no giant final-letter claim before exam.

Expose course grading configuration in a compact course-settings area:
- `Екзамен A–FX` -> `CourseGradingType.EXAM_LETTER`;
- `Поточне оцінювання A–FX` -> `CONTINUOUS_LETTER`;
- `Зараховано / не зараховано` -> `PASS_FAIL`.

Changing the type calls `StudyPlanRepository.setGradingType`.

For `PASS_FAIL`, show `Зараховано`, `Не зараховано`, or `Результату ще немає`, plus a three-state control that calls `setPassFailResult(courseId, null/PASSED/FAILED)`.

- [ ] **Step 6: Build Assessments tab around pre-exam task importance**

For `EXAM_LETTER`:
- list scored `GradeItem` rows;
- show max points, earned points, due date, completed status;
- show maximum possible letter from `GradeProjection`;
- show compact legend behind an info button/sheet;
- show Upcoming items.

Do not display `GradeSummary.letter` as the student's current final grade when the exam is unfinished.

- [ ] **Step 7: Build Tasks tab**

List `StudyTask`, visually distinguish:
- required for exam,
- ordinary homework,
- completed,
- incomplete.

At top show:

```kotlin
Text(
    if (eligibility.eligible) {
        "Допуск: готово"
    } else {
        "Для допуску: " + eligibility.completedCount + " / " + eligibility.requiredCount
    }
)
```

- [ ] **Step 8: Preserve custom grade scales and explicit manual final results**

Keep the existing course-specific grade-scale editor.

Keep the existing manual A–FX override, but label it explicitly as `Підсумкова оцінка вручну`. For `EXAM_LETTER` courses, place that control in the Exam tab/final-result area rather than presenting it as a pre-exam prediction. `GradeProjection` must ignore the manual override; when a manual final result is set, the UI may show it as the explicit final result with a `Вручну` label.

- [ ] **Step 9: Build Notes tab**

Reuse `CourseNoteCard`, but move permanent delete icons into contextual overflow/actions.

- [ ] **Step 10: Compile**

```bash
gradle :feature:subjects:assembleDebug :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 11: Commit**

```bash
git add feature/subjects app
git commit -m "feat: redesign course detail tabs"
```

---

### Task 7: Implement exam-period card view and exam editor

**Files:**
- Create: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/ExamProgressCards.kt`
- Create: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/EditExamInfoSheet.kt`
- Create: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/ExamMaterialsSection.kt`
- Modify: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/CourseExamTab.kt`
- Modify: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/AddGradeItemSheet.kt`
- Modify: `feature/subjects/src/main/java/com/kpyruy/takt/feature/subjects/AddTaskSheet.kt`

- [ ] **Step 1: Render the approved four-block exam layout**

`ExamProgressCards` shows:
1. Final exam
2. Course points
3. Still on exam
4. Possible grade

Use projection data:

```kotlin
val projection = GradeProjection.calculate(gradeItems, gradeScale)
val rangeText = when {
    projection.minimumPossibleLetter == null -> "—"
    projection.minimumPossibleLetter == projection.maximumPossibleLetter ->
        projection.minimumPossibleLetter.name
    else -> projection.minimumPossibleLetter.name + " – " + projection.maximumPossibleLetter?.name
}
```

- [ ] **Step 2: Show required exam points per letter**

For A–E, render:
- needed points when map value is non-null;
- `Недосяжно` when null;
- do not show FX as a target.

- [ ] **Step 3: Add visual switch between preparation and exam focus**

Inside the exam-capable course progress area use a local two-state segmented control:
- `Підготовка`
- `Екзамен`

This is a visual mode switch only; it does not mutate academic data.

- [ ] **Step 4: Build exam metadata editor**

Fields:
- date,
- start/end time,
- room,
- attempt number,
- maximum attempts fixed at 3 for this release,
- notes,
- linked exam assessment.

Use the Material date/time pickers from Phase 2.

- [ ] **Step 5: Add materials**

Allow:
- title,
- URI/link string,
- delete action.

Persist through `ExamRepository`. For Android document-picker files, use `ActivityResultContracts.OpenDocument`, call `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` when allowed, then store the URI string. Web links are stored directly.

- [ ] **Step 6: Extend assessment form**

`AddGradeItemSheet` must add:
- due date,
- completed toggle,
- required-for-exam toggle.

When type is `EXAM`, label completion as `Екзамен складено`.

- [ ] **Step 7: Extend task form**

`AddTaskSheet` adds:
- `Потрібно для допуску до екзамену` switch.

- [ ] **Step 8: Compile**

```bash
gradle :feature:subjects:assembleDebug :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add feature/subjects
git commit -m "feat: add exam period progress experience"
```

---

### Task 8: Academic-progress verification

**Files:** verification only unless fixes are needed.

- [ ] **Step 1: Domain tests**

```bash
gradle :core:model:test --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Data tests**

```bash
gradle :core:data:testDebugUnitTest --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Full Android unit tests**

```bash
gradle :core:data:testDebugUnitTest :app:testDebugUnitTest --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Lint**

```bash
gradle :app:lintDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Assemble**

```bash
gradle :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Verify academic scenarios manually**

Use explicit fixtures/data:
- exam course with 3 × 10-point tasks scored 0 and 70-point pending exam -> maximum possible C;
- exam course with 30 secured and 70 pending -> A requires 62 exam points with default 92% A threshold;
- required unscored tasks 2/3 complete -> eligibility not met;
- 3/3 complete -> eligibility met;
- pass/fail course -> no A–FX widgets;
- exam result recorded -> possible range collapses to actual grade;
- v1 backup import -> no crash, safe defaults;
- v2 backup round-trip -> exam metadata/materials and visual settings preserved.

- [ ] **Step 7: Commit verification-driven fixes only**

```bash
git add -A
git commit -m "fix: stabilize academic progress redesign"
```

Skip the commit if there are no changes.
