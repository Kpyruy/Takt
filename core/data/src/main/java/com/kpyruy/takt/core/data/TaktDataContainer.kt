package com.kpyruy.takt.core.data

import android.content.Context
import androidx.room.Room
import com.kpyruy.takt.core.database.TaktDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TaktDataContainer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
            TaktDatabase.MIGRATION_13_14,
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
    val planningSnapshot: StateFlow<PlanningSnapshot?> = observePlanningSnapshot(
        studyPlanRepository, scheduleRepository, studyContentRepository, gradeRepository, settingsRepository,
    ).map<PlanningSnapshot, PlanningSnapshot?> { it }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Wait for the shared first frame to catch up after an onboarding restore. */
    suspend fun awaitRestoredPlanning() {
        val expected = PlanningSnapshot(
            courses = studyPlanRepository.observeCourses().first(),
            absences = scheduleRepository.observeAbsences().first(),
            rules = scheduleRepository.observeRules().first(),
            oneOffEvents = scheduleRepository.observeOneOffEvents().first(),
            exceptions = scheduleRepository.observeExceptions().first(),
            tasks = studyContentRepository.observeAllTasks().first(),
            assessments = gradeRepository.observeAllItems().first(),
            settings = settingsRepository.settings.first(),
        )
        withTimeoutOrNull(5_000L) { planningSnapshot.first { it == expected } }
    }
    val backupRepository: BackupRepository =
        RoomBackupRepository(database, settingsRepository)
    val documentStore = TaktDocumentStore(context.applicationContext, database, backupRepository, settingsRepository)
    val firstRunRepository = FirstRunRepository(context.applicationContext, this)
}
