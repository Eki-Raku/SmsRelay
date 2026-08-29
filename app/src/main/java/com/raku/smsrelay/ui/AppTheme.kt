package com.raku.smsrelay.ui

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object RelayPalette {
    val Brand = Color(0xFF5E5CE6)
    val BrandDark = Color(0xFF7D7AFF)
    val BrandSoft = Color(0xFFEAE9FF)
    val BrandSoftDark = Color(0xFF30304F)

    val Canvas = Color(0xFFF5F5F7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0xFFEFEFF4)
    val TextPrimary = Color(0xFF1D1D1F)
    val TextSecondary = Color(0xFF6E6E73)
    val Separator = Color(0x293C3C43)

    val CanvasDark = Color(0xFF000000)
    val SurfaceDark = Color(0xFF1C1C1E)
    val SurfaceSecondaryDark = Color(0xFF2C2C2E)
    val TextPrimaryDark = Color(0xFFF5F5F7)
    val TextSecondaryDark = Color(0xFFAEAEB2)
    val SeparatorDark = Color(0x5C545458)

    val Success = Color(0xFF34C759)
    val SuccessDark = Color(0xFF30D158)
    val Warning = Color(0xFFFF9F0A)
    val WarningDark = Color(0xFFFFD60A)
    val Error = Color(0xFFFF3B30)
    val ErrorDark = Color(0xFFFF453A)

    // The brand artwork is authored against this deep indigo tile.
    val Indigo = Color(0xFF17152F)
}

private val LightColors = lightColorScheme(
    primary = RelayPalette.Brand,
    onPrimary = Color.White,
    primaryContainer = RelayPalette.BrandSoft,
    onPrimaryContainer = Color(0xFF302F83),
    secondary = RelayPalette.Success,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5F7EA),
    onSecondaryContainer = Color(0xFF176B32),
    tertiary = RelayPalette.Warning,
    background = RelayPalette.Canvas,
    onBackground = RelayPalette.TextPrimary,
    surface = RelayPalette.Surface,
    onSurface = RelayPalette.TextPrimary,
    surfaceVariant = RelayPalette.SurfaceSecondary,
    onSurfaceVariant = RelayPalette.TextSecondary,
    error = RelayPalette.Error,
    errorContainer = Color(0xFFFFE9E7),
    onErrorContainer = Color(0xFF8A1510),
    outline = Color(0xFFAEAEB2),
    outlineVariant = RelayPalette.Separator,
)

private val DarkColors = darkColorScheme(
    primary = RelayPalette.BrandDark,
    onPrimary = Color(0xFF111018),
    primaryContainer = RelayPalette.BrandSoftDark,
    onPrimaryContainer = Color(0xFFE5E4FF),
    secondary = RelayPalette.SuccessDark,
    onSecondary = Color(0xFF002109),
    secondaryContainer = Color(0xFF173A22),
    onSecondaryContainer = Color(0xFFB9F5C9),
    tertiary = RelayPalette.WarningDark,
    background = RelayPalette.CanvasDark,
    onBackground = RelayPalette.TextPrimaryDark,
    surface = RelayPalette.SurfaceDark,
    onSurface = RelayPalette.TextPrimaryDark,
    surfaceVariant = RelayPalette.SurfaceSecondaryDark,
    onSurfaceVariant = RelayPalette.TextSecondaryDark,
    error = RelayPalette.ErrorDark,
    errorContainer = Color(0xFF4A1715),
    onErrorContainer = Color(0xFFFFDAD7),
    outline = Color(0xFF636366),
    outlineVariant = RelayPalette.SeparatorDark,
)

private val RelayTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.45).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
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
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
)

private val RelayShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

internal object RelaySpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

@Immutable
data class RelayMotionScheme(
    val reducedMotion: Boolean,
    val pressMillis: Int,
    val iconMillis: Int,
    val listMillis: Int,
    val stateMillis: Int,
    val pageMillis: Int,
    val standardEasing: CubicBezierEasing,
) {
    fun duration(fullMotionMillis: Int): Int = if (reducedMotion) 100 else fullMotionMillis
}

private val LocalRelayMotionScheme = staticCompositionLocalOf {
    RelayMotionScheme(
        reducedMotion = false,
        pressMillis = 90,
        iconMillis = 160,
        listMillis = 180,
        stateMillis = 220,
        pageMillis = 320,
        standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    )
}

object RelayTheme {
    val colors: ColorScheme
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme

    val motion: RelayMotionScheme
        @Composable @ReadOnlyComposable get() = LocalRelayMotionScheme.current
}

internal const val MotionDurationShort = 180
internal const val MotionDurationMedium = 320

@Composable
fun SmsRelayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    val motion = remember(reducedMotion) {
        RelayMotionScheme(
            reducedMotion = reducedMotion,
            pressMillis = if (reducedMotion) 0 else 90,
            iconMillis = if (reducedMotion) 100 else 160,
            listMillis = if (reducedMotion) 100 else 180,
            stateMillis = if (reducedMotion) 100 else 220,
            pageMillis = if (reducedMotion) 100 else 320,
            standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
        )
    }
    CompositionLocalProvider(LocalRelayMotionScheme provides motion) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = RelayTypography,
            shapes = RelayShapes,
            content = content,
        )
    }
}
