package com.kpyruy.takt.feature.subjects

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.GradeLetter
import com.kpyruy.takt.core.ui.components.CourseGradeBadge
import com.kpyruy.takt.core.ui.components.GlassSurface
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun SubjectCard(course: Course, progressLine: String, onClick: () -> Unit, earned: Double = 0.0, maximum: Double = 0.0, scoreLabel: String = t("For the semester"), grade: GradeLetter? = course.officialGrade) {
    GlassSurface(Modifier.fillMaxWidth().testTag("subjects-course-${course.id}").clickable(onClick = onClick), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SubjectMonogram(course)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(course.title, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                SmallText(course.code + " · " + course.credits + t(" credits"))
                if (progressLine.isNotBlank()) SmallText(progressLine)
            }
            if (grade != null) CourseGradeBadge(grade, course.fulfilledOn.takeIf { course.officialGrade != null })
            else Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp))
        }
    }
}
