package com.kpyruy.takt.feature.studyplan

import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.contentDescription
import com.kpyruy.takt.core.ui.motion.TaktMotion
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.model.*
import com.kpyruy.takt.core.ui.components.*
import com.kpyruy.takt.core.ui.i18n.TaktI18n
import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.ui.i18n.tf
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency

/** UIS-backed study records; choosing an archived record never changes the live planning repositories. */
@Composable
fun StudiesScreen(
    repository: StudyPlanRepository,
    gradeRepository: GradeRepository,
    settingsRepository: AppSettingsRepository,
    universityAccountRepository: UniversityAccountRepository,
    studentHubRepository: StudentHubRepository,
    planning: PlanningSnapshot?,
    onCourseClick: (String) -> Unit,
    onAddCourse: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPaymentRecord: (String) -> Unit,
    onDetailChanged: (Boolean) -> Unit = {},
) {
    val state by studentHubRepository.state.collectAsStateWithLifecycle()
    val courses = planning?.courses ?: remember(repository) { repository.observeCourses() }
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    val settings = planning?.settings ?: settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings()).value
    val hasAccount by universityAccountRepository.hasAccount.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editingCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var iconCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectingSemester by rememberSaveable { mutableStateOf(false) }
    StudiesContent(
        state = state, localCourses = courses, settings = settings, gradeRepository = gradeRepository, uisManaged = hasAccount,
        onSelectStudy = { scope.launch { studentHubRepository.selectStudy(it) } },
        onDisplayNameChanged = { scope.launch { studentHubRepository.setDisplayName(it) } },
        onRefresh = { scope.launch { studentHubRepository.refresh() } },
        onCourseClick = onCourseClick, onCourseStatus = { editingCourseId = it.id },
        onChooseSemester = { selectingSemester = true }, onAddCourse = onAddCourse,
        onOpenSettings = onOpenSettings, onOpenPaymentRecord = onOpenPaymentRecord, onDetailChanged = onDetailChanged,
    )
    courses.firstOrNull { it.id == editingCourseId }?.let { course ->
        CourseStatusSheet(course, uisManaged = hasAccount, onStatus = { status ->
            if (!hasAccount) scope.launch { repository.updateStatus(course.id, status) }
            editingCourseId = null
        }, onIcon = { iconCourseId = course.id; editingCourseId = null }, onDismiss = { editingCourseId = null })
    }
    courses.firstOrNull { it.id == iconCourseId }?.let { course ->
        CourseIconPicker(course.iconKey, onSelect = { key ->
            scope.launch { repository.setIcon(course.id, key) }; iconCourseId = null
        }, onDismiss = { iconCourseId = null })
    }
    if (selectingSemester && !hasAccount) CurrentSemesterSheet(
        semesters = courses.groupBy { it.semester }.mapValues { (_, value) -> value.count { it.status == CourseStatus.ENROLLED } },
        currentSemester = settings.effectiveCurrentSemester(courses), manualSemester = settings.currentSemester,
        onSelect = { selected -> scope.launch { settingsRepository.setCurrentSemester(selected); selectingSemester = false } },
        onDismiss = { selectingSemester = false },
    )
}

