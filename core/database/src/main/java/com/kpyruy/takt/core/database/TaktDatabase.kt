package com.kpyruy.takt.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LessonAbsenceEntity::class,
        CourseEntity::class,
        ScheduleRuleEntity::class,
        OneOffScheduleEventEntity::class,
        ScheduleExceptionEntity::class,
        GradeItemEntity::class,
        GradeScaleEntity::class,
        GradeOverrideEntity::class,
        StudyTaskEntity::class,
        CourseNoteEntity::class,
        ExamInfoEntity::class,
        ExamMaterialEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class TaktDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun gradeDao(): GradeDao
    abstract fun studyContentDao(): StudyContentDao
    abstract fun examDao(): ExamDao

    companion object {
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS lesson_absences (eventId TEXT NOT NULL, dateEpochDay INTEGER NOT NULL, isOneOff INTEGER NOT NULL, PRIMARY KEY(eventId, dateEpochDay, isOneOff))")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN iconKey TEXT")
                db.execSQL("ALTER TABLE schedule_rules ADD COLUMN lessonType TEXT NOT NULL DEFAULT 'UNSPECIFIED'")
                db.execSQL("ALTER TABLE schedule_one_off ADD COLUMN lessonType TEXT NOT NULL DEFAULT 'UNSPECIFIED'")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedule_rules (
                        id TEXT NOT NULL PRIMARY KEY,
                        courseId TEXT,
                        title TEXT NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        startMinute INTEGER NOT NULL,
                        endMinute INTEGER NOT NULL,
                        recurrence TEXT NOT NULL,
                        room TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedule_one_off (
                        id TEXT NOT NULL PRIMARY KEY,
                        courseId TEXT,
                        title TEXT NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        startMinute INTEGER NOT NULL,
                        endMinute INTEGER NOT NULL,
                        room TEXT,
                        type TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grade_items (
                        id TEXT NOT NULL PRIMARY KEY,
                        courseId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        earnedPoints REAL NOT NULL,
                        maxPoints REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grade_scales (
                        courseId TEXT NOT NULL PRIMARY KEY,
                        aMin REAL NOT NULL,
                        bMin REAL NOT NULL,
                        cMin REAL NOT NULL,
                        dMin REAL NOT NULL,
                        eMin REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedule_exceptions (
                        id TEXT NOT NULL PRIMARY KEY,
                        ruleId TEXT NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        replacementDateEpochDay INTEGER,
                        replacementStartMinute INTEGER,
                        replacementEndMinute INTEGER,
                        replacementRoom TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS study_tasks (
                        id TEXT NOT NULL PRIMARY KEY,
                        courseId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        dueDateEpochDay INTEGER,
                        completed INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS course_notes (
                        id TEXT NOT NULL PRIMARY KEY,
                        courseId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE grade_items ADD COLUMN recordedAtEpochMillis INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grade_overrides (
                        courseId TEXT NOT NULL PRIMARY KEY,
                        grade TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE courses ADD COLUMN gradingType TEXT NOT NULL DEFAULT 'CONTINUOUS_LETTER'"
                )
                db.execSQL("ALTER TABLE courses ADD COLUMN passFailResult TEXT")
                db.execSQL("ALTER TABLE grade_items ADD COLUMN dueDateEpochDay INTEGER")
                db.execSQL(
                    "ALTER TABLE grade_items ADD COLUMN completed INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE grade_items ADD COLUMN requiredForExam INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE study_tasks ADD COLUMN requiredForExam INTEGER NOT NULL DEFAULT 0"
                )
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
    }
}
