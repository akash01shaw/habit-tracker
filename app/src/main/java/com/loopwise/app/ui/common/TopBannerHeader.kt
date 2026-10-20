package com.loopwise.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loopwise.app.ui.theme.BuildPalette
import com.loopwise.app.ui.theme.LoopwisePrimary
import java.time.LocalDate

/**
 * Replaces the plain "Today"/"Schedule"/etc. TopAppBar on all 5 main tabs with a
 * colored header carrying the day's motivational quote — same quote across every
 * tab since it's derived from the date, not randomized per screen.
 */
@Composable
fun TopBannerHeader(
    screenLabel: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val quote = remember { MotivationalQuotes.forDate(LocalDate.now()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(LoopwisePrimary, BuildPalette.Violet)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    screenLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                }
            }
            Text(
                quote,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
