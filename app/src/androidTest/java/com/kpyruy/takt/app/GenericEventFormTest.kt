package com.kpyruy.takt.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.database.CourseEntity
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.LessonTypeSelector
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.theme.TaktTheme
import com.kpyruy.takt.feature.calendar.AddLessonForm
import com.kpyruy.takt.feature.calendar.OneOffEventForm
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** New candidate tests for root. No baseline tests or APK fixtures are modified. */
class GenericEventFormTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.of(2026, 11, 19)
    private lateinit var context: Context
    private lateinit var db: TaktDatabase
    private lateinit var settings: AppSettingsRepository
    private lateinit var schedule: RoomScheduleRepository
    private lateinit var studyPlan: RoomStudyPlanRepository
    private lateinit var grades: RoomGradeRepository
    private lateinit var study: RoomStudyContentRepository
    private lateinit var exams: RoomExamRepository
    private lateinit var documents: TaktDocumentStore
    private var visible by mutableStateOf(true)

    @Before fun prepare() {
        TaktI18n.use(AppLanguage.UKRAINIAN)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TaktDatabase::class.java).build()
        settings = object : AppSettingsRepository by SharedPreferencesAppSettingsRepository(context) {
            override val settings = MutableStateFlow(AppSettings())
        }
        schedule = RoomScheduleRepository(db)
        studyPlan = RoomStudyPlanRepository(db, settings)
        grades = RoomGradeRepository(db)
        study = RoomStudyContentRepository(db)
        exams = RoomExamRepository(db)
        documents = TaktDocumentStore(context, db, RoomBackupRepository(db, settings), settings)
    }

    @After fun cleanup() {
        compose.runOnIdle { visible = false }
        compose.waitForIdle()
        db.close()
        TaktI18n.use(AppLanguage.UKRAINIAN)
    }

    @Test fun eventRouteDefaultsToGenericAndSavesUnlinkedDraftOnItsDate() {
        var saves = 0
        compose.setContent {
            TaktTheme {
                if (visible) CreateItemScreen(CreateItemType.EVENT, null,
                    CreateItemDraft(CreateItemType.EVENT, title = "Chess tournament", dueDate = date,
                        startTime = LocalTime.of(16, 0), endTime = LocalTime.of(18, 0)),
                    studyPlanRepository = studyPlan, scheduleRepository = schedule,
                    settingsRepository = settings, gradeRepository = grades,
                    studyContentRepository = study, documentStore = documents,
                    onBack = {}, onSaved = { saves++ })
            }
        }
        compose.onNodeWithTag("lesson-type-trigger").assertTextContains(t("Подія"))
        compose.onNodeWithText(t("Додаткова пара")).assertDoesNotExist()
        compose.onNodeWithText(t("Блокова акція")).assertDoesNotExist()
        select("LAB")
        compose.onNodeWithText(t("Додаткова пара")).assertExists()
        select("EVENT")
        compose.onNodeWithText(t("Додаткова пара")).assertDoesNotExist()
        compose.onNodeWithTag("oneoff-title").assertTextContains("Chess tournament")
        compose.onNodeWithTag("oneoff-save").performScrollTo().performClick()
        compose.waitUntil(5_000) { saves == 1 }
        val saved = runBlocking { schedule.observeOneOffEvents().first().single() }
        assertEquals(LessonType.EVENT, saved.lessonType)
        assertEquals("Chess tournament", saved.title)
        assertNull(saved.courseId)
        assertEquals(date, saved.date)
        assertEquals(LocalTime.of(16, 0), saved.startTime)
        assertEquals(LocalTime.of(18, 0), saved.endTime)
        assertEquals(OneOffScheduleEventType.EXTRA, saved.type)
        assertTrue(runBlocking { schedule.observeRules().first().isEmpty() })
    }

    @Test fun storedEventReopensAndUpdatesSameIdWithoutCourseOrCategoryLoss() {
        val initial = OneOffScheduleEvent("chess", null, "Chess tournament", date,
            LocalTime.of(16, 0), LocalTime.of(18, 0), "Club", OneOffScheduleEventType.EXTRA, LessonType.EVENT)
        runBlocking { schedule.upsertOneOffEvent(initial) }
        val reopened = runBlocking { RoomScheduleRepository(db).observeOneOffEvents().first().single() }
        var saved: OneOffScheduleEvent? = null
        compose.setContent {
            TaktTheme {
                if (visible) OneOffEventForm(date, initialEvent = reopened,
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("lesson-type-trigger").assertTextContains(t("Подія"))
        compose.onNodeWithTag("oneoff-title").performTextReplacement("Updated tournament")
        select("EVENT")
        compose.onNodeWithTag("oneoff-save").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(initial.copy(title = "Updated tournament"), saved) }
        runBlocking {
            schedule.upsertOneOffEvent(checkNotNull(saved))
            assertEquals(listOf(initial.copy(title = "Updated tournament")),
                RoomScheduleRepository(db).observeOneOffEvents().first())
        }
    }

    @Test fun lessonPopupKeepsOptionalCourseAndCustomEventTitleThroughOnceAndRecurringSaves() {
        val course = Course("course", "CODE", "Academic course", 5, 2, CourseStatus.ENROLLED)
        val once = mutableListOf<OneOffScheduleEvent>()
        val recurring = mutableListOf<ScheduleRule>()
        compose.setContent {
            TaktTheme {
                if (visible) AddLessonForm(date.dayOfWeek, courses = listOf(course), initialDate = date,
                    initialCourseId = course.id, initialTitle = "Chess club",
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    onSaveOneOff = { once.add(it) }, onSave = { recurring.add(it) })
            }
        }
        compose.onNodeWithTag("lesson-type-trigger").assertTextContains(t("Не вказано"))
        select("EVENT")
        compose.onNodeWithTag("lesson-custom-title").assertTextContains("Chess club")
        compose.onNodeWithText(t("Зберегти")).performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(recurring.isEmpty())
            assertEquals(date, once.single().date)
            assertEquals(LessonType.EVENT, once.single().lessonType)
            assertEquals("Chess club", once.single().title)
            assertEquals(course.id, once.single().courseId)
        }
        compose.onNodeWithTag("lesson-repeat-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-repeat-WEEKLY").performScrollTo().performClick()
        compose.onNodeWithText(t("Зберегти")).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, once.size)
            assertEquals(ScheduleRecurrence.WEEKLY, recurring.single().recurrence)
            assertEquals(LessonType.EVENT, recurring.single().lessonType)
            assertEquals("Chess club", recurring.single().title)
        }
    }

    @Test fun quickAddUsesSamePopupAndCarriesSelectionToFullFormDraft() {
        var saved: ScheduleRule? = null
        var draft: CreateItemDraft? = null
        compose.setContent {
            TaktTheme {
                if (visible) QuickAddSheet(emptyList(), onDismiss = {}, onSaveTask = {},
                    onSaveLesson = { saved = it }, onSaveNote = {}, onOpenFull = { draft = it })
            }
        }
        compose.onNodeWithText(t("Пара")).performClick()
        select("EVENT")
        compose.onNode(hasSetTextAction() and hasText(t("Назва"))).performTextReplacement("Chess club")
        compose.onNodeWithText(t("Зберегти")).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(LessonType.EVENT, saved?.lessonType)
            assertNull(saved?.courseId)
            assertEquals("Chess club", saved?.title)
        }
        compose.onNodeWithText(t("Більше параметрів")).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(LessonType.EVENT, draft?.lessonType)
            assertEquals("Chess club", draft?.title)
            assertEquals(CreateItemType.CLASS, draft?.type)
        }
    }

    @Test fun reminderStillSavesAsReminderAndHelpRefersToNotificationSettings() {
        var saved: OneOffScheduleEvent? = null
        compose.setContent {
            TaktTheme {
                if (visible) OneOffEventForm(date, initialType = OneOffScheduleEventType.REMINDER,
                    initialTitle = "Prepare for tournament", modifier = Modifier.verticalScroll(rememberScrollState()),
                    onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("lesson-type-trigger").assertDoesNotExist()
        compose.onNodeWithText(t("Сповіщення для нагадувань можна налаштувати в «Під себе» → «Сповіщення».")).assertExists()
        compose.onNodeWithTag("oneoff-save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(OneOffScheduleEventType.REMINDER, saved?.type)
            assertEquals(LessonType.UNSPECIFIED, saved?.lessonType)
        }
    }

    @Test fun hiddenAcademicKindAndDraftFieldsReturnAfterSwitchingAwayFromEvent() {
        val initial = OneOffScheduleEvent("lab", null, "Lab draft", date,
            LocalTime.of(10, 0), LocalTime.of(11, 0), "207", OneOffScheduleEventType.BLOCK_ACTION, LessonType.LAB)
        var saved: OneOffScheduleEvent? = null
        compose.setContent {
            TaktTheme {
                if (visible) OneOffEventForm(date, initialEvent = initial,
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("oneoff-title").performTextReplacement("Kept draft")
        select("EVENT")
        compose.onNodeWithText(t("Блокова акція")).assertDoesNotExist()
        compose.onNodeWithTag("oneoff-room").performScrollTo().assertTextContains("207")
        select("LAB")
        compose.onNodeWithText(t("Блокова акція")).assertIsSelected()
        compose.onNodeWithTag("oneoff-save").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(initial.copy(title = "Kept draft"), saved) }
    }

    @Test fun genericEventRetainsEndAfterStartValidation() {
        var saved: OneOffScheduleEvent? = null
        compose.setContent {
            TaktTheme {
                if (visible) OneOffEventForm(date, initialTitle = "Chess tournament",
                    initialStartTime = LocalTime.NOON, initialEndTime = LocalTime.of(11, 0),
                    modifier = Modifier.verticalScroll(rememberScrollState()), onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("oneoff-save").performScrollTo().performClick()
        compose.onNodeWithText(t("Кінець має бути пізніше початку.")).assertExists()
        compose.runOnIdle { assertNull(saved) }
    }

    @Test fun customTagPrefixesKeepTwoSelectorsIndependent() {
        var first by mutableStateOf(LessonType.UNSPECIFIED)
        var second by mutableStateOf(LessonType.LAB)
        compose.setContent {
            TaktTheme {
                if (visible) Column {
                    LessonTypeSelector(first, tagPrefix = "first-type") { first = it }
                    LessonTypeSelector(second, tagPrefix = "second-type") { second = it }
                }
            }
        }
        compose.onNodeWithTag("first-type-trigger").performClick()
        compose.onNodeWithTag("first-type-EVENT").performScrollTo().performClick()
        compose.onNodeWithTag("first-type-trigger").assertTextContains(t("Подія"))
        compose.onNodeWithTag("second-type-trigger").assertTextContains(t(LessonType.LAB.label))
        compose.runOnIdle { assertEquals(LessonType.LAB, second) }
    }

    @Test fun actualSubjectHeaderKeepsLargerAvatarAndAccessibleIconPicker() {
        runBlocking {
            db.courseDao().insertAll(listOf(CourseEntity("course", "CODE", "Course", 5, 2,
                "enrolled", "COMPULSORY", null, iconKey = "Speed")))
        }
        compose.setContent {
            TaktTheme {
                if (visible) Box(Modifier.fillMaxSize().background(Color.Black)) {
                    SubjectDetailScreen(studyPlan, grades, schedule, settings, study, exams, documents,
                        courseId = "course", onBack = {})
                }
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("subject-header-avatar").fetchSemanticsNodes().isNotEmpty()
        }
        val avatar = compose.onNodeWithTag("subject-header-avatar")
            .assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(56.dp)
            .assert(hasContentDescription(t("Змінити іконку предмета"))).assertHasClickAction()
        val pixels = avatar.captureToImage().toPixelMap()
        // 6dp,6dp lies inside the 13dp rounded square, outside a 56dp circle.
        val inset = (pixels.width * 6f / 56f).toInt()
        assertNotEquals("Backing must extend outside the old circular ancestor clip", Color.Black, pixels[inset, inset])
        assertEquals("The backing still has rounded corners", Color.Black, pixels[0, 0])
        avatar.performClick()
        compose.onNodeWithTag("course-icon-grid").assertExists()
    }

    private fun select(type: String) {
        compose.onNodeWithTag("lesson-type-trigger").performScrollTo().performClick()
        compose.onNodeWithTag("takt-choice-menu").assertIsDisplayed()
        compose.onNodeWithTag("lesson-type-$type").performScrollTo().performClick()
        compose.onNodeWithTag("lesson-type-$type").assertDoesNotExist()
    }
}
