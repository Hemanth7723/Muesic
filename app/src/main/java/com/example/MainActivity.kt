package com.example

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.Playlist
import com.example.data.entity.Song
import com.example.data.repository.AutoPlayMode
import com.example.data.repository.AutoPlaySuggestion
import com.example.ui.MusicViewModel
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.ImportExportDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.PlaylistDetailDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.MuesicTheme

enum class AppDestination(val label: String) {
    HOME("Discover"),
    SEARCH("Search"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            MuesicTheme(themeMode = themeMode) {
                MuesicApp(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.playerController.youTubeWebPlayer.release()
    }
}

@Composable
fun MuesicApp(viewModel: MusicViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.HOME) }

    // Dialog & Sheet States
    var showNowPlayingSheet by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    // Collect View Model States
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val cachedSongs by viewModel.cachedSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val librarySavedSongs by viewModel.librarySavedSongs.collectAsStateWithLifecycle()
    val lastPlayedSongs by viewModel.lastPlayedSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    // Playback States
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isLowBatteryMode by viewModel.isLowBatteryMode.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val isAutoPlay by viewModel.isAutoPlay.collectAsStateWithLifecycle()
    val nextSong by viewModel.nextSong.collectAsStateWithLifecycle()
    val upcomingQueue by viewModel.upcomingQueue.collectAsStateWithLifecycle()
    val autoPlaySuggestionsList by viewModel.autoPlaySuggestionsList.collectAsStateWithLifecycle()
    val autoPlaySuggestion by viewModel.autoPlaySuggestion.collectAsStateWithLifecycle()
    val autoPlayMode by viewModel.autoPlayMode.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()

    // Search & Stream States
    val onlineSearchResults by viewModel.onlineSearchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val activeSearchSource by viewModel.searchSource.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    // 6 New Discover Suggestions Sections (powered by JioSaavn)
    val trendingSongs by viewModel.trendingSongs.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedTopSongsDimension by viewModel.selectedTopSongsDimension.collectAsStateWithLifecycle()
    val topSongsFilters by viewModel.topSongsFilters.collectAsStateWithLifecycle()
    val selectedTopSongFilter by viewModel.selectedTopSongFilter.collectAsStateWithLifecycle()
    val topSongsList by viewModel.topSongsList.collectAsStateWithLifecycle()
    val movieReleases by viewModel.movieReleases.collectAsStateWithLifecycle()
    val recommendedArtists by viewModel.recommendedArtists.collectAsStateWithLifecycle()
    val topMixes by viewModel.topMixes.collectAsStateWithLifecycle()
    val isDiscoverRefreshing by viewModel.isDiscoverRefreshing.collectAsStateWithLifecycle()
    val lastDiscoverRefreshedTime by viewModel.lastDiscoverRefreshedTime.collectAsStateWithLifecycle()