/** Stateless data boundary makes unavailable, cached and previous-study states directly reviewable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudiesContent(
    state: StudentHubState,
    localCourses: List<Course> = emptyList(),
    settings: AppSettings = AppSettings(),
    gradeRepository: GradeRepository? = null,
    uisManaged: Boolean = false,
    onSelectStudy: (String) -> Unit = {},
    onDisplayNameChanged: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onCourseClick: (String) -> Unit = {},
    onCourseStatus: (Course) -> Unit = {},
    onChooseSemester: () -> Unit = {},
    onAddCourse: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenPaymentRecord: (String) -> Unit = {},
    onDetailChanged: (Boolean) -> Unit = {},
    initialTab: Int = 0,
    initialProgressFilter: Int = 0,
) {
    val snapshot = remember(state.snapshot, settings.language, settings.ukrainianCourseNameFallback) {
        state.snapshot?.let { source -> source.copy(studies = source.studies.map { it.copy(title = settings.displayStudyTitle(it)) }) }
    }
    val study = snapshot?.viewedStudy()
    val archived = study != null && study.id != snapshot.currentStudyId
    // Only the selected current study can use live course IDs, grades, status or semester controls.
    val courses = remember(archived, study?.courses, localCourses, settings.language, settings.courseNameLanguage, settings.ukrainianCourseNameFallback) {
        (if (archived) study!!.courses else localCourses.ifEmpty { study?.courses.orEmpty() })
            .map { it.copy(title = settings.displayCourseTitle(it)) }
    }
    val currentSemester = when {
        archived -> study?.currentSemester
        uisManaged -> settings.uisCurrentSemester ?: study?.currentSemester
        else -> settings.effectiveCurrentSemester(courses) ?: study?.currentSemester
    }
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    var filter by rememberSaveable(initialProgressFilter) { mutableIntStateOf(initialProgressFilter) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var selector by rememberSaveable { mutableStateOf(false) }
    var editName by rememberSaveable { mutableStateOf(false) }
    var nameDraft by rememberSaveable(snapshot?.displayName, snapshot?.ownerKey) { mutableStateOf(snapshot?.displayName?.takeIf { it.isNotBlank() } ?: snapshot?.identity?.name.orEmpty()) }
    var search by rememberSaveable { mutableStateOf(false) }
    var filterMenu by rememberSaveable { mutableStateOf(false) }
    var expandedSemesters by rememberSaveable(study?.id, currentSemester) { mutableStateOf(currentSemester?.let(::setOf).orEmpty()) }
    val detailChanged by rememberUpdatedState(onDetailChanged)
    LaunchedEffect(detail != null) { detailChanged(detail != null) }
    DisposableEffect(Unit) { onDispose { detailChanged(false) } }
    BackHandler(enabled = detail != null) { detail = null }
    val isic = study?.fees?.firstOrNull { it.title.contains("ISIC", ignoreCase = true) }
    val selectedFee = if (detail?.startsWith("fee:") == true) study?.fees?.firstOrNull { it.id == detail!!.removePrefix("fee:") } else null
    val archivedCourse = if (detail?.startsWith("course:") == true) courses.firstOrNull { it.id == detail!!.removePrefix("course:") } else null
    val openCourse: (String) -> Unit = { id -> if (archived || localCourses.none { it.id == id }) detail = "course:$id" else onCourseClick(id) }
    val listState = rememberLazyListState()
    LaunchedEffect(detail, tab, study?.id) { listState.scrollToItem(0) }
    LazyColumn(
        Modifier.fillMaxSize().testTag("studies-screen").windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = if (detail == null) 112.dp else 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            val detailTitle = when {
                detail == "profile" -> t(UiKey.STUDIES_PROFILE)
                detail == "isic" || selectedFee?.title?.contains("ISIC", true) == true -> t(UiKey.STUDIES_ISIC_FEE)
                selectedFee != null -> t(UiKey.STUDIES_FEE_DETAILS)
                archivedCourse != null -> t(UiKey.STUDIES_ARCHIVED_COURSE)
                else -> t(UiKey.STUDIES_TITLE)
            }
            ScreenHeader(title = detailTitle,
                navigation = if (detail != null) ({ TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t(UiKey.ACTION_BACK), { detail = null }, Modifier.testTag("studies-back")) }) else null,
                action = if (detail == null || detail == "profile") ({
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (detail == null) TaktIconButton(Icons.Outlined.School, t(UiKey.STUDIES_SELECT_STUDY),
                            { selector = true }, Modifier.testTag("studies-study-selector"))
                        TaktIconButton(Icons.Outlined.Settings, t(UiKey.TEXT_SETTINGS), onOpenSettings, Modifier.testTag("studies-settings"))
                    }
                }) else null)
        }
        when {
            detail == "profile" -> profileItems(snapshot, study, onEditName = { editName = true },
                onSelectStudy = { id -> onSelectStudy(id); detail = null })
            detail == "isic" || detail?.startsWith("fee:") == true -> feeItems(selectedFee ?: isic, study, snapshot?.identity, onOpenPaymentRecord)
            archivedCourse != null -> item {
                StudyPane { ReadOnlyCourse(archivedCourse, onClick = null); RecordField(t(UiKey.STUDIES_STUDY), study?.title); Text(t(UiKey.STUDIES_ARCHIVE_NOTE), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            else -> {
                item(key = "tabs-and-credits") {
                    StudiesTabsAndCredits(tab, { tab = it }) {
                        CreditCard(study, courses, currentSemester, onChooseSemester.takeUnless { archived || uisManaged })
                    }
                }
                if (archived) item { StudyPane { Text(t(UiKey.STUDIES_ARCHIVE_NOTE), style = MaterialTheme.typography.bodySmall); snapshot?.currentStudyId?.let { id -> TextButton(onClick = { onSelectStudy(id) }, modifier = Modifier.testTag("studies-return-current")) { Text(t(UiKey.STUDIES_RETURN_CURRENT)) } } } }
                if (state.problem in setOf(StudentHubProblem.SIGN_IN_REQUIRED, StudentHubProblem.SECOND_FACTOR_REQUIRED, StudentHubProblem.SAVE_FAILED) && !state.loading)
                    item { StudyStatus(state, onRefresh, onOpenSettings) }
                when (tab) {
                    0 -> {
                        item { SectionHeading(t(UiKey.STUDIES_FINANCES), action = { TextButton(onClick = { tab = 2 }) { Text(t(UiKey.STUDIES_VIEW_ALL)) } }) }
                        item(key = "finance-tiles") { FinanceSummary(study) { tab = 2 } }
                        item(key = "profile-isic") { StudyPane {
                            NavigationRow(Icons.Outlined.School, t(UiKey.STUDIES_PROFILE), t(UiKey.STUDIES_PROFILE_SUBTITLE), "studies-profile") { detail = "profile" }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            NavigationRow(Icons.Outlined.CreditCard, t(UiKey.STUDIES_ISIC_FEE), t(UiKey.STUDIES_ISIC_SUBTITLE), "studies-isic") { detail = "isic" }
                        } }
                    }
                    1 -> {
                        item {
                            val filterLabels = listOf(t(UiKey.TEXT_ALL), t(UiKey.TEXT_ACTIVE), t(UiKey.TEXT_COMPLETED))
                            Row(Modifier.fillMaxWidth().testTag("studies-progress-tools"),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { search = true }, enabled = courses.isNotEmpty(),
                                    modifier = Modifier.testTag("progress-search-open"), contentPadding = PaddingValues(vertical = 12.dp)) {
                                    Icon(Icons.Outlined.Search, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp)); Text(t(UiKey.TEXT_FIND_A_SUBJECT))
                                }
                                Box(Modifier.tourTarget("progress-tabs")) {
                                    TaktIconButton(if (filter == 0) Icons.Outlined.FilterList else Icons.Outlined.FilterAlt,
                                        t(UiKey.TEXT_COURSE_FILTERS), { filterMenu = true },
                                        Modifier.testTag("studies-progress-filter-open").semantics { stateDescription = filterLabels[filter] })
                                    TaktChoiceMenu(filterMenu, { filterMenu = false }) {
                                        filterLabels.forEachIndexed { index, label ->
                                            TaktChoiceMenuItem(text = { Text(label) }, onClick = { filter = index; filterMenu = false },
                                                modifier = Modifier.testTag("studies-progress-filter-$index").semantics { selected = filter == index },
                                                trailingIcon = if (filter == index) ({ Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }) else null)
                                        }
                                    }
                                }
                            }
                        }
                        if (courses.isEmpty()) item {
                            StudyPane { Text(t(UiKey.STUDIES_NO_COURSES), color = MaterialTheme.colorScheme.onSurfaceVariant); if (study == null) TextButton(onClick = onAddCourse) { Text(t(UiKey.TEXT_ADD_COURSE)) } }
                        }
                        if (filter == 0) {
                            val semesters = courses.groupBy { it.semester }.toSortedMap()
                            semesters.forEach { (semester, semesterCourses) ->
                                item(key = "semester-$semester") {
                                    if (!archived && gradeRepository != null) SemesterSection(semester, semesterCourses, gradeRepository, semester == currentSemester, semester in expandedSemesters,
                                        { expanded -> expandedSemesters = if (expanded) expandedSemesters + semester else expandedSemesters - semester }, openCourse, onCourseStatus, compactPreview = true)
                                    else ReadOnlySemester(semester, semesterCourses, semester == currentSemester, semester in expandedSemesters,
                                        { expandedSemesters = if (semester in expandedSemesters) expandedSemesters - semester else expandedSemesters + semester }, openCourse)
                                }
                            }

                        } else {
                            val filtered = courses.filter { it.status == if (filter == 1) CourseStatus.ENROLLED else CourseStatus.FULFILLED }
                            items(filtered, key = { "course-${it.id}" }) { course -> StudyPane {
                                if (!archived && gradeRepository != null) ProgressCourseRow(course, gradeRepository, { openCourse(course.id) }, { onCourseStatus(course) }, compactPresentation = true)
                                else ReadOnlyCourse(course) { openCourse(course.id) }
                            } }
                            if (filtered.isEmpty() && courses.isNotEmpty()) item { Text(t(if (filter == 1) UiKey.TEXT_THERE_ARE_NO_ACTIVE_SUBJECTS_YET_CHOOSE_THEM_IN_THE_PLAN else UiKey.TEXT_NO_COMPLETED_SUBJECTS_YET), style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                    2 -> financeItems(study) { detail = "fee:${it.id}" }
                }
            }
        }
        study?.updatedAtEpochMillis?.let { updated -> item(key = "updated") {
            Text(tf(UiKey.STUDIES_UPDATED, dateText(Instant.ofEpochMilli(updated).atZone(ZoneId.systemDefault()).toLocalDate())),
                Modifier.fillMaxWidth().padding(top = 2.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
    if (selector) TaktFullSheet(onDismissRequest = { selector = false }) {
        LazyColumn(Modifier.fillMaxWidth().testTag("studies-selector-body").padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t(UiKey.STUDIES_SELECT_STUDY), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                TaktIconButton(Icons.Outlined.Close, t(UiKey.ACTION_CLOSE), { selector = false })
            } }
            val currentStudies = snapshot?.studies.orEmpty().filter { it.id == snapshot?.currentStudyId }
            if (currentStudies.isNotEmpty()) item { Muted(t(UiKey.STUDIES_CURRENT)) }
            items(currentStudies, key = { it.id }) { item ->
                StudyChoiceRow(item, item.id == study?.id, current = true) { onSelectStudy(item.id) }
            }
            val otherStudies = snapshot?.studies.orEmpty().filter { it.id != snapshot?.currentStudyId }
            if (otherStudies.isNotEmpty()) item { Muted(t(UiKey.STUDIES_HISTORY)) }
            items(otherStudies, key = { it.id }) { item ->
                StudyChoiceRow(item, item.id == study?.id) { onSelectStudy(item.id) }
            }
            if (snapshot?.studies.isNullOrEmpty()) item { Text(unavailable(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { FilledTonalButton(onClick = { selector = false }, modifier = Modifier.fillMaxWidth().testTag("studies-selector-done")) { Text(t(UiKey.TEXT_DONE)) } }
        }
    }
    if (editName) {
        AlertDialog(onDismissRequest = { editName = false }, title = { Text(t(UiKey.STUDIES_EDIT_PROFILE)) },
            text = { OutlinedTextField(nameDraft, { nameDraft = it.take(100) }, label = { Text(t(UiKey.STUDIES_DISPLAY_NAME)) }, singleLine = true, modifier = Modifier.testTag("studies-display-name")) },
            confirmButton = { TextButton(onClick = { onDisplayNameChanged(nameDraft.trim()); editName = false }, modifier = Modifier.testTag("studies-save-name")) { Text(t(UiKey.TEXT_APPLY)) } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(t(UiKey.ACTION_CANCEL)) } })
    }
    if (search) CourseSearchSheet(courses, { id -> search = false; openCourse(id) }, { search = false })
}

/** The collapsing slot starts at the rail's actual bottom edge. Its gap is part of the
 * same measured motion, so removing the card never removes an extra lazy-list gap. */
