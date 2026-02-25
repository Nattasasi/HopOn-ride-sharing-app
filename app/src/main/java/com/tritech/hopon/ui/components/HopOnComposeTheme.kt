package com.tritech.hopon.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tritech.hopon.R

val hopOnFontFamily = FontFamily(
    Font(R.font.urbanist_regular, FontWeight.Normal),
    Font(R.font.urbanist_semibold, FontWeight.SemiBold)
)

private val hopOnTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = hopOnFontFamily),
    displayMedium = Typography().displayMedium.copy(fontFamily = hopOnFontFamily),
    displaySmall = Typography().displaySmall.copy(fontFamily = hopOnFontFamily),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = hopOnFontFamily),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = hopOnFontFamily),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = hopOnFontFamily, fontSize = 24.sp),
    titleLarge = Typography().titleLarge.copy(fontFamily = hopOnFontFamily, fontSize = 22.sp),
    titleMedium = Typography().titleMedium.copy(fontFamily = hopOnFontFamily, fontSize = 16.sp),
    titleSmall = Typography().titleSmall.copy(fontFamily = hopOnFontFamily, fontSize = 14.sp),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = hopOnFontFamily, fontSize = 15.sp),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = hopOnFontFamily, fontSize = 14.sp),
    bodySmall = Typography().bodySmall.copy(fontFamily = hopOnFontFamily, fontSize = 12.sp),
    labelLarge = Typography().labelLarge.copy(fontFamily = hopOnFontFamily, fontSize = 14.sp),
    labelMedium = Typography().labelMedium.copy(fontFamily = hopOnFontFamily, fontSize = 12.sp),
    labelSmall = Typography().labelSmall.copy(fontFamily = hopOnFontFamily, fontSize = 11.sp)
)

private val hopOnLightColorScheme = lightColorScheme(
    primary = Color(0xFF1896F2),
    onPrimary = Color.White,
    secondary = Color(0xFFFFCC00),
    onSecondary = Color.Black,
    background = Color.White,
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF6B7280),
    outlineVariant = Color(0xFFD9DDE2)
)

@Composable
fun hopOnComposeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = hopOnLightColorScheme,
        typography = hopOnTypography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
