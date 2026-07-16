package com.winlator

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DarkBackground = Color(0xFF0F172A) // Slate 900
val DarkSurface = Color(0xFF1E293B)    // Slate 800
val DarkSurfaceVariant = Color(0xFF334155) // Slate 700
val PrimarySky = Color(0xFF0EA5E9)    // Sky 500
val SecondaryTeal = Color(0xFF14B8A6)  // Teal 500
val AccentCyan = Color(0xFF06B6D4)     // Cyan 500
val TextPrimary = Color(0xFFF8FAFC)    // Slate 50
val TextSecondary = Color(0xFF94A3B8)  // Slate 400

private val DarkColorScheme = darkColorScheme(
    primary = PrimarySky,
    secondary = SecondaryTeal,
    tertiary = AccentCyan,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimarySky,
    secondary = SecondaryTeal,
    tertiary = AccentCyan,
    background = Color(0xFFF1F5F9), // Slate 100
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0), // Slate 200
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

val RofwinTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace, // retro vibe for system configs
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun RofwinTheme(
    darkTheme: Boolean = true, // Force dark theme for a premium emulator look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RofwinTypography,
        content = content
    )
}
