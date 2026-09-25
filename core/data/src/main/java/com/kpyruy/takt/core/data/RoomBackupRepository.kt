package com.kpyruy.takt.core.data

import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase
import kotlinx.coroutines.flow.first

class RoomBackupRepository(
    private val database: TaktDatabase,
    private val settingsRepository: AppSettingsRepository,
) : BackupRepository {
    override suspend fun exportJson(): String {
        val settings = settingsRepository.settings.first()
        val payload = database.withTransaction {
            BackupPayload(
                lessonAbsences = database.scheduleDao().getAbsencesSnapshot().map { it.toBackup() },
                courses = database.courseDao().getAllSnapshot().map { it.toBackup() },
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
                settings = settings.toBackup(),
            )
        }
        return BackupPayloadCodec.encode(payload)
    }

    override suspend fun importJson(raw: String) {
        val payload = BackupPayloadCodec.decode(raw)

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
            courseDao.deleteAll()

            courseDao.insertAll(payload.courses.map { it.toEntity() })
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

        val settings = payload.settings.toModel()
        settingsRepository.setCancellationStyle(settings.cancellationStyle)
        settingsRepository.setShowHiddenLessons(settings.showHiddenLessons)
        settingsRepository.setParityOverride(settings.parityOverride)
        settingsRepository.setCardAppearance(settings.cardAppearance)
        settingsRepository.setThemeFamily(settings.themeFamily)
        settingsRepository.setThemeMode(settings.themeMode)
        settingsRepository.setWeekLayout(settings.weekLayout)
        settingsRepository.setHomeWorkFilter(settings.homeWorkFilter)
        settingsRepository.setSemesterPeriods(settings.semesterPeriods)
        settingsRepository.setCurrentSemester(settings.currentSemester)
    }
}
