package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import androidx.compose.ui.unit.dp

@Composable
fun SubjectCard(
    course: Course,
    progressLine: String,
    onClick: () -> Unit,
) {
    val subjectColor = taktSubjectColor(course.id)
    SectionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(42.dp)
                    .background(subjectColor, RoundedCornerShape(999.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(course.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(
                    course.code + " · " + course.credits + " кредитів",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(
                text = when (course.status) {
                    CourseStatus.FULFILLED -> "Закрито"
                    CourseStatus.ENROLLED -> "Активний"
                    CourseStatus.PLANNED -> "Заплановано"
                    CourseStatus.NOT_ENROLLED -> "Не записаний"
                    CourseStatus.NOT_NEEDED -> "Не потрібно"
                }
            )
        }
        Text(
            progressLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}