    // Whenever app returns to foreground, refresh Discover if more than 5 minutes have elapsed
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (System.currentTimeMillis() - viewModel.lastDiscoverRefreshedTime.value > 5 * 60 * 1000L) {
            viewModel.refreshDiscover(isUserTriggered = false)
        }
    }

    val appTheme = LocalAppThemeColors.current

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = appTheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini Player Bar when song is loaded with liquid entry/exit spring animation
                    AnimatedVisibility(
                        visible = currentSong != null,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(280)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeOut(animationSpec = tween(200))
                    ) {
                        val miniSong = currentSong
                        if (miniSong != null) {
                            MiniPlayerBar(
                                song = miniSong,
                                isPlaying = isPlaying,
                                progressMs = currentPositionMs,
                                durationMs = durationMs,
                                isBuffering = isBuffering,
                                onPlayPause = { viewModel.togglePlayPause() },
                                onNext = { viewModel.skipToNext() },
                                onClick = { showNowPlayingSheet = true },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Navigation Bar
                    NavigationBar(
                        containerColor = appTheme.surfaceGlass,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("main_navigation_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.HOME,
                            onClick = { currentDestination = AppDestination.HOME },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Discover") },
                            label = { Text(AppDestination.HOME.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (appTheme.isLight) Color.White else MidnightBlack,
                                indicatorColor = appTheme.accent,
                                unselectedIconColor = appTheme.textMuted,
                                selectedTextColor = appTheme.accent,
                                unselectedTextColor = appTheme.textMuted
                            ),
                            modifier = Modifier.testTag("nav_item_home")
                        )

                        NavigationBarItem(
                            selected = currentDestination == AppDestination.SEARCH,
                            onClick = { currentDestination = AppDestination.SEARCH },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text(AppDestination.SEARCH.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (appTheme.isLight) Color.White else MidnightBlack,
                                indicatorColor = appTheme.accent,
                                unselectedIconColor = appTheme.textMuted,
                                selectedTextColor = appTheme.accent,
                                unselectedTextColor = appTheme.textMuted
                            ),
                            modifier = Modifier.testTag("nav_item_search")
                        )

                        NavigationBarItem(
                            selected = currentDestination == AppDestination.LIBRARY,
                            onClick = { currentDestination = AppDestination.LIBRARY },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                            label = { Text(AppDestination.LIBRARY.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (appTheme.isLight) Color.White else MidnightBlack,
                                indicatorColor = appTheme.accent,
                                unselectedIconColor = appTheme.textMuted,
                                selectedTextColor = appTheme.accent,
                                unselectedTextColor = appTheme.textMuted
                            ),
                            modifier = Modifier.testTag("nav_item_library")
                        )

                        NavigationBarItem(
                            selected = currentDestination == AppDestination.SETTINGS,
                            onClick = { currentDestination = AppDestination.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text(AppDestination.SETTINGS.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (appTheme.isLight) Color.White else MidnightBlack,
                                indicatorColor = appTheme.accent,
                                unselectedIconColor = appTheme.textMuted,
                                selectedTextColor = appTheme.accent,
                                unselectedTextColor = appTheme.textMuted
                            ),
                            modifier = Modifier.testTag("nav_item_settings")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            appTheme.backgroundGradientTop,
                            appTheme.backgroundGradientBottom,
                            appTheme.backgroundGradientBottom
                        )
                    )
                )
        ) {
            // Background audio playback engine for YouTube and YouTube Music.
            // Strictly positioned off-screen and non-interfering so no video, player, or error UI is ever superimposed on the screen.
            Box(
                modifier = Modifier
                    .offset(x = (-2000).dp, y = (-2000).dp)
                    .size(240.dp, 240.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        try {
                            val wv = viewModel.playerController.youTubeWebPlayer.initWebView(ctx)
                            if (wv != null) {
                                (wv.parent as? ViewGroup)?.removeView(wv)
                                wv.onResume()
                                wv.resumeTimers()
                                wv
                            } else {
                                android.view.View(ctx)
                            }
                        } catch (e: Throwable) {
                            android.view.View(ctx)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetX = { fullWidth -> (fullWidth * 0.22f * direction).toInt() }
                    ) + fadeIn(
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            targetOffsetX = { fullWidth -> (-fullWidth * 0.22f * direction).toInt() }
                        ) + fadeOut(
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        )
                    )
                },
                label = "liquid_screen_nav",
                modifier = Modifier.fillMaxSize()
            ) { destination ->
                when (destination) {
                    AppDestination.HOME -> {
                        HomeScreen(
                            recommendations = recommendations,
                            playlists = playlists,
                            lastPlayedSongs = lastPlayedSongs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            isLowBatteryMode = isLowBatteryMode,
                            trendingSongs = trendingSongs,
                            categories = categories,
                            selectedTopSongsDimension = selectedTopSongsDimension,
                            topSongsFilters = topSongsFilters,
                            selectedTopSongFilter = selectedTopSongFilter,
                            topSongsList = topSongsList,
                            onSelectTopSongsDimension = { dim -> viewModel.selectTopSongsDimension(dim) },
                            onSelectTopSongFilter = { filter -> viewModel.selectTopSongFilter(filter) },
                            movieReleases = movieReleases,
                            recommendedArtists = recommendedArtists,
                            topMixes = topMixes,
                            isRefreshing = isDiscoverRefreshing,
                            onRefresh = { viewModel.refreshDiscover(isUserTriggered = true) },
                            lastRefreshedTime = lastDiscoverRefreshedTime,
                            onPlaySongsQueue = { songs, index -> viewModel.playSongsQueue(songs, index) },
                            onSongClick = { song -> viewModel.playSong(song) },
                            onPlaylistClick = { playlist -> selectedPlaylistForDetail = playlist },
                            onOpenEqualizer = { showEqualizerDialog = true },
                            onToggleLowBattery = { viewModel.toggleLowBatteryMode() },
                            onAddToPlaylist = { song -> songToAddToPlaylist = song }
                        )
                    }

                    AppDestination.SEARCH -> {
                        SearchScreen(
                            allSongs = allSongs,
                            onlineSearchResults = onlineSearchResults,
                            isSearching = isSearching,
                            searchError = searchError,
                            activeSource = activeSearchSource,
                            searchHistory = searchHistory,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onSearch = { q -> viewModel.searchOnline(q, saveToHistory = false) },
                            onExecuteSearch = { q -> viewModel.searchOnline(q, saveToHistory = true) },
                            onSelectSource = { src -> viewModel.setSearchSource(src) },
                            onDeleteHistoryItem = { keyword -> viewModel.deleteSearchHistoryKeyword(keyword) },
                            onClearHistory = { viewModel.clearAllSearchHistory() },
                            onSongClick = { song -> viewModel.playSong(song) },
                            onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                            onCacheSong = { song -> viewModel.cacheSong(song) },
                            onAddToPlaylist = { song -> songToAddToPlaylist = song }
                        )
                    }

                    AppDestination.LIBRARY -> {
                        LibraryScreen(
                            allSongs = librarySavedSongs,
                            cachedSongs = cachedSongs,
                            favoriteSongs = favoriteSongs,
                            playlists = playlists,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onSongClick = { song -> viewModel.playSong(song) },
                            onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                            onCacheSong = { song -> viewModel.cacheSong(song) },
                            onAddToPlaylist = { song -> songToAddToPlaylist = song },
                            onPlaylistClick = { playlist -> selectedPlaylistForDetail = playlist },
                            onOpenImportSync = { showImportExportDialog = true },
                            onOpenBackupVault = { showImportExportDialog = true },
                            onCreatePlaylist = { showCreatePlaylistDialog = true },
                            onRefreshOfflineFolder = { viewModel.refreshOfflineFolder() }
                        )
                    }

                    AppDestination.SETTINGS -> {
                        SettingsScreen(
                            currentThemeMode = themeMode,
                            isLowBatteryMode = isLowBatteryMode,
                            repository = viewModel.repository,
                            onThemeModeChange = { viewModel.setThemeMode(it) },
                            onToggleLowBattery = { viewModel.toggleLowBatteryMode() }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Full Player with Next Song & Lyrics
    val activeSong = currentSong
    if (showNowPlayingSheet && activeSong != null) {
        NowPlayingSheet(
            song = activeSong,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            repeatMode = repeatMode,
            isShuffle = isShuffle,
            playbackSpeed = playbackSpeed,
            isBuffering = isBuffering,
            isAutoPlay = isAutoPlay,
            nextSong = nextSong,
            upcomingQueue = upcomingQueue,
            autoPlaySuggestions = autoPlaySuggestionsList,
            autoPlaySuggestion = autoPlaySuggestion,
            autoPlayMode = autoPlayMode,
            onSetAutoPlayMode = { mode -> viewModel.setAutoPlayMode(mode) },
            onRefreshAutoPlaySuggestion = { viewModel.refreshAutoPlaySuggestion() },
            onPlayPause = { viewModel.togglePlayPause() },
            onSeek = { pos -> viewModel.seekTo(pos) },
            onNext = { viewModel.skipToNext() },
            onPrevious = { viewModel.skipToPrevious() },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onCycleRepeat = { viewModel.cycleRepeatMode() },
            onToggleAutoPlay = { viewModel.toggleAutoPlay() },
            onPlayNextSong = { song -> viewModel.playAutoPlaySuggestion(song) },
            onPlayFromUpcomingQueue = { song -> viewModel.playFromUpcomingQueue(song) },
            onRemoveFromQueue = { song -> viewModel.removeFromUpcomingQueue(song) },
            onToggleFavorite = { viewModel.toggleFavorite(activeSong) },
            onCacheSong = { viewModel.cacheSong(activeSong) },
            onAddToPlaylist = { songToAddToPlaylist = activeSong },
            onOpenEqualizer = { showEqualizerDialog = true },
            onDismiss = { showNowPlayingSheet = false }
        )
    }

    // Equalizer Dialog
    if (showEqualizerDialog) {
        EqualizerDialog(
            state = equalizerState,
            onStateChange = { viewModel.updateEqualizer(it) },
            onApplyPreset = { viewModel.applyEqualizerPreset(it) },
            onDismiss = { showEqualizerDialog = false }
        )
    }

    // Import / Export / Vault / Collaborative Room Dialog
    if (showImportExportDialog) {
        ImportExportDialog(
            repository = viewModel.repository,
            onDismiss = { showImportExportDialog = false }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onCreate = { title, desc, isCollab ->
                viewModel.createPlaylist(title, desc, isCollab)
            },
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }

    // Playlist Detail Dialog with Add Songs, Remove Songs, and Delete Playlist
    val activePlaylist = selectedPlaylistForDetail
    if (activePlaylist != null) {
        val playlistSongs by viewModel.getSongsForPlaylist(activePlaylist.id).collectAsState(initial = emptyList())
        PlaylistDetailDialog(
            playlist = activePlaylist,
            songs = playlistSongs,
            availableSongs = allSongs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onSongClick = { song -> viewModel.playSong(song) },
            onPlayAll = { viewModel.playPlaylist(activePlaylist, playlistSongs) },
            onAddSong = { song -> viewModel.addSongToPlaylist(activePlaylist.id, song) },
            onRemoveSong = { song -> viewModel.removeSongFromPlaylist(activePlaylist.id, song.id) },
            onDeletePlaylist = { p ->
                viewModel.deletePlaylist(p.id)
                selectedPlaylistForDetail = null
            },
            onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
            onCacheSong = { song -> viewModel.cacheSong(song) },
            onDismiss = { selectedPlaylistForDetail = null }
        )
    }

    // Add To Playlist Dialog
    val songToAdd = songToAddToPlaylist
    if (songToAdd != null) {
        AddToPlaylistDialog(
            song = songToAdd,
            playlists = playlists,
            onSelectPlaylist = { playlist ->
                viewModel.addSongToPlaylist(playlist.id, songToAdd)
                songToAddToPlaylist = null
            },
            onCreateNewPlaylist = { title ->
                viewModel.createPlaylistAndAddSong(title, "Created with love", false, songToAdd)
                songToAddToPlaylist = null
            },
            onDismiss = { songToAddToPlaylist = null }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
