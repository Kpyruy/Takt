package com.kpyruy.takt.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kpyruy.takt.core.ui.R

private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun type(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = Inter, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight,
)

internal val TaktTypography = Typography(
    displayLarge = type(48, 56, FontWeight.SemiBold),
    displayMedium = type(40, 48, FontWeight.SemiBold),
    displaySmall = type(36, 44, FontWeight.SemiBold),
    headlineLarge = type(30, 38, FontWeight.SemiBold).copy(letterSpacing = (-0.8).sp),
    headlineMedium = type(29, 34, FontWeight.Bold).copy(letterSpacing = (-0.7).sp),
    headlineSmall = type(24, 31, FontWeight.SemiBold).copy(letterSpacing = (-0.5).sp),
    titleLarge = type(20, 27, FontWeight.SemiBold),
    titleMedium = type(15, 20, FontWeight.SemiBold),
    titleSmall = type(14, 19, FontWeight.SemiBold),
    bodyLarge = type(16, 24), bodyMedium = type(13, 19), bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.Medium),
    labelMedium = type(12, 18, FontWeight.Medium),
    labelSmall = type(10, 14, FontWeight.Medium),
)