@Composable
private fun StudiesTabsAndCredits(tab: Int, onTab: (Int) -> Unit, credits: @Composable () -> Unit) {
    val reveal by androidx.compose.animation.core.animateFloatAsState(
        if (tab == 0) 1f else 0f, TaktMotion.content(), label = "studies-credit-reveal")
    Column(Modifier.fillMaxWidth()) {
        TaktSegmentedTabs(listOf(t(UiKey.STUDIES_OVERVIEW), t(UiKey.STUDIES_PROGRESS), t(UiKey.STUDIES_FINANCES)),
            tab, onTab, modifier = Modifier.testTag("studies-tabs"), tabTag = { "studies-tab-$it" })
        androidx.compose.ui.layout.Layout(
            content = { if (reveal > 0f) credits() },
            modifier = Modifier.fillMaxWidth().clipToBounds().testTag("studies-credit-slot"),
        ) { measurables, constraints ->
            val card = measurables.firstOrNull()?.measure(constraints.copy(minHeight = 0))
            val fullHeight = (card?.height ?: 0) + 14.dp.roundToPx()
            val visibleHeight = (fullHeight * reveal).toInt().coerceAtLeast(0)
            layout(constraints.maxWidth, visibleHeight) {
                card?.placeRelative(0, visibleHeight - card.height)
            }
        }
    }
}

