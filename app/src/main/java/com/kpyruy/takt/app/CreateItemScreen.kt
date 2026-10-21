package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.components.rememberSaveAction
import com.kpyruy.takt.core.ui.components.SaveErrorDialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.model.GradeItemType
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.model.OneOffScheduleEventType
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics
import com.kpyruy.takt.feature.calendar.AddLessonForm
import com.kpyruy.takt.feature.calendar.OneOffEventForm
import com.kpyruy.takt.feature.subjects.AddGradeItemForm
import com.kpyruy.takt.feature.subjects.AddNoteForm
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.model.DeviceAuthenticationResult
import com.kpyruy.takt.feature.settings.DeviceAuthenticationRequest
import android.security.keystore.UserNotAuthenticatedException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.kpyruy.takt.feature.subjects.AddTaskForm
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
fun CreateItemScreen(
    type: CreateItemType,
    courseId: String?,
    draft: CreateItemDraft?,
    courses: List<com.kpyruy.takt.core.model.Course> = emptyList(),
    studyPlanRepository: StudyPlanRepository,
    scheduleRepository: ScheduleRepository,
    settingsRepository: AppSettingsRepository,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    documentStore: TaktDocumentStore,
    universityAccountRepository: UniversityAccountRepository? = null,
    authenticateDevice: DeviceAuthenticationRequest? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    // Saving may resume from a Room transaction; navigation and feedback belong on Main.
    val saves = rememberSaveAction()
    SaveErrorDialog(saves)
    val haptics = rememberTaktHaptics()
    val settings = settingsRepository.settings.collectAsState().value
    val academicSemester = courses.firstOrNull { it.id == courseId }?.let {
        settings.academicSemester(it, settings.effectiveCurrentSemester(courses))
    }
    val uisAccount = universityAccountRepository?.hasAccount?.collectAsState()?.value == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = t(when (type) {
                CreateItemType.COURSE -> UiKey.TEXT_ADD_COURSE
                CreateItemType.CLASS -> UiKey.TEXT_ADD_CLASS
                CreateItemType.TASK -> UiKey.WORK_ADD_TASK
                CreateItemType.TEST -> UiKey.TEXT_ADD_TEST
                CreateItemType.EXAM -> UiKey.TEXT_ADD_AN_EXAM
                CreateItemType.NOTE -> UiKey.TEXT_ADD_A_NOTE
                CreateItemType.EVENT -> UiKey.TEXT_ADD_EVENT
                CreateItemType.REMINDER -> UiKey.TEXT_ADD_REMINDER
            }),
            subtitle = courses.firstOrNull { it.id == courseId }?.title,
            navigation = {
                TaktIconButton(Icons.Default.ArrowBack, t(UiKey.ACTION_BACK), onBack)
            },
        )

        if (type.requiresCourse && courseId == null) {
            SectionCard {
                Text(t(UiKey.TEXT_FIRST_CHOOSE_A_SUBJECT), style = MaterialTheme.typography.titleMedium)
            }
            return@Column
        }

        when (type) {
            CreateItemType.COURSE -> if (uisAccount) {
                Text(t(UiKey.TEXT_SUBJECTS_ARE_ADDED_AUTOMATICALLY_FROM_UIS))
            } else AddCourseForm(studyPlanRepository, onSaved)
            CreateItemType.CLASS -> AddLessonForm(
                courses = courses,
                initialCourseId = courseId ?: draft?.courseId,
                initialLessonType = draft?.lessonType ?: com.kpyruy.takt.core.model.LessonType.UNSPECIFIED,
                initialDay = (draft?.dueDate ?: LocalDate.now()).dayOfWeek,
                initialDate = draft?.dueDate,
                initialTitle = draft?.title.orEmpty(),
                initialStartTime = draft?.startTime,
                initialEndTime = draft?.endTime,
                showHeading = false,
                onLoadUisLessons = universityAccountRepository?.takeIf { uisAccount }?.let { account ->
                    { course ->
                        try {
                            account.loadLessonOptions(course.code)
                        } catch (error: UserNotAuthenticatedException) {
                            val authenticate = authenticateDevice ?: throw error
                            val result = suspendCancellableCoroutine<DeviceAuthenticationResult> { continuation ->
                                authenticate(t(UiKey.TEXT_LOAD_LESSONS_FROM_UIS)) { value ->
                                    if (continuation.isActive) continuation.resume(value)
                                }
                            }
                            if (result != DeviceAuthenticationResult.SUCCESS) throw error
                            account.loadLessonOptions(course.code)
                        }
                    }
                },
                onSaveOneOff = { event ->
                    saves.launch {
                        scheduleRepository.upsertOneOffEvent(event)
                        haptics.confirm()
                        onSaved()
                    }
                },
                onSave = { rule ->
                    saves.launch {
                        scheduleRepository.upsertRule(rule)
                        haptics.confirm()
                        onSaved()
                    }
                },
            )

            CreateItemType.EVENT,
            CreateItemType.REMINDER -> OneOffEventForm(
                courses = courses,
                initialDate = draft?.dueDate ?: LocalDate.now(),
                initialType = if (type == CreateItemType.REMINDER) {
                    OneOffScheduleEventType.REMINDER
                } else {
                    OneOffScheduleEventType.EXTRA
                },
                initialCourseId = courseId ?: draft?.courseId,
                initialLessonType = if (type == CreateItemType.REMINDER) LessonType.UNSPECIFIED
                    else draft?.lessonType?.takeUnless { it == LessonType.UNSPECIFIED } ?: LessonType.EVENT,
                initialTitle = draft?.title.orEmpty(),
                initialStartTime = draft?.startTime,
                initialEndTime = draft?.endTime,
                showHeading = false,
                onSave = { event ->
                    saves.launch {
                        scheduleRepository.upsertOneOffEvent(event)
                        haptics.confirm()
                        onSaved()
                    }
                },
            )

            CreateItemType.TASK -> AddTaskForm(
                courseId = courseId.orEmpty(),
                initialTitle = draft?.title.orEmpty(),
                initialDescription = draft?.details.orEmpty(),
                initialDueDate = draft?.dueDate,
                initialRequiredForExam = draft?.requiredForExam ?: false,
                showHeading = false,
                onSave = { task ->
                    saves.launch {
                        studyContentRepository.upsertTask(task)
                        haptics.confirm()
                        onSaved()
                    }
                },
            )

            CreateItemType.TEST,
            CreateItemType.EXAM -> AddGradeItemForm(
                courseId = courseId.orEmpty(),
                academicSemester = academicSemester,
                scheduleRepository = scheduleRepository,
                settingsRepository = settingsRepository,
                initialType = if (type == CreateItemType.TEST) GradeItemType.TEST else GradeItemType.EXAM,
                initialTitle = draft?.title.orEmpty(),
                initialDueDate = draft?.dueDate,
                initialRequiredForExam = draft?.requiredForExam ?: false,
                showHeading = false,
                onSave = { item ->
                    saves.launch {
                        gradeRepository.upsertItem(item)
                        haptics.confirm()
                        onSaved()
                    }
                },
            )

            CreateItemType.NOTE -> AddNoteForm(
                courseId = courseId.orEmpty(),
                courseCode = courses.firstOrNull { it.id == courseId }?.code ?: courseId.orEmpty(),
                documentStore = documentStore,
                initialTitle = draft?.title.orEmpty(),
                initialContent = draft?.details.orEmpty(),
                showHeading = false,
                onSave = { note ->
                    saves.launch {
                        studyContentRepository.upsertNote(note)
                        haptics.confirm()
                        onSaved()
                    }
                },
            )
        }
    }
}
