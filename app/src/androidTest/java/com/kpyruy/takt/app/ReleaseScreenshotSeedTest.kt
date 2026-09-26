package com.kpyruy.takt.app

import androidx.test.platform.app.InstrumentationRegistry
import com.kpyruy.takt.core.data.TaktDataContainer
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.model.AppThemeMode
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.ExamInfo
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.model.ScheduleRecurrence
import com.kpyruy.takt.core.model.ScheduleRule
import com.kpyruy.takt.core.model.SemesterPeriod
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.model.ThemeFamily
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Disposable-emulator data for release screenshots; never included in the installed app. */
class ReleaseScreenshotSeedTest {
    @Test fun seedEnglishShowcase() {
        assumeTrue(
            "Use -e releaseScreenshots true only on a disposable emulator",
            InstrumentationRegistry.getArguments().getString("releaseScreenshots") == "true",
        )
        val data = TaktDataContainer(InstrumentationRegistry.getInstrumentation().targetContext)
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

        data.database.clearAllTables()
        runBlocking {
            val names = mapOf(
                "DIVR_6B" to "Digital Media and Virtual Reality",
                "TPAR_6B" to "Control Systems",
                "ZAST_6B" to "Statistics",
                "FYZI_6B" to "Physics",
                "TEVE1_6B" to "Table Tennis",
                "CVSS_6B" to "Final State Examination",
            )
            val icons = mapOf(
                "MATM2_6B" to "Functions",
                "TPAR_6B" to "Sensors",
                "ZAST_6B" to "QueryStats",
                "FYZI_6B" to "Speed",
                "TEVE1_6B" to "SportsBasketball",
                "DIVR_6B" to "DesignServices",
            )
            data.database.courseDao().insertAll(StudyPlanSeed.courses.map { course ->
                course.copy(
                    title = names[course.id] ?: course.title,
                    status = if (course.id == "DIVR_6B") CourseStatus.FULFILLED.storageValue else course.status,
                    iconKey = icons[course.id],
                )
            })
            data.settingsRepository.setLanguage(AppLanguage.ENGLISH)
            data.settingsRepository.setThemeMode(AppThemeMode.LIGHT)
            data.settingsRepository.setThemeFamily(ThemeFamily.BLUE)
            data.settingsRepository.setCurrentSemester(3)
            data.settingsRepository.setSemesterPeriods(mapOf(3 to SemesterPeriod(
                studyStart = monday.minusWeeks(3),
                studyEnd = monday.plusWeeks(11),
                examStart = monday.plusWeeks(13),
                examEnd = monday.plusWeeks(16),
            )))
            data.studyPlanRepository.setGradingType("FYZI_6B", CourseGradingType.EXAM_LETTER)
            data.studyPlanRepository.setGradingType("TPAR_6B", CourseGradingType.EXAM_LETTER)

            listOf(
                lesson("physics-lecture", "FYZI_6B", "Physics", DayOfWeek.MONDAY, 8, 0, 9, 30, "B-214", LessonType.LECTURE),
                lesson("control-seminar", "TPAR_6B", "Control Systems", DayOfWeek.MONDAY, 10, 0, 11, 30, "C-108", LessonType.SEMINAR),
                lesson("statistics-lecture", "ZAST_6B", "Statistics", DayOfWeek.MONDAY, 13, 15, 14, 45, "A-305", LessonType.LECTURE),
                lesson("control-lab", "TPAR_6B", "Control Systems", DayOfWeek.TUESDAY, 9, 0, 10, 30, "Lab 2", LessonType.LAB),
                lesson("physics-practice", "FYZI_6B", "Physics", DayOfWeek.TUESDAY, 11, 0, 12, 30, "B-214", LessonType.PRACTICE),
                lesson("statistics-practice", "ZAST_6B", "Statistics", DayOfWeek.WEDNESDAY, 8, 0, 9, 30, "A-305", LessonType.PRACTICE),
                lesson("control-lecture", "TPAR_6B", "Control Systems", DayOfWeek.THURSDAY, 10, 0, 11, 30, "C-108", LessonType.LECTURE),
                lesson("physics-lab", "FYZI_6B", "Physics", DayOfWeek.FRIDAY, 8, 0, 9, 30, "Lab 4", LessonType.LAB),
                lesson("statistics-seminar", "ZAST_6B", "Statistics", DayOfWeek.FRIDAY, 10, 0, 11, 30, "A-305", LessonType.SEMINAR),
            ).forEach { data.scheduleRepository.upsertRule(it) }

            listOf(
                StudyTask("demo-review", "FYZI_6B", "Review wave equations", null, today, false),
                StudyTask("demo-lab-report", "FYZI_6B", "Submit motion lab report", "Attach the graphs and final measurements.", today.plusDays(1), false, true, maxPoints = 10.0, minimumPointsForExam = 6.0),
                StudyTask("demo-plc", "TPAR_6B", "Finish PLC simulation", null, today.plusDays(2), false, true, maxPoints = 15.0, minimumPointsForExam = 8.0),
                StudyTask("demo-probability", "ZAST_6B", "Solve probability exercises", null, today.plusDays(3), false),
                StudyTask("demo-presentation", "FYZI_6B", "Prepare project presentation", null, today.plusDays(7), false),
                StudyTask("demo-complete", "TPAR_6B", "Read the sensor datasheet", null, today.minusDays(2), true),
            ).forEach { data.studyContentRepository.upsertTask(it) }

            listOf(
                GradeItem("demo-physics-lab", "FYZI_6B", "Motion lab", GradeItemType.LAB, 16.0, 20.0),
                GradeItem("demo-physics-project", "FYZI_6B", "Semester project", GradeItemType.PROJECT, 8.0, 10.0),
                GradeItem("demo-physics-quiz", "FYZI_6B", "Wave equations quiz", GradeItemType.TEST, 0.0, 10.0,
                    dueDate = today, completed = false, requiredForExam = true, minimumPointsForExam = 6.0,
                    lessonId = "physics-lecture", durationMinutes = 45),
                GradeItem("demo-physics-exam", "FYZI_6B", "Physics final exam", GradeItemType.EXAM, 0.0, 60.0,
                    dueDate = monday.plusWeeks(14), completed = false),
                GradeItem("demo-control-lab", "TPAR_6B", "Sensors lab", GradeItemType.LAB, 18.0, 25.0),
                GradeItem("demo-control-test", "TPAR_6B", "Control theory test", GradeItemType.MIDTERM, 0.0, 15.0,
                    dueDate = today.plusDays(3), completed = false, requiredForExam = true,
                    minimumPointsForExam = 8.0, durationMinutes = 60),
                GradeItem("demo-control-exam", "TPAR_6B", "Control Systems final exam", GradeItemType.EXAM, 0.0, 60.0,
                    dueDate = monday.plusWeeks(15), completed = false),
                GradeItem("demo-stats-quiz", "ZAST_6B", "Descriptive statistics quiz", GradeItemType.TEST, 9.0, 10.0),
                GradeItem("demo-stats-project", "ZAST_6B", "Data analysis project", GradeItemType.PROJECT, 0.0, 20.0,
                    dueDate = today.plusDays(4), completed = false),
            ).forEach { data.gradeRepository.upsertItem(it) }

            data.examRepository.upsertExamInfo(ExamInfo("FYZI_6B", "demo-physics-exam", monday.plusWeeks(14),
                LocalTime.of(9, 0), LocalTime.of(11, 0), "B-214"))
            data.examRepository.upsertExamInfo(ExamInfo("TPAR_6B", "demo-control-exam", monday.plusWeeks(15),
                LocalTime.of(10, 0), LocalTime.of(12, 0), "C-108"))
            data.studyContentRepository.upsertNote(CourseNote("demo-note", "FYZI_6B", "Exam preparation",
                "Focus on waves, dynamics and the lab calculations.", System.currentTimeMillis()))
        }
    }

    private fun lesson(
        id: String, courseId: String, title: String, day: DayOfWeek,
        startHour: Int, startMinute: Int, endHour: Int, endMinute: Int,
        room: String, type: LessonType,
    ) = ScheduleRule(id, courseId, title, day,
        LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute),
        ScheduleRecurrence.WEEKLY, room, type)
}
