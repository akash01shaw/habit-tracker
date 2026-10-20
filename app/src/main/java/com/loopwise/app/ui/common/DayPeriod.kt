package com.loopwise.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalTime

/**
 * Drives the header's gradient + icon from the device clock — dawn/day/dusk/night,
 * same four bands every day (not weather-aware, just time-of-day). Deliberately coarse
 * (4 bands, whole-hour cutoffs) rather than a continuous color interpolation: simpler to
 * reason about and the jump between bands reads as "time of day changed," not a glitch.
 */
enum class DayPeriod(val icon: ImageVector, val gradientStart: Color, val gradientEnd: Color) {
    DAWN(Icons.Filled.WbTwilight, Color(0xFFFF8C69), Color(0xFFFFC371)),
    DAY(Icons.Filled.WbSunny, Color(0xFF4FACFE), Color(0xFFFFB347)),
    DUSK(Icons.Filled.WbTwilight, Color(0xFFFF7E5F), Color(0xFF6A3093)),
    NIGHT(Icons.Filled.NightlightRound, Color(0xFF0F2447), Color(0xFF1B1B3A)),
    ;

    companion object {
        fun forHour(hour: Int): DayPeriod = when (hour) {
            in 5..7 -> DAWN
            in 8..16 -> DAY
            in 17..19 -> DUSK
            else -> NIGHT
        }

        fun current(): DayPeriod = forHour(LocalTime.now().hour)
    }
}
