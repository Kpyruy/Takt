package com.kpyruy.takt.core.data

import java.util.Locale

/** Each selection names the source to take from a backup; unselected sections stay local. */
enum class BackupSection {
    SUBJECTS, ICONS, PROGRESS, SCHEDULE, PERIODS, MATERIALS,
    LANGUAGE, APPEARANCE, CURRENT_SEMESTER, PREFERENCES, TEACHERS, STUDIES,
}

object BackupMerger {
    /** Only offer choices that would change the phone's data. */
    fun changedSections(local: BackupPayload, incoming: BackupPayload): Set<BackupSection> {
        val subjectsOnly = merge(local, incoming, setOf(BackupSection.SUBJECTS))
        return BackupSection.entries.filterTo(mutableSetOf()) { section ->
            !sameReviewData(merge(local, incoming, setOf(section)), local) ||
                (section in setOf(BackupSection.ICONS, BackupSection.PROGRESS,
                    BackupSection.SCHEDULE, BackupSection.MATERIALS, BackupSection.TEACHERS) &&
                    !sameReviewData(merge(local, incoming, setOf(BackupSection.SUBJECTS, section)), subjectsOnly))
        }
    }

    private fun sameReviewData(a: BackupPayload, b: BackupPayload): Boolean {
        fun <T> sameItems(left: List<T>, right: List<T>) =
            left.size == right.size && left.groupingBy { it }.eachCount() == right.groupingBy { it }.eachCount()
        fun normalizedSettings(value: BackupSettings) = value.copy(
            reminders = value.reminders.toModel().toBackup(),
            semesterPeriods = value.semesterPeriods.sortedBy { it.semester },
            homeWorkFilter = value.homeWorkFilter.copy(types = value.homeWorkFilter.types.sorted(), courseIds = value.homeWorkFilter.courseIds.sorted()),
            wallpaperRecorded = true,
            wallpaper = value.wallpaper?.copy(name = ""),
        )
        return a.studentHub == b.studentHub && (a.teaching ?: BackupTeaching()) == (b.teaching ?: BackupTeaching()) && sameItems(a.courses, b.courses) &&
            sameItems(a.scheduleRules, b.scheduleRules) &&
            sameItems(a.oneOffEvents, b.oneOffEvents) &&
            sameItems(a.lessonAbsences, b.lessonAbsences) &&
            sameItems(a.scheduleExceptions, b.scheduleExceptions) &&
            sameItems(a.gradeItems, b.gradeItems) &&
            sameItems(a.gradeScales, b.gradeScales) &&
            sameItems(a.gradeOverrides, b.gradeOverrides) &&
            sameItems(a.studyTasks, b.studyTasks) &&
            sameItems(a.courseNotes, b.courseNotes) &&
            sameItems(a.examInfo, b.examInfo) &&
            sameItems(a.examMaterials, b.examMaterials) &&
            normalizedSettings(a.settings) == normalizedSettings(b.settings)
    }

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
                    officialGrade = if (BackupSection.PROGRESS in fromBackup || old == null) source.officialGrade
                        else old.officialGrade,
                    fulfilledOnEpochDay = if (BackupSection.PROGRESS in fromBackup || old == null) source.fulfilledOnEpochDay
                        else old.fulfilledOnEpochDay,
                    iconKey = if (BackupSection.ICONS in fromBackup) source.iconKey else old?.iconKey,
                    colorKey = if (BackupSection.ICONS in fromBackup) source.colorKey else old?.colorKey,
                )
            }
            imported + local.courses.filter { it.code.uppercase(Locale.ROOT) !in incomingByCode }
        } else if (BackupSection.PROGRESS in fromBackup || BackupSection.ICONS in fromBackup) {
            local.courses.map { old ->
                val source = incomingByCode[old.code.uppercase(Locale.ROOT)]
                if (source == null) old else old.copy(
                    status = if (BackupSection.PROGRESS in fromBackup) source.status else old.status,
                    passFailResult = if (BackupSection.PROGRESS in fromBackup) source.passFailResult else old.passFailResult,
                    officialGrade = if (BackupSection.PROGRESS in fromBackup) source.officialGrade else old.officialGrade,
                    fulfilledOnEpochDay = if (BackupSection.PROGRESS in fromBackup) source.fulfilledOnEpochDay else old.fulfilledOnEpochDay,
                    iconKey = if (BackupSection.ICONS in fromBackup) source.iconKey else old.iconKey,
                    colorKey = if (BackupSection.ICONS in fromBackup) source.colorKey else old.colorKey,
                )
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
        val targetRules = if (schedule) incoming.scheduleRules.map { it.copy(courseId = mapped(it.courseId)) } else local.scheduleRules
        val targetEvents = if (schedule) incoming.oneOffEvents.map { it.copy(courseId = mapped(it.courseId)) } else local.oneOffEvents
        val targetLessons = (targetRules.map { it.id to it.courseId } + targetEvents.map { it.id to it.courseId }).toMap()
        fun BackupGradeItem.validLessonLink(): BackupGradeItem = if (lessonId == null ||
            (targetLessons.containsKey(lessonId) && targetLessons[lessonId] == courseId)) this else copy(lessonId = null)
        val targetGrades = if (progress) local.gradeItems.filterNot { covered(it.courseId) } +
            incoming.gradeItems.mapNotNull { item -> mapped(item.courseId)?.let { item.copy(courseId = it) } }
            else local.gradeItems
        val phoneSettings = local.settings
        val fileSettings = incoming.settings
        val other = BackupSection.PREFERENCES in fromBackup
        val settings = phoneSettings.copy(
            wallpaperRecorded = if (BackupSection.APPEARANCE in fromBackup && fileSettings.wallpaperRecorded) true else phoneSettings.wallpaperRecorded,
            wallpaper = if (BackupSection.APPEARANCE in fromBackup && fileSettings.wallpaperRecorded) fileSettings.wallpaper else phoneSettings.wallpaper,
            reminders = if (other) fileSettings.reminders else phoneSettings.reminders,
            cancellationStyle = if (other) fileSettings.cancellationStyle else phoneSettings.cancellationStyle,
            showHiddenLessons = if (other) fileSettings.showHiddenLessons else phoneSettings.showHiddenLessons,
            parityOverride = if (other) fileSettings.parityOverride else phoneSettings.parityOverride,
            weekLayout = if (other) fileSettings.weekLayout else phoneSettings.weekLayout,
            homeWorkFilter = if (other) fileSettings.homeWorkFilter.copy(
                courseId = mapped(fileSettings.homeWorkFilter.courseId),
                courseIds = fileSettings.homeWorkFilter.courseIds.mapNotNull(::mapped).distinct()) else phoneSettings.homeWorkFilter,
            uisProgressFrequency = if (other) fileSettings.uisProgressFrequency else phoneSettings.uisProgressFrequency,
            uisSubjectFrequency = if (other) fileSettings.uisSubjectFrequency else phoneSettings.uisSubjectFrequency,
            uisPeriodFrequency = if (other) fileSettings.uisPeriodFrequency else phoneSettings.uisPeriodFrequency,
            uisTimetableFrequency = if (other) fileSettings.uisTimetableFrequency else phoneSettings.uisTimetableFrequency,
            uisApplyProgressAutomatically = if (other) fileSettings.uisApplyProgressAutomatically
                else phoneSettings.uisApplyProgressAutomatically,
            language = if (BackupSection.LANGUAGE in fromBackup) fileSettings.language else phoneSettings.language,
            courseNameLanguage = if (BackupSection.LANGUAGE in fromBackup) fileSettings.courseNameLanguage
                else phoneSettings.courseNameLanguage,
            ukrainianCourseNameFallback = if (BackupSection.LANGUAGE in fromBackup)
                fileSettings.ukrainianCourseNameFallback else phoneSettings.ukrainianCourseNameFallback,
            themeMode = if (BackupSection.APPEARANCE in fromBackup) fileSettings.themeMode else phoneSettings.themeMode,
            themeFamily = if (BackupSection.APPEARANCE in fromBackup) fileSettings.themeFamily else phoneSettings.themeFamily,
            customThemes = if (BackupSection.APPEARANCE in fromBackup) fileSettings.customThemes
                else phoneSettings.customThemes,
            selectedCustomThemeId = if (BackupSection.APPEARANCE in fromBackup) fileSettings.selectedCustomThemeId
                else phoneSettings.selectedCustomThemeId,
            addButtonPlacement = if (BackupSection.APPEARANCE in fromBackup) fileSettings.addButtonPlacement else phoneSettings.addButtonPlacement,
            cardAppearance = if (BackupSection.APPEARANCE in fromBackup) fileSettings.cardAppearance
                else phoneSettings.cardAppearance,
            currentSemester = if (BackupSection.CURRENT_SEMESTER in fromBackup) fileSettings.currentSemester
                else phoneSettings.currentSemester,
            semesterPeriods = if (BackupSection.PERIODS in fromBackup) fileSettings.semesterPeriods
                else phoneSettings.semesterPeriods,
        )
        return local.copy(
            teaching = if (BackupSection.TEACHERS in fromBackup && incoming.teaching != null) {
                val old = local.teaching ?: BackupTeaching()
                val codes = destinationByCode.keys
                val teams = incoming.teaching.teams.filter { teachingCode(it.courseCode) in codes }
                val replacedCodes = teams.map { teachingCode(it.courseCode) }.toSet()
                val mergedTeams = (old.teams.filterNot { teachingCode(it.courseCode) in replacedCodes } + teams).takeLast(128)
                val ids = mergedTeams.flatMap { it.teachers }.map { it.personId }.toSet()
                val profiles = incoming.teaching.profiles.filter { it.personId in ids }
                val keys = profiles.map { it.personId to it.language }.toSet()
                BackupTeaching(mergedTeams, (old.profiles.filterNot { (it.personId to it.language) in keys } + profiles).takeLast(256))
            } else local.teaching,
            courses = courses,
            scheduleRules = targetRules,
            oneOffEvents = targetEvents,
            scheduleExceptions = if (schedule) incoming.scheduleExceptions else local.scheduleExceptions,
            lessonAbsences = if (schedule) incoming.lessonAbsences else local.lessonAbsences,
            gradeItems = if (progress || schedule) targetGrades.map { it.validLessonLink() } else targetGrades,
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
            studentHub = if (BackupSection.STUDIES in fromBackup) incoming.studentHub else local.studentHub,
            settings = settings,
            version = BackupPayload.CURRENT_VERSION,
        )
    }
}
