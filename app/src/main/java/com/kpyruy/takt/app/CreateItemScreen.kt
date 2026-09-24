package com.kpyruy.takt.app

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
import com.kpyruy.takt.feature.subjects.AddTaskForm
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun CreateItemScreen(
    type: CreateItemType,
    courseId: String?,
    draft: CreateItemDraft?,
    courses: List<com.kpyruy.takt.core.model.Course> = emptyList(),
    scheduleRepository: ScheduleRepository,
    settingsRepository: AppSettingsRepository,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberTaktHaptics()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = type.label,
            subtitle = "Повна форма",
            navigation = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
        )

        if (type.requiresCourse && courseId == null) {
            SectionCard {
                Text("Спочатку обери предмет.", style = MaterialTheme.typography.titleMedium)
            }
            return@Column
        }

        when (type) {
            CreateItemType.CLASS -> AddLessonForm(
                courses = courses,
                initialCourseId = courseId ?: draft?.courseId,
                initialLessonType = draft?.lessonType ?: com.kpyruy.takt.core.model.LessonType.UNSPECIFIED,
                initialDay = LocalDate.now().dayOfWeek,
                initialTitle = draft?.title.orEmpty(),
                initialStartTime = draft?.startTime,
                initialEndTime = draft?.endTime,
                showHeading = false,
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
