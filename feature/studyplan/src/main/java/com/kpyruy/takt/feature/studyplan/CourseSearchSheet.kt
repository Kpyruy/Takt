package com.kpyruy.takt.feature.studyplan

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.ui.components.CourseAvatar
import com.kpyruy.takt.core.ui.components.GlassSurface
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import com.kpyruy.takt.core.ui.components.TaktIconButton
import com.kpyruy.takt.core.ui.components.TaktTextField
import com.kpyruy.takt.core.ui.i18n.t

@Composable
internal fun CourseSearchSheet(courses: List<Course>, onCourseClick: (String) -> Unit, onDismiss: () -> Unit) {
    // A fresh sheet owns its query; dismissing or selecting cannot filter the underlying plan.
    var query by remember { mutableStateOf("") }
    val results = remember(courses, query) { searchCourses(courses, query) }
    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 560.dp).imePadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp).testTag("progress-search-sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t(UiKey.TEXT_FIND_A_SUBJECT), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TaktIconButton(Icons.Default.Close, t(UiKey.ACTION_CLOSE), onClick = onDismiss,
                    modifier = Modifier.testTag("progress-search-close"))
            }
            TaktTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().testTag("progress-search-query"),
                label = { Text(t(UiKey.TEXT_FIND_A_SUBJECT)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) TaktIconButton(Icons.Default.Close, t(UiKey.TEXT_CLEAR),
                        onClick = { query = "" }, modifier = Modifier.testTag("progress-search-clear"))
                },
                singleLine = true,
            )
            LazyColumn(Modifier.weight(1f, fill = false).testTag("progress-search-results"),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.id }) { course ->
                    GlassSurface(
                        Modifier.fillMaxWidth().testTag("progress-search-result-${course.id}")
                            .clickable { onCourseClick(course.id) },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CourseAvatar(course, Modifier.size(36.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(course.title, style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(course.code + " · " + t(uiMessage(UiKey.FORMAT_SEMESTER_VALUE, course.semester)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (results.isEmpty()) item {
                    Text(t(UiKey.TEXT_NO_SUBJECTS_FOUND), Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
