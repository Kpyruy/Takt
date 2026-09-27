package com.kpyruy.takt.core.data

import java.util.Locale

/** Each selection names the source to take from a backup; unselected sections stay local. */
enum class BackupSection { SUBJECTS, PROGRESS, SCHEDULE, PERIODS, MATERIALS, PREFERENCES }

object BackupMerger {
    fun merge(local: BackupPayload, incoming: BackupPayload, fromBackup: Set<BackupSection>): BackupPayload {
        if (fromBackup.isEmpty()) return local
        val localByCode = local.courses.associateBy { it.code.uppercase(Locale.ROOT) }
        val incomingByCode = incoming.courses.associateBy { it.code.uppercase(Locale.ROOT) }
        val courses = if (BackupSection.SUBJECTS in fromBackup) {
            val imported = incoming.courses.map { source ->
                val old = localByCode[source.code.uppercase(Locale.ROOT)]
                source.copy(
                    id = old?.id ?: source.id,
                    status = if (BackupSection.PROGRESS in fromBackup) source.status else old?.status ?: source.status,
                    passFailResult = if (BackupSection.PROGRESS in fromBackup || old == null) source.passFailResult
                        else old.passFailResult,
                    iconKey = old?.iconKey ?: source.iconKey,
                )
            }
            imported + local.courses.filter { it.code.uppercase(Locale.ROOT) !in incomingByCode }
        } else if (BackupSection.PROGRESS in fromBackup) {
            local.courses.map { old ->
                val source = incomingByCode[old.code.uppercase(Locale.ROOT)]
                if (source == null) old else old.copy(status = source.status, passFailResult = source.passFailResult)
            }
        } else local.courses
        val destinationByCode = courses.associateBy { it.code.uppercase(Locale.ROOT) }
        val sourceToDestination = incoming.courses.mapNotNull { source ->
            destinationByCode[source.code.uppercase(Locale.ROOT)]?.let { source.id to it.id }
        }.toMap()
        fun mapped(id: String?): String? = id?.let(sourceToDestination::get)
        fun covered(id: String) = id in sourceToDestination.values
        val progress = BackupSection.PROGRESS in fromBackup
        val schedule = BackupSection.SCHEDULE in fromBackup
        val materials = BackupSection.MATERIALS in fromBackup
        val settings = if (BackupSection.PREFERENCES in fromBackup) incoming.settings else local.settings
        return local.copy(
            courses = courses,
            scheduleRules = if (schedule) incoming.scheduleRules.map { it.copy(courseId = mapped(it.courseId)) } else local.scheduleRules,
            oneOffEvents = if (schedule) incoming.oneOffEvents.map { it.copy(courseId = mapped(it.courseId)) } else local.oneOffEvents,
            scheduleExceptions = if (schedule) incoming.scheduleExceptions else local.scheduleExceptions,
            lessonAbsences = if (schedule) incoming.lessonAbsences else local.lessonAbsences,
            gradeItems = if (progress) local.gradeItems.filterNot { covered(it.courseId) } +
                incoming.gradeItems.mapNotNull { item -> mapped(item.courseId)?.let { item.copy(courseId = it) } }
                else local.gradeItems,
            gradeScales = if (progress) local.gradeScales.filterNot { covered(it.courseId) } +
                incoming.gradeScales.mapNotNull { item -> mapped(item.courseId)?.let { item.copy(courseId = it) } }
                else local.gradeScales,
            gradeOverrides = if (progress) local.gradeOverrides.filterNot { covered(it.courseId) } +
                incoming.gradeOverrides.mapNotNull { item -> mapped(item.courseId)?.let { item.copy(courseId = it) } }
                else local.gradeOverrides,
            studyTasks = if (progress) local.studyTasks.filterNot { covered(it.courseId) } +
                incoming.studyTasks.mapNotNull { item -> mapped(item.courseId)?.let { item.copy(courseId = it) } }
                else local.studyTasks,
            examInfo = if (progress) local.examInfo.filterNot { covered(it.courseId) } +
                incoming.examInfo.mapNotNull { item -> mapped(item.courseId)?.let { item.copy(courseId = it) } }
                else local.examInfo,
            courseNotes = if (materials) local.courseNotes.filterNot { covered(it.courseId) } +
                incoming.courseNotes.mapNotNull { item -> mapped(item.courseId)?.let { item.copy(courseId = it) } }
                else local.courseNotes,
            examMaterials = if (materials) local.examMaterials.filterNot { covered(it.courseId) } +
                incoming.examMaterials.mapNotNull { item -> mapped(item.courseId)?.let { item.copy(courseId = it) } }
                else local.examMaterials,
            settings = settings.copy(
                semesterPeriods = if (BackupSection.PERIODS in fromBackup) incoming.settings.semesterPeriods
                    else local.settings.semesterPeriods,
            ),
            version = BackupPayload.CURRENT_VERSION,
        )
    }
}
