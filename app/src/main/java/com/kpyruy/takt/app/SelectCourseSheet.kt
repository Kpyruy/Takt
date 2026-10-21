package com.kpyruy.takt.app

import com.kpyruy.takt.core.model.uiMessage
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.ui.i18n.t

import com.kpyruy.takt.core.ui.components.CourseAvatar
import com.kpyruy.takt.core.ui.components.TaktFullSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.clickable
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.activeCourseChoices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCourseSheet(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onSelected: (Course) -> Unit,
) {
    val ordered = courses.activeCourseChoices().sortedWith(
        compareBy<Course> { it.semester }
            .thenBy { it.title }
    )

    TaktFullSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                t(UiKey.TEXT_CHOOSE_A_SUBJECT),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (ordered.isEmpty()) {
                Text(
                    t(UiKey.TEXT_THERE_ARE_NO_ACTIVE_SUBJECTS_MARK_THE_SUBJECT_AS_ACTIVE_IN_PROGRESS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ordered.forEach { course ->
                GlassSurface(Modifier.fillMaxWidth().clickable { onSelected(course) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CourseAvatar(course)
                        Column(Modifier.weight(1f)) {
                            Text(course.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                t(uiMessage(UiKey.FORMAT_VALUE_VALUE_CREDITS_SEMESTER_VALUE, course.code, course.credits, course.semester)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
