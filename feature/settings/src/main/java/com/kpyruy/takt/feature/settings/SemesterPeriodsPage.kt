package com.kpyruy.takt.feature.settings

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton as Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.kpyruy.takt.core.ui.components.TaktSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import com.kpyruy.takt.core.ui.components.TaktIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.AppSettings
import com.kpyruy.takt.core.model.AssessmentPhase
import com.kpyruy.takt.core.model.AssessmentPhaseMode
import com.kpyruy.takt.core.model.SemesterPeriod
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.TaktDatePickerField
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@Composable
internal fun SemesterPeriodsPage(
    semesters: List<Int>,
    currentSemester: Int?,
    settings: AppSettings,
    onBack: () -> Unit,
    onSave: suspend (Map<Int, SemesterPeriod>) -> Unit,
    onReviewDocuments: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    val available = semesters.ifEmpty { listOf(1) }
    var selectedSemester by remember(available, currentSemester) {
        mutableIntStateOf(currentSemester?.takeIf { it in available } ?: available.first())
    }
    val semesterListState = rememberLazyListState()
    LaunchedEffect(currentSemester, available) {
        val index = available.indexOf(currentSemester)
        if (index >= 0) semesterListState.scrollToItem(index)
    }
    var drafts by remember(settings.semesterPeriods) { mutableStateOf(settings.semesterPeriods) }
    val draft = drafts[selectedSemester] ?: SemesterPeriod()
    val changed = drafts != settings.semesterPeriods
    val valid = drafts.values.all(SemesterPeriod::hasValidDates)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    fun updateDraft(next: SemesterPeriod) {
        drafts = if (next.isEmpty) drafts - selectedSemester else drafts + (selectedSemester to next)
    }
    val now = LocalDate.now()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        saving = true
                        scope.launch {
                            val result = runCatching { onSave(drafts) }
                            saving = false
                            snackbarHostState.showSnackbar(
                                if (result.isSuccess) t(UiKey.TEXT_PERIODS_SAVED) else t(UiKey.TEXT_PERIODS_COULD_NOT_BE_SAVED),
                            )
                        }
                    },
                    enabled = changed && valid && !saving,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(54.dp),
                ) { Text(t(UiKey.TEXT_SAVE_PERIODS)) }
            }
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenHeader(title = t(UiKey.TEXT_STUDY_PERIODS),
                navigation = { TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t(UiKey.ACTION_BACK), onBack) })
            if (onReviewDocuments != null) {
                TextButton(onClick = onReviewDocuments) {
                    Text(t(UiKey.TEXT_REVIEW_DIFFERENCES_WITH_DOCUMENTS_TAKT))
                }
            }
            LazyRow(Modifier.fillMaxWidth(), state = semesterListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(available) { semester ->
                    val selected = semester == selectedSemester
                    Surface(
                        modifier = Modifier.clickable { selectedSemester = semester },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE, semester)) + if (semester == currentSemester) t(UiKey.TEXT_NOW) else "",
                            Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            key(selectedSemester) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t(UiKey.TEXT_ACADEMIC_PERIOD), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (draft.studyStart != null || draft.studyEnd != null) {
                            TextButton(onClick = { updateDraft(draft.copy(studyStart = null, studyEnd = null)) }) { Text(t(UiKey.TEXT_CLEAR)) }
                        }
                    }
                    TaktDatePickerField(t(UiKey.TEXT_START_OF_CLASSES), draft.studyStart,
                        { updateDraft(draft.copy(studyStart = it)) }, Modifier.fillMaxWidth())
                    TaktDatePickerField(t(UiKey.TEXT_END_OF_CLASSES), draft.studyEnd,
                        { updateDraft(draft.copy(studyEnd = it)) }, Modifier.fillMaxWidth())
                }
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t(UiKey.TEXT_EXAMINATION_PERIOD), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (draft.examStart != null || draft.examEnd != null) {
                            TextButton(onClick = { updateDraft(draft.copy(examStart = null, examEnd = null)) }) { Text(t(UiKey.TEXT_CLEAR)) }
                        }
                    }
                    TaktDatePickerField(t(UiKey.TEXT_BEGINNING_OF_EXAMS), draft.examStart,
                        { updateDraft(draft.copy(examStart = it)) }, Modifier.fillMaxWidth())
                    TaktDatePickerField(t(UiKey.TEXT_END_OF_EXAMS), draft.examEnd,
                        { updateDraft(draft.copy(examEnd = it)) }, Modifier.fillMaxWidth())
                }
            }
            val studyEnd = draft.studyEnd
            val examStart = draft.examStart
            if (studyEnd != null && examStart != null && examStart > studyEnd.plusDays(1)) {
                val days = ChronoUnit.DAYS.between(studyEnd, examStart) - 1
                Text(t(uiMessage(UiKey.FORMAT_BREAK_BETWEEN_PERIODS_VALUE_DAYS, days)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!valid) {
                Text(t(UiKey.TEXT_FOR_EACH_PERIOD_PUT_BOTH_DATES_IN_THE_CORRECT_ORDER),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            SettingsSectionTitle(t(UiKey.TEXT_ASSESSMENT_MODE))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    AssessmentPhaseMode.AUTO to t(UiKey.TEXT_AUTO),
                    AssessmentPhaseMode.STUDY to t(UiKey.TEXT_STUDY),
                    AssessmentPhaseMode.EXAM to t(UiKey.TEXT_EXAMINATIONS),
                ).forEach { (mode, label) ->
                    val selected = draft.assessmentMode == mode
                    Surface(
                        modifier = Modifier.weight(1f).clickable { updateDraft(draft.copy(assessmentMode = mode)) },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(label, Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 13.dp),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Text(t(UiKey.TEXT_NOW_71A21BC7) + if (draft.assessmentPhase(now) == AssessmentPhase.EXAM) t(UiKey.TEXT_EXAMS_D6BD4331) else t(UiKey.TEXT_STUDY_0C87ED81),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium)
        }
    }
}
