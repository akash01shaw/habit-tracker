package com.loopwise.app.ui.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.loopwise.app.ui.reports.HeatmapDay

/**
 * GitHub-style contribution grid (spec §2.7): a strip of small squares, one per day,
 * colored in the habit's color with opacity scaled by that day's completion intensity.
 * Non-scheduled days (e.g. a weekday-only habit's off days) render as a neutral gray
 * rather than "not done" red/faded, so the habit isn't visually penalized for them.
 */
@Composable
fun HeatmapStrip(habitColor: Color, days: List<HeatmapDay>, modifier: Modifier = Modifier) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        items(days) { day ->
            val cellColor = if (!day.isScheduled) {
                Color.Gray.copy(alpha = 0.15f)
            } else {
                habitColor.copy(alpha = 0.12f + 0.88f * day.intensity)
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(cellColor),
            )
        }
    }
}
