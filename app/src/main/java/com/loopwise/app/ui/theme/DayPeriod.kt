package com.loopwise.app.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.loopwise.app.R
import java.time.LocalTime

/**
 * Drives the app's time-of-day theming from the device clock — dawn/day/dusk/night, same
 * four bands every day (not weather-aware, just time-of-day). Each band carries the hero
 * illustration used behind the header and a couple of representative colors ([tint] for a
 * subtle app-wide background wash, [accent] for the bottom nav and small highlights) sampled
 * by eye from that illustration, so the whole app reads as "the same scene" as the banner.
 */
enum class DayPeriod(@DrawableRes val backgroundRes: Int, val tint: Color, val accent: Color) {
    DAWN(R.drawable.bg_dawn, Color(0xFFFF8A65), Color(0xFFFF7043)),
    DAY(R.drawable.bg_day, Color(0xFF42A5F5), Color(0xFF29B6F6)),
    DUSK(R.drawable.bg_dusk, Color(0xFFE39FC2), Color(0xFFBA68C8)),
    NIGHT(R.drawable.bg_night, Color(0xFF26307A), Color(0xFF7986CB)),
    ;

    companion object {
        /** Each band gets a fixed 6-hour window: dawn 5-11, day 11-17, dusk 17-23, night 23-5. */
        fun forHour(hour: Int): DayPeriod = when (hour) {
            in 5..10 -> DAWN
            in 11..16 -> DAY
            in 17..22 -> DUSK
            else -> NIGHT
        }

        fun current(): DayPeriod = forHour(LocalTime.now().hour)
    }
}
