package com.kpyruy.takt.app

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.TaktDataContainer
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.model.*
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assume.assumeTrue

/** Explicitly opt-in and emulator-only; this data is never part of an installed release. */
internal suspend fun TaktDataContainer.seedLiquidGlassReview() {
    assumeTrue(InstrumentationRegistry.getArguments().getString("liquidGlassReview") == "true")
    assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone") || Build.HARDWARE == "ranchu")
    val day = LocalDate.now()
    database.clearAllTables()
    val active = listOf(
        CourseEntity("glass-physics", "FYZI_6B", "Фізика", 5, 3, "enrolled", "COMPULSORY", null,
            gradingType = "EXAM_LETTER", iconKey = "Atom"),
        CourseEntity("glass-programming", "TPAR_6B", "Теорія програмування", 6, 3, "enrolled", "COMPULSORY", null,
            gradingType = "EXAM_LETTER", iconKey = "Computer"),
        CourseEntity("glass-math", "MATE_6B", "Математика", 5, 3, "enrolled", "COMPULSORY", null,
            iconKey = "Functions"),
        CourseEntity("glass-english", "ANJA_6B", "Англійська мова", 3, 3, "fulfilled", "COMPULSORY", null,
            iconKey = "Chat", officialGrade = "A", fulfilledOnEpochDay = day.minusDays(4).toEpochDay()),
        CourseEntity("glass-code", "PROG_6B", "Програмування", 6, 3, "fulfilled", "COMPULSORY", null,
            iconKey = "Code", officialGrade = "B", fulfilledOnEpochDay = day.minusDays(2).toEpochDay()),
    )
    val planned = (1..6).filter { it != 3 }.flatMap { semester ->
        (1..5).map { index -> CourseEntity("glass-$semester-$index", "STUD${semester}_$index", "Предмет $index", 6,
            semester, if (semester < 3) "fulfilled" else "planned", "COMPULSORY", null,
            iconKey = "MenuBook", officialGrade = if (semester < 3) "B" else null) }
    }
    database.courseDao().insertAll(active + planned)
    settingsRepository.setLanguage(AppLanguage.UKRAINIAN)
    settingsRepository.setThemeMode(AppThemeMode.LIGHT)
    settingsRepository.setThemeFamily(ThemeFamily.BLUE)
    settingsRepository.setCardAppearance(CardAppearance.GLASS)
    settingsRepository.setCurrentSemester(3)
    settingsRepository.setWeekLayout(WeekLayout.TIMETABLE)
    settingsRepository.setSemesterPeriods(mapOf(3 to SemesterPeriod(day.minusWeeks(3), day.plusWeeks(10),
        day.plusWeeks(11), day.plusWeeks(14))))
    listOf(
        ScheduleRule("glass-lecture", "glass-physics", "Фізика", day.dayOfWeek, LocalTime.of(8, 0), LocalTime.of(9, 30),
            ScheduleRecurrence.WEEKLY, "302", LessonType.LECTURE),
        ScheduleRule("glass-practice", "glass-programming", "Теорія програмування", day.dayOfWeek,
            LocalTime.of(10, 0), LocalTime.of(11, 30), ScheduleRecurrence.WEEKLY, "215", LessonType.PRACTICE),
        ScheduleRule("glass-seminar", "glass-math", "Математика", day.dayOfWeek, LocalTime.NOON, LocalTime.of(13, 30),
            ScheduleRecurrence.WEEKLY, "118", LessonType.SEMINAR),
    ).forEach { scheduleRepository.upsertRule(it) }
    listOf(
        GradeItem("glass-gate", "glass-physics", "Допусковий тест", GradeItemType.TEST, 0.0, 20.0,
            completed = false, dueDate = day.minusDays(3), requiredForExam = true, minimumPointsForExam = 10.0,
            durationMinutes = 45, contributesToGrade = false, awaitingResult = true),
        GradeItem("glass-overdue", "glass-physics", "Звіт з лабораторної", GradeItemType.LAB, 0.0, 20.0,
            completed = false, dueDate = day.minusDays(3)),
        GradeItem("glass-lab", "glass-physics", "Лабораторна робота", GradeItemType.LAB, 20.0, 20.0,
            completed = true, dueDate = day.minusDays(3)),
        GradeItem("glass-exam", "glass-physics", "Підсумковий екзамен", GradeItemType.EXAM, 0.0, 100.0,
            completed = false, dueDate = day.plusMonths(2)),
        GradeItem("glass-logic", "glass-programming", "Тест з логіки", GradeItemType.TEST, 0.0, 20.0,
            completed = false, dueDate = day.plusDays(3)),
    ).forEach { gradeRepository.upsertItem(it) }
    studyContentRepository.upsertTask(StudyTask("glass-report", "glass-physics", "Лабораторна №3", null,
        day.plusDays(1), false, maxPoints = 20.0))
    examRepository.upsertExamInfo(ExamInfo("glass-physics", "glass-exam", day.plusMonths(2), LocalTime.of(9, 0),
        LocalTime.of(11, 0), "302"))
    val epoch = System.currentTimeMillis()
    listOf(
        CourseNote("glass-formulas", "glass-physics", "Формули для кінематики", "Рівномірний рух і прискорення. Основні формули для задач.", epoch,
            attachments = listOf(NoteAttachment.webLink("https://is.stuba.sk", "Матеріали курсу"))),
        CourseNote("glass-notes", "glass-physics", "Конспект лекції №4", "Енергія та робота в механіці. Потенціальна і кінетична енергія.", epoch - 1000),
        CourseNote("glass-questions", "glass-physics", "Питання до екзамену", "Основні теми та приклади задач для підготовки.", epoch - 2000,
            attachments = listOf(NoteAttachment.webLink("https://is.stuba.sk", "Навчальна програма"))),
    ).forEach { studyContentRepository.upsertNote(it) }
    awaitRestoredPlanning()
}
