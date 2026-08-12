package com.raku.smsrelay.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object RelayPalette {
    val Indigo = Color(0xFF1B1938)
    val IndigoDeep = Color(0xFF0E0C1F)
    val Violet = Color(0xFFC9B4FA)
    val VioletSoft = Color(0xFFE9E1FC)
    val Teal = Color(0xFF155555)
    val TealDeep = Color(0xFF0E3030)
    val WarmWhite = Color(0xFFFAFAF8)
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFF292827)
    val InkMuted = Color(0xFF73706D)
    val InkFaint = Color(0xFF9A9794)
    val Hairline = Color(0xFFE8E4DD)
    val DarkHairline = Color(0xFF3F3A52)
    val Amber = Color(0xFF9A6812)
    val Error = Color(0xFFB3261E)
}

private val LightColors = lightColorScheme(
    primary = RelayPalette.Indigo,
    onPrimary = Color.White,
    primaryContainer = RelayPalette.VioletSoft,
    onPrimaryContainer = RelayPalette.Indigo,
    secondary = RelayPalette.Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E9E6),
    onSecondaryContainer = RelayPalette.TealDeep,
    tertiary = RelayPalette.Amber,
    background = RelayPalette.WarmWhite,
    onBackground = RelayPalette.Ink,
    surface = RelayPalette.Paper,
    onSurface = RelayPalette.Ink,
    surfaceVariant = Color(0xFFF3F1ED),
    onSurfaceVariant = RelayPalette.InkMuted,
    error = RelayPalette.Error,
    outline = RelayPalette.Hairline,
    outlineVariant = Color(0xFFF0EDE8),
)

private val DarkColors = darkColorScheme(
    primary = RelayPalette.Violet,
    onPrimary = RelayPalette.Indigo,
    primaryContainer = Color(0xFF332D5A),
    onPrimaryContainer = Color(0xFFF0EAFE),
    secondary = Color(0xFF9CC8C3),
    onSecondary = RelayPalette.TealDeep,
    secondaryContainer = RelayPalette.TealDeep,
    onSecondaryContainer = Color(0xFFD8F2EE),
    tertiary = Color(0xFFE8BD69),
    background = RelayPalette.IndigoDeep,
    onBackground = Color(0xFFF4F1EE),
    surface = RelayPalette.Indigo,
    onSurface = Color(0xFFF4F1EE),
    surfaceVariant = Color(0xFF26233E),
    onSurfaceVariant = Color(0xFFBCBAC9),
    error = Color(0xFFFFB4AB),
    outline = RelayPalette.DarkHairline,
    outlineVariant = Color(0xFF2C2943),
)

private val RelayTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 29.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 23.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.35).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

private val RelayShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SmsRelayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = RelayTypography,
        shapes = RelayShapes,
        content = content,
    )
}
