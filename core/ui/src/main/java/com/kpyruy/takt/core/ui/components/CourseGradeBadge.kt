package com.kpyruy.takt.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.model.GradeLetter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val gradeDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun CourseGradeBadge(grade: GradeLetter, fulfilledOn: LocalDate?, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Column(modifier.widthIn(min = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(42.dp).glassSurface(RoundedCornerShape(11.dp), MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) {
            Text(grade.name, color = color, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
        }
        if (fulfilledOn != null) {
            Spacer(Modifier.height(4.dp))
            Text(fulfilledOn.format(gradeDateFormat), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
