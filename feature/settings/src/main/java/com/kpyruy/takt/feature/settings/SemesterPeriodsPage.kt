package com.kpyruy.takt.feature.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                                if (result.isSuccess) "Періоди збережено" else "Не вдалося зберегти періоди",
                            )
                        }
                    },
                    enabled = changed && valid && !saving,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(54.dp),
                ) { Text("Зберегти періоди") }
            }
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenHeader(title = "Періоди навчання",
                navigation = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") } })
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
                        Text("$semester семестр" + if (semester == currentSemester) " · зараз" else "",
                            Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            key(selectedSemester) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Навчальний період", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (draft.studyStart != null || draft.studyEnd != null) {
                            TextButton(onClick = { updateDraft(draft.copy(studyStart = null, studyEnd = null)) }) { Text("Очистити") }
                        }
                    }
                    TaktDatePickerField("Початок навчання", draft.studyStart,
                        { updateDraft(draft.copy(studyStart = it)) }, Modifier.fillMaxWidth())
                    TaktDatePickerField("Кінець навчання", draft.studyEnd,
                        { updateDraft(draft.copy(studyEnd = it)) }, Modifier.fillMaxWidth())
                }
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Екзаменаційний період", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (draft.examStart != null || draft.examEnd != null) {
                            TextButton(onClick = { updateDraft(draft.copy(examStart = null, examEnd = null)) }) { Text("Очистити") }
                        }
                    }
                    TaktDatePickerField("Початок екзаменів", draft.examStart,
                        { updateDraft(draft.copy(examStart = it)) }, Modifier.fillMaxWidth())
                    TaktDatePickerField("Кінець екзаменів", draft.examEnd,
                        { updateDraft(draft.copy(examEnd = it)) }, Modifier.fillMaxWidth())
                }
            }
            val studyEnd = draft.studyEnd
            val examStart = draft.examStart
            if (studyEnd != null && examStart != null && examStart > studyEnd.plusDays(1)) {
                val days = ChronoUnit.DAYS.between(studyEnd, examStart) - 1
                Text("Перерва між періодами · $days дн.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!valid) {
                Text("Для кожного періоду вкажи обидві дати в правильному порядку.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            SettingsSectionTitle("Режим оцінювання")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    AssessmentPhaseMode.AUTO to "Авто",
                    AssessmentPhaseMode.STUDY to "Навчання",
                    AssessmentPhaseMode.EXAM to "Екзамени",
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
            Text("Зараз: " + if (draft.assessmentPhase(now) == AssessmentPhase.EXAM) "екзамени" else "навчання",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium)
        }
    }
}
