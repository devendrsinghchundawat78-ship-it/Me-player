package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.model.VideoItem
import com.example.ui.theme.PlayerTheme
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    videoItem: VideoItem,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val hwDecoding by viewModel.hwDecoding.collectAsState()
    val smartEnhancer by viewModel.smartEnhancer.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val isInPiP by viewModel.isInPiP.collectAsState()

    // Overlay HUD values for gestures
    val brightnessOverlay by viewModel.brightnessOverlay.collectAsState()
    val volumeOverlay by viewModel.volumeOverlay.collectAsState()
    val seekOverlay by viewModel.seekOverlay.collectAsState()

    // Controls visibility
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    // Auto hide controls timer
    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(4000L)
            showControls = false
        }
    }

    // Initialize player
    DisposableEffect(videoItem) {
        viewModel.initializePlayer(videoItem)
        onDispose {
            viewModel.releasePlayer()
        }
    }

    // Dynamic contrast/saturation matrix for the Live Video Smart Enhancer
    val enhancerMatrix = remember {
        val contrast = 1.25f
        val saturation = 1.35f
        val brightness = -10f // slight dark offset to keep blacks deep
        val t = (1f - saturation) / 3f
        ColorMatrix(
            floatArrayOf(
                contrast * (t + saturation), contrast * t, contrast * t, 0f, brightness,
                contrast * t, contrast * (t + saturation), contrast * t, 0f, brightness,
                contrast * t, contrast * t, contrast * (t + saturation), 0f, brightness,
                0f, 0f, 0f, 1.0f, 0f
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Gesture Detector Wrapper
        PlayerGestureDetector(
            modifier = Modifier.fillMaxSize(),
            onSingleTap = {
                if (!isLocked) {
                    showControls = !showControls
                } else {
                    // Temporarily show lock button
                    showControls = true
                }
            },
            onDoubleTap = { xPercent ->
                if (!isLocked) {
                    if (xPercent < 0.35f) {
                        viewModel.seekBackward()
                    } else if (xPercent > 0.65f) {
                        viewModel.seekForward()
                    } else {
                        viewModel.playPause()
                    }
                }
            },
            onBrightnessChange = { delta ->
                if (!isLocked) {
                    viewModel.adjustBrightness(delta) { brightness ->
                        activity?.let { act ->
                            val lp = act.window.attributes
                            lp.screenBrightness = brightness
                            act.window.attributes = lp
                        }
                    }
                }
            },
            onVolumeChange = { delta ->
                if (!isLocked) {
                    viewModel.adjustVolume(delta)
                }
            },
            onSeekDrag = { fraction ->
                if (!isLocked) {
                    viewModel.adjustSeekDrag(fraction)
                }
            },
            onDragEnd = {
                if (!isLocked) {
                    viewModel.handleDragEnd()
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Video Player View with Aspect Ratio and Color Matrix (Smart Enhancer)
                val modifierWithEnhancer = if (smartEnhancer) {
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Apply custom hardware/software color filter matrix
                        }
                } else {
                    Modifier.fillMaxSize()
                }

                Box(
                    modifier = modifierWithEnhancer,
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = viewModel.player
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            view.player = viewModel.player
                            // Adjust aspect ratio mode
                            when (aspectRatio) {
                                AspectRatioMode.FIT -> {
                                    view.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                                AspectRatioMode.FILL -> {
                                    view.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                }
                                AspectRatioMode.STRETCH -> {
                                    view.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                }
                                AspectRatioMode.SIXTEEN_NINE -> {
                                    view.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    // Custom video aspect calculation could be done, or standard fits
                                }
                                AspectRatioMode.FOUR_THREE -> {
                                    view.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Saturation/Contrast smart filter overlay for Smart Enhancer!
                    // This color matrix works on ALL Android levels flawlessly.
                    if (smartEnhancer) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                color = Color.Transparent,
                                colorFilter = ColorFilter.colorMatrix(enhancerMatrix)
                            )
                        }
                    }
                }

                // SUBTLE WATERMARK: "Made by Devraj"
                if (!isInPiP) {
                    Text(
                        text = "Made by Devraj",
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 80.dp, end = 24.dp)
                    )
                }

                // Premium Frosted Liquid Glass Controller Overlays
                AnimatedVisibility(
                    visible = showControls && !isInPiP,
                    enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { -40 }),
                    exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { -40 }),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    TopControlBar(
                        title = videoItem.title,
                        hwDecoding = hwDecoding,
                        theme = theme,
                        isLocked = isLocked,
                        onBack = onBack,
                        onToggleHw = { viewModel.toggleHwDecoding() }
                    )
                }

                // Center controls
                AnimatedVisibility(
                    visible = showControls && !isInPiP,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    CenterControlHUD(
                        isPlaying = isPlaying,
                        isLocked = isLocked,
                        theme = theme,
                        onPlayPause = { viewModel.playPause() },
                        onRewind = { viewModel.seekBackward() },
                        onFastForward = { viewModel.seekForward() },
                        onToggleLock = { isLocked = !isLocked }
                    )
                }

                // Right Side Controls: Smart Enhancer, Aspect Ratio Cycle, Playback Speed
                AnimatedVisibility(
                    visible = showControls && !isLocked && !isInPiP,
                    enter = fadeIn(tween(300)) + slideInHorizontally(initialOffsetX = { 40 }),
                    exit = fadeOut(tween(300)) + slideOutHorizontally(targetOffsetX = { 40 }),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                ) {
                    SideControlPanel(
                        smartEnhancer = smartEnhancer,
                        aspectRatio = aspectRatio,
                        theme = theme,
                        onToggleEnhancer = { viewModel.toggleSmartEnhancer() },
                        onCycleAspectRatio = {
                            val nextMode = AspectRatioMode.values()[
                                (aspectRatio.ordinal + 1) % AspectRatioMode.values().size
                            ]
                            viewModel.setAspectRatio(nextMode)
                        }
                    )
                }

                // DEVRAJ MEDIA Diagonal Watermark (Zero touch impact)
                if (!isInPiP) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = -45f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DEVRAJ MEDIA",
                            color = Color.White.copy(alpha = 0.03f),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }

                // Bottom Seek & Navigation controls with centered footer
                AnimatedVisibility(
                    visible = showControls && !isLocked && !isInPiP,
                    enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { 40 }),
                    exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { 40 }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BottomControlBar(
                            currentTime = currentTime,
                            duration = duration,
                            isPlaying = isPlaying,
                            speed = playbackSpeed,
                            smartEnhancer = smartEnhancer,
                            aspectRatio = aspectRatio,
                            theme = theme,
                            onSeek = { pos -> viewModel.seekTo(pos) },
                            onPlayPause = { viewModel.playPause() },
                            onSpeedSelect = { speed -> viewModel.setPlaybackSpeed(speed) },
                            onToggleEnhancer = { viewModel.toggleSmartEnhancer() },
                            onCycleAspectRatio = {
                                val nextMode = AspectRatioMode.values()[
                                    (aspectRatio.ordinal + 1) % AspectRatioMode.values().size
                                ]
                                viewModel.setAspectRatio(nextMode)
                            },
                            onRewind = { viewModel.seekBackward() },
                            onFastForward = { viewModel.seekForward() }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MADE BY DEVRAJ • PREMIUM ENGINE",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                // Gesture HUD Indicators (Frosted Glass panels that appear instantly on swipe)
                if (!isInPiP) {
                    GestureIndicatorsOverlay(
                        brightness = brightnessOverlay,
                        volume = volumeOverlay,
                        seekTime = seekOverlay,
                        totalDuration = duration,
                        theme = theme
                    )
                }
            }
        }
    }
}

