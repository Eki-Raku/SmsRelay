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
    val Blue = Color(0xFF0A84FF)
    val BlueSoft = Color(0xFFE5F2FF)
    val Indigo = Color(0xFF17152F)
    val IndigoDeep = Color(0xFF090816)
    val Violet = Color(0xFFC7B6FF)
    val VioletSoft = Color(0xFFEEE9FF)
    val Teal = Color(0xFF2A9D8F)
    val Green = Color(0xFF30A46C)
    val Amber = Color(0xFFFFB340)
    val Error = Color(0xFFFF453A)

    val GroupedBackground = Color(0xFFF2F2F7)
    val Paper = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1C1C1E)
    val InkMuted = Color(0xFF6E6E73)
    val InkFaint = Color(0xFFAEAEB2)
    val Hairline = Color(0xFFD1D1D6)

    val DarkBackground = Color(0xFF000000)
    val DarkSurface = Color(0xFF1C1C1E)
    val DarkRaised = Color(0xFF2C2C2E)
    val DarkHairline = Color(0xFF38383A)
}

private val LightColors = lightColorScheme(
    primary = RelayPalette.Blue,
    onPrimary = Color.White,
    primaryContainer = RelayPalette.BlueSoft,
    onPrimaryContainer = Color(0xFF004A80),
    secondary = RelayPalette.Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF5F1),
    onSecondaryContainer = Color(0xFF075E55),
    tertiary = RelayPalette.Amber,
    background = RelayPalette.GroupedBackground,
    onBackground = RelayPalette.Ink,
    surface = RelayPalette.Paper,
    onSurface = RelayPalette.Ink,
    surfaceVariant = Color(0xFFE9E9EE),
    onSurfaceVariant = RelayPalette.InkMuted,
    error = Color(0xFFD70015),
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFF8A000C),
    outline = RelayPalette.Hairline,
    outlineVariant = Color(0xFFE5E5EA),
)

private val DarkColors = darkColorScheme(
    primary = RelayPalette.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003E69),
    onPrimaryContainer = Color(0xFFB8DCFF),
    secondary = Color(0xFF62D6C7),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF123D39),
    onSecondaryContainer = Color(0xFFA7F3EA),
    tertiary = RelayPalette.Amber,
    background = RelayPalette.DarkBackground,
    onBackground = Color(0xFFF2F2F7),
    surface = RelayPalette.DarkSurface,
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = RelayPalette.DarkRaised,
    onSurfaceVariant = Color(0xFFAEAEB2),
    error = Color(0xFFFF6961),
    errorContainer = Color(0xFF5B1114),
    onErrorContainer = Color(0xFFFFDAD8),
    outline = RelayPalette.DarkHairline,
    outlineVariant = Color(0xFF2C2C2E),
)

private val RelayTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.35).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.25).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
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
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

private val RelayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

internal const val MotionDurationShort = 180
internal const val MotionDurationMedium = 320

@Composable
fun SmsRelayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = RelayTypography,
        shapes = RelayShapes,
        content = content,
    )
}
