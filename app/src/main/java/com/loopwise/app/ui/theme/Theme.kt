package com.loopwise.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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

private fun ColorScheme.tintedFor(period: DayPeriod): ColorScheme = copy(
    background = lerp(background, period.tint, BackgroundTintFraction),
    surface = lerp(surface, period.tint, SurfaceTintFraction),
)

@Composable
fun LoopwiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    dayPeriod: DayPeriod = remember { DayPeriod.current() },
    content: @Composable () -> Unit,
) {
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