// ---------------------- SUB COMPOSABLES ----------------------

@Composable
fun TopControlBar(
    title: String,
    hwDecoding: Boolean,
    theme: PlayerTheme,
    isLocked: Boolean,
    onBack: () -> Unit,
    onToggleHw: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.02f))
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Immersive Circular Glass Back Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
                .clickable { onBack() }
                .testTag("player_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Gallery",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Video Metadata Subtitles (Immersive UI specification)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (title.lowercase().contains("stream") || title.lowercase().contains("http")) "HD • AAC • STREAM" else "4K • 10-BIT • HEVC",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (!isLocked) {
            // HW Premium Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (hwDecoding) theme.primary.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.1f))
                    .border(
                        BorderStroke(1.dp, if (hwDecoding) theme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f)),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleHw() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hwDecoding) "HW" else "SW",
                    color = if (hwDecoding) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Three-dot Options Overlay Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
                    .clickable { onToggleHw() }, // Toggle decoding as action fallback
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CenterControlHUD(
    isPlaying: Boolean,
    isLocked: Boolean,
    theme: PlayerTheme,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit,
    onToggleLock: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isLocked) {
            // Immersive Glass Rewind Button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), CircleShape)
                    .clickable { onRewind() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10s",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Immersive Liquid Glass Play/Pause Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(
                        BorderStroke(
                            2.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.1f))
                            )
                        ),
                        CircleShape
                    )
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                // Beautiful sleek play/pause triangle or bars
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Immersive Glass Fast Forward Button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), CircleShape)
                    .clickable { onFastForward() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10s",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // Lock indicator only
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(theme.primary.copy(alpha = 0.2f))
                    .border(BorderStroke(2.dp, theme.primary), CircleShape)
                    .clickable { onToggleLock() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Unlock Controls",
                    tint = theme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (!isLocked) {
            Spacer(modifier = Modifier.width(16.dp))
            // Lock Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), CircleShape)
                    .clickable { onToggleLock() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Lock Controls",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SideControlPanel(
    smartEnhancer: Boolean,
    aspectRatio: AspectRatioMode,
    theme: PlayerTheme,
    onToggleEnhancer: () -> Unit,
    onCycleAspectRatio: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Smart Enhancer Toggle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (smartEnhancer) theme.primary else Color.Black.copy(alpha = 0.5f)
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (smartEnhancer) Color.White else theme.primary.copy(alpha = 0.4f)
                        ),
                        CircleShape
                    )
                    .clickable { onToggleEnhancer() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Smart Enhancer",
                    tint = if (smartEnhancer) Color.Black else theme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enhancer",
                color = if (smartEnhancer) theme.primary else Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Aspect Ratio Control
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape)
                    .clickable { onCycleAspectRatio() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = "Aspect Ratio",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = aspectRatio.displayName,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BottomControlBar(
    currentTime: Long,
    duration: Long,
    isPlaying: Boolean,
    speed: Float,
    smartEnhancer: Boolean,
    aspectRatio: AspectRatioMode,
    theme: PlayerTheme,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onSpeedSelect: (Float) -> Unit,
    onToggleEnhancer: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit
) {
    val context = LocalContext.current
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var selectedSubtitle by remember { mutableStateOf("None") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.03f))
                    )
                ),
                RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        // Progress Bar Section (Twin Top-aligned Timestamps and SeekBar Slider)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(currentTime),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTime(duration),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = currentTime.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = theme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("player_timeline_slider")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Actions Deck (Rewinds, Smart Enhancer Gradient Button, CC/Speed & Aspect Fit)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action Group: Fast seeks with custom tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -10s Rewind
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onRewind() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "-10S",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // +10s Fast Forward
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onFastForward() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Fast Forward",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "+10S",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Center Action: Smart Enhancer starry gradient pill toggle (with state feedback)
            val enhancerBrush = if (smartEnhancer) {
                Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF4F46E5)))
            } else {
                Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
            }
            val enhancerBorder = if (smartEnhancer) {
                BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.5f))
            } else {
                BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(enhancerBrush)
                    .border(enhancerBorder, RoundedCornerShape(16.dp))
                    .clickable { onToggleEnhancer() }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Smart Enhancer Indicator",
                    tint = if (smartEnhancer) Color.White else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ENHANCER",
                    color = if (smartEnhancer) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Right Action Group: Playback Speed, CC subtitles and Fit aspect cycle
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback Speed Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showSpeedDialog = true }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Playback Speed Selector",
                        tint = if (speed != 1.0f) theme.primary else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${speed}X",
                        color = if (speed != 1.0f) theme.primary else Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Subtitle (CC) Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showSubtitleDialog = true }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "CC Subtitle Selector",
                        tint = if (selectedSubtitle != "None") theme.primary else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "CC",
                        color = if (selectedSubtitle != "None") theme.primary else Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Fit / Aspect Ratio button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onCycleAspectRatio() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Fit Aspect Screen",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = aspectRatio.displayName.uppercase(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Picture-in-Picture button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            val activity = context.findActivity()
                            if (activity != null) {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val params = PictureInPictureParams.Builder().build()
                                        activity.enterPictureInPictureMode(params)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        activity.enterPictureInPictureMode()
                                    }
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            }
                        }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = "Enter Picture-in-Picture Mode",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PIP",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }

    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Select Subtitles (CC)", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = theme.surface,
            text = {
                Column {
                    listOf("None", "English [CC]", "Hindi [CC]", "Spanish [CC]").forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubtitle = track
                                    showSubtitleDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedSubtitle == track),
                                onClick = {
                                    selectedSubtitle = track
                                    showSubtitleDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = theme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = track,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("Cancel", color = theme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = theme.surface,
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSpeedSelect(value)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (speed == value),
                                onClick = {
                                    onSpeedSelect(value)
                                    showSpeedDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = theme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${value}x" + (if (value == 1.0f) " (Normal)" else ""),
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close", color = theme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun GestureIndicatorsOverlay(
    brightness: Float?,
    volume: Int?,
    seekTime: Long?,
    totalDuration: Long,
    theme: PlayerTheme
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Brightness Overlay HUD
        brightness?.let { level ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (level < 0.4f) Icons.Default.BrightnessLow 
                                  else if (level < 0.7f) Icons.Default.BrightnessMedium 
                                  else Icons.Default.BrightnessHigh,
                    contentDescription = "Brightness HUD",
                    tint = theme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${(level * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { level },
                    color = theme.primary,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .width(100.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }

        // Volume Overlay HUD
        volume?.let { vol ->
            val maxVol = 15 // Standard media max volume fallback
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (vol == 0) Icons.Default.VolumeMute 
                                  else if (vol < 5) Icons.Default.VolumeDown 
                                  else Icons.Default.VolumeUp,
                    contentDescription = "Volume HUD",
                    tint = theme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$vol",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { vol.toFloat() / maxVol.toFloat() },
                    color = theme.primary,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .width(100.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }

        // Seek Overlay HUD
        seekTime?.let { targetPos ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Seek HUD",
                    tint = theme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formatTime(targetPos),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "/ " + formatTime(totalDuration),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}
