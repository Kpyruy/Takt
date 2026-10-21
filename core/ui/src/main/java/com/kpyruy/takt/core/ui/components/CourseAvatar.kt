package com.kpyruy.takt.core.ui.components

import com.kpyruy.takt.core.ui.i18n.t

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.model.Course
import com.kpyruy.takt.core.ui.theme.taktSubjectColor

val LocalCourseCodeKeys = staticCompositionLocalOf<Map<String, String>> { emptyMap() }

val LocalCourseIconKeys = staticCompositionLocalOf<Map<String, String?>> { emptyMap() }

@Composable
fun CourseAvatar(course: Course, modifier: Modifier = Modifier) {
    val color = taktSubjectColor(course.id)
    val icon = CourseIcons.find(course.iconKey)
    val shape = RoundedCornerShape(13.dp)
    // A restrained subject-colour tile avoids the white glass rim around this small icon.
    Box(modifier.size(46.dp).clip(shape).background(color.copy(alpha = .16f), shape)
        .border(.75.dp, color.copy(alpha = .28f), shape), contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon.vector, t(icon.label), Modifier.size(26.dp), tint = color)
        else Text(course.code.substringBefore("_").take(2), color = color, fontSize = 14.sp, lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
    }
}

@Composable
fun CourseInlineIcon(courseId: String?) {
    CourseIcons.find(LocalCourseIconKeys.current[courseId])?.let { icon ->
        Icon(icon.vector, t(icon.label), Modifier.padding(end = 6.dp).size(18.dp), tint = taktSubjectColor(courseId.orEmpty()))
    }
}