@Composable
private fun StudyChoiceRow(study: StudentStudy, selected: Boolean, current: Boolean = false, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .12f) else Color.Transparent)
        .testTag("studies-select-${study.id}")
        .selectable(selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onSelect)
        .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RadioButton(selected, onClick = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(study.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            study.university?.takeIf { it.isNotBlank() }?.let { Muted(it) }
            val metadata = listOfNotNull(study.degree, study.periodLabel,
                study.currentSemester?.takeIf { current && it > 0 }?.let { tf(UiKey.STUDIES_YEAR, (it + 1) / 2) }).joinToString(" · ")
            if (metadata.isNotBlank()) Muted(metadata)
        }
    }
}

@Composable
private fun StudyPane(modifier: Modifier = Modifier, verticalPadding: Dp = 17.dp, spacing: Dp = 10.dp, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = verticalPadding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}

@Composable
private fun CreditCard(study: StudentStudy?, courses: List<Course>, semester: Int?, onChooseSemester: (() -> Unit)?) {
    val earned = study?.earnedCredits ?: courses.takeIf { it.isNotEmpty() }?.filter { it.status == CourseStatus.FULFILLED }?.sumOf { it.credits }
    // Planned local credits are not the official required-credit total.
    val required = study?.requiredCredits
    StudyPane(Modifier.testTag("studies-credit-card").semantics { contentDescription = t(UiKey.STUDIES_STUDY_PROGRESS) }, verticalPadding = 12.dp, spacing = 8.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(earned?.toString() ?: "—", fontSize = 35.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
            Text(tf(UiKey.STUDIES_CREDITS, "", required?.toString() ?: "—").trim(), Modifier.padding(start = 5.dp, bottom = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { if (earned != null && required != null && required > 0) (earned.toFloat() / required).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(5.dp), gapSize = 0.dp, drawStopIndicator = {}, trackColor = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().heightIn(min = 32.dp).testTag("studies-current-semester").then(if (onChooseSemester != null) Modifier.clickable(onClick = onChooseSemester) else Modifier), verticalAlignment = Alignment.CenterVertically) {
            Muted(semester?.let { tf(UiKey.FORMAT_SEMESTER_VALUE, it) + t(if (it % 2 == 1) UiKey.TEXT_WINTER_11577647 else UiKey.TEXT_SUMMER_16945261) } ?: unavailable(), Modifier.weight(1f))
            if (onChooseSemester != null) Icon(Icons.Outlined.ExpandMore, t(UiKey.TEXT_CHOOSE_A_SEMESTER), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FinanceSummary(study: StudentStudy?, onClick: () -> Unit) {
    val outstanding = study?.takeIf { it.feesLoaded }?.outstandingFees()
    val payment = study?.fees?.filter { it.status == StudentFeeStatus.UNPAID || it.status == StudentFeeStatus.PARTIAL }
        ?.minByOrNull { it.dueDate ?: LocalDate.MAX }
    val scholarship = study?.takeIf { it.scholarshipsLoaded }?.scholarships?.maxByOrNull { it.paidOn ?: LocalDate.MIN }
    val payments: @Composable (Modifier) -> Unit = { modifier ->
        FinanceTile(t(UiKey.STUDIES_PAYMENTS_DUE), outstanding.money(), outstanding == null, Icons.Outlined.AccountBalanceWallet,
            payment?.let { it.title + (it.dueDate?.let { date -> " · ${dateText(date)}" } ?: "") },
            modifier, "studies-payments-value", onClick)
    }
    val scholarships: @Composable (Modifier) -> Unit = { modifier ->
        FinanceTile(t(UiKey.STUDIES_LAST_SCHOLARSHIP), scholarship?.let { money(it.amountMinor, it.currency) } ?: money(0L, "EUR", currencyCode = true),
            false, Icons.Outlined.AccountBalance, scholarship?.paidOn?.let { "${t(UiKey.STUDIES_PAID)} · ${dateText(it)}" },
            modifier, "studies-scholarship-value", onClick)
    }
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 320.dp * fontScale) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            payments(Modifier.fillMaxWidth()); scholarships(Modifier.fillMaxWidth())
        } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            payments(Modifier.weight(1f).fillMaxHeight()); scholarships(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun FinanceTile(label: String, value: String, unavailable: Boolean, icon: ImageVector, note: String?, modifier: Modifier, valueTag: String, onClick: () -> Unit) {
    StudyPane(modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, Modifier.size(18.dp).testTag("$valueTag-icon"), tint = MaterialTheme.colorScheme.primary)
            val style = MaterialTheme.typography.labelSmall
            BasicText(label, Modifier.weight(1f).testTag("$valueTag-label"),
                style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1, softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = style.fontSize * 0.75f, maxFontSize = style.fontSize))
        }
        Text(value, Modifier.fillMaxWidth().testTag(valueTag),
            style = if (unavailable) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.headlineSmall,
            fontWeight = if (unavailable) FontWeight.Medium else FontWeight.SemiBold)
        if (note != null) Muted(note)
    }
}

private fun LazyListScope.financeItems(study: StudentStudy?, onFeeClick: (StudentFee) -> Unit) {
    item(key = "outstanding") { StudyPane(Modifier.testTag("studies-outstanding")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Muted(t(UiKey.STUDIES_OUTSTANDING)); Text(study?.takeIf { it.feesLoaded }?.outstandingFees().money(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                if (study?.feesLoaded == true && study.outstandingFees() == null) Muted(t(UiKey.STUDIES_UNKNOWN_BALANCE))
            }
            StudyIcon(Icons.Outlined.AccountBalanceWallet)
        }
    } }
    item { SectionHeading(t(UiKey.STUDIES_PAYMENTS)) }
    if (study?.feesLoaded != true) item { StudyPane { Muted(unavailable()) } }
    else if (study.fees.isEmpty()) item { StudyPane { Muted(t(UiKey.STUDIES_NO_FEES)) } }
    else items(study.fees, key = { "fee-${it.id}" }) { fee ->
        StudyPane(Modifier.testTag("studies-fee-${fee.id}").clickable { onFeeClick(fee) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudyIcon(Icons.Outlined.CreditCard)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(fee.title, fontWeight = FontWeight.SemiBold); Muted(fee.dueDate?.let { "${t(UiKey.STUDIES_DUE)} · ${dateText(it)}" } ?: unavailable()) }
                Column(horizontalAlignment = Alignment.End) { Text(money(fee.amountMinor, fee.currency), fontWeight = FontWeight.SemiBold); Muted(fee.status.label()) }
            }
        }
    }
    item { SectionHeading(t(UiKey.STUDIES_SCHOLARSHIPS)) }
    if (study?.scholarshipsLoaded != true) item { StudyPane { Muted(unavailable()) } }
    else if (study.scholarships.isEmpty()) item { StudyPane { Muted(t(UiKey.STUDIES_NO_SCHOLARSHIPS)) } }
    else {
        item { StudyPane { RecordField(t(UiKey.STUDIES_TOTAL_PAID), study.scholarships.groupBy { it.currency }.mapValues { (_, payments) -> payments.sumOf { it.amountMinor } }.money()) } }
        items(study.scholarships.sortedByDescending { it.paidOn }, key = { "scholarship-${it.id}" }) { payment -> StudyPane {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudyIcon(Icons.Outlined.Check)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(payment.title, fontWeight = FontWeight.SemiBold); Muted(listOfNotNull(payment.paidOn?.let(::dateText), payment.periodLabel).joinToString(" · ").ifBlank { unavailable() }) }
                Column(horizontalAlignment = Alignment.End) { Text(money(payment.amountMinor, payment.currency), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary); Muted(t(UiKey.STUDIES_PAID)) }
            }
        } }
    }
}

private fun LazyListScope.profileItems(snapshot: StudentHubSnapshot?, study: StudentStudy?, onEditName: () -> Unit, onSelectStudy: (String) -> Unit) {
    item { StudyPane(Modifier.testTag("studies-profile-hero")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            EntityIconTile(MaterialTheme.colorScheme.primary, Modifier.size(62.dp)) { color ->
                val name = snapshot?.displayName?.takeIf { it.isNotBlank() } ?: snapshot?.identity?.name
                Text(name?.split(Regex("\\s+")).orEmpty().filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = color)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(snapshot?.displayName?.takeIf { it.isNotBlank() } ?: snapshot?.identity?.name ?: unavailable(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Muted(snapshot?.identity?.university ?: study?.university ?: unavailable())
                Text(t(UiKey.STUDIES_STUDENT), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Muted(t(UiKey.STUDIES_DISPLAY_PROFILE)); TextButton(onClick = onEditName, modifier = Modifier.testTag("studies-edit-profile")) { Text(t(UiKey.STUDIES_EDIT_PROFILE)) }
        }
    } }
    item { SectionHeading(t(UiKey.STUDIES_UNIVERSITY_RECORD)) }
    item { StudyPane {
        RecordField(t(UiKey.STUDIES_NAME_IN_UIS), snapshot?.identity?.name)
        RecordField(t(UiKey.STUDIES_UNIVERSITY), snapshot?.identity?.university ?: study?.university)
        RecordField(t(UiKey.STUDIES_DEGREE), study?.degree)
        RecordField(t(UiKey.STUDIES_PROGRAMME), study?.title)
        RecordField(t(UiKey.STUDIES_YEARS), study?.periodLabel)
        RecordField(t(UiKey.STUDIES_STATUS), study?.let { t(if (it.active) UiKey.STUDIES_ACTIVE_STATUS else UiKey.TEXT_COMPLETED) + (it.currentSemester?.takeIf { value -> value > 0 && it.active }?.let { value -> " · " + tf(UiKey.STUDIES_YEAR, (value + 1) / 2) } ?: "") })
    } }
    item { SectionHeading(t(UiKey.STUDIES_HISTORY)) }
    item { StudyPane {
        snapshot?.studies?.firstOrNull { it.id == snapshot.currentStudyId }?.let { item -> NavigationRow(Icons.Outlined.School, item.title, listOfNotNull(item.periodLabel, t(UiKey.STUDIES_CURRENT)).joinToString(" · "), "studies-history-${item.id}") { onSelectStudy(item.id) } }
        if (snapshot != null) snapshot.studies.filter { it.id != snapshot.currentStudyId }.forEach { item ->
            NavigationRow(Icons.Outlined.History, item.title, listOfNotNull(item.periodLabel, t(UiKey.STUDIES_PREVIOUS)).joinToString(" · "), "studies-history-${item.id}") { onSelectStudy(item.id) }
        }
    } }
    item { Muted(t(UiKey.STUDIES_PROFILE_NOTE)) }
}

private fun LazyListScope.feeItems(fee: StudentFee?, study: StudentStudy?, identity: StudentIdentity?, onOpenRecord: (String) -> Unit) {
    if (fee == null) {
        item { StudyPane { StudyIcon(Icons.Outlined.CreditCard); Text(t(UiKey.STUDIES_NO_ISIC), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        return
    }
    item { StudyPane(Modifier.testTag("studies-fee-detail")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { StudyIcon(Icons.Outlined.CreditCard); Column { Text(fee.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); study?.periodLabel?.let { Muted(it) } } }
        Text(money(fee.amountMinor, fee.currency), fontSize = 43.sp, lineHeight = 55.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.5).sp)
        Surface(shape = RoundedCornerShape(9.dp), color = if (fee.status == StudentFeeStatus.PAID) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer) { Text(fee.status.label(), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RecordField(t(UiKey.STUDIES_DUE), fee.dueDate?.let(::dateText))
    } }
    item { SectionHeading(t(UiKey.STUDIES_FEE_DETAILS)) }
    item { StudyPane {
        RecordField(t(UiKey.STUDIES_FEE_TYPE), fee.title)
        RecordField(t(UiKey.STUDIES_ISSUED), fee.issuedDate?.let(::dateText))
        RecordField(t(UiKey.STUDIES_AMOUNT_DUE), fee.outstandingMinor()?.let { money(it, fee.currency) })
        RecordField(t(UiKey.STUDIES_PAID), fee.paidMinor?.let { money(it, fee.currency) })
        RecordField(t(UiKey.STUDIES_STUDENT), identity?.name)
        RecordField(t(UiKey.STUDIES_STUDY), study?.title)
    } }
    val recordUrl = fee.recordUrl?.takeIf { url -> runCatching { val uri = java.net.URI(url); uri.scheme == "https" && !uri.host.isNullOrBlank() }.getOrDefault(false) }
    if (recordUrl != null) item {
        FilledTonalButton(onClick = { onOpenRecord(recordUrl) }, modifier = Modifier.fillMaxWidth().testTag("studies-open-payment-record"), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Text(t(UiKey.STUDIES_PAYMENT_RECORD), Modifier.weight(1f)); Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp))
        }
    }
    item { StudyPane { Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { Icon(Icons.Outlined.Info, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Muted(t(UiKey.STUDIES_FEE_NOTE), Modifier.weight(1f)) } } }
}

@Composable
private fun ReadOnlySemester(semester: Int, courses: List<Course>, current: Boolean, expanded: Boolean, onToggle: () -> Unit, onOpenCourse: (String) -> Unit) {
    StudyPane {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("semester-toggle-$semester").clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EntityIconTile(MaterialTheme.colorScheme.primary, Modifier.size(39.dp)) { tint -> if (courses.all { it.status == CourseStatus.FULFILLED }) Icon(Icons.Outlined.Check, null, tint = tint) else Text(semester.toString(), color = tint) }
            Column(Modifier.weight(1f)) { Text(tf(UiKey.FORMAT_SEMESTER_VALUE, semester), fontWeight = FontWeight.SemiBold); if (current) Muted(t(UiKey.TEXT_CURRENT_SEMESTER_A9B3EA6D)) }
            Muted(tf(UiKey.FORMAT_VALUE_VALUE_CREDITS, courses.filter { it.status == CourseStatus.FULFILLED }.sumOf { it.credits }, courses.filter { it.status != CourseStatus.NOT_NEEDED }.sumOf { it.credits }))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
        }
        if (expanded) courses.forEach { course -> HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); ReadOnlyCourse(course) { onOpenCourse(course.id) } }
    }
}

@Composable
private fun ReadOnlyCourse(course: Course, onClick: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("studies-readonly-course-${course.id}").then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CourseAvatar(course, Modifier.size(36.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(course.title, style = MaterialTheme.typography.titleSmall); Muted(tf(UiKey.FORMAT_VALUE_CREDITS_6F9D64BE, course.credits) + " · " + course.status.title()) }
        Box(Modifier.width(88.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)).testTag("studies-result-${course.id}"), contentAlignment = Alignment.Center) {
            if (course.officialGrade != null) CourseGradeBadge(course.officialGrade!!, course.fulfilledOn, Modifier.fillMaxWidth())
            else Icon(course.status.icon(), null, Modifier.size(20.dp), tint = course.status.tint())
        }
    }
}

@Composable
private fun NavigationRow(icon: ImageVector, title: String, subtitle: String, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag(tag).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StudyIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); Muted(subtitle) }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun StudyIcon(icon: ImageVector) { EntityIconTile(MaterialTheme.colorScheme.primary, Modifier.size(39.dp)) { tint -> Icon(icon, null, Modifier.size(20.dp), tint = tint) } }
@Composable private fun Muted(text: String, modifier: Modifier = Modifier) { Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable private fun SectionHeading(title: String, action: (@Composable () -> Unit)? = null) { Row(Modifier.fillMaxWidth().heightIn(min = 30.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); action?.invoke() } }
@Composable private fun RecordField(label: String, value: String?) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) { Muted(label, Modifier.weight(1f)); Text(value?.takeIf { it.isNotBlank() } ?: unavailable(), Modifier.weight(1.15f), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.End) } }
@Composable private fun StudyStatus(state: StudentHubState, onRetry: () -> Unit, onSettings: () -> Unit) {
    StudyPane(Modifier.testTag("studies-status")) {
        val key = when (state.problem) { StudentHubProblem.SIGN_IN_REQUIRED -> UiKey.STUDIES_SIGN_IN; StudentHubProblem.SECOND_FACTOR_REQUIRED -> UiKey.STUDIES_SECOND_FACTOR; StudentHubProblem.SAVE_FAILED -> UiKey.STUDIES_SAVE_ERROR; null -> UiKey.STUDIES_LOADING; else -> UiKey.STUDIES_LOAD_ERROR }
        Text(t(key), style = MaterialTheme.typography.bodySmall)
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        else if (state.problem == StudentHubProblem.SIGN_IN_REQUIRED || state.problem == StudentHubProblem.SECOND_FACTOR_REQUIRED) TextButton(onClick = onSettings) { Text(t(UiKey.TEXT_SETTINGS)) }
        else TextButton(onClick = onRetry) { Text(t(UiKey.TEXT_RETRY)) }
    }
}
private fun unavailable() = t(UiKey.STUDIES_UNAVAILABLE)
private fun dateText(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(TaktI18n.locale))
private fun money(minor: Long, currency: String, currencyCode: Boolean = false): String = runCatching {
    val unit = Currency.getInstance(currency)
    val digits = unit.defaultFractionDigits.coerceAtLeast(0)
    if (currencyCode) NumberFormat.getNumberInstance(TaktI18n.locale).apply { minimumFractionDigits = digits; maximumFractionDigits = digits }
        .format(java.math.BigDecimal.valueOf(minor, digits)) + " " + currency
    else NumberFormat.getCurrencyInstance(TaktI18n.locale).apply { this.currency = unit; minimumFractionDigits = digits; maximumFractionDigits = digits }.format(java.math.BigDecimal.valueOf(minor, digits))
}.getOrElse { java.math.BigDecimal.valueOf(minor, 2).toPlainString() + " " + currency }
private fun Map<String, Long>?.money(): String = when { this == null -> unavailable(); isEmpty() -> t(UiKey.STUDIES_NO_OUTSTANDING); else -> entries.joinToString(" · ") { (currency, amount) -> money(amount, currency) } }
private fun StudentFee.outstandingMinor(): Long? = when { status == StudentFeeStatus.PAID || status == StudentFeeStatus.CANCELLED -> 0L; paidMinor != null -> (amountMinor - paidMinor!!).coerceAtLeast(0L); status == StudentFeeStatus.UNPAID -> amountMinor; else -> null }
private fun StudentFeeStatus.label(): String = t(when (this) { StudentFeeStatus.PAID -> UiKey.STUDIES_PAID; StudentFeeStatus.UNPAID -> UiKey.STUDIES_UNPAID; StudentFeeStatus.PARTIAL -> UiKey.STUDIES_PARTIAL; StudentFeeStatus.CANCELLED -> UiKey.STUDIES_CANCELLED; StudentFeeStatus.UNKNOWN -> UiKey.STUDIES_UNAVAILABLE })
