package com.loopwise.app.ui.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The app's one headline streak (Duolingo-style), front and center at the top of Today —
 * the first thing seen on open. Zero streak still shows an inviting prompt rather than
 * nothing, so the mechanic is visible even before it's started.
 */
@Composable
fun DayStreakBanner(streak: Int, accentColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = if (streak > 0) 0.16f else 0.08f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = "Day streak",
            tint = if (streak > 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier,
        )
        if (streak > 0) {
            Text(
                "$streak day${if (streak == 1) "" else "s"} streak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        } else {
            Text(
                "Complete something today to start your streak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
