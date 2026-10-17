package com.kpyruy.takt.core.data

import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomBackupRepository(
    private val database: TaktDatabase,
    private val settingsRepository: AppSettingsRepository,
    private val wallpaperStore: WallpaperStore? = null,
    private val teachingStore: TeachingStore? = null,
) : BackupRepository {
    override val changes: kotlinx.coroutines.flow.Flow<Unit> get() = teachingStore?.snapshot?.map { Unit } ?: kotlinx.coroutines.flow.emptyFlow()
    override suspend fun exportJson(): String {
        val settings = settingsRepository.settings.first()
        val photo = wallpaperStore?.exportWallpaper(settings.customWallpaper)
        val payload = database.withTransaction {
            val courses = database.courseDao().getAllSnapshot().map { it.toBackup() }
            val teaching = teachingStore?.snapshot?.value?.forCourses(courses.map { it.code })
            BackupPayload(
                lessonAbsences = database.scheduleDao().getAbsencesSnapshot().map { it.toBackup() },
                courses = courses,
                courseIdAliases = database.courseDao().getAliasesSnapshot().map { BackupCourseIdAlias(it.oldId, it.courseId) },
                courseImportDecisions = database.courseDao().getImportDecisionsSnapshot().map { it.courseId },
                scheduleRules = database.scheduleDao().getRulesSnapshot().map { it.toBackup() },
                oneOffEvents = database.scheduleDao().getOneOffEventsSnapshot().map { it.toBackup() },
                scheduleExceptions = database.scheduleDao().getExceptionsSnapshot().map { it.toBackup() },
                gradeItems = database.gradeDao().getItemsSnapshot().map { it.toBackup() },
                gradeScales = database.gradeDao().getScalesSnapshot().map { it.toBackup() },
                gradeOverrides = database.gradeDao().getOverridesSnapshot().map { it.toBackup() },
                studyTasks = database.studyContentDao().getTasksSnapshot().map { it.toBackup() },
                courseNotes = database.studyContentDao().getNotesSnapshot().map { it.toBackup() },
                examInfo = database.examDao().getExamInfoSnapshot().map { it.toBackup() },
                examMaterials = database.examDao().getMaterialsSnapshot().map { it.toBackup() },
                teaching = teaching,
                settings = settings.toBackup().copy(wallpaperRecorded = wallpaperStore != null && (settings.customWallpaper == null || photo != null), wallpaper = photo),
            )
        }
        return BackupPayloadCodec.encode(payload)
    }

    override suspend fun importJson(raw: String) {
        val payload = BackupPayloadCodec.decode(raw)
        val previousPhoto = settingsRepository.settings.value.customWallpaper
        val restoredPhoto = if (payload.settings.wallpaperRecorded && wallpaperStore != null) {
            payload.settings.wallpaper?.let { wallpaperStore.restoreWallpaper(it, previousPhoto) }
        } else previousPhoto

        database.withTransaction {
            val courseDao = database.courseDao()
            val scheduleDao = database.scheduleDao()
            val gradeDao = database.gradeDao()
            val studyDao = database.studyContentDao()
            val examDao = database.examDao()

            examDao.deleteAllMaterials()
            examDao.deleteAllExamInfo()
            studyDao.deleteAllNotes()
            studyDao.deleteAllTasks()
            gradeDao.deleteAllOverrides()
            gradeDao.deleteAllScales()
            gradeDao.deleteAllItems()
            scheduleDao.deleteAllAbsences()
            scheduleDao.deleteAllExceptions()
            scheduleDao.deleteAllOneOffEvents()
            scheduleDao.deleteAllRules()
            courseDao.deleteAllImportDecisions()
            courseDao.deleteAllAliases()
            courseDao.deleteAll()

            courseDao.insertAll(payload.courses.map { it.toEntity() })
            courseDao.upsertAliases(payload.courseIdAliases.map { com.kpyruy.takt.core.database.CourseIdAliasEntity(it.oldId, it.courseId) })
            courseDao.upsertImportDecisions(payload.courseImportDecisions.map { com.kpyruy.takt.core.database.CourseImportDecisionEntity(it) })
            scheduleDao.upsertAbsences(payload.lessonAbsences.map { it.toEntity() })
            scheduleDao.upsertRules(payload.scheduleRules.map { it.toEntity() })
            scheduleDao.upsertOneOffEvents(payload.oneOffEvents.map { it.toEntity() })
            scheduleDao.upsertExceptions(payload.scheduleExceptions.map { it.toEntity() })
            gradeDao.upsertItems(payload.gradeItems.map { it.toEntity() })
            gradeDao.upsertScales(payload.gradeScales.map { it.toEntity() })
            gradeDao.upsertOverrides(payload.gradeOverrides.map { it.toEntity() })
            studyDao.upsertTasks(payload.studyTasks.map { it.toEntity() })
            studyDao.upsertNotes(payload.courseNotes.map { it.toEntity() })
            examDao.upsertExamInfo(payload.examInfo.map { it.toEntity() })
            examDao.upsertMaterials(payload.examMaterials.map { it.toEntity() })
        }

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { payload.teaching?.let { teachingStore?.replace(it) } }

        // The UIS-detected term is local state, so a Documents restore must not erase it.
        val settings = payload.settings.toModel(settingsRepository.settings.value.uisCurrentSemester,
            localWallpaper = restoredPhoto)
        settingsRepository.setCancellationStyle(settings.cancellationStyle)
        settingsRepository.setShowHiddenLessons(settings.showHiddenLessons)
        settingsRepository.setParityOverride(settings.parityOverride)
        settingsRepository.setAppearance(settings)
        settingsRepository.setWeekLayout(settings.weekLayout)
        settingsRepository.setHomeWorkFilter(settings.homeWorkFilter)
        settingsRepository.setSemesterPeriods(settings.semesterPeriods)
        settingsRepository.setCurrentSemester(settings.currentSemester)
        settingsRepository.setUisCurrentSemester(settings.uisCurrentSemester)
        settingsRepository.setLanguage(settings.language)
        settingsRepository.setCourseNameLanguage(settings.courseNameLanguage)
        settingsRepository.setUkrainianCourseNameFallback(settings.ukrainianCourseNameFallback)
        settingsRepository.setUisAutoSync(settings.uisAutoSync)
        settingsRepository.setReminders(settings.reminders)
    }
}
