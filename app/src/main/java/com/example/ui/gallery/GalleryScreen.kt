package com.example.ui.gallery

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.VideoItem
import com.example.ui.player.PlayerViewModel
import com.example.ui.player.formatTime
import com.example.ui.theme.PlayerTheme

enum class GalleryTab(val title: String, val activeIcon: androidx.compose.ui.graphics.vector.ImageVector, val inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector) {
    VIDEO("Video", Icons.Default.MovieFilter, Icons.Default.VideoLibrary),
    AUDIO("Audio", Icons.Default.MusicNote, Icons.Default.Audiotrack),
    STREAMING("Streaming", Icons.Default.Language, Icons.Default.AddLink)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: PlayerViewModel,
    onVideoSelected: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    val theme by viewModel.selectedTheme.collectAsState()
    val videos by viewModel.playlist.collectAsState(initial = emptyList())
    val refreshRate by viewModel.refreshRate.collectAsState()
    val appIcon by viewModel.appIcon.collectAsState()

    var activeTab by remember { mutableStateOf(GalleryTab.VIDEO) }
    var searchQuery by remember { mutableStateOf("") }
    var showSettingsPanel by remember { mutableStateOf(false) }

    // Mock Audio Playlist for Tab 1 (Premium soundscapes/loops)
    val audioPlaylist = remember {
        listOf(
            VideoItem(
                id = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                title = "Cyber Synth Soundscape (Chundawat)",
                duration = 372000L,
                size = 14500000L,
                isStreaming = true,
                thumbnailUrl = "https://images.unsplash.com/photo-1614149162883-504ce4d13909?q=80&w=300",
                addedDate = System.currentTimeMillis()
            ),
            VideoItem(
                id = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                title = "Lofi Sunset Horizon (Hans Zimmer Theme)",
                duration = 425000L,
                size = 16800000L,
                isStreaming = true,
                thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=300",
                addedDate = System.currentTimeMillis() - 500
            ),
            VideoItem(
                id = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                title = "Interstellar Ambient Space Wave",
                duration = 302000L,
                size = 11200000L,
                isStreaming = true,
                thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=300",
                addedDate = System.currentTimeMillis() - 1000
            )
        )
    }

