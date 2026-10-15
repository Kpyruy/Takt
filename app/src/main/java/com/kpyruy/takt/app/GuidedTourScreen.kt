package com.kpyruy.takt.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.AppReleaseRepository
import com.kpyruy.takt.core.data.TaktDataContainer
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.motion.TaktMotion
import com.kpyruy.takt.feature.calendar.CalendarScreen
import com.kpyruy.takt.feature.home.HomeScreen
import com.kpyruy.takt.feature.settings.DeviceAuthenticationRequest
import com.kpyruy.takt.feature.settings.SettingsScreen
import com.kpyruy.takt.feature.studyplan.StudyPlanScreen
import com.kpyruy.takt.feature.subjects.SubjectDetailScreen
import com.kpyruy.takt.feature.subjects.SubjectsScreen

@Composable
internal fun GuidedTourScreen(
    data: TaktDataContainer,
    settings: AppSettings,
    appReleaseRepository: AppReleaseRepository,
    authenticateDevice: DeviceAuthenticationRequest,
    onFinish: () -> Unit,
) {
    var stepId by rememberSaveable { mutableStateOf(GuidedTourStep.entries.first().name) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val step = GuidedTourStep.entries.firstOrNull { it.name == stepId } ?: GuidedTourStep.entries.first()
    val example = remember(settings.language) { GuidedTourData(settings) }
    val targets = remember { TaktTourTargets() }
    var origin by remember { mutableStateOf(Offset.Zero) }

    if (completed) {
        TourCelebration(onFinish)
        BackHandler(onBack = onFinish)
        return
    }

    CompositionLocalProvider(LocalTaktTourTargets provides targets,
        LocalCourseIconKeys provides example.courses.associate { it.id to it.iconKey },
        LocalCourseCodeKeys provides example.courses.associate { it.id to it.code }) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .onGloballyPositioned { origin = it.positionInWindow() }.testTag("guided-tour")) {
            TaktContentBackdropHost {
                // Background controls are demonstrative. All touch and accessibility input goes to the tour card.
                AnimatedContent(step.scene, Modifier.fillMaxSize().clearAndSetSemantics {},
                    transitionSpec = { (fadeIn(TaktMotion.content()) + slideInHorizontally(TaktMotion.menu()) { it / 8 })
                        .togetherWith(fadeOut(TaktMotion.content())) }, label = "tour-scene") { scene ->
                    TourScene(scene, step, example, data, appReleaseRepository, authenticateDevice)
                }
            }
            Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
            TourSpotlight(
                step = step,
                bounds = targets.bounds(step.target)?.translate(-origin),
                onBack = { if (step.ordinal > 0) stepId = GuidedTourStep.entries[step.ordinal - 1].name },
                onNext = {
                    if (step == GuidedTourStep.entries.last()) completed = true
                    else stepId = GuidedTourStep.entries[step.ordinal + 1].name
                },
                onExit = { confirmExit = true },
            )
        }
    }
    // Register after feature-screen handlers, so system Back always asks before leaving the tour.
    BackHandler { confirmExit = true }
    if (confirmExit) {
        AlertDialog(onDismissRequest = { confirmExit = false },
            title = { Text(t("Завершити екскурсію?")) },
            text = { Text(t("Можеш повернутися до неї будь-коли: Налаштування → Допомога → Екскурсія по Takt.")) },
            confirmButton = { TextButton(onClick = onFinish, modifier = Modifier.testTag("tour-exit-confirm")) { Text(t("Так, вийти")) } },
            dismissButton = { TextButton(onClick = { confirmExit = false }, modifier = Modifier.testTag("tour-exit-cancel")) { Text(t("Продовжити екскурсію")) } },
            modifier = Modifier.glassSurface(RoundedCornerShape(28.dp)), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
}

@Composable
private fun TourScene(
    scene: String,
    step: GuidedTourStep,
    example: GuidedTourData,
    data: TaktDataContainer,
    releases: AppReleaseRepository,
    authenticate: DeviceAuthenticationRequest,
) {
    val labels = listOf(t("Сьогодні"), t("Календар"), t("Предмети"), t("Прогрес"))
    val icons = listOf(TaktIcons.Home, TaktIcons.Calendar, TaktIcons.Book, TaktIcons.Progress)
    val selected = when (scene) { "home" -> 0; "calendar" -> 1; "progress" -> 3; else -> 2 }
    val root = scene in setOf("home", "calendar", "subjects", "progress")
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentWindowInsets = WindowInsets(0),
        bottomBar = { if (root) TaktBottomNavigation(labels.mapIndexed { index, label ->
            TaktNavItem(label, icons[index], index == selected) { }
        }) }, floatingActionButton = { if (root) TaktAddFab {} }) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())
            .captureTaktContentBackdrop(enabled = root)) {
            when (scene) {
                "home" -> HomeScreen(example.planning, example, example, {}, {}, true, {}, {}, {})
                "calendar" -> CalendarScreen(example.planning, example, example, {}, {}, {}, guidedViewMode = step.tab,
                    guidedWeekLayout = if (step == GuidedTourStep.CALENDAR_LAYOUT) com.kpyruy.takt.core.model.WeekLayout.COMPACT_LIST else null)
                "subjects" -> SubjectsScreen(repository = example, gradeRepository = example,
                    studyContentRepository = example, onCourseClick = {}, onAddCourse = {}, showAddButton = false,
                    settingsRepository = example, planning = example.snapshot)
                "subject" -> SubjectDetailScreen(repository = example, gradeRepository = example,
                    scheduleRepository = example, settingsRepository = example, studyContentRepository = example,
                    examRepository = example, documentStore = data.documentStore,
                    courseId = example.courses.first().id, onBack = {},
                    guidedTab = when (step.tab) { 1 -> "tasks"; 3 -> "notes"; else -> "overview" })
                "progress" -> StudyPlanScreen(example, example, example, data.universityAccountRepository,
                    {}, {}, example.snapshot, guidedFilter = step.tab)
                "add" -> Box(Modifier.fillMaxSize().padding(top = 18.dp).glassSurface(RoundedCornerShape(28.dp))) {
                    GlobalAddContent(true, {}, {}, {})
                }
                else -> SettingsScreen(settingsRepository = example, backupRepository = data.backupRepository,
                    documentStore = data.documentStore, studyPlanRepository = example,
                    universityAccountRepository = data.universityAccountRepository, authenticateDevice = authenticate,
                    appReleaseRepository = releases, onBack = {}, guidedPage = scene.removePrefix("settings-"))
            }
        }
    }
}
