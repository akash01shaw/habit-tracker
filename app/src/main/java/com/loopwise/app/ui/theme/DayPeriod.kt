package com.loopwise.app.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.loopwise.app.R
import java.time.LocalTime

/**
 * Drives the app's time-of-day theming from the device clock — dawn/day/dusk/night, same
 * four bands every day (not weather-aware, just time-of-day). Each band carries the hero
 * illustration used behind the header and a small palette (all sampled by eye from that
 * illustration) that drives the rest of the app, not just the banner:
 * - [tint]: subtle app-wide background/surface wash.
 * - [accent]: the period's primary color — bottom nav, selected date, FAB, "Daily Habits".
 * - [accentTodo]: a complementary secondary hue — "To-Do List" section + task rows, and
 *   anything using Material's default secondary (filter chips, etc).
 * - [accentQuit]: a muted/darker variant for "Habits I'm Quitting" — the avoidance side of
 *   the app is deliberately restrained rather than bright (see [QuitPalette]), so this stays
 *   desaturated even though it's still drawn from the same scene.
 * - [accentHabit]: a vivid, warm highlight (the sun/moon glow in each illustration) used as
 *   the starting color for a newly created habit, so new entries read as "of this theme"
 *   instead of always defaulting to the same blue.
 * - [isDarkPalette]: dawn/day render light, dusk/night render dark — fixed per band rather
 *   than following the device's system light/dark setting.
 *
 * Deliberately does NOT touch a habit's own chosen color ([BuildPalette]/[QuitPalette], picked
 * per-habit when creating it, free to be edited afterward) or a journal entry's category
 * color — those are the user's own identity/categorization choices and should stay stable
 * regardless of time of day; only the *starting* color for a brand-new habit is theme-led.
 */
enum class DayPeriod(
    @DrawableRes val backgroundRes: Int,
    val tint: Color,
    val accent: Color,
    val accentTodo: Color,
    val accentQuit: Color,
    val accentHabit: Color,
    val isDarkPalette: Boolean,
) {
    DAWN(R.drawable.bg_dawn, Color(0xFFFF8A65), Color(0xFFFF7043), Color(0xFFF06292), Color(0xFF6D4C41), Color(0xFFFFCA28), false),
    DAY(R.drawable.bg_day, Color(0xFF42A5F5), Color(0xFF29B6F6), Color(0xFF66BB6A), Color(0xFF455A64), Color(0xFFFFB300), false),
    DUSK(R.drawable.bg_dusk, Color(0xFFE39FC2), Color(0xFFBA68C8), Color(0xFFF48FB1), Color(0xFF4A3B5C), Color(0xFFFFAB40), true),
    NIGHT(R.drawable.bg_night, Color(0xFF26307A), Color(0xFF7986CB), Color(0xFF90A4AE), Color(0xFF263238), Color(0xFFFFD54F), true),
    ;

    companion object {
        /** Dawn/dusk get a short 4-hour window, day/night get a longer 8-hour one:
         *  dawn 5-9, day 9-17, dusk 17-21, night 21-5. */
        fun forHour(hour: Int): DayPeriod = when (hour) {
            in 5..8 -> DAWN
            in 9..16 -> DAY
            in 17..20 -> DUSK
            else -> NIGHT
        }

        fun current(): DayPeriod = forHour(LocalTime.now().hour)
    }
}
