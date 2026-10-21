package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.components.rememberSaveAction
import com.kpyruy.takt.core.ui.components.SaveErrorDialog
import com.kpyruy.takt.core.ui.components.tourTarget
import com.kpyruy.takt.core.ui.i18n.t

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.kpyruy.takt.core.ui.components.CourseAvatar
import com.kpyruy.takt.core.ui.components.taktPressFeedback
import com.kpyruy.takt.core.data.ExamRepository
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.ScheduleRepository
import com.kpyruy.takt.core.data.AppSettingsRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.data.TaktDocumentStore
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.ExamEligibilityCalculator
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.TaktUnderlineTabs
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.ui.motion.TaktMotion
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SubjectDetailScreen(
    repository: StudyPlanRepository,
    gradeRepository: GradeRepository,
    scheduleRepository: ScheduleRepository,
    settingsRepository: AppSettingsRepository,
    studyContentRepository: StudyContentRepository,
    examRepository: ExamRepository,
    documentStore: TaktDocumentStore,
    uisManaged: Boolean = false,
    courseId: String,
    onBack: () -> Unit,
    initialTab: String = "overview",
    guidedTab: String? = null,
    teachingRepository: com.kpyruy.takt.core.data.CourseTeachingRepository? = null,
    courseDocumentsRepository: com.kpyruy.takt.core.data.CourseDocumentsRepository? = null,
    authenticateTeaching: (String, (com.kpyruy.takt.core.model.DeviceAuthenticationResult) -> Unit) -> Unit = { _, reply -> reply(com.kpyruy.takt.core.model.DeviceAuthenticationResult.ERROR) },
    onOpenUisSettings: () -> Unit = {},
    onOpenDocumentSettings: () -> Unit = onOpenUisSettings,
) {
    val documentFolderState by documentStore.status.collectAsStateWithLifecycle()
    val documentAccountGeneration by (courseDocumentsRepository?.accountGeneration ?: remember { kotlinx.coroutines.flow.MutableStateFlow(0L) }).collectAsStateWithLifecycle()
    val documentsContext = androidx.compose.ui.platform.LocalContext.current
    val downloads = remember(documentStore, documentAccountGeneration) { com.kpyruy.takt.core.data.CourseDocumentDownloads(documentsContext, documentStore, documentAccountGeneration) }
    val course by remember(repository, courseId) { repository.observeCourse(courseId) }.collectAsStateWithLifecycle(initialValue = null)
    val allCourses by remember(repository) { repository.observeCourses() }.collectAsStateWithLifecycle(initialValue = emptyList())
    val gradeItems by remember(gradeRepository, courseId) { gradeRepository.observeItems(courseId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val gradeScale by remember(gradeRepository, courseId) { gradeRepository.observeScale(courseId) }.collectAsStateWithLifecycle(initialValue = GradeScale.default())
    val manualGrade by remember(gradeRepository, courseId) { gradeRepository.observeManualGrade(courseId) }.collectAsStateWithLifecycle(initialValue = null)
    val tasks by remember(studyContentRepository, courseId) { studyContentRepository.observeTasks(courseId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by remember(studyContentRepository, courseId) { studyContentRepository.observeNotes(courseId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val examInfo by remember(examRepository, courseId) { examRepository.observeExamInfo(courseId) }.collectAsStateWithLifecycle(initialValue = null)
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val lifecycleOwner = LocalLifecycleOwner.current
    val clock by androidx.compose.runtime.produceState(java.time.LocalDateTime.now(), lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = java.time.LocalDateTime.now(); delay(60_000L) }
        }
    }
    val today = clock.toLocalDate()
    val nextLesson = rememberNextCourseLesson(scheduleRepository, settings, allCourses, courseId, clock)
    val eligibility = remember(tasks, gradeItems) {
        ExamEligibilityCalculator.calculate(tasks, gradeItems)
    }

    val saves = rememberSaveAction()
    SaveErrorDialog(saves)
    val haptics = rememberTaktHaptics()
    var showIconPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(if (initialTab == "grades") "tasks" else initialTab) }
    if (guidedTab != null) LaunchedEffect(guidedTab) { selectedTab = guidedTab }
    // The navigation entry owns editor state; canonical UIS adoption can change courseId mid-draft.
    var showAddGrade by rememberSaveable { mutableStateOf(false) }
    var editingGradeId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingGrade = gradeItems.firstOrNull { it.id == editingGradeId }
    var showScaleEditor by remember { mutableStateOf(false) }
    var showAddTask by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingTask = tasks.firstOrNull { it.id == editingTaskId }
    var showAddNote by rememberSaveable { mutableStateOf(false) }
    var editingNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingNote = notes.firstOrNull { it.id == editingNoteId }

    BackHandler(enabled = selectedTab == "exam") { selectedTab = "overview" }
    val item = course
    if (item == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ScreenHeader(
                title = t("Subject"),
                navigation = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = t("Back"))
                    }
                },
            )
            Text(t("Subject not found"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val assessmentPhase = settings.assessmentPhase(item, today, settings.effectiveCurrentSemester(allCourses))
    val examPeriod = settings.semesterPeriods[
        settings.academicSemester(item, settings.effectiveCurrentSemester(allCourses))
    ]
    val showFinalGrade = examPeriod?.containsExamDate(today) == true
    val tabs = if (item.gradingType != CourseGradingType.PASS_FAIL) {
        listOf("overview", "tasks", "exam", "notes")
    } else {
        listOf("overview", "tasks", "notes")
    }

    LaunchedEffect(tabs) {
        if (selectedTab !in tabs) selectedTab = if (selectedTab == "grades") "tasks" else "overview"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().testTag("subject-header"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TaktIconButton(Icons.Default.ArrowBack, t("Back"), onClick = {
                    if (selectedTab == "exam") selectedTab = "overview" else onBack()
                }, modifier = Modifier.testTag("subject-header-back"))
                val avatarInteraction = remember { MutableInteractionSource() }
                Box(modifier = Modifier.size(56.dp).taktPressFeedback(avatarInteraction)
                    .clip(RoundedCornerShape(13.dp))
                    .clickable(interactionSource = avatarInteraction, indication = null, role = Role.Button,
                        onClick = { showIconPicker = true })
                    .testTag("subject-header-avatar").tourTarget("subject-icon").semantics {
                        contentDescription = t("Change subject icon")
                    }) { CourseAvatar(item, Modifier.fillMaxSize()) }
                Text(item.title, Modifier.weight(1f).testTag("subject-header-title"),
                    fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                TaktIconButton(Icons.Default.MoreHoriz, t("Subject settings"),
                    onClick = { showSettings = true }, modifier = Modifier.testTag("subject-header-menu"))
            }
            // 48dp back control + 10dp gap: metadata starts at the avatar, below all controls.
            Text(item.code + " · " + item.credits + t(" credits · ") + t("Semester ${item.semester}"),
                Modifier.fillMaxWidth().padding(start = 58.dp).testTag("subject-header-metadata"),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        TaktSegmentedTabs(labels = tabs.map(::subjectTabLabel), selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
            onSelected = { selectedTab = tabs[it] }, modifier = Modifier.tourTarget("subject-tabs"))

        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                fadeIn(animationSpec = TaktMotion.content()) togetherWith
                    fadeOut(animationSpec = TaktMotion.content())
            },
            label = "course-tab",
        ) { tab ->
            when (tab) {
                "overview" -> CourseOverviewTab(
                    course = item,
                    phase = assessmentPhase,
                    gradeItems = gradeItems,
                    gradeScale = gradeScale,
                    tasks = tasks,
                    eligibility = eligibility,
                    showFinalGrade = showFinalGrade,
                    manualGrade = manualGrade,
                    onManualGradeChange = { grade ->
                        saves.queue { gradeRepository.setManualGrade(courseId, grade) }
                    },
                    onTaskCompleted = { task, completed -> saves.queue { studyContentRepository.setTaskCompleted(task.id, completed) } },
                    onEditTask = { editingTaskId = it.id },
                    onEditGrade = { editingGradeId = it.id },
                    onExam = { selectedTab = "exam" },
                    nextLesson = nextLesson,
                    onSettings = { showSettings = true },
                    documentsContent = {
                        androidx.compose.runtime.key(documentAccountGeneration) { CourseDocumentsPanel(item, allCourses.filter { it.status == CourseStatus.ENROLLED }.map { it.code }.toSet(),
                            courseDocumentsRepository, downloads, uisManaged, authenticateTeaching, onOpenUisSettings, onOpenDocumentSettings,
                            documentFolderState != com.kpyruy.takt.core.data.DocumentSyncStatus.DISCONNECTED && documentStore.isConnected) }
                    },
                    teachersContent = {
                        CourseTeachersPanel(item, teachingRepository, settings.language, uisManaged,
                            authenticateTeaching, onOpenUisSettings)
                    },
                )
                "tasks" -> CourseTasksTab(
                    today = today,
                    tasks = tasks,
                    gradeItems = gradeItems,
                    eligibility = eligibility,
                    onCompletedChange = { task, completed ->
                        saves.queue { studyContentRepository.setTaskCompleted(task.id, completed) }
                    },
                    onEdit = { editingTaskId = it.id },
                    onDelete = { task ->
                        saves.queue { studyContentRepository.deleteTask(task.id) }
                    },
                    onAddTask = { showAddTask = true },
                    onEditGrade = { editingGradeId = it.id },
                    onDeleteGrade = { grade -> saves.queue { gradeRepository.deleteItem(grade.id) } },
                    onAddGrade = { showAddGrade = true },
                )
                "exam" -> CourseExamTab(
                    courseId = courseId,
                    gradeItems = gradeItems,
                    gradeScale = gradeScale,
                    tasks = tasks,
                    eligibility = eligibility,
                    examInfo = examInfo,
                    onAdmission = { selectedTab = "tasks" },
                    onSaveExamInfo = { info ->
                        saves.queue {
                            examRepository.upsertExamInfo(info)
                            haptics.confirm()
                        }
                    },
                )
                "notes" -> CourseNotesTab(
                    notes = notes,
                    documentStore = documentStore,
                    onEdit = { editingNoteId = it.id },
                    onDelete = { note ->
                        saves.queue { studyContentRepository.deleteNote(note.id) }
                    },
                    onAddNote = { showAddNote = true },
                )
            }
        }
    }

    if (showIconPicker) {
        com.kpyruy.takt.core.ui.components.CourseIconPicker(item.iconKey, onSelect = { key ->
            saves.queue { repository.setIcon(item.id, key) }
            showIconPicker = false
        }, onDismiss = { showIconPicker = false })
    }
    if (showSettings) {
        CourseSettingsSheet(
            course = item,
            uisManaged = uisManaged,
            onDismiss = { showSettings = false },
            onIcon = { showSettings = false; showIconPicker = true },
            onStatus = { status -> if (!uisManaged) saves.queue { repository.updateStatus(item.id, status) } },
            onGrading = { type -> saves.queue { repository.setGradingType(item.id, type) } },
            onResult = { result -> saves.queue { repository.setPassFailResult(item.id, result) } },
            onScale = { showSettings = false; showScaleEditor = true },
        )
    }

    if (showAddGrade || editingGrade != null) {
        AddGradeItemSheet(
            courseId = courseId,
            academicSemester = settings.academicSemester(item, settings.effectiveCurrentSemester(allCourses)),
            scheduleRepository = scheduleRepository,
            settingsRepository = settingsRepository,
            initialItem = editingGrade,
            onDismiss = {
                showAddGrade = false
                editingGradeId = null
            },
            onSave = { gradeItem ->
                saves.launch {
                    gradeRepository.upsertItem(gradeItem)
                    haptics.confirm()
                    showAddGrade = false
                    editingGradeId = null
                }
            },
        )
    }

    if (showScaleEditor) {
        EditGradeScaleSheet(
            initialScale = gradeScale,
            onDismiss = { showScaleEditor = false },
            onSave = { scale ->
                saves.launch {
                    gradeRepository.setScale(courseId, scale)
                    haptics.confirm()
                    showScaleEditor = false
                }
            },
        )
    }

    if (showAddTask || editingTask != null) {
        AddTaskSheet(
            courseId = courseId,
            initialTask = editingTask,
            onDismiss = {
                showAddTask = false
                editingTaskId = null
            },
            onSave = { task ->
                saves.launch {
                    studyContentRepository.upsertTask(task)
                    haptics.confirm()
                    showAddTask = false
                    editingTaskId = null
                }
            },
        )
    }

    if (showAddNote || editingNote != null) {
        AddNoteSheet(
            courseId = courseId,
            courseCode = item.code,
            documentStore = documentStore,
            initialNote = editingNote,
            onDismiss = {
                showAddNote = false
                editingNoteId = null
            },
            onSave = { note ->
                saves.launch {
                    studyContentRepository.upsertNote(note)
                    haptics.confirm()
                    showAddNote = false
                    editingNoteId = null
                }
            },
        )
    }
}

private fun subjectTabLabel(tab: String): String = when (tab) {
    "tasks" -> t("Tasks")
    "exam" -> t("Exam")
    "notes" -> t("Notes")
    else -> t("Overview")
}
