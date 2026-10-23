package com.kpyruy.takt.core.data

import android.content.Context
import androidx.room.Room
import com.kpyruy.takt.core.database.TaktDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.emptyFlow
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
            TaktDatabase.MIGRATION_14_15,
            TaktDatabase.MIGRATION_15_16,
            TaktDatabase.MIGRATION_16_17,
            TaktDatabase.MIGRATION_17_18,
            TaktDatabase.MIGRATION_18_19,
            TaktDatabase.MIGRATION_19_20,
        )
        .build()

    val settingsRepository: AppSettingsRepository = SharedPreferencesAppSettingsRepository(context)
    val studyPlanRepository: StudyPlanRepository = RoomStudyPlanRepository(database, settingsRepository)
    val scheduleRepository: ScheduleRepository = RoomScheduleRepository(database)
    val gradeRepository: GradeRepository = RoomGradeRepository(database)
    val studyContentRepository: StudyContentRepository =
        RoomStudyContentRepository(database)
    val examRepository: ExamRepository =
        RoomExamRepository(database)
    val teachingStore = TeachingStore(java.io.File(context.filesDir, "teaching.json"))
    val backupRepository = RoomBackupRepository(database, settingsRepository, WallpaperStore(context.applicationContext), teachingStore)
    val universityAccountRepository = UniversityAccountRepository(context, studyPlanRepository,
        scheduleRepository, settingsRepository, teachingStore, backupRepository, RoomStudentHubStore(database))
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val planningSnapshot: StateFlow<PlanningSnapshot?> = backupRepository.restoreStatus.flatMapLatest { status ->
        if (status == BackupRestoreStatus.READY) observePlanningSnapshot(
            studyPlanRepository, scheduleRepository, studyContentRepository, gradeRepository, settingsRepository, examRepository,
        ).map<PlanningSnapshot, PlanningSnapshot?> { it }
        else emptyFlow() // Retain the last good frame; do not dispose the importing Settings screen.
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val reminderCoordinator = ReminderCoordinator(context.applicationContext, planningSnapshot, ::readPlanningSnapshot)

    /** Fresh source read for cold-process reminder validation; caller runs on IO. */
    suspend fun readPlanningSnapshot(): PlanningSnapshot = backupRepository.withStableData { PlanningSnapshot(
        courses = studyPlanRepository.observeCourses().first(),
        absences = scheduleRepository.observeAbsences().first(),
        rules = scheduleRepository.observeRules().first(),
        oneOffEvents = scheduleRepository.observeOneOffEvents().first(),
        exceptions = scheduleRepository.observeExceptions().first(),
        tasks = studyContentRepository.observeAllTasks().first(),
        assessments = gradeRepository.observeAllItems().first(),
        settings = settingsRepository.settings.value,
        examInfos = examRepository.observeAllExamInfo().first(),
    ) }

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
            examInfos = examRepository.observeAllExamInfo().first(),
        )
        withTimeoutOrNull(5_000L) { planningSnapshot.first { it == expected } }
    }
    val documentStore = TaktDocumentStore(context.applicationContext, database, backupRepository, settingsRepository)
    val firstRunRepository = FirstRunRepository(context.applicationContext, this)
    init { scope.launch { runCatching { backupRepository.recoverPendingRestore() } } }
}
