package com.loopwise.app.ui.today

import java.time.Duration

/** Formats an avoidance-habit streak as "Xd, Yh, Zm, Ws", dropping leading zero units. */
fun formatStreak(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0)
    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return when {
        days > 0 -> "${days}d, ${hours}h, ${minutes}m, ${seconds}s"
        hours > 0 -> "${hours}h, ${minutes}m, ${seconds}s"
        else -> "${minutes}m, ${seconds}s"
    }
}