    // Scans local videos if permission granted
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Scanning storage for media...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Using premium online cloud streaming index.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Devraj Media",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 24.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "iOS PREMIUM LIQUID PLATFORM",
                            color = theme.primary.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                actions = {
                    // Open Unified Premium Control Center & Settings
                    IconButton(
                        onClick = { showSettingsPanel = true },
                        modifier = Modifier
                            .testTag("settings_selector_button")
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Control Panel",
                            tint = theme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        },
        containerColor = theme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content body using beautiful iOS-like transition animations
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn(tween(250))).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut(tween(200))
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(tween(250))).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut(tween(200))
                        )
                    }
                },
                label = "TabTransition"
            ) { targetTab ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    when (targetTab) {
                        GalleryTab.VIDEO -> {
                            // Video Tab content
                            VideoTabScreen(
                                viewModel = viewModel,
                                videos = videos,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onVideoSelected = onVideoSelected,
                                theme = theme
                            )
                        }
                        GalleryTab.AUDIO -> {
                            // Audio Tab content
                            AudioTabScreen(
                                audioItems = audioPlaylist,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onAudioSelected = onVideoSelected,
                                theme = theme
                            )
                        }
                        GalleryTab.STREAMING -> {
                            // Custom Network Streaming URL playback console
                            NetStreamingTabScreen(
                                viewModel = viewModel,
                                onStreamSelected = onVideoSelected,
                                theme = theme
                            )
                        }
                    }
                }
            }

            // FLOATING iOS-Style Liquid Glass Bottom Navigation Bar
            // Hovers nicely above the bottom edge with margins and backdrop glassmorphism border styling
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.03f))
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GalleryTab.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 0.95f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "TabScale"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .scale(scale)
                                .clickable {
                                    activeTab = tab
                                    searchQuery = "" // Reset search bar on tab swap
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) theme.primary else Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title.uppercase(),
                                color = if (isSelected) theme.primary else Color.White.copy(alpha = 0.45f),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Unified Premium iOS Settings & Control Panel (Theme, 120Hz refresh, Dynamic Icons)
    if (showSettingsPanel) {
        var animateBranding by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animateBranding = true
        }

        AlertDialog(
            onDismissRequest = { showSettingsPanel = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Stars",
                        tint = theme.primary
                    )
                    Text(
                        text = "Premium Control Center",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            containerColor = theme.surface,
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    // Part 1: Theme selection segment
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SELECT COLOR PALETTE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayerTheme.values().forEach { t ->
                                val isThemeSelected = theme == t
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(t.primary)
                                        .border(
                                            BorderStroke(
                                                if (isThemeSelected) 3.dp else 0.dp,
                                                Color.White
                                            ),
                                            CircleShape
                                        )
                                        .clickable { viewModel.setTheme(t) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isThemeSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // Part 2: 120Hz high refresh rate control (60Hz, 90Hz, 120Hz)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "REFRESH RATE (HIGH SMOOTHNESS)",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("60Hz", "90Hz", "120Hz").forEach { rate ->
                                val isRateSelected = refreshRate == rate
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isRateSelected) theme.primary else Color.Transparent)
                                        .clickable {
                                            viewModel.setRefreshRate(rate)
                                            Toast.makeText(context, "Renderer updated to $rate", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rate,
                                        color = if (isRateSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // Part 3: Multiple Dynamic App Icons switcher section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "DYNAMIC APP ICON DESIGN",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Default", "Neon Blue", "Cyberpunk Pink", "Sunset Gold").forEach { iconName ->
                                val isIconSelected = appIcon == iconName
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isIconSelected) theme.primary else Color.Transparent)
                                        .clickable {
                                            viewModel.setAppIcon(iconName)
                                            Toast.makeText(context, "App Icon applied: $iconName", Toast.LENGTH_LONG).show()
                                        }
                                        .padding(vertical = 6.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = iconName.split(" ")[0],
                                        color = if (isIconSelected) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Bottom branding with smooth entry fade animation
                    AnimatedVisibility(
                        visible = animateBranding,
                        enter = fadeIn(tween(1000, easing = LinearOutSlowInEasing)) + slideInVertically(initialOffsetY = { 20 }),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "MADE BY DEVRAJ",
                                color = theme.primary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Premium Dynamic Engine",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsPanel = false },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ColumnScope.VideoTabScreen(
    viewModel: PlayerViewModel,
    videos: List<VideoItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onVideoSelected: (VideoItem) -> Unit,
    theme: PlayerTheme
) {
    // Interactive Search Bar
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search videos...", color = Color.White.copy(alpha = 0.4f)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("search_text_field"),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search icon",
                tint = theme.primary
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = Color.White
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedContainerColor = theme.surface.copy(alpha = 0.4f),
            unfocusedContainerColor = theme.surface.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )

    // Filter videos based on search
    val filteredVideos = remember(videos, searchQuery) {
        if (searchQuery.isEmpty()) {
            videos
        } else {
            videos.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (filteredVideos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "No Videos",
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No videos found",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp, top = 6.dp)
        ) {
            items(filteredVideos) { video ->
                VideoCardItem(
                    video = video,
                    theme = theme,
                    onPlay = { onVideoSelected(video) },
                    onDelete = { viewModel.deleteVideo(video.id) }
                )
            }
        }
    }
}

@Composable
fun ColumnScope.AudioTabScreen(
    audioItems: List<VideoItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAudioSelected: (VideoItem) -> Unit,
    theme: PlayerTheme
) {
    // Interactive Search Bar
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search soundtrack cues...", color = Color.White.copy(alpha = 0.4f)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("audio_search_field"),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = "Search audio icon",
                tint = theme.primary
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = Color.White
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.primary,
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedContainerColor = theme.surface.copy(alpha = 0.4f),
            unfocusedContainerColor = theme.surface.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )

    // Filter audio items based on search
    val filteredAudio = remember(audioItems, searchQuery) {
        if (searchQuery.isEmpty()) {
            audioItems
        } else {
            audioItems.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (filteredAudio.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "No Audio",
                    tint = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No audio cues found",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp, top = 6.dp)
        ) {
            items(filteredAudio) { audio ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.surface.copy(alpha = 0.4f))
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onAudioSelected(audio) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio Vinyl Placeholder
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = audio.thumbnailUrl,
                            contentDescription = audio.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        // Centered vinyl record pin
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(theme.surface)
                                .border(BorderStroke(1.dp, theme.primary), CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audio.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(theme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AUDIO",
                                    color = theme.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = formatTime(audio.duration),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Audio",
                        tint = theme.primary,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NetStreamingTabScreen(
    viewModel: PlayerViewModel,
    onStreamSelected: (VideoItem) -> Unit,
    theme: PlayerTheme
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }

    // List of recently streamed or recommended premium HLS/MP4 streams
    val recommendedStreams = remember {
        listOf(
            VideoItem(
                id = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Big Buck Bunny (H264 MP4)",
                isStreaming = true,
                thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=300"
            ),
            VideoItem(
                id = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                title = "Sintel CGI Storybook (H264 MP4)",
                isStreaming = true,
                thumbnailUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?q=80&w=300"
            ),
            VideoItem(
                id = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                title = "Tears of Steel VFX (H264 MP4)",
                isStreaming = true,
                thumbnailUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?q=80&w=300"
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
    ) {
        item {
            // Direct Stream Link Input Deck
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(theme.surface.copy(alpha = 0.35f))
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "NETWORK STREAMING CONSOLE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = { Text("Custom Stream Title...", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("Paste HTTP / HLS .m3u8 URL here...", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            val finalTitle = if (titleInput.isBlank()) "Custom Net Stream" else titleInput.trim()
                            val streamItem = VideoItem(
                                id = urlInput.trim(),
                                title = finalTitle,
                                isStreaming = true,
                                thumbnailUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?q=80&w=300"
                            )
                            viewModel.addStreamingVideo(finalTitle, urlInput.trim())
                            onStreamSelected(streamItem)
                        } else {
                            Toast.makeText(context, "Please paste a streaming link first!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Flash On",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LAUNCH IMMERSIVE PLAYBACK",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "HIGH COMPATIBILITY RECOMMENDED FEEDS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        items(recommendedStreams) { recommendation ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.surface.copy(alpha = 0.25f))
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onStreamSelected(recommendation) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 86.dp, height = 58.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = recommendation.thumbnailUrl,
                        contentDescription = recommendation.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ONLINE SOURCE",
                        color = theme.primary.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Recommendations",
                    tint = theme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun VideoCardItem(
    video: VideoItem,
    theme: PlayerTheme,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(theme.surface.copy(alpha = 0.4f))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable { onPlay() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Image
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 74.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (video.thumbnailUrl != null) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = if (video.isStreaming) Icons.Default.Language else Icons.Default.MovieFilter,
                    contentDescription = "Video Thumbnail",
                    tint = theme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }

            // Duration overlay at bottom right
            if (video.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatTime(video.duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Titles and metadata
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (video.isStreaming) Color(0xFF00C853).copy(alpha = 0.15f)
                            else theme.primary.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (video.isStreaming) "STREAM" else "LOCAL",
                        color = if (video.isStreaming) Color(0xFF00E676) else theme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (video.size > 0) {
                    Text(
                        text = formatSize(video.size),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Action menu (e.g. Delete)
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Delete",
                tint = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
