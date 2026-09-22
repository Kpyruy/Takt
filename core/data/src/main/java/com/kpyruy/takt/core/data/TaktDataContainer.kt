package com.kpyruy.takt.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase

class TaktDataContainer(context: Context) {
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

    suspend fun seedIfNeeded() {
        database.withTransaction {
            val courseDao = database.courseDao()
            if (courseDao.count() == 0) {
                courseDao.insertAll(StudyPlanSeed.courses)
            }
        }

        val scheduleDao = database.scheduleDao()
        if (scheduleDao.countRules() == 0) {
            scheduleRepository.upsertRules(DefaultTimetable.rules)
        }
        if (scheduleDao.countOneOffEvents() == 0) {
            scheduleRepository.upsertOneOffEvents(DefaultTimetable.oneOffEvents)
        }
    }
}
