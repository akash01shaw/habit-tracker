package com.loopwise.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.loopwise.app.notifications.ACTION_OPEN_NIGHTLY_REVIEW
import com.loopwise.app.ui.navigation.Destination
import com.loopwise.app.ui.navigation.LoopwiseNavHost
import com.loopwise.app.ui.theme.LoopwiseTheme

class MainActivity : ComponentActivity() {
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute = routeFromIntent(intent)
        setContent {
            LoopwiseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RequestNotificationPermission()
                    LoopwiseNavHost(
                        pendingRoute = pendingRoute,
                        onPendingRouteConsumed = { pendingRoute = null },
                    )
                }
            }
        }
    }

    // The activity is already running (standard launchMode, FLAG_ACTIVITY_CLEAR_TOP) when a
    // notification is tapped while the app is backgrounded — onCreate doesn't re-run, so the
    // deep link has to be picked up here instead.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = routeFromIntent(intent)
    }

    private fun routeFromIntent(intent: Intent?): String? =
        if (intent?.action == ACTION_OPEN_NIGHTLY_REVIEW) Destination.NightlyReview.route else null
}

@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
