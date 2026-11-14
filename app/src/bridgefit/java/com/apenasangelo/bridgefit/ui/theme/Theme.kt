package com.apenasangelo.bridgefit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The surface and text colors of one appearance (dark or light), in the iOS grouped-list style.
 */
@Immutable
data class FitSurfaces(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val cardPressed: Color,
    val separator: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val tabBar: Color,
    val chartGrid: Color,
    val control: Color,
)

val DarkSurfaces = FitSurfaces(
    isDark = true,
    background = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    cardPressed = Color(0xFF2C2C2E),
    separator = Color(0xFF38383A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF636366),
    tabBar = Color(0xF0121212),
    chartGrid = Color(0xFF2C2C2E),
    control = Color(0xFF2C2C2E),
)

val LightSurfaces = FitSurfaces(
    isDark = false,
    background = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    cardPressed = Color(0xFFE5E5EA),
    separator = Color(0xFFC6C6C8),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF6C6C70),
    textTertiary = Color(0xFFAEAEB2),
    tabBar = Color(0xF0F9F9F9),
    chartGrid = Color(0xFFE5E5EA),
    control = Color(0xFFE5E5EA),
)

/**
 * The fixed accent colors of each metric. They are the same in both appearances.
 */
object FitColors {
    // Activity rings: each ring is drawn with a sweep from its start to its end color.
    val moveStart = Color(0xFFE5004F)
    val moveEnd = Color(0xFFFF3D7F)
    val exerciseStart = Color(0xFF7BE000)
    val exerciseEnd = Color(0xFFC8FF2E)
    val stepsStart = Color(0xFF00C7E8)
    val stepsEnd = Color(0xFF4DF2F7)

    val move = Color(0xFFFF2D6F)
    val exercise = Color(0xFFA4F02A)
    val steps = Color(0xFF2EDCF0)

    val heart = Color(0xFFFF375F)
    val sleep = Color(0xFF7D7AFF)
    val spo2 = Color(0xFF64D2FF)
    val hrv = Color(0xFFFF6482)
    val stress = Color(0xFF40C8E0)
    val temperature = Color(0xFFFF9F0A)
    val distance = Color(0xFFFFB340)
    val battery = Color(0xFF30D158)
    val batteryLow = Color(0xFFFF453A)
    val accent = Color(0xFF0A84FF)
    val workouts = Color(0xFFA4F02A)

    // Sleep stages.
    val sleepAwake = Color(0xFFFF8A65)
    val sleepRem = Color(0xFF5AC8FA)
    val sleepCore = Color(0xFF0A84FF)
    val sleepDeep = Color(0xFF3634A3)

    // Stress zones: relaxed, mild, moderate, high.
    val stressZones = listOf(Color(0xFF40C8E0), Color(0xFFFFD60A), Color(0xFFFF9F0A), Color(0xFFFF453A))
}

/**
 * The type ramp, after the iOS text styles.
 */
object FitType {
    val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.01.em, lineHeight = 41.sp)
    val title1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
    val title2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
    val title3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 25.sp)
    val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val body = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp)
    val subheadline = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val subheadlineBold = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
    val footnote = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)
    val captionBold = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp)

    /** Big metric numbers, as in the Summary cards. */
    val value = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em, lineHeight = 34.sp)

    /** The hero number of a detail screen. */
    val heroValue = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em, lineHeight = 48.sp)

    /** The unit next to a value ("BPM", "KCAL"). */
    val unit = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
}

val LocalFitSurfaces = staticCompositionLocalOf { DarkSurfaces }

/**
 * Shortcut to the current appearance's surfaces.
 */
val fit: FitSurfaces
    @Composable get() = LocalFitSurfaces.current

@Composable
fun BridgeFitTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val surfaces = if (darkTheme) DarkSurfaces else LightSurfaces
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = FitColors.accent,
            background = surfaces.background,
            surface = surfaces.card,
            surfaceContainer = surfaces.card,
            surfaceContainerHigh = surfaces.cardPressed,
            onBackground = surfaces.textPrimary,
            onSurface = surfaces.textPrimary,
            onSurfaceVariant = surfaces.textSecondary,
            outline = surfaces.separator,
        )
    } else {
        lightColorScheme(
            primary = FitColors.accent,
            background = surfaces.background,
            surface = surfaces.card,
            surfaceContainer = surfaces.card,
            surfaceContainerHigh = surfaces.cardPressed,
            onBackground = surfaces.textPrimary,
            onSurface = surfaces.textPrimary,
            onSurfaceVariant = surfaces.textSecondary,
            outline = surfaces.separator,
        )
    }
    CompositionLocalProvider(LocalFitSurfaces provides surfaces) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
