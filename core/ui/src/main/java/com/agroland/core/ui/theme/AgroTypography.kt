package com.agroland.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.agroland.core.ui.R

/**
 * OpenSans қаріптері — Flutter нұсқасымен бірдей 12 face стилі.
 * Google Fonts репосынан алынды; Medium face репода жоқ → FontWeight.Medium
 * SemiBold файлына мэппингтеледі (ISSUES.md #1).
 */
val OpenSansFamily = FontFamily(
    Font(R.font.opensans_light, FontWeight.Light),
    Font(R.font.opensans_regular, FontWeight.Normal),
    Font(R.font.opensans_semibold, FontWeight.Medium),
    Font(R.font.opensans_semibold, FontWeight.SemiBold),
    Font(R.font.opensans_bold, FontWeight.Bold),
    Font(R.font.opensans_extrabold, FontWeight.ExtraBold),
    Font(R.font.opensans_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.opensans_lightitalic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.opensans_semibolditalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.opensans_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.opensans_bolditalic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.opensans_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),
)

/**
 * AppTextStyles картасы (Flutter) → Compose:
 * displayLarge 26/w500, displayMedium 16/w400, displaySmall 18/w400,
 * headlineMedium 20/w400, bodyLarge 18/w500, bodyMedium 16/w500, bodySmall 14/w400,
 * labelLarge 14/w500, labelMedium 12/w400, labelSmall 10/w400.
 */
val AgroTypography = Typography(
    displayLarge = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W500, fontSize = 26.sp, lineHeight = 32.sp),
    displayMedium = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 22.sp),
    displaySmall = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W400, fontSize = 18.sp, lineHeight = 24.sp),
    headlineMedium = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W400, fontSize = 20.sp, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W500, fontSize = 18.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W500, fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = OpenSansFamily, fontWeight = FontWeight.W400, fontSize = 10.sp, lineHeight = 14.sp),
)