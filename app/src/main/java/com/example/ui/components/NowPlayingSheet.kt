package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.Song
import com.example.data.network.LyricsService
import com.example.data.network.SongLyrics
import com.example.data.repository.AutoPlayMode
import com.example.data.repository.AutoPlaySuggestion
import com.example.playback.RepeatMode
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.NeonPink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    playbackSpeed: Float = 1.0f,
    isBuffering: Boolean = false,
    isAutoPlay: Boolean = true,
    nextSong: Song? = null,
    upcomingQueue: List<Song> = emptyList(),
    autoPlaySuggestions: List<AutoPlaySuggestion> = emptyList(),
    autoPlaySuggestion: AutoPlaySuggestion? = null,
    autoPlayMode: AutoPlayMode = AutoPlayMode.SMART_MIX,
    onSetAutoPlayMode: (AutoPlayMode) -> Unit = {},
    onRefreshAutoPlaySuggestion: () -> Unit = {},
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoPlay: () -> Unit = {},
    onPlayNextSong: (Song) -> Unit = {},
    onPlayFromUpcomingQueue: (Song) -> Unit = onPlayNextSong,
    onRemoveFromQueue: (Song) -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onCacheSong: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onDismiss: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isUserSeeking by remember { mutableStateOf(false) }
    var userSeekFraction by remember { mutableFloatStateOf(0f) }

    // Tab state: false = Vinyl artwork, true = Lyrics
    var showLyricsView by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<SongLyrics?>(null) }
    var isLoadingLyrics by remember { mutableStateOf(false) }
    var lyricsFetchTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(song.id, lyricsFetchTrigger) {
        isLoadingLyrics = true
        lyrics = LyricsService.fetchLyrics(song.title, song.artist, song.durationMs)
        isLoadingLyrics = false
    }

    val displayQueue = remember(upcomingQueue, autoPlaySuggestions, nextSong) {
        val list = mutableListOf<Song>()
        if (upcomingQueue.isNotEmpty()) {
            list.addAll(upcomingQueue)
        } else if (autoPlaySuggestions.isNotEmpty()) {
            list.addAll(autoPlaySuggestions.map { it.song })
        } else if (nextSong != null) {
            list.add(nextSong)
        }
        list.distinctBy { it.id }
    }

    val suggestionMap = remember(autoPlaySuggestions, autoPlaySuggestion) {
        val map = mutableMapOf<String, AutoPlaySuggestion>()
        autoPlaySuggestions.forEach { map[it.song.id] = it }
        if (autoPlaySuggestion != null) {
            map[autoPlaySuggestion.song.id] = autoPlaySuggestion
        }
        map
    }

    val actualFraction = (currentPositionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val sliderValue = if (isUserSeeking) userSeekFraction else actualFraction

    // Vinyl rotation animation with GPU graphics layer
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "spin"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = appTheme.backgroundPrimary,
        dragHandle = null,
        modifier = Modifier.fillMaxSize().testTag("now_playing_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (appTheme.isLight) {
                            listOf(appTheme.backgroundSecondary, appTheme.backgroundPrimary, appTheme.backgroundPrimary)
                        } else {
                            listOf(Color(0xFF13182E), appTheme.backgroundPrimary, appTheme.backgroundPrimary)
                        }
                    )
                )
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(48.dp).testTag("close_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = appTheme.textPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // View Toggle Pill (Cover vs Lyrics)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = appTheme.surfaceGlass,
                    border = BorderStroke(1.dp, appTheme.cardBorder),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showLyricsView = false },
                            shape = RoundedCornerShape(50),
                            color = if (!showLyricsView) appTheme.accent else Color.Transparent
                        ) {
                            Text(
                                text = "Cover",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (!showLyricsView) (if (appTheme.isLight) Color.White else Color.Black) else appTheme.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            onClick = { showLyricsView = true },
                            shape = RoundedCornerShape(50),
                            color = if (showLyricsView) appTheme.accent else Color.Transparent
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (showLyricsView) (if (appTheme.isLight) Color.White else Color.Black) else appTheme.textSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Lyrics",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showLyricsView) (if (appTheme.isLight) Color.White else Color.Black) else appTheme.textSecondary
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Listening to ${song.title}")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🎧 Currently streaming on Muesic:\n\"${song.title}\" by ${song.artist}\n[${song.audioQuality}] • Private & Ad-free."
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                    },
                    modifier = Modifier.size(48.dp).testTag("share_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = appTheme.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Center Display: Either Vinyl Visualizer OR Interactive Lyrics Sheet
            if (!showLyricsView) {
                // Vinyl Visualizer Art
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x3300F5D4),
                                    Color(0x227928CA),
                                    Color(0xFF0F1322)
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                2.dp,
                                Brush.sweepGradient(listOf(appTheme.accent, appTheme.accentSecondary, NeonPink, appTheme.accent))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .graphicsLayer {
                                rotationZ = if (isPlaying) rotation else 0f
                            }
                            .clip(CircleShape)
                            .background(Color(0xFF0C0F1A))
                            .border(1.dp, Color(0x22FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0x18FFFFFF), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF1E1538), Color(0xFF0B2433))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!song.artworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = song.artworkUrl,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(104.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(104.dp)
                                        .background(Color(0x66000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AudioVisualizerWave(
                                        isPlaying = isPlaying,
                                        barCount = 10,
                                        maxHeight = 32.dp,
                                        barWidth = 3.dp
                                    )
                                }
                            } else {
                                AudioVisualizerWave(
                                    isPlaying = isPlaying,
                                    barCount = 12,
                                    maxHeight = 36.dp,
                                    barWidth = 3.dp
                                )
                            }
                        }
                    }
                }
            } else {
                // Interactive Lyrics View
                LyricsPlayerDisplay(
                    lyrics = lyrics,
                    isLoading = isLoadingLyrics,
                    currentPositionMs = currentPositionMs,
                    onSeekToLyric = onSeek,
                    onRetry = { lyricsFetchTrigger++ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track Title & Artist
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                color = appTheme.textPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = appTheme.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quality Badge
            HifiBadge(quality = song.audioQuality)

            Spacer(modifier = Modifier.height(16.dp))

            // Scrubber
            Slider(
                value = sliderValue,
                onValueChange = {
                    isUserSeeking = true
                    userSeekFraction = it
                },
                onValueChangeFinished = {
                    val targetMs = (userSeekFraction * durationMs).toLong()
                    onSeek(targetMs)
                    coroutineScope.launch {
                        delay(400)
                        isUserSeeking = false
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = appTheme.accent,
                    activeTrackColor = appTheme.accent,
                    inactiveTrackColor = if (appTheme.isLight) Color(0x22000000) else Color(0x2BFFFFFF)
                ),
                modifier = Modifier.fillMaxWidth().testTag("playback_scrubber")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentSec = if (isUserSeeking) (userSeekFraction * durationMs / 1000).toLong() else (currentPositionMs / 1000)
                val totalSec = durationMs / 1000
                Text(
                    text = formatTime(currentSec),
                    style = MaterialTheme.typography.labelSmall,
                    color = appTheme.textSecondary
                )
                Text(
                    text = "-${formatTime((totalSec - currentSec).coerceAtLeast(0))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = appTheme.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(48.dp).testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) appTheme.accent else appTheme.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp).testTag("prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = appTheme.textPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause FAB
                FilledIconButton(
                    onClick = onPlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = appTheme.accent),
                    modifier = Modifier.size(68.dp).testTag("main_play_pause_button")
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = if (appTheme.isLight) Color.White else Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (scaleIn(tween(220)) + fadeIn(tween(220)))
                                    .togetherWith(scaleOut(tween(160)) + fadeOut(tween(160)))
                            },
                            label = "play_pause_fab"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = if (appTheme.isLight) Color.White else Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp).testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = appTheme.textPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.size(48.dp).testTag("repeat_button")
                ) {
                    val icon = when (repeatMode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val tint = if (repeatMode != RepeatMode.OFF) appTheme.accent else appTheme.textSecondary
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action toolbar: Equalizer, Offline Cache, Speed, Lyrics, Favorite
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = appTheme.surfaceGlass,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Equalizer
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.size(44.dp).testTag("open_equalizer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = appTheme.accent
                        )
                    }

                    // Add to Playlist
                    IconButton(
                        onClick = onAddToPlaylist,
                        modifier = Modifier.size(44.dp).testTag("add_to_playlist_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = appTheme.textPrimary
                        )
                    }

                    // Lyrics Toggle Icon
                    IconButton(
                        onClick = { showLyricsView = !showLyricsView },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Lyrics",
                            tint = if (showLyricsView) appTheme.accent else appTheme.textSecondary
                        )
                    }

                    // Cache / Offline button
                    IconButton(
                        onClick = onCacheSong,
                        modifier = Modifier.size(44.dp).testTag("cache_song_button")
                    ) {
                        Icon(
                            imageVector = if (song.isCached) Icons.Default.DownloadDone else Icons.Default.FileDownload,
                            contentDescription = if (song.isCached) "Cached Offline" else "Save Offline",
                            tint = if (song.isCached) Color(0xFF10B981) else appTheme.textSecondary
                        )
                    }

                    // Favorite
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(44.dp).testTag("favorite_button")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) NeonPink else appTheme.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // UP NEXT & AUTO PLAY Section under player (Requirement 1)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .testTag("up_next_section"),
                backgroundColor = appTheme.surfaceGlass,
                borderBrush = Brush.linearGradient(
                    if (isAutoPlay) listOf(appTheme.accent.copy(alpha = 0.4f), appTheme.accent.copy(alpha = 0.2f))
                    else listOf(appTheme.cardBorder, appTheme.cardBorder.copy(alpha = 0.5f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = if (isAutoPlay) appTheme.accent else appTheme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UP NEXT",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAutoPlay) appTheme.accent else appTheme.textSecondary,
                                letterSpacing = 1.sp
                            )
                            if (displayQueue.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(appTheme.accent.copy(alpha = 0.2f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${displayQueue.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = appTheme.accent
                                    )
                                }
                            }
                            if (isAutoPlay) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = onRefreshAutoPlaySuggestion,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Recommendation",
                                        tint = appTheme.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Auto Play toggle chip
                        Surface(
                            onClick = onToggleAutoPlay,
                            shape = RoundedCornerShape(50),
                            color = if (isAutoPlay) appTheme.accent.copy(alpha = 0.2f) else appTheme.surfaceGlass,
                            border = BorderStroke(1.dp, if (isAutoPlay) appTheme.accent else appTheme.cardBorder),
                            modifier = Modifier.testTag("auto_play_toggle")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isAutoPlay) appTheme.accent else appTheme.textMuted)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAutoPlay) "Auto-Play ON" else "Auto-Play OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAutoPlay) appTheme.accent else appTheme.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isAutoPlay) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Suggestion Strategy Mode selector: Smart Mix, Related, History
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(AutoPlayMode.SMART_MIX, "Smart Mix", Icons.Default.AutoAwesome),
                                Triple(AutoPlayMode.RELATED_SONGS, "Related", Icons.Default.QueueMusic),
                                Triple(AutoPlayMode.USER_HISTORY, "History & Likes", Icons.Default.Favorite)
                            ).forEach { (mode, label, icon) ->
                                val isSelected = autoPlayMode == mode
                                Surface(
                                    onClick = { onSetAutoPlayMode(mode) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) appTheme.accent.copy(alpha = 0.2f) else appTheme.surfaceGlass,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) appTheme.accent else appTheme.cardBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) appTheme.accent else appTheme.textMuted,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) appTheme.accent else appTheme.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (displayQueue.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayQueue.forEachIndexed { index, queueSong ->
                                val sugg = suggestionMap[queueSong.id]
                                val isImmediateNext = index == 0
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isImmediateNext) appTheme.accent.copy(alpha = 0.15f) else appTheme.surfaceGlass,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isImmediateNext) appTheme.accent.copy(alpha = 0.4f) else appTheme.cardBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(if (isImmediateNext) "up_next_song_card" else "upcoming_queue_item_${queueSong.id}")
                                        .clickable { onPlayFromUpcomingQueue(queueSong) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Rank Pill
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isImmediateNext) appTheme.accent else appTheme.cardBorder
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isImmediateNext) (if (appTheme.isLight) Color.White else Color.Black) else appTheme.textSecondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Artwork
                                        if (!queueSong.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = queueSong.artworkUrl,
                                                contentDescription = queueSong.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(appTheme.surfaceGlass),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = appTheme.accent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Title, Artist, & Suggestion Reason Badge
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = queueSong.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = appTheme.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = queueSong.artist,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = appTheme.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (queueSong.genre.isNotBlank()) {
                                                    Text(
                                                        text = " • ${queueSong.genre}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = appTheme.textMuted,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            if (sugg != null) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (sugg.isFromHistory) Color(0x28FF007F) else appTheme.accent.copy(alpha = 0.2f)
                                                        )
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (sugg.isFromHistory) Icons.Default.Favorite else Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = if (sugg.isFromHistory) NeonPink else appTheme.accent,
                                                        modifier = Modifier.size(9.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = sugg.reason,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (sugg.isFromHistory) NeonPink else appTheme.accent,
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // Play action button
                                        IconButton(
                                            onClick = { onPlayFromUpcomingQueue(queueSong) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Play Track Now",
                                                tint = appTheme.accent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // Remove from queue action button
                                        IconButton(
                                            onClick = { onRemoveFromQueue(queueSong) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove Track From Queue",
                                                tint = appTheme.textMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isAutoPlay) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = appTheme.accent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Finding music related to ${song.artist} or based on your history...",
                                style = MaterialTheme.typography.bodySmall,
                                color = appTheme.textMuted
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = appTheme.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Queue is empty. Turn on Auto-Play to automatically keep playing music.",
                                style = MaterialTheme.typography.bodySmall,
                                color = appTheme.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsPlayerDisplay(
    lyrics: SongLyrics?,
    isLoading: Boolean,
    currentPositionMs: Long,
    onSeekToLyric: (Long) -> Unit,
    onRetry: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(appTheme.surfaceGlass)
            .border(BorderStroke(1.dp, appTheme.cardBorder), RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = appTheme.accent, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Fetching synced lyrics from LrcLib...",
                    style = MaterialTheme.typography.bodySmall,
                    color = appTheme.textSecondary
                )
            }
        } else if (lyrics == null || (lyrics.lines.isEmpty() && lyrics.plainText.isBlank())) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = appTheme.textMuted, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No lyrics available for this track.", style = MaterialTheme.typography.bodyMedium, color = appTheme.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = onRetry,
                    shape = RoundedCornerShape(50),
                    color = appTheme.accent.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = appTheme.accent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", style = MaterialTheme.typography.labelSmall, color = appTheme.accent)
                    }
                }
            }
        } else if (lyrics.isSynced && lyrics.lines.isNotEmpty()) {
            val lines = lyrics.lines
            val activeIndex = lines.indexOfLast { it.timestampMs <= currentPositionMs }.coerceAtLeast(0)
            val listState = rememberLazyListState()

            LaunchedEffect(activeIndex) {
                if (activeIndex >= 0) {
                    listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(lines) { index, line ->
                    val isActive = index == activeIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) appTheme.accent.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onSeekToLyric(line.timestampMs) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = line.text,
                            style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) appTheme.accent else appTheme.textSecondary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Plain text lyrics
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = lyrics.plainText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = appTheme.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}
