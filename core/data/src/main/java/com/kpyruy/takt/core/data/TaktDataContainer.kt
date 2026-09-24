package com.kpyruy.takt.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase

class TaktDataContainer(context: Context) {
    private val bootstrapPreferences = context.applicationContext.getSharedPreferences(
        "takt_bootstrap", Context.MODE_PRIVATE,
    )
    val database: TaktDatabase = Room.databaseBuilder(
        context.applicationContext,
        TaktDatabase::class.java,
        "takt.db",
    )
        .addMigrations(
            TaktDatabase.MIGRATION_1_2,
            TaktDatabase.MIGRATION_2_3,
            TaktDatabase.MIGRATION_3_4,
            TaktDatabase.MIGRATION_4_5,
            TaktDatabase.MIGRATION_5_6,
            TaktDatabase.MIGRATION_6_7,
            TaktDatabase.MIGRATION_7_8,
            TaktDatabase.MIGRATION_8_9,
            TaktDatabase.MIGRATION_9_10,
            TaktDatabase.MIGRATION_10_11,
            TaktDatabase.MIGRATION_11_12,
            TaktDatabase.MIGRATION_12_13,
        )
        .build()

    val studyPlanRepository: StudyPlanRepository = RoomStudyPlanRepository(database.courseDao())
    val scheduleRepository: ScheduleRepository = RoomScheduleRepository(database.scheduleDao())
    val gradeRepository: GradeRepository = RoomGradeRepository(database.gradeDao())
    val studyContentRepository: StudyContentRepository =
        RoomStudyContentRepository(database.studyContentDao())
    val examRepository: ExamRepository =
        RoomExamRepository(database.examDao())
    val settingsRepository: AppSettingsRepository =
        SharedPreferencesAppSettingsRepository(context)
    val backupRepository: BackupRepository =
        RoomBackupRepository(database, settingsRepository)
    val documentStore = TaktDocumentStore(context.applicationContext, database, backupRepository, settingsRepository)

    suspend fun seedIfNeeded() {
        if (bootstrapPreferences.getBoolean("initial_seed_complete", false)) return
        database.withTransaction {
            val courseDao = database.courseDao()
            val scheduleDao = database.scheduleDao()
            if (courseDao.count() == 0 && scheduleDao.countRules() == 0 && scheduleDao.countOneOffEvents() == 0) {
                courseDao.insertAll(StudyPlanSeed.courses)
                scheduleDao.upsertRules(DefaultTimetable.rules.map { it.toEntity() })
                scheduleDao.upsertOneOffEvents(DefaultTimetable.oneOffEvents.map { it.toEntity() })
            }
        }
        bootstrapPreferences.edit().putBoolean("initial_seed_complete", true).commit()
    }
}
