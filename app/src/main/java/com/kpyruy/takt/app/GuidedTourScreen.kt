package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.UiKey
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
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    val scenes = remember { GuidedTourStep.entries.map { it.scene }.distinct().associateWith { TourSceneSession() } }
    var replay by rememberSaveable { mutableIntStateOf(0) }
    var visits by rememberSaveable { mutableIntStateOf(0) }
    var demoBusy by remember { mutableStateOf(step.hasDemonstration) }
    var demoPhase by remember { mutableStateOf("IDLE") }
    val session = scenes.getValue(step.scene)
    LaunchedEffect(step, replay, confirmExit) {
        if (confirmExit || !step.hasDemonstration) { demoBusy = false; demoPhase = "IDLE"; return@LaunchedEffect }
        demoBusy = true
        try {
            visits++
            runTourDemonstration(step, session, visits) { demoPhase = it }
        } finally {
            session.actions.performIfPresent("calendar-close-menu")
            demoBusy = false
        }
    }

    if (completed) {
        TourCelebration(onFinish)
        BackHandler(onBack = onFinish)
        return
    }

    CompositionLocalProvider(
        LocalCourseIconKeys provides example.courses.associate { it.id to it.iconKey },
        LocalCourseCodeKeys provides example.courses.associate { it.id to it.code }) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("guided-tour")
            .semantics { this[TaktTourSemantics.Playback] = "${step.name}:$demoPhase" }) {
            // A scene owns both its explanation and graphics recording throughout a transition.
            AnimatedContent(targetState = step, contentKey = { it.scene }, modifier = Modifier.fillMaxSize(),
                transitionSpec = { (fadeIn(TaktMotion.content()) + slideInHorizontally(TaktMotion.menu()) { it / 8 })
                    .togetherWith(fadeOut(TaktMotion.content())) }, label = "tour-scene") { shown ->
                val shownSession = scenes.getValue(shown.scene)
                var origin by remember { mutableStateOf(Offset.Zero) }
                CompositionLocalProvider(LocalTaktTourTargets provides shownSession.targets,
                    LocalTaktTourActions provides shownSession.actions) {
                    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionOnScreen() }
                        .testTag("tour-scene-${shown.scene}")) {
                        TaktContentBackdropHost {
                            Box(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                                TourScene(shown.scene, shown, example, data, appReleaseRepository, authenticateDevice)
                            }
                        }
                        Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
                        TourSpotlight(
                            step = shown,
                            bounds = shownSession.targets.bounds(shown.target)?.translate(-origin),
                            nextEnabled = !shown.hasDemonstration || (shown == step && !demoBusy),
                            onReplay = if (shown.hasDemonstration) ({ replay++ }) else null,
                            onBack = { if (shown == step && step.ordinal > 0) stepId = GuidedTourStep.entries[step.ordinal - 1].name },
                            onNext = {
                                if (shown == step) {
                                    if (step == GuidedTourStep.entries.last()) completed = true
                                    else stepId = GuidedTourStep.entries[step.ordinal + 1].name
                                }
                            },
                            onExit = { shownSession.actions.performIfPresent("calendar-close-menu"); confirmExit = true },
                        )
                    }
                }
            }
        }
    }
    // Register after feature-screen handlers, so system Back always asks before leaving the tour.
    BackHandler { confirmExit = true }
    if (confirmExit) {
        AlertDialog(onDismissRequest = { confirmExit = false },
            title = { Text(t(UiKey.TEXT_EXIT_THE_TOUR)) },
            text = { Text(t(UiKey.TEXT_YOU_CAN_RETURN_ANYTIME_SETTINGS_HELP_TOUR_OF_TAKT)) },
            confirmButton = { TextButton(onClick = onFinish, modifier = Modifier.testTag("tour-exit-confirm")) { Text(t(UiKey.TEXT_YES_EXIT)) } },
            dismissButton = { TextButton(onClick = { confirmExit = false }, modifier = Modifier.testTag("tour-exit-cancel")) { Text(t(UiKey.TEXT_CONTINUE_TOUR)) } },
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
    val labels = listOf(t(UiKey.TEXT_TODAY), t(UiKey.TEXT_CALENDAR), t(UiKey.TEXT_SUBJECTS), t(UiKey.TEXT_PROGRESS))
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
                "calendar" -> CalendarScreen(example.planning, example, example, {}, {}, {}, guidedViewMode = step.tab)
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
