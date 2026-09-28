package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
    val scope = rememberCoroutineScope()
    val haptics = rememberTaktHaptics()
    val uisAccount = universityAccountRepository?.hasAccount?.collectAsState()?.value == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = type.label,
            subtitle = t("Повна форма"),
            navigation = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = t("Назад"))
                }
            },
        )

        if (type.requiresCourse && courseId == null) {
            SectionCard {
                Text(t("Спочатку обери предмет."), style = MaterialTheme.typography.titleMedium)
            }
            return@Column
        }

        when (type) {
            CreateItemType.COURSE -> if (uisAccount) {
                Text(t("Предмети додаються з UIS автоматично."))
            } else AddCourseForm(studyPlanRepository, onSaved)
            CreateItemType.CLASS -> AddLessonForm(
                courses = courses,
                initialCourseId = courseId ?: draft?.courseId,
                initialLessonType = draft?.lessonType ?: com.kpyruy.takt.core.model.LessonType.UNSPECIFIED,
                initialDay = LocalDate.now().dayOfWeek,
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
                                authenticate(t("Отримати пари з UIS")) { value ->
                                    if (continuation.isActive) continuation.resume(value)
                                }
                            }
                            if (result != DeviceAuthenticationResult.SUCCESS) throw error
                            account.loadLessonOptions(course.code)
                        }
                    }
                },
                onSaveOneOff = { event ->
                    scope.launch {
                        scheduleRepository.upsertOneOffEvent(event)
                        haptics.confirm()
                        onSaved()
                    }
                },
                onSave = { rule ->
                    scope.launch {
                        scheduleRepository.upsertRule(rule)
                        haptics.confirm()
                        onSaved()
                    }
                },
            )

            CreateItemType.EVENT,
            CreateItemType.REMINDER -> OneOffEventForm(
                courses = courses,
                initialDate = LocalDate.now(),
                initialType = if (type == CreateItemType.REMINDER) {
                    OneOffScheduleEventType.REMINDER
                } else {
                    OneOffScheduleEventType.EXTRA
                },
                initialTitle = draft?.title.orEmpty(),
                initialStartTime = draft?.startTime,
                initialEndTime = draft?.endTime,
                showHeading = false,
                onSave = { event ->
                    scope.launch {
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
                    scope.launch {
                        studyContentRepository.upsertTask(task)
                        haptics.confirm()
                        onSaved()
                    }
                },
            )

            CreateItemType.TEST,
            CreateItemType.EXAM -> AddGradeItemForm(
                courseId = courseId.orEmpty(),
                scheduleRepository = scheduleRepository,
                settingsRepository = settingsRepository,
                initialType = if (type == CreateItemType.TEST) GradeItemType.TEST else GradeItemType.EXAM,
                initialTitle = draft?.title.orEmpty(),
                showHeading = false,
                onSave = { item ->
                    scope.launch {
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
                    scope.launch {
                        studyContentRepository.upsertNote(note)
                        haptics.confirm()
                        onSaved()
                    }
                },
            )
        }
    }
}
