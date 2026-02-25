package com.tritech.hopon.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    headlineSmall = Typography().headlineSmall.copy(fontFamily = hopOnFontFamily),
    titleLarge = Typography().titleLarge.copy(fontFamily = hopOnFontFamily),
    titleMedium = Typography().titleMedium.copy(fontFamily = hopOnFontFamily),
    titleSmall = Typography().titleSmall.copy(fontFamily = hopOnFontFamily),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = hopOnFontFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = hopOnFontFamily),
    bodySmall = Typography().bodySmall.copy(fontFamily = hopOnFontFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = hopOnFontFamily),
    labelMedium = Typography().labelMedium.copy(fontFamily = hopOnFontFamily),
    labelSmall = Typography().labelSmall.copy(fontFamily = hopOnFontFamily)
)

@Composable
fun hopOnComposeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = hopOnTypography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
