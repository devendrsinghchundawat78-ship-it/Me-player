package com.example.ui.player

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.example.data.PreferencesManager
import com.example.data.VideoDatabase
import com.example.data.VideoRepository
import com.example.model.VideoItem
import com.example.ui.theme.PlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AspectRatioMode(val displayName: String) {
    FIT("Fit"),
    FILL("Fill"),
    STRETCH("Stretch"),
    SIXTEEN_NINE("16:9"),
    FOUR_THREE("4:3")
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs = PreferencesManager(context)
    private val database = VideoDatabase.getDatabase(context)
    private val repository = VideoRepository(database.videoDao())

    // ExoPlayer Instance
    var player: ExoPlayer? = null
        private set

    // Preference / Settings states
    private val _selectedTheme = MutableStateFlow(prefs.getSelectedTheme())
    val selectedTheme: StateFlow<PlayerTheme> = _selectedTheme.asStateFlow()

    private val _hwDecoding = MutableStateFlow(prefs.isHardwareDecoding())
    val hwDecoding: StateFlow<Boolean> = _hwDecoding.asStateFlow()

    private val _smartEnhancer = MutableStateFlow(prefs.isSmartEnhancerEnabled())
    val smartEnhancer: StateFlow<Boolean> = _smartEnhancer.asStateFlow()

    private val _refreshRate = MutableStateFlow(prefs.getRefreshRate())
    val refreshRate: StateFlow<String> = _refreshRate.asStateFlow()

    private val _appIcon = MutableStateFlow(prefs.getAppIcon())
    val appIcon: StateFlow<String> = _appIcon.asStateFlow()

    // Playlist / Current playback states
    val playlist = repository.allVideos

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatio: StateFlow<AspectRatioMode> = _aspectRatio.asStateFlow()

    private val _isInPiP = MutableStateFlow(false)
    val isInPiP: StateFlow<Boolean> = _isInPiP.asStateFlow()

    fun setIsInPiP(value: Boolean) {
        _isInPiP.value = value
    }

    // Seek states
    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Gesture Overlay (HUD) states
    private val _brightnessOverlay = MutableStateFlow<Float?>(null)
    val brightnessOverlay: StateFlow<Float?> = _brightnessOverlay.asStateFlow()

    private val _volumeOverlay = MutableStateFlow<Int?>(null)
    val volumeOverlay: StateFlow<Int?> = _volumeOverlay.asStateFlow()

    private val _seekOverlay = MutableStateFlow<Long?>(null)
    val seekOverlay: StateFlow<Long?> = _seekOverlay.asStateFlow()

    // Helpers
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var progressTrackingJob: Job? = null
    private var hudDismissJob: Job? = null

    // Tracking brightness value in-memory
    private var currentBrightness = 0.5f

    init {
        // Pre-populate with high quality streaming video resources if empty
        viewModelScope.launch {
            repository.allVideos.collect { list ->
                if (list.isEmpty()) {
                    val sampleVideos = listOf(
                        VideoItem(
                            id = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                            title = "Big Buck Bunny (Animation)",
                            duration = 596000L,
                            size = 158008350L,
                            isStreaming = true,
                            thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=300",
                            addedDate = System.currentTimeMillis() - 1000
                        ),
                        VideoItem(
                            id = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                            title = "Sintel (Fantasy CGI Story)",
                            duration = 888000L,
                            size = 210450000L,
                            isStreaming = true,
                            thumbnailUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?q=80&w=300",
                            addedDate = System.currentTimeMillis() - 2000
                        ),
                        VideoItem(
                            id = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                            title = "Tears of Steel (Sci-Fi Film)",
                            duration = 734000L,
                            size = 180350000L,
                            isStreaming = true,
                            thumbnailUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?q=80&w=300",
                            addedDate = System.currentTimeMillis() - 3000
                        )
                    )
                    sampleVideos.forEach { repository.insertVideo(it) }
                }
            }
        }
    }

    fun setTheme(theme: PlayerTheme) {
        _selectedTheme.value = theme
        prefs.setSelectedTheme(theme)
    }

    fun toggleHwDecoding() {
        val nextValue = !_hwDecoding.value
        _hwDecoding.value = nextValue
        prefs.setHardwareDecoding(nextValue)
        
        // Reinitialize player to apply new hardware decoding choice if a video is currently playing!
        _currentVideo.value?.let { video ->
            val pos = player?.currentPosition ?: 0L
            initializePlayer(video, pos)
        }
    }

    fun toggleSmartEnhancer() {
        val nextValue = !_smartEnhancer.value
        _smartEnhancer.value = nextValue
        prefs.setSmartEnhancerEnabled(nextValue)
    }

    fun setRefreshRate(rate: String) {
        _refreshRate.value = rate
        prefs.setRefreshRate(rate)
    }

    fun setAppIcon(iconName: String) {
        _appIcon.value = iconName
        prefs.setAppIcon(iconName)
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatio.value = mode
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player?.playbackParameters = PlaybackParameters(speed)
    }

    // Main Player Controller Methods
    fun initializePlayer(videoItem: VideoItem, resumePosition: Long = -1L) {
        // Release previous player if active
        releasePlayer()

        _currentVideo.value = videoItem

        val preferSoftware = !_hwDecoding.value
        val customCodecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            val codecs = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
            if (preferSoftware) {
                codecs.sortedWith { codec1, codec2 ->
                    val name1 = codec1.name.lowercase()
                    val name2 = codec2.name.lowercase()
                    
                    val isSw1 = name1.startsWith("omx.google.") || name1.startsWith("c2.android.") || name1.contains(".sw.") || name1.contains("google") || name1.contains("software")
                    val isSw2 = name2.startsWith("omx.google.") || name2.startsWith("c2.android.") || name2.contains(".sw.") || name2.contains("google") || name2.contains("software")
                    
                    when {
                        isSw1 && !isSw2 -> -1
                        !isSw1 && isSw2 -> 1
                        else -> 0
                    }
                }
            } else {
                codecs
            }
        }

        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setMediaCodecSelector(customCodecSelector)
        }

        player = ExoPlayer.Builder(context, renderersFactory)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(Uri.parse(videoItem.id))
                setMediaItem(mediaItem)
                
                val startPos = if (resumePosition >= 0) resumePosition else videoItem.lastPlaybackPosition
                if (startPos > 0) {
                    seekTo(startPos)
                }

                playbackParameters = PlaybackParameters(_playbackSpeed.value)
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        // Handle size changes if needed
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            _duration.value = duration
                        }
                    }
                })
            }

        startTrackingProgress()
    }

    private fun startTrackingProgress() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch {
            while (true) {
                player?.let {
                    _currentTime.value = it.currentPosition
                    _duration.value = it.duration
                }
                delay(200L)
            }
        }
    }

    fun playPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
        _currentTime.value = position
    }

    fun seekForward() {
        player?.let {
            val target = (it.currentPosition + 10000L).coerceAtMost(it.duration)
            seekTo(target)
        }
    }

    fun seekBackward() {
        player?.let {
            val target = (it.currentPosition - 10000L).coerceAtLeast(0L)
            seekTo(target)
        }
    }

    fun addStreamingVideo(title: String, url: String) {
        viewModelScope.launch {
            val videoItem = VideoItem(
                id = url,
                title = title,
                duration = 0L,
                size = 0L,
                isStreaming = true,
                thumbnailUrl = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?q=80&w=300",
                addedDate = System.currentTimeMillis()
            )
            repository.insertVideo(videoItem)
        }
    }

    fun deleteVideo(id: String) {
        viewModelScope.launch {
            repository.deleteVideo(id)
            if (_currentVideo.value?.id == id) {
                releasePlayer()
                _currentVideo.value = null
            }
        }
    }

    // Swipes & Gesture HUD methods
    fun adjustBrightness(delta: Float, activityBrightnessUpdater: (Float) -> Unit) {
        // Change local memory brightness level
        currentBrightness = (currentBrightness + delta).coerceIn(0.01f, 1.0f)
        _brightnessOverlay.value = currentBrightness
        
        // Trigger actual window brightness update via Activity
        activityBrightnessUpdater(currentBrightness)
        
        triggerHudDismissTimer()
    }

    fun adjustVolume(delta: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVolume <= 0) return
        
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val deltaSteps = (delta * maxVolume).toInt()
        
        val targetVolume = (currentVol + deltaSteps).coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0) // 0 flags to avoid standard OS UI popup, enabling our frosted glass HUD
        
        _volumeOverlay.value = targetVolume
        triggerHudDismissTimer()
    }

    fun adjustSeekDrag(deltaFraction: Float) {
        val currentPos = player?.currentPosition ?: 0L
        val totalDuration = player?.duration ?: 0L
        if (totalDuration <= 0) return

        // Scale seek drag - full screen width is equal to seeking 2 minutes (120,000ms)
        val seekDelta = (deltaFraction * 120000L).toLong()
        val targetPosition = (currentPos + seekDelta).coerceIn(0L, totalDuration)
        
        _seekOverlay.value = targetPosition
        triggerHudDismissTimer()
    }

    fun handleDragEnd() {
        // If a seek drag was active, apply the final position!
        _seekOverlay.value?.let { finalPosition ->
            seekTo(finalPosition)
        }
        
        // Let the HUD fade out after a slight delay
        viewModelScope.launch {
            delay(500)
            _brightnessOverlay.value = null
            _volumeOverlay.value = null
            _seekOverlay.value = null
        }
    }

    private fun triggerHudDismissTimer() {
        hudDismissJob?.cancel()
        hudDismissJob = viewModelScope.launch {
            delay(1500)
            _brightnessOverlay.value = null
            _volumeOverlay.value = null
            _seekOverlay.value = null
        }
    }

    fun releasePlayer() {
        progressTrackingJob?.cancel()
        player?.let {
            val lastPos = it.currentPosition
            _currentVideo.value?.let { video ->
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updatePlaybackPosition(video.id, lastPos)
                }
            }
            it.release()
        }
        player = null
        _isPlaying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
