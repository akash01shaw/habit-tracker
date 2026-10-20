package com.loopwise.app.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = LoopwisePrimary,
    onPrimary = LoopwiseOnPrimary,
    background = LoopwiseBackground,
    surface = LoopwiseSurface,
    onSurface = LoopwiseOnSurface,
    onSurfaceVariant = LoopwiseOnSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = LoopwisePrimary,
    onPrimary = LoopwiseOnPrimary,
    background = LoopwiseBackgroundDark,
    surface = LoopwiseSurfaceDark,
    onSurface = LoopwiseOnSurfaceDark,
    onSurfaceVariant = LoopwiseOnSurfaceVariantDark,
)

/** The time-of-day band currently driving the header art and app-wide tint; read this
 *  wherever a screen wants to pick up the same accent color (e.g. the bottom nav). */
val LocalDayPeriod = compositionLocalOf { DayPeriod.current() }

private const val BackgroundTintFraction = 0.12f
private const val SurfaceTintFraction = 0.08f
private const val ContainerTintFraction = 0.35f

/**
 * Re-colors the whole Material3 role set from the current [DayPeriod] rather than just
 * tinting the background — primary/secondary (and their containers) drive most of Compose
 * Material3's own default coloring (selected date, FAB, selected filter chips, etc.), so
 * retinting them here is what makes those stock components pick up the theme automatically
 * instead of needing every screen to be hand-wired to [LocalDayPeriod].
 */
private fun ColorScheme.tintedFor(period: DayPeriod): ColorScheme = copy(
    background = lerp(background, period.tint, BackgroundTintFraction),
    surface = lerp(surface, period.tint, SurfaceTintFraction),
    primary = period.accent,
    onPrimary = Color.White,
    primaryContainer = lerp(surface, period.accent, ContainerTintFraction),
    onPrimaryContainer = period.accent,
    secondary = period.accentTodo,
    onSecondary = Color.White,
    secondaryContainer = lerp(surface, period.accentTodo, ContainerTintFraction),
    onSecondaryContainer = period.accentTodo,
)

@Composable
fun LoopwiseTheme(
    dayPeriod: DayPeriod = remember { DayPeriod.current() },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Dawn/day render light, dusk/night render dark — tied to the time-of-day band rather
    // than the device's system light/dark setting, so the two stay in sync with each other.
    val darkTheme = dayPeriod.isDarkPalette
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    CompositionLocalProvider(LocalDayPeriod provides dayPeriod) {
        MaterialTheme(
            colorScheme = baseColorScheme.tintedFor(dayPeriod),
            typography = LoopwiseTypography,
            content = content,
        )
    }
}
