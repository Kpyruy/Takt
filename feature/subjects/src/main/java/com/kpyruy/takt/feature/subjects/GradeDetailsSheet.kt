package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.components.TaktPrimaryButton
import com.kpyruy.takt.core.ui.components.TaktSegmentedTabs
import com.kpyruy.takt.core.ui.components.TaktSurface
import com.kpyruy.takt.core.ui.i18n.t

internal enum class GradeDetailsTab(val label: String) {
    WHEN("When"), RESULT("Result"), RULES("Rules"),
}

internal enum class GradeDateMode(val label: String) {
    DATE("Date"), SCHEDULE("By timetable"),
}

internal enum class GradeResultState(val label: String) {
    PLANNED("Scheduled"), AWAITING("Awaiting grade"), KNOWN("Result known"),
}

/** View state belongs to the parent form; this sheet only edits its unsaved draft. */
@Composable
internal fun GradeDetailsSheet(
    selectedTab: GradeDetailsTab,
    onTabSelected: (GradeDetailsTab) -> Unit,
    onDismiss: () -> Unit,
    error: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Short categories wrap their content; longer text/IME states scroll only the body.
    val maximumHeight = (LocalConfiguration.current.screenHeightDp.dp * .8f).coerceAtMost(640.dp)
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = maximumHeight).imePadding()
            .padding(horizontal = 20.dp).testTag("grade-details")) {
            Text(t("Details"), style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp))
            TaktSegmentedTabs(
                labels = GradeDetailsTab.entries.map { t(it.label) },
                selectedIndex = selectedTab.ordinal,
                onSelected = { onTabSelected(GradeDetailsTab.entries[it]) },
                railCornerRadius = 12.dp,
                tabTag = { "grade-details-tab-${GradeDetailsTab.entries[it].name}" },
            )
            key(selectedTab) {
                Column(Modifier.fillMaxWidth().weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("grade-details-error"))
                    }
                    content()
                }
            }
            TaktPrimaryButton(onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("grade-details-done")) {
                Text(t("Done"))
            }
        }
    }
}

@Composable
internal fun GradeResultChoices(selected: GradeResultState, onSelected: (GradeResultState) -> Unit) {
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GradeResultState.entries.forEach { state ->
            TaktSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().selectable(selected == state, role = Role.RadioButton,
                    onClick = { onSelected(state) }).testTag("grade-result-${state.name}")
                    .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RadioButton(selected = selected == state, onClick = null)
                    Text(t(state.label), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
internal fun GradeRuleToggle(tag: String, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    TaktSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
            .testTag(tag).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(checked, onCheckedChange = null)
        }
    }
}
