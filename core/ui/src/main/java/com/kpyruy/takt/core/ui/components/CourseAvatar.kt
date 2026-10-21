package com.kpyruy.takt.core.ui.components

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.model.LessonType
import com.kpyruy.takt.core.ui.theme.taktSubjectColor
import com.kpyruy.takt.core.ui.theme.readableAccentColor
import com.kpyruy.takt.core.ui.theme.LocalTaktCardAppearance
import com.kpyruy.takt.core.model.CardAppearance

val LocalCourseCodeKeys = staticCompositionLocalOf<Map<String, String>> { emptyMap() }

val LocalCourseIconKeys = staticCompositionLocalOf<Map<String, String?>> { emptyMap() }

@Composable
fun CourseAvatar(course: Course, modifier: Modifier = Modifier) {
    val color = taktSubjectColor(course.id)
    val icon = CourseIcons.find(course.iconKey)
    EntityIconTile(color, modifier) { foreground ->
        if (icon != null) Icon(icon.vector, t(icon.label), Modifier.size(26.dp), tint = foreground)
        else Text(course.code.substringBefore("_").take(2), color = foreground, fontSize = 14.sp, lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
    }
}

/** Unlinked events use the same restrained tile and readable subject-colour glyph as courses. */
@Composable
fun EventAvatar(type: LessonType, subjectKey: String, modifier: Modifier = Modifier) {
    val color = taktSubjectColor(subjectKey)
    EntityIconTile(color, modifier) { foreground ->
        LessonTypeIcon(type.takeUnless { it == LessonType.UNSPECIFIED } ?: LessonType.EVENT,
            Modifier.size(26.dp), tint = foreground)
    }
}

/** Shared rounded-square, opaque icon tile for courses, events and academic work. */
@Composable
fun EntityIconTile(color: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.(Color) -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    val base = if (LocalTaktCardAppearance.current == CardAppearance.TONAL_FILLED)
        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    // Match the painted background to the contrast calculation, including photo-backed glass.
    val fill = color.copy(alpha = .16f).compositeOver(base)
    val foreground = readableAccentColor(color, fill)
    // A restrained subject-colour tile avoids the white glass rim around this small icon.
    Box(modifier.size(46.dp).clip(shape).background(fill, shape)
        .border(.75.dp, color.copy(alpha = .28f), shape), contentAlignment = Alignment.Center) {
        content(foreground)
    }
}

@Composable
fun CourseInlineIcon(courseId: String?) {
    CourseIcons.find(LocalCourseIconKeys.current[courseId])?.let { icon ->
        Icon(icon.vector, t(icon.label), Modifier.padding(end = 6.dp).size(18.dp), tint = taktSubjectColor(courseId.orEmpty()))
    }
}
