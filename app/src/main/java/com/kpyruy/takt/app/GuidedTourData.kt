package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.i18n.t
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.LocalTime

/** The tour never seeds Room, saves preferences, sends notifications or imports UIS data. */
internal class GuidedTourData(appearance: AppSettings, today: LocalDate = LocalDate.now()) :
    StudyPlanRepository, GradeRepository, StudyContentRepository, ScheduleRepository, ExamRepository, AppSettingsRepository {
    override val settings = MutableStateFlow(appearance.copy(currentSemester = 1, uisCurrentSemester = null,
        semesterPeriods = emptyMap(), weekLayout = WeekLayout.TIMETABLE))
    val courses = listOf(
        Course("tour-physics", "FYZI", t(UiKey.TEXT_PHYSICS), 6, 1, CourseStatus.ENROLLED,
            gradingType = CourseGradingType.EXAM_LETTER, iconKey = "Atom"),
        Course("tour-code", "PROG", t(UiKey.TEXT_PROGRAMMING), 5, 1, CourseStatus.ENROLLED, iconKey = "Code"),
        Course("tour-math", "MATH", t(UiKey.TEXT_MATHEMATICS), 6, 1, CourseStatus.FULFILLED,
            iconKey = "Functions", officialGrade = GradeLetter.A, fulfilledOn = today.minusDays(3)),
        Course("tour-language", "LANG", t(UiKey.TEXT_FOREIGN_LANGUAGE), 3, 2, CourseStatus.PLANNED, iconKey = "Translate"),
    )
    private val events = listOf(0L, 1L, 3L).flatMap { offset ->
        listOf(
            OneOffScheduleEvent("tour-physics-$offset", courses[0].id, courses[0].title, today.plusDays(offset),
                LocalTime.of(9, 0), LocalTime.of(10, 30), "T 101", OneOffScheduleEventType.EXTRA, LessonType.LECTURE),
            OneOffScheduleEvent("tour-code-$offset", courses[1].id, courses[1].title, today.plusDays(offset),
                LocalTime.of(11, 0), LocalTime.of(12, 30), "T 205", OneOffScheduleEventType.EXTRA, LessonType.LAB),
        )
    }
    private val tasks = listOf(StudyTask("tour-task", courses[0].id, t(UiKey.TEXT_LAB_ASSIGNMENT), null,
        today.plusDays(2), false, requiredForExam = true, maxPoints = 20.0, minimumPointsForExam = 10.0))
    private val notes = listOf(CourseNote("tour-note", courses[0].id, t(UiKey.TEXT_CLASS_MATERIALS),
        t(UiKey.TEXT_KEEP_YOUR_NOTES_FILES_AND_USEFUL_LINKS_HERE), today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
    val snapshot = PlanningSnapshot(courses, emptyList(), emptyList(), events,
        emptyList(), tasks, emptyList(), settings.value)
    val planning = MutableStateFlow<PlanningSnapshot?>(snapshot)

    override fun observeCourses() = flowOf(courses)
    override fun observeSemester(semester: Int) = flowOf(courses.filter { it.semester == semester })
    override fun observeCourse(courseId: String) = flowOf(courses.find { it.id == courseId })
    override fun observeItems(courseId: String) = flowOf(emptyList<GradeItem>())
    override fun observeAllItems() = flowOf(emptyList<GradeItem>())
    override fun observeRecentItems(limit: Int) = flowOf(emptyList<GradeItem>())
    override fun observeScale(courseId: String) = flowOf(GradeScale.default())
    override fun observeManualGrade(courseId: String) = flowOf<GradeLetter?>(null)
    override fun observeAllTasks() = flowOf(tasks)
    override fun observeTasks(courseId: String) = flowOf(tasks.filter { it.courseId == courseId })
    override fun observeNotes(courseId: String) = flowOf(notes.filter { it.courseId == courseId })
    override fun observeRules() = flowOf(emptyList<ScheduleRule>())
    override fun observeOneOffEvents() = flowOf(events)
    override fun observeExceptions() = flowOf(emptyList<ScheduleException>())
    override fun observeAbsences() = flowOf(emptyList<LessonAbsence>())
    override fun observeAllExamInfo() = flowOf(emptyList<ExamInfo>())
    override fun observeExamInfo(courseId: String) = flowOf<ExamInfo?>(null)
    override fun observeMaterials(courseId: String) = flowOf(emptyList<ExamMaterial>())

    // Defensive no-ops: even an accessibility action cannot write through a demo repository.
    override suspend fun upsertImportedCourses(courses: List<Course>, selectedLocalIds: Map<String, String>) = 0
    override suspend fun importedCourseChoices() = emptySet<String>()
    override suspend fun addCourse(title: String, code: String, credits: Int, semester: Int) = "tour-only"
    override suspend fun setIcon(courseId: String, iconKey: String?) = Unit
    override suspend fun updateStatus(courseId: String, status: CourseStatus) = Unit
    override suspend fun setGradingType(courseId: String, gradingType: CourseGradingType) = Unit
    override suspend fun setPassFailResult(courseId: String, result: PassFailResult?) = Unit
    override suspend fun setOfficialResult(courseId: String, grade: GradeLetter?, fulfilledOn: LocalDate?) = Unit
    override suspend fun upsertItem(item: GradeItem) = Unit
    override suspend fun deleteItem(id: String) = Unit
    override suspend fun setScale(courseId: String, scale: GradeScale) = Unit
    override suspend fun setManualGrade(courseId: String, grade: GradeLetter?) = Unit
    override suspend fun upsertTask(task: StudyTask) = Unit
    override suspend fun setTaskCompleted(id: String, completed: Boolean) = Unit
    override suspend fun deleteTask(id: String) = Unit
    override suspend fun upsertNote(note: CourseNote) = Unit
    override suspend fun deleteNote(id: String) = Unit
    override suspend fun setAbsent(event: ResolvedScheduleEvent, absent: Boolean) = Unit
    override suspend fun replaceSchedule(rules: List<ScheduleRule>, events: List<OneOffScheduleEvent>) = Unit
    override suspend fun upsertRule(rule: ScheduleRule) = Unit
    override suspend fun upsertRules(rules: List<ScheduleRule>) = Unit
    override suspend fun upsertOneOffEvent(event: OneOffScheduleEvent) = Unit
    override suspend fun upsertOneOffEvents(events: List<OneOffScheduleEvent>) = Unit
    override suspend fun deleteOneOffEvent(id: String) = Unit
    override suspend fun upsertException(exception: ScheduleException) = Unit
    override suspend fun deleteRule(id: String) = Unit
    override suspend fun deleteException(id: String) = Unit
    override suspend fun deleteExceptionForOccurrence(ruleId: String, date: LocalDate) = Unit
    override suspend fun upsertExamInfo(info: ExamInfo) = Unit
    override suspend fun upsertMaterial(material: ExamMaterial) = Unit
    override suspend fun deleteMaterial(id: String) = Unit
    override suspend fun setReminders(reminders: ReminderSettings) = Unit
    override suspend fun updateReminders(transform: (ReminderSettings) -> ReminderSettings) = Unit
    override suspend fun setCancellationStyle(style: CancellationDisplayStyle) = Unit
    override suspend fun setShowHiddenLessons(show: Boolean) = Unit
    override suspend fun setParityOverride(override: ParityOverride) = Unit
    override suspend fun setCardAppearance(appearance: CardAppearance) = Unit
    override suspend fun setThemeFamily(themeFamily: ThemeFamily) = Unit
    override suspend fun setThemeMode(themeMode: AppThemeMode) = Unit
    override suspend fun setLanguage(language: AppLanguage) = Unit
    override suspend fun setCourseNameLanguage(language: CourseNameLanguage) = Unit
    override suspend fun setUkrainianCourseNameFallback(language: CourseNameLanguage) = Unit
    override suspend fun setUisAutoSync(settings: UisAutoSyncSettings) = Unit
    override suspend fun setAppearance(themeMode: AppThemeMode, themeFamily: ThemeFamily, cardAppearance: CardAppearance) = Unit
    override suspend fun setAppearance(appearance: AppSettings) = Unit
    override suspend fun setWeekLayout(layout: WeekLayout) = Unit
    override suspend fun setHomeWorkFilter(filter: HomeWorkFilter) = Unit
    override suspend fun setSemesterPeriods(periods: Map<Int, SemesterPeriod>) = Unit
    override suspend fun setCurrentSemester(semester: Int?) = Unit
    override fun setUisCurrentSemester(semester: Int?) = Unit
}
