package com.kpyruy.takt.feature.subjects

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.ExamRepository
import com.kpyruy.takt.core.data.GradeRepository
import com.kpyruy.takt.core.data.StudyContentRepository
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.model.PassFailResult
import com.kpyruy.takt.core.model.CourseGradingType
import com.kpyruy.takt.core.model.CourseNote
import com.kpyruy.takt.core.model.ExamEligibilityCalculator
import com.kpyruy.takt.core.model.GradeItem
import com.kpyruy.takt.core.model.GradeScale
import com.kpyruy.takt.core.model.StudyTask
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.TaktUnderlineTabs
import com.kpyruy.takt.core.ui.motion.TaktMotion
import com.kpyruy.takt.core.ui.motion.rememberTaktHaptics
import kotlinx.coroutines.launch

@Composable
fun SubjectDetailScreen(
    repository: StudyPlanRepository,
    gradeRepository: GradeRepository,
    studyContentRepository: StudyContentRepository,
    examRepository: ExamRepository,
    courseId: String,
    onBack: () -> Unit,
    initialTab: String = "Огляд",
) {
    val course by remember(repository, courseId) { repository.observeCourse(courseId) }.collectAsStateWithLifecycle(initialValue = null)
    val gradeItems by remember(gradeRepository, courseId) { gradeRepository.observeItems(courseId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val gradeScale by remember(gradeRepository, courseId) { gradeRepository.observeScale(courseId) }.collectAsStateWithLifecycle(initialValue = GradeScale.default())
    val manualGrade by remember(gradeRepository, courseId) { gradeRepository.observeManualGrade(courseId) }.collectAsStateWithLifecycle(initialValue = null)
    val tasks by remember(studyContentRepository, courseId) { studyContentRepository.observeTasks(courseId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by remember(studyContentRepository, courseId) { studyContentRepository.observeNotes(courseId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val examInfo by remember(examRepository, courseId) { examRepository.observeExamInfo(courseId) }.collectAsStateWithLifecycle(initialValue = null)
    val examMaterials by remember(examRepository, courseId) { examRepository.observeMaterials(courseId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val eligibility = remember(tasks, gradeItems) {
        ExamEligibilityCalculator.calculate(tasks, gradeItems)
    }

    val scope = rememberCoroutineScope()
    val haptics = rememberTaktHaptics()
    var showIconPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var showAddGrade by remember { mutableStateOf(false) }
    var editingGrade by remember { mutableStateOf<GradeItem?>(null) }
    var showScaleEditor by remember { mutableStateOf(false) }
    var showAddTask by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<StudyTask?>(null) }
    var showAddNote by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<CourseNote?>(null) }

    BackHandler(enabled = selectedTab == "Екзамен") { selectedTab = "Огляд" }
    val item = course
    if (item == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ScreenHeader(
                title = "Предмет",
                navigation = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
            Text("Предмет не знайдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val tabs = if (item.gradingType == CourseGradingType.EXAM_LETTER) {
        listOf("Огляд", "Бали", "Завдання", "Екзамен", "Нотатки")
    } else {
        listOf("Огляд", "Бали", "Завдання", "Нотатки")
    }

    LaunchedEffect(tabs) {
        if (selectedTab !in tabs) selectedTab = "Огляд"
    }

    Column(
        modifier = Modifier.fillMaxSize().background(subjectBackground(paper = selectedTab != "Екзамен")).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { if (selectedTab == "Екзамен") selectedTab = "Огляд" else onBack() }) { Icon(Icons.Default.ArrowBack, "Назад") }
            Text(if (selectedTab == "Екзамен") item.title else "Предмет", Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.MoreHoriz, "Налаштування предмета") }
        }
        if (selectedTab == "Екзамен") {
            ScreenHeader(title = "Екзамен")
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { ScreenHeader(title = item.title, subtitle = item.code + " · " + item.credits + " кредитів · " + item.semester + " семестр") }
                IconButton(onClick = { showIconPicker = true }, modifier = Modifier.semantics { contentDescription = "Змінити іконку предмета" }) {
                    SubjectMonogram(item)
                }
            }
            TaktUnderlineTabs(labels = tabs, selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0), onSelected = { selectedTab = tabs[it] })
        }

        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                fadeIn(animationSpec = TaktMotion.fast()) togetherWith
                    fadeOut(animationSpec = TaktMotion.fast())
            },
            label = "course-tab",
        ) { tab ->
            when (tab) {
                "Огляд" -> CourseOverviewTab(
                    course = item,
                    gradeItems = gradeItems,
                    gradeScale = gradeScale,
                    tasks = tasks,
                    eligibility = eligibility,
                    onTaskCompleted = { task, completed -> scope.launch { studyContentRepository.setTaskCompleted(task.id, completed) } },
                    onEditTask = { editingTask = it },
                    onEditGrade = { editingGrade = it },
                    onNotes = { selectedTab = "Нотатки" },
                    onExam = { selectedTab = "Екзамен" },
                )
                "Бали" -> CourseAssessmentsTab(
                    course = item,
                    gradeItems = gradeItems,
                    tasks = tasks,
                    gradeScale = gradeScale,
                    eligibility = eligibility,
                    manualGrade = manualGrade,
                    onAddGrade = { showAddGrade = true },
                    onEditGrade = { editingGrade = it },
                    onEditTask = { editingTask = it },
                    onDeleteGrade = { id -> scope.launch { gradeRepository.deleteItem(id) } },
                    onEditScale = { showScaleEditor = true },
                    onManualGradeChange = { grade ->
                        scope.launch { gradeRepository.setManualGrade(courseId, grade) }
                    },
                )
                "Завдання" -> CourseTasksTab(
                    tasks = tasks,
                    eligibility = eligibility,
                    onCompletedChange = { task, completed ->
                        scope.launch { studyContentRepository.setTaskCompleted(task.id, completed) }
                    },
                    onEdit = { editingTask = it },
                    onDelete = { task ->
                        scope.launch { studyContentRepository.deleteTask(task.id) }
                    },
                    onAddTask = { showAddTask = true },
                )
                "Екзамен" -> CourseExamTab(
                    courseId = courseId,
                    gradeItems = gradeItems,
                    gradeScale = gradeScale,
                    tasks = tasks,
                    eligibility = eligibility,
                    examInfo = examInfo,
                    materials = examMaterials,
                    manualGrade = manualGrade,
                    onAdmission = { selectedTab = if (tasks.any { it.requiredForExam && !it.meetsAdmissionRequirement }) "Завдання"
                        else if (gradeItems.any { it.requiredForExam && !it.meetsAdmissionRequirement }) "Бали" else "Завдання" },
                    onSaveExamInfo = { info ->
                        scope.launch {
                            examRepository.upsertExamInfo(info)
                            haptics.confirm()
                        }
                    },
                    onAddMaterial = { material ->
                        scope.launch {
                            examRepository.upsertMaterial(material)
                            haptics.confirm()
                        }
                    },
                    onDeleteMaterial = { material ->
                        scope.launch { examRepository.deleteMaterial(material.id) }
                    },
                    onManualGradeChange = { grade ->
                        scope.launch { gradeRepository.setManualGrade(courseId, grade) }
                    },
                )
                "Нотатки" -> CourseNotesTab(
                    notes = notes,
                    onEdit = { editingNote = it },
                    onDelete = { note ->
                        scope.launch { studyContentRepository.deleteNote(note.id) }
                    },
                    onAddNote = { showAddNote = true },
                )
            }
        }
    }

    if (showIconPicker) {
        com.kpyruy.takt.core.ui.components.CourseIconPicker(item.iconKey, onSelect = { key ->
            scope.launch { repository.setIcon(item.id, key) }
            showIconPicker = false
        }, onDismiss = { showIconPicker = false })
    }
    if (showSettings) {
        CourseSettingsSheet(
            course = item,
            onDismiss = { showSettings = false },
            onIcon = { showSettings = false; showIconPicker = true },
            onStatus = { status -> scope.launch { repository.updateStatus(item.id, status) } },
            onGrading = { type -> scope.launch { repository.setGradingType(item.id, type) } },
            onResult = { result -> scope.launch { repository.setPassFailResult(item.id, result) } },
            onScale = { showSettings = false; showScaleEditor = true },
        )
    }

    if (showAddGrade || editingGrade != null) {
        AddGradeItemSheet(
            courseId = courseId,
            initialItem = editingGrade,
            onDismiss = {
                showAddGrade = false
                editingGrade = null
            },
            onSave = { gradeItem ->
                scope.launch {
                    gradeRepository.upsertItem(gradeItem)
                    haptics.confirm()
                    showAddGrade = false
                    editingGrade = null
                }
            },
        )
    }

    if (showScaleEditor) {
        EditGradeScaleSheet(
            initialScale = gradeScale,
            onDismiss = { showScaleEditor = false },
            onSave = { scale ->
                scope.launch {
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
                editingTask = null
            },
            onSave = { task ->
                scope.launch {
                    studyContentRepository.upsertTask(task)
                    haptics.confirm()
                    showAddTask = false
                    editingTask = null
                }
            },
        )
    }

    if (showAddNote || editingNote != null) {
        AddNoteSheet(
            courseId = courseId,
            initialNote = editingNote,
            onDismiss = {
                showAddNote = false
                editingNote = null
            },
            onSave = { note ->
                scope.launch {
                    studyContentRepository.upsertNote(note)
                    haptics.confirm()
                    showAddNote = false
                    editingNote = null
                }
            },
        )
    }
}
