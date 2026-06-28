package com.example

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.model.VideoItem
import com.example.ui.gallery.GalleryScreen
import com.example.ui.player.PlayerViewModel
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.theme.VideoPlayerTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val theme by viewModel.selectedTheme.collectAsState()
            val refreshRate by viewModel.refreshRate.collectAsState()
            val appIcon by viewModel.appIcon.collectAsState()
            var selectedVideo by remember { mutableStateOf<VideoItem?>(null) }

            // Dynamic Immersive Fullscreen Mode
            LaunchedEffect(selectedVideo) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (selectedVideo != null) {
                    // Hide top status bar and bottom navigation bar for complete immersion
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                    windowInsetsController.systemBarsBehavior = 
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    // Restore standard system bars
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            // High Refresh Rate Setup (60Hz, 90Hz, 120Hz support)
            LaunchedEffect(refreshRate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val rateFloat = when(refreshRate) {
                        "60Hz" -> 60f
                        "90Hz" -> 90f
                        "120Hz" -> 120f
                        else -> 0f
                    }
                    if (rateFloat > 0f) {
                        try {
                            val layoutParams = window.attributes
                            val field = layoutParams.javaClass.getField("preferredFrameRate")
                            field.set(layoutParams, rateFloat)
                            window.attributes = layoutParams
                        } catch (e: Exception) {
                            // Safe fallback
                        }
                    }
                }
            }

            // Dynamic App Icon Switcher implementation
            LaunchedEffect(appIcon) {
                changeAppIcon(this@MainActivity, appIcon)
            }

            VideoPlayerTheme(playerTheme = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = theme.background
                ) {
                    if (selectedVideo == null) {
                        GalleryScreen(
                            viewModel = viewModel,
                            onVideoSelected = { video ->
                                selectedVideo = video
                            }
                        )
                    } else {
                        VideoPlayerScreen(
                            videoItem = selectedVideo!!,
                            viewModel = viewModel,
                            onBack = {
                                selectedVideo = null
                            }
                        )
                    }
                }
            }
        }
    }

    private fun changeAppIcon(context: Context, iconName: String) {
        val pm = context.packageManager
        val packageName = context.packageName

        val aliases = listOf(
            "Default" to "$packageName.MainActivity",
            "Neon Blue" to "$packageName.MainActivityNeonBlue",
            "Cyberpunk Pink" to "$packageName.MainActivityCyberPink",
            "Sunset Gold" to "$packageName.MainActivitySunsetGold"
        )

        aliases.forEach { (name, className) ->
            val state = if (name == iconName) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    ComponentName(context, className),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                // Safe ignore or fallback
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.currentVideo.value != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val params = PictureInPictureParams.Builder().build()
                    enterPictureInPictureMode(params)
                } else {
                    @Suppress("DEPRECATION")
                    enterPictureInPictureMode()
                }
            } catch (e: Exception) {
                // Safe fallback
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setIsInPiP(isInPictureInPictureMode)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer()
    }
}
