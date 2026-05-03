package com.gaitvision.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ThemeConfig {
    var isDarkMode by mutableStateOf<Boolean?>(null)
}

// Legacy aliases — screens reference these; underlying values match Android colors.xml via [AppColors].
val BgUltraDarkBlue = AppColors.ActivityContainerBg
val BgDarkBlue = Color(0xFF16213E)
val CardSurfaceDark = AppColors.CardSurfaceDark
val TextLight = AppColors.TextWhite
val TextLightMuted = Color(0xFFE2E8F0)
val TextSlate = AppColors.TextTertiary

val PrimaryBlue = AppColors.PrimaryBlue
val PrimaryPurple = AppColors.AccentPurple
val SecondaryTeal = AppColors.SecondaryTeal
val AccentGreen = AppColors.AccentGreen
val ButtonDanger = AppColors.ButtonDanger

private val DarkColorPalette = darkColors(
    primary = AppColors.PrimaryBlue,
    primaryVariant = AppColors.PrimaryBlueDark,
    secondary = AppColors.SecondaryTeal,
    background = AppColors.ActivityContainerBg,
    surface = AppColors.CardSurfaceDark,
    onPrimary = AppColors.TextWhite,
    onSecondary = AppColors.TextWhite,
    onBackground = AppColors.TextWhite,
    onSurface = AppColors.TextWhite
)

private val LightColorPalette = lightColors(
    primary = AppColors.PrimaryBlue,
    primaryVariant = AppColors.PrimaryBlueDark,
    secondary = AppColors.SecondaryTeal,
    background = AppColors.SurfaceLight,
    surface = AppColors.CardBackground,
    onPrimary = AppColors.TextWhite,
    onSecondary = AppColors.TextWhite,
    onBackground = AppColors.TextPrimary,
    onSurface = Color(0xFF1E293B)
)

/** Typography aligned with Android `themes.xml` text roles (SectionTitle 16sp bold, InputLabel 14sp, Primary button 16sp bold). */
val AppTypography = Typography(
    h1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    h4 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    h5 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    h6 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    subtitle1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    subtitle2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    body1 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    overline = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.1.sp
    )
)

@Composable
fun GaitVisionTheme(
    darkTheme: Boolean = ThemeConfig.isDarkMode ?: true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    MaterialTheme(
        colors = colors,
        typography = AppTypography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
