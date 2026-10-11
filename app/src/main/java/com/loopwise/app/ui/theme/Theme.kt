package com.loopwise.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

@Composable
fun LoopwiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LoopwiseTypography,
        content = content,
    )
}
