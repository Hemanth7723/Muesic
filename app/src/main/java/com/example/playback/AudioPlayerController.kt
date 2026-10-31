package com.example.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import com.example.data.entity.Song
import com.example.data.network.JioSaavnService
import com.example.data.repository.AutoPlaySuggestion
import com.example.util.OriginalSongFilter
import com.example.util.SongTitleCleaner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class RepeatMode {
    OFF, ALL, ONE
}

private enum class MediaPlayerInternalState {
    IDLE,
    INITIALIZED,
    PREPARING,
    PREPARED,
    STARTED,
    PAUSED,
    STOPPED,
    COMPLETED,
    ERROR
}

class AudioPlayerController(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var ytStallJob: Job? = null
    private var mediaPlayerState = MediaPlayerInternalState.IDLE
    private var playWhenReady = true
    private var pendingSeekPositionMs = 0L
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var progressTrackerJob: Job? = null

    var onSongUpdatedListener: ((Song) -> Unit)? = null
    var onSongCompletedListener: ((Song) -> Unit)? = null
    var simulateMediaPlayerForTesting: Boolean = false

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isAutoPlay = MutableStateFlow(true)
    val isAutoPlay: StateFlow<Boolean> = _isAutoPlay.asStateFlow()

    private val _nextSong = MutableStateFlow<Song?>(null)
    val nextSong: StateFlow<Song?> = _nextSong.asStateFlow()

    private val _upcomingQueue = MutableStateFlow<List<Song>>(emptyList())
    val upcomingQueue: StateFlow<List<Song>> = _upcomingQueue.asStateFlow()

    var autoPlaySuggestionProvider: (suspend (currentSong: Song?, currentQueue: List<Song>) -> AutoPlaySuggestion?)? = null
    var autoPlaySuggestionsProvider: (suspend (currentSong: Song?, currentQueue: List<Song>) -> List<AutoPlaySuggestion>)? = null
    private val _autoPlaySuggestion = MutableStateFlow<AutoPlaySuggestion?>(null)
    val autoPlaySuggestion: StateFlow<AutoPlaySuggestion?> = _autoPlaySuggestion.asStateFlow()

    private val _autoPlaySuggestionsList = MutableStateFlow<List<AutoPlaySuggestion>>(emptyList())
    val autoPlaySuggestionsList: StateFlow<List<AutoPlaySuggestion>> = _autoPlaySuggestionsList.asStateFlow()
    private var autoPlayJob: Job? = null

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var isSeeking = false
    private var lastSeekTimestamp = 0L
    private var streamRecoveryAttempts = 0

    private val _isLowBatteryMode = MutableStateFlow(false)
    val isLowBatteryMode: StateFlow<Boolean> = _isLowBatteryMode.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private var directStreamFallbackTrackId: String? = null

    val youTubeWebPlayer = YouTubeWebPlayer(context)

    init {
        initMediaPlayer()
        setupYouTubePlayer()
    }

    private fun setupYouTubePlayer() {
        youTubeWebPlayer.onPlaybackStateChanged = { playing ->
            if (isCurrentSongYouTube()) {
                if (!playWhenReady && playing) {
                    // User has paused; immediately enforce pause on web player
                    youTubeWebPlayer.pause()
                    _isPlaying.value = false
                } else {
                    _isPlaying.value = playing
                    if (playing) {
                        ytStallJob?.cancel()
                        _isBuffering.value = false
                        _currentSong.value?.let { s -> onSongUpdatedListener?.invoke(s) }
                    }
                }
            }
        }
        youTubeWebPlayer.onBufferingChanged = { buffering ->
            if (isCurrentSongYouTube()) {
                _isBuffering.value = buffering
            }
        }
        youTubeWebPlayer.onProgressUpdated = { curMs, durMs ->
            if (isCurrentSongYouTube()) {
                if (curMs > 0L) {
                    ytStallJob?.cancel()
                }
                _currentPositionMs.value = curMs
                if (durMs > 0L) {
                    _durationMs.value = durMs
                }
            }
        }
        youTubeWebPlayer.onTrackCompleted = {
            if (isCurrentSongYouTube()) {
                handleTrackCompletion()
            }
        }
        youTubeWebPlayer.onPlayerErrorCallback = { errorCode ->
            if (isCurrentSongYouTube()) {
                val song = _currentSong.value
                Log.w("AudioPlayerController", "YouTube player error $errorCode on current track: ${song?.title}")
                ytStallJob?.cancel()
                _isBuffering.value = false
                _isPlaying.value = false
                if (song != null) {
                    // Embed restricted (Error 150/101), video not found (100), invalid param (2), or playback error
                    resolveAuthenticAudioStream(song)
                }
            }
        }
        youTubeWebPlayer.onRenderProcessGoneCallback = {
            if (isCurrentSongYouTube()) {
                val song = _currentSong.value
                Log.w("AudioPlayerController", "YouTube web renderer process crashed or was terminated for track: ${song?.title}. Falling back to authentic stream...")
                ytStallJob?.cancel()
                _isBuffering.value = false
                _isPlaying.value = false
                if (song != null) {
                    resolveAuthenticAudioStream(song)
                }
            }
        }
    }

    private fun startYouTubeStallCheck(song: Song) {
        ytStallJob?.cancel()
        ytStallJob = scope.launch {
            delay(3500L)
            if (_currentSong.value?.id == song.id && !_isPlaying.value && _currentPositionMs.value == 0L) {
                Log.w("AudioPlayerController", "YouTube audio stalled at 0s for ${song.title}. Retrying resume...")
                youTubeWebPlayer.resume()
                delay(2500L)
                if (_currentSong.value?.id == song.id && !_isPlaying.value && _currentPositionMs.value == 0L) {
                    Log.w("AudioPlayerController", "YouTube audio still unplayable at 0s for ${song.title}. Resolving authentic stream...")
                    resolveAuthenticAudioStream(song)
                }
            }
        }
    }

    private fun resolveAuthenticAudioStream(song: Song, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                val queries = SongTitleCleaner.generateSearchQueries(song.title, song.artist)
                val primaryTitle = SongTitleCleaner.cleanPrimaryTitle(song.title, song.artist)
                var resolvedMatch: Song? = null

                for (query in queries) {
                    val matches = JioSaavnService.search(query)
                    // Strict match: must match primary title and be an original track
                    val candidate = matches.firstOrNull { c ->
                        c.streamUrl.isNotBlank() &&
                        OriginalSongFilter.isOriginal(c, false) &&
                        SongTitleCleaner.isFuzzyMatch(c.title, primaryTitle)
                    }

                    if (candidate != null) {
                        resolvedMatch = candidate
                        break
                    }
                }

                if (resolvedMatch != null && _currentSong.value?.id == song.id) {
                    Log.i("AudioPlayerController", "Resolved authentic stream for ${song.title} -> ${resolvedMatch.streamUrl}")
                    withContext(Dispatchers.Main) {
                        // Crucial: NEVER change song.title, song.artist, or song.album to another song
                        val updatedSong = song.copy(
                            streamUrl = resolvedMatch.streamUrl,
                            artworkUrl = resolvedMatch.artworkUrl?.takeIf { it.isNotBlank() } ?: song.artworkUrl,
                            audioQuality = "320kbps HD Direct",
                            durationMs = if (resolvedMatch.durationMs > 0) resolvedMatch.durationMs else song.durationMs
                        )
                        directStreamFallbackTrackId = updatedSong.id
                        _currentSong.value = updatedSong
                        _durationMs.value = updatedSong.durationMs
                        onSongUpdatedListener?.invoke(updatedSong)
                        onComplete?.invoke(true)
                        playViaMediaPlayer(resolvedMatch.streamUrl, updatedSong)
                    }
                    return@launch
                }
            } catch (e: Exception) {
                Log.w("AudioPlayerController", "Could not resolve stream for ${song.title}: ${e.message}")
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke(false)
                if (_currentSong.value?.id == song.id && _isPlaying.value) {
                    _isBuffering.value = false
                }
            }
        }
    }

    fun isCurrentSongYouTube(): Boolean {
        val song = _currentSong.value ?: return false
        // If an authentic direct audio stream was resolved for this song, route playback natively to MediaPlayer
        if (song.id == directStreamFallbackTrackId) {
            return false
        }
        return song.source.contains("YouTube", ignoreCase = true) ||
                song.id.startsWith("yt_") ||
                song.id.startsWith("ytm_") ||
                song.streamUrl.contains("youtube.com") ||
                song.streamUrl.contains("youtu.be")
    }

    fun extractYouTubeVideoId(song: Song): String {
        if (song.id.startsWith("ytm_") || song.id.startsWith("yt_")) {
            return song.id.substringAfter('_')
        }
        val url = song.streamUrl
        val vParam = url.substringAfter("v=", "").substringBefore('&')
        if (vParam.isNotBlank()) return vParam
        val beParam = url.substringAfter("youtu.be/", "").substringBefore('?')
        if (beParam.isNotBlank()) return beParam
        return song.id
    }

    private fun createNewMediaPlayer(): MediaPlayer {
        return MediaPlayer().apply {
            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setVolume(1.0f, 1.0f)
        }
    }

    private fun setupMediaPlayerListeners(player: MediaPlayer) {
        player.setOnPreparedListener { mp ->
            mediaPlayerState = MediaPlayerInternalState.PREPARED
            _isBuffering.value = false
            streamRecoveryAttempts = 0
            try {
                mp.setVolume(1.0f, 1.0f)
                _durationMs.value = mp.duration.toLong().coerceAtLeast(1L)
                if (pendingSeekPositionMs > 0L) {
                    mp.seekTo(pendingSeekPositionMs.toInt())
                    pendingSeekPositionMs = 0L
                }
                // Apply speed if customized, strictly maintaining pitch
                if (_playbackSpeed.value != 1.0f) {
                    try {
                        val params = PlaybackParams().apply {
                            speed = _playbackSpeed.value
                            pitch = 1.0f
                            audioFallbackMode = PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT
                        }
                        mp.playbackParams = params
                    } catch (e: Exception) {
                        Log.w("AudioPlayerController", "Could not apply initial speed", e)
                    }
                }
                if (playWhenReady) {
                    mp.start()
                    mediaPlayerState = MediaPlayerInternalState.STARTED
                    _isPlaying.value = true
                    startProgressTracker()
                } else {
                    mediaPlayerState = MediaPlayerInternalState.PAUSED
                    _isPlaying.value = false
                }
                applyEqualizerToSession(mp.audioSessionId)
            } catch (e: Exception) {
                Log.e("AudioPlayerController", "Error in onPrepared callback", e)
                mediaPlayerState = MediaPlayerInternalState.ERROR
            }
        }
        player.setOnInfoListener { _, what, _ ->
            when (what) {
                MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                    _isBuffering.value = true
                }
                MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                    _isBuffering.value = false
                }
            }
            false
        }
        player.setOnSeekCompleteListener { mp ->
            isSeeking = false
            try {
                _currentPositionMs.value = mp.currentPosition.toLong()
            } catch (e: Exception) {
                // Ignore
            }
        }
        player.setOnCompletionListener { mp ->
            _isBuffering.value = false
            val currentPos = try { mp.currentPosition.toLong() } catch (e: Exception) { _currentPositionMs.value }
            val duration = try { mp.duration.toLong() } catch (e: Exception) { _durationMs.value }
            // If current position is far from duration, stream was terminated prematurely by network/server EOF
            val isPremature = duration > 8000L && currentPos < (duration - 4000L)
            if (isPremature) {
                Log.w("AudioPlayerController", "Premature completion detected at pos=$currentPos / dur=$duration. Recovering stream...")
                attemptStreamRecovery(currentPos)
            } else {
                mediaPlayerState = MediaPlayerInternalState.COMPLETED
                handleTrackCompletion()
            }
        }
        player.setOnErrorListener { mp, what, extra ->
            Log.w("AudioPlayerController", "MediaPlayer error callback: what=$what, extra=$extra")
            _isBuffering.value = false

            // Check if error is network / IO / timeout related
            if (extra == -1004 || extra == -110 || extra == -1010 || what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                val lastPos = _currentPositionMs.value
                try {
                    mp.reset()
                    mediaPlayerState = MediaPlayerInternalState.IDLE
                } catch (e: Exception) {
                    mediaPlayer = null
                }
                attemptStreamRecovery(lastPos)
                return@setOnErrorListener true
            }

            mediaPlayerState = MediaPlayerInternalState.ERROR
            _isPlaying.value = false
            progressTrackerJob?.cancel()

            // Safely reset so native MediaPlayer does not stay permanently broken
            try {
                mp.reset()
                mediaPlayerState = MediaPlayerInternalState.IDLE
            } catch (e: Exception) {
                try {
                    mp.release()
                } catch (ignored: Exception) {}
                mediaPlayer = null
                mediaPlayerState = MediaPlayerInternalState.IDLE
            }

            true
        }
    }

    private fun attemptStreamRecovery(resumePositionMs: Long) {
        val song = _currentSong.value ?: return
        if (streamRecoveryAttempts >= 3) {
            Log.e("AudioPlayerController", "Max stream recovery attempts reached. Advancing track.")
            streamRecoveryAttempts = 0
            mediaPlayerState = MediaPlayerInternalState.COMPLETED
            handleTrackCompletion()
            return
        }
        streamRecoveryAttempts++
        _isBuffering.value = true
        pendingSeekPositionMs = resumePositionMs
        scope.launch {
            delay(500L * streamRecoveryAttempts)
            if (_currentSong.value?.id == song.id) {
                val localPath = song.localFilePath
                val isLocalFileUsable = localPath != null && File(localPath).exists() && File(localPath).length() > 5000
                val rawSource = if (isLocalFileUsable) localPath!! else song.streamUrl
                val targetSource = when (streamRecoveryAttempts) {
                    1 -> rawSource.replace("_320.mp4", "_160.mp4")
                    2 -> rawSource.replace("_320.mp4", "_96.mp4").replace("_160.mp4", "_96.mp4")
                    else -> rawSource
                }
                playViaMediaPlayer(targetSource, song)
            }
        }
    }

    private fun initMediaPlayer() {
        try {
            mediaPlayer?.let { mp ->
                try {
                    mp.setOnPreparedListener(null)
                    mp.setOnCompletionListener(null)
                    mp.setOnErrorListener(null)
                    mp.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            mediaPlayer = createNewMediaPlayer().also { setupMediaPlayerListeners(it) }
            mediaPlayerState = MediaPlayerInternalState.IDLE
        } catch (e: Exception) {
            e.printStackTrace()
            mediaPlayerState = MediaPlayerInternalState.ERROR
        }
    }

    private fun applyEqualizerToSession(audioSessionId: Int) {
        try {
            if (audioSessionId != 0) {
                equalizer?.release()
                bassBoost?.release()

                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = _equalizerState.value.isEnabled
                }
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = _equalizerState.value.isEnabled
                    if (strengthSupported) {
                        setStrength((_equalizerState.value.bassBoost * 10).toInt().toShort())
                    }
                }
            }
        } catch (e: Exception) {
            // Equalizer hardware effect may not be present on all emulators/devices; handled gracefully
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _queue.value = songs
        _currentIndex.value = startIndex.coerceIn(0, songs.size - 1)
        updateNextSong()
        playSong(songs[_currentIndex.value])
    }

    fun updateNextSong() {
        val q = _queue.value
        val idx = _currentIndex.value
        if (q.isEmpty() || idx !in q.indices) {
            _nextSong.value = null
            _autoPlaySuggestion.value = null
            _upcomingQueue.value = emptyList()
            return
        }

        val remainingFromQueue = if (idx + 1 < q.size) q.subList(idx + 1, q.size) else emptyList()

        if (_isShuffle.value) {
            val remainingIndices = q.indices.filter { it != idx }
            val nIndex = if (remainingIndices.isNotEmpty()) remainingIndices.random() else -1
            _nextSong.value = if (nIndex in q.indices) q[nIndex] else null
            _autoPlaySuggestion.value = null
            _upcomingQueue.value = remainingIndices.map { q[it] }
            return
        }

        if (remainingFromQueue.isNotEmpty()) {
            _nextSong.value = remainingFromQueue.first()
            _upcomingQueue.value = remainingFromQueue
            _autoPlaySuggestion.value = null
            // Pre-fetch auto-play recommendations if queue has 2 or fewer remaining songs
            if (remainingFromQueue.size <= 2 && _isAutoPlay.value) {
                fetchAutoPlaySuggestions(q[idx], q, appendToUpcoming = false)
            }
        } else {
            val suggestions = _autoPlaySuggestionsList.value.map { it.song }
            if (suggestions.isNotEmpty()) {
                _nextSong.value = suggestions.first()
                _upcomingQueue.value = suggestions
            } else {
                _nextSong.value = null
                _upcomingQueue.value = emptyList()
            }
            if (_isAutoPlay.value) {
                fetchAutoPlaySuggestions(q[idx], q, appendToUpcoming = true)
            } else if (_repeatMode.value == RepeatMode.ALL) {
                _nextSong.value = q[0]
                _upcomingQueue.value = q
                _autoPlaySuggestion.value = null
            } else {
                _autoPlaySuggestion.value = null
            }
        }
    }

    fun fetchAutoPlaySuggestions(current: Song?, currentQ: List<Song>, appendToUpcoming: Boolean = true) {
        autoPlayJob?.cancel()
        autoPlayJob = scope.launch {
            try {
                // Try multi-suggestion provider first
                val suggestions = autoPlaySuggestionsProvider?.invoke(current, currentQ)
                if (!suggestions.isNullOrEmpty()) {
                    _autoPlaySuggestionsList.value = suggestions
                    val first = suggestions.first()
                    _autoPlaySuggestion.value = first
                    if (appendToUpcoming || _upcomingQueue.value.isEmpty()) {
                        _nextSong.value = first.song
                        _upcomingQueue.value = suggestions.map { it.song }
                    }
                    return@launch
                }

                // Fallback to single suggestion provider
                val single = autoPlaySuggestionProvider?.invoke(current, currentQ)
                if (single != null) {
                    _autoPlaySuggestion.value = single
                    _autoPlaySuggestionsList.value = listOf(single)
                    if (appendToUpcoming || _upcomingQueue.value.isEmpty()) {
                        _nextSong.value = single.song
                        _upcomingQueue.value = listOf(single.song)
                    }
                } else if (_repeatMode.value == RepeatMode.ALL && currentQ.isNotEmpty()) {
                    _autoPlaySuggestion.value = null
                    _autoPlaySuggestionsList.value = emptyList()
                    _nextSong.value = currentQ[0]
                    _upcomingQueue.value = currentQ
                } else {
                    _autoPlaySuggestion.value = null
                    _autoPlaySuggestionsList.value = emptyList()
                    if (appendToUpcoming) {
                        _upcomingQueue.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.w("AudioPlayerController", "Error fetching auto-play suggestions: ${e.message}")
            }
        }
    }

    fun fetchAutoPlaySuggestion(current: Song?, currentQ: List<Song>) {
        fetchAutoPlaySuggestions(current, currentQ, appendToUpcoming = true)
    }

    fun refreshAutoPlaySuggestion() {
        fetchAutoPlaySuggestions(_currentSong.value, _queue.value, appendToUpcoming = true)
    }

    fun playAutoPlaySuggestion(song: Song) {
        val currentQ = _queue.value.toMutableList()
        val existingIndex = currentQ.indexOfFirst { it.id == song.id }
        if (existingIndex >= 0) {
            _currentIndex.value = existingIndex
            playSong(song)
        } else {
            currentQ.add(song)
            _queue.value = currentQ
            _currentIndex.value = currentQ.size - 1
            playSong(song)
        }
        _autoPlaySuggestionsList.value = _autoPlaySuggestionsList.value.filter { it.song.id != song.id }
        updateNextSong()
    }

    fun playFromUpcomingQueue(song: Song) {
        val q = _queue.value
        val matchIdx = q.indexOfFirst { it.id == song.id }
        if (matchIdx >= 0) {
            _currentIndex.value = matchIdx
            updateNextSong()
            playSong(q[matchIdx])
        } else {
            playAutoPlaySuggestion(song)
        }
    }

    fun removeFromUpcomingQueue(song: Song) {
        val q = _queue.value.toMutableList()
        val idx = _currentIndex.value
        val matchIdx = q.indexOfFirst { it.id == song.id }
        if (matchIdx > idx) {
            q.removeAt(matchIdx)
            _queue.value = q
            updateNextSong()
        } else {
            _autoPlaySuggestionsList.value = _autoPlaySuggestionsList.value.filter { it.song.id != song.id }
            _upcomingQueue.value = _upcomingQueue.value.filter { it.id != song.id }
            if (_nextSong.value?.id == song.id) {
                _nextSong.value = _upcomingQueue.value.firstOrNull()
            }
        }
    }

    fun addToQueue(song: Song) {
        val q = _queue.value.toMutableList()
        if (q.isEmpty()) {
            playQueue(listOf(song), 0)
        } else {
            q.add(song)
            _queue.value = q
            updateNextSong()
        }
    }

    fun setPlaybackStateForTesting(playing: Boolean) {
        _isPlaying.value = playing
        playWhenReady = playing
        mediaPlayerState = if (playing) MediaPlayerInternalState.STARTED else MediaPlayerInternalState.PAUSED
    }

    fun updateCurrentSongState(updatedSong: Song) {
        if (_currentSong.value?.id == updatedSong.id) {
            _currentSong.value = updatedSong
        }
    }

    fun playSong(song: Song) {
        val isDifferentSong = _currentSong.value?.id != song.id
        _currentSong.value = song
        _durationMs.value = song.durationMs.coerceAtLeast(1L)
        if (isDifferentSong) {
            _currentPositionMs.value = 0L
            pendingSeekPositionMs = 0L
            directStreamFallbackTrackId = null
        }
        _isBuffering.value = false
        streamRecoveryAttempts = 0
        ytStallJob?.cancel()

        val currentQ = _queue.value
        val existingIndex = currentQ.indexOfFirst { it.id == song.id }
        if (existingIndex >= 0) {
            _currentIndex.value = existingIndex
        } else {
            _queue.value = listOf(song)
            _currentIndex.value = 0
        }
        updateNextSong()

        if (isCurrentSongYouTube()) {
            try {
                if (mediaPlayer?.isPlaying == true || mediaPlayerState == MediaPlayerInternalState.STARTED || mediaPlayerState == MediaPlayerInternalState.PREPARING) {
                    mediaPlayer?.pause()
                    mediaPlayer?.stop()
                }
                mediaPlayer?.reset()
                mediaPlayerState = MediaPlayerInternalState.IDLE
            } catch (e: Exception) {
                // Ignore
            }
            progressTrackerJob?.cancel()

            val videoId = extractYouTubeVideoId(song)
            _isPlaying.value = false
            _isBuffering.value = true
            youTubeWebPlayer.loadAndPlay(videoId, true)
            startYouTubeStallCheck(song)
            return
        }

        // Native MediaPlayer flow for JioSaavn / local tracks
        youTubeWebPlayer.pause()
        youTubeWebPlayer.stop()

        val localPath = song.localFilePath
        val isLocalFileUsable = localPath != null && File(localPath).exists() && File(localPath).length() > 5000
        val targetSource = if (isLocalFileUsable) localPath!! else song.streamUrl

        if (targetSource.isBlank()) {
            Log.e("AudioPlayerController", "Cannot play track with empty stream URL: ${song.title}")
            _isPlaying.value = false
            return
        }

        playViaMediaPlayer(targetSource, song)
    }

    private fun playViaMediaPlayer(dataSource: String, song: Song) {
        if (simulateMediaPlayerForTesting) {
            mediaPlayerState = MediaPlayerInternalState.STARTED
            _isPlaying.value = true
            _durationMs.value = song.durationMs.coerceAtLeast(1L)
            _isBuffering.value = false
            return
        }
        try {
            playWhenReady = true
            _isPlaying.value = true
            progressTrackerJob?.cancel()

            var player = mediaPlayer
            if (player == null || mediaPlayerState == MediaPlayerInternalState.ERROR) {
                initMediaPlayer()
                player = mediaPlayer
            } else {
                try {
                    player.setOnPreparedListener(null)
                    player.setOnCompletionListener(null)
                    player.setOnErrorListener(null)
                    if (mediaPlayerState == MediaPlayerInternalState.STARTED) {
                        player.stop()
                    }
                    player.reset()
                    mediaPlayerState = MediaPlayerInternalState.IDLE
                } catch (e: Exception) {
                    initMediaPlayer()
                    player = mediaPlayer
                }
            }

            if (player != null) {
                setupMediaPlayerListeners(player)
                player.setVolume(1.0f, 1.0f)
                val cleanSource = dataSource
                    .replace("http://aac.saavn.cdn.jiosaavn.com", "https://aac.saavncdn.com")
                    .replace("https://aac.saavn.cdn.jiosaavn.com", "https://aac.saavncdn.com")
                    .replace("http://", "https://")
                if (cleanSource.startsWith("http://") || cleanSource.startsWith("https://")) {
                    val headers = HashMap<String, String>().apply {
                        put("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        put("Accept", "*/*")
                        put("Connection", "keep-alive")
                    }
                    player.setDataSource(context, Uri.parse(cleanSource), headers)
                } else {
                    player.setDataSource(cleanSource)
                }
                mediaPlayerState = MediaPlayerInternalState.INITIALIZED
                mediaPlayerState = MediaPlayerInternalState.PREPARING
                player.prepareAsync()
            } else {
                throw IllegalStateException("MediaPlayer could not be initialized")
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerController", "Failed to prepare MediaPlayer for ${song.title}", e)
            mediaPlayerState = MediaPlayerInternalState.ERROR
            _durationMs.value = song.durationMs
            _isPlaying.value = false
        }
    }

    fun pause() {
        playWhenReady = false
        _isPlaying.value = false
        _isBuffering.value = false
        ytStallJob?.cancel()
        progressTrackerJob?.cancel()

        // Halt YouTube player immediately
        youTubeWebPlayer.pause()

        if (simulateMediaPlayerForTesting) {
            mediaPlayerState = MediaPlayerInternalState.PAUSED
            return
        }

        // Halt Native MediaPlayer immediately
        try {
            if (mediaPlayer?.isPlaying == true ||
                mediaPlayerState == MediaPlayerInternalState.STARTED ||
                mediaPlayerState == MediaPlayerInternalState.PREPARING ||
                mediaPlayerState == MediaPlayerInternalState.PREPARED) {
                mediaPlayer?.pause()
            }
            mediaPlayerState = MediaPlayerInternalState.PAUSED
        } catch (e: Exception) {
            Log.w("AudioPlayerController", "Error pausing MediaPlayer: ${e.message}")
            mediaPlayerState = MediaPlayerInternalState.PAUSED
        }
    }

    fun resume() {
        val song = _currentSong.value ?: return
        playWhenReady = true
        _isPlaying.value = true

        if (isCurrentSongYouTube()) {
            if (_currentPositionMs.value == 0L && !_isBuffering.value) {
                _isBuffering.value = true
                val videoId = extractYouTubeVideoId(song)
                youTubeWebPlayer.loadAndPlay(videoId, true)
                startYouTubeStallCheck(song)
            } else {
                youTubeWebPlayer.resume()
            }
            return
        }

        if (simulateMediaPlayerForTesting) {
            mediaPlayerState = MediaPlayerInternalState.STARTED
            return
        }

        when (mediaPlayerState) {
            MediaPlayerInternalState.PREPARING -> {
                // playWhenReady = true will automatically start once onPrepared completes
            }
            MediaPlayerInternalState.PREPARED,
            MediaPlayerInternalState.PAUSED,
            MediaPlayerInternalState.COMPLETED -> {
                try {
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                    mediaPlayer?.start()
                    mediaPlayerState = MediaPlayerInternalState.STARTED
                    startProgressTracker()
                } catch (e: Exception) {
                    Log.e("AudioPlayerController", "Error starting MediaPlayer, replaying", e)
                    mediaPlayerState = MediaPlayerInternalState.ERROR
                    playSong(song)
                }
            }
            else -> {
                playSong(song)
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        pendingSeekPositionMs = positionMs
        isSeeking = true
        lastSeekTimestamp = System.currentTimeMillis()
        if (simulateMediaPlayerForTesting) {
            isSeeking = false
            return
        }
        if (isCurrentSongYouTube()) {
            youTubeWebPlayer.seekTo(positionMs / 1000f)
            isSeeking = false
            return
        }
        when (mediaPlayerState) {
            MediaPlayerInternalState.PREPARING -> {
                pendingSeekPositionMs = positionMs
            }
            MediaPlayerInternalState.PREPARED,
            MediaPlayerInternalState.STARTED,
            MediaPlayerInternalState.PAUSED,
            MediaPlayerInternalState.COMPLETED -> {
                try {
                    mediaPlayer?.seekTo(positionMs.toInt())
                } catch (e: Exception) {
                    Log.w("AudioPlayerController", "Error seeking MediaPlayer: ${e.message}")
                    isSeeking = false
                }
            }
            else -> {
                isSeeking = false
            }
        }
    }

    fun skipToNext() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        if (_isShuffle.value) {
            val remaining = currentQ.indices.filter { it != _currentIndex.value }
            val nextIndex = if (remaining.isNotEmpty()) remaining.random() else 0
            _currentIndex.value = nextIndex
            updateNextSong()
            playSong(currentQ[nextIndex])
            return
        }

        if (_currentIndex.value + 1 < currentQ.size) {
            _currentIndex.value = _currentIndex.value + 1
            updateNextSong()
            playSong(currentQ[_currentIndex.value])
        } else if (_isAutoPlay.value) {
            val nextSuggestion = _autoPlaySuggestionsList.value.firstOrNull()?.song ?: _autoPlaySuggestion.value?.song
            if (nextSuggestion != null) {
                playAutoPlaySuggestion(nextSuggestion)
            } else {
                scope.launch {
                    val freshList = autoPlaySuggestionsProvider?.invoke(_currentSong.value, currentQ)
                    val fresh = freshList?.firstOrNull() ?: autoPlaySuggestionProvider?.invoke(_currentSong.value, currentQ)
                    if (fresh != null) {
                        playAutoPlaySuggestion(fresh.song)
                    } else if (_repeatMode.value == RepeatMode.ALL) {
                        _currentIndex.value = 0
                        updateNextSong()
                        playSong(currentQ[0])
                    } else {
                        _isPlaying.value = false
                        seekTo(0)
                    }
                }
            }
        } else if (_repeatMode.value == RepeatMode.ALL) {
            _currentIndex.value = 0
            updateNextSong()
            playSong(currentQ[0])
        } else {
            _isPlaying.value = false
            seekTo(0)
        }
    }

    fun skipToPrevious() {
        val currentQ = _queue.value
        if (currentQ.isEmpty()) return

        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = if (_currentIndex.value - 1 < 0) currentQ.size - 1 else _currentIndex.value - 1
        _currentIndex.value = prevIndex
        updateNextSong()
        playSong(currentQ[prevIndex])
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        updateNextSong()
    }

    fun toggleAutoPlay() {
        _isAutoPlay.value = !_isAutoPlay.value
        updateNextSong()
    }

    fun setAutoPlay(enabled: Boolean) {
        _isAutoPlay.value = enabled
        updateNextSong()
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        updateNextSong()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        if (isCurrentSongYouTube()) {
            youTubeWebPlayer.setPlaybackRate(speed)
            return
        }
        try {
            if (mediaPlayerState == MediaPlayerInternalState.STARTED || mediaPlayerState == MediaPlayerInternalState.PAUSED) {
                mediaPlayer?.let {
                    val params = PlaybackParams().apply {
                        this.speed = speed
                        this.pitch = 1.0f
                        this.audioFallbackMode = PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT
                    }
                    it.playbackParams = params
                }
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerController", "Could not set playback speed: ${e.message}")
        }
    }

    fun toggleLowBatteryMode() {
        _isLowBatteryMode.value = !_isLowBatteryMode.value
    }

    fun updateEqualizer(state: EqualizerState) {
        _equalizerState.value = state
        try {
            equalizer?.enabled = state.isEnabled
            bassBoost?.enabled = state.isEnabled
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength((state.bassBoost * 10).toInt().toShort())
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    fun applyPreset(presetName: String) {
        val preset = EqualizerState.PRESETS[presetName] ?: return
        updateEqualizer(preset)
    }

    private fun handleTrackCompletion() {
        _currentSong.value?.let { onSongCompletedListener?.invoke(it) }
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentSong.value?.let { playSong(it) }
            }
            RepeatMode.ALL -> {
                skipToNext()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value < _queue.value.size - 1) {
                    skipToNext()
                } else if (_isAutoPlay.value && _queue.value.isNotEmpty()) {
                    skipToNext()
                } else {
                    _isPlaying.value = false
                    seekTo(0)
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    val timeSinceSeek = System.currentTimeMillis() - lastSeekTimestamp
                    if (timeSinceSeek > 600L && !isSeeking) {
                        if (mediaPlayerState == MediaPlayerInternalState.STARTED || mediaPlayerState == MediaPlayerInternalState.PAUSED) {
                            val pos = mediaPlayer?.currentPosition?.toLong() ?: _currentPositionMs.value
                            if (pos >= 0L) {
                                _currentPositionMs.value = pos
                            }
                            val dur = mediaPlayer?.duration?.toLong() ?: _durationMs.value
                            if (dur > 0) _durationMs.value = dur
                        }
                    }
                } catch (e: Exception) {
                    // Ignore state race exceptions
                }
                val delayTime = if (_isLowBatteryMode.value) 1000L else 250L
                delay(delayTime)
            }
        }
    }

    fun release() {
        progressTrackerJob?.cancel()
        youTubeWebPlayer.release()
        equalizer?.release()
        bassBoost?.release()
        try {
            mediaPlayer?.setOnPreparedListener(null)
            mediaPlayer?.setOnCompletionListener(null)
            mediaPlayer?.setOnErrorListener(null)
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        mediaPlayerState = MediaPlayerInternalState.IDLE
    }
}
