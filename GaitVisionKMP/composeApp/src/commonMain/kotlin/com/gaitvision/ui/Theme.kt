package com.gaitvision.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeConfig {
    var isDarkMode by mutableStateOf<Boolean?>(null)
}

// App Colors from original Android app design
val BgUltraDarkBlue = Color(0xFF1A1A2E)
val BgDarkBlue = Color(0xFF16213E)
val CardSurfaceDark = Color(0xFF252542)
val TextLight = Color(0xFFFFFFFF)
val TextLightMuted = Color(0xFFE2E8F0)
val TextSlate = Color(0xFF94A3B8)

val PrimaryBlue = Color(0xFF3B82F6)
val PrimaryPurple = Color(0xFF8B5CF6)
val SecondaryTeal = Color(0xFF0891B2)
val AccentGreen = Color(0xFF10B981)
val ButtonDanger = Color(0xFFF43F5E)

private val DarkColorPalette = darkColors(
    primary = PrimaryBlue,
    primaryVariant = PrimaryPurple,
    secondary = AccentGreen,
    background = BgUltraDarkBlue,
    surface = CardSurfaceDark,
    onPrimary = TextLight,
    onSecondary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight
)

private val LightColorPalette = lightColors(
    primary = PrimaryBlue,
    primaryVariant = PrimaryPurple,
    secondary = AccentGreen,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
)

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
        fontSize = 14.sp,
        letterSpacing = 1.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
)

@Composable
fun GaitVisionTheme(
    // We enforce dark theme by default based on screenshots, 
    // unless explicitly changed.
    darkTheme: Boolean = ThemeConfig.isDarkMode ?: true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = AppTypography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
