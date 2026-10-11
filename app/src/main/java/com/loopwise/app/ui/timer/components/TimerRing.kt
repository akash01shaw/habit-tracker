package com.loopwise.app.ui.timer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A static track plus a colored progress arc (0f for stopwatch, since there's no fixed target). */
@Composable
fun TimerRing(
    progress: Float,
    timeText: String,
    subtitleText: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(260.dp)) {
            val strokeWidth = 14.dp.toPx()
            drawArc(
                color = color.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(timeText, fontSize = 44.sp, style = MaterialTheme.typography.titleLarge)
            Text(subtitleText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
