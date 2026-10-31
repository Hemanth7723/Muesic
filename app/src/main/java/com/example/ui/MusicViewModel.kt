package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.Playlist
import com.example.data.entity.Song
import com.example.data.model.CategoryPlaylist
import com.example.data.model.MovieReleasePlaylist
import com.example.data.model.RecommendedArtist
import com.example.data.model.TopMixPlaylist
import com.example.data.model.TopSongFilter
import com.example.data.model.TopSongsDimension
import com.example.data.network.JioSaavnService
import com.example.data.repository.AutoPlayMode
import com.example.data.repository.AutoPlaySuggestion
import com.example.data.repository.DiscoverContentProvider
import com.example.data.repository.MusicRepository
import com.example.playback.AudioPlayerController
import com.example.playback.EqualizerState
import com.example.playback.MuesicPlaybackService
import com.example.playback.RepeatMode
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MusicRepository(application)
    val playerController = AudioPlayerController(application)

    companion object {
        private const val TAG = "MusicViewModel"
    }

    enum class SearchSourceFilter(val displayName: String) {
        ALL("All Sources"),
        JIOSAAVN("JioSaavn"),
        YOUTUBE_MUSIC("YouTube Music"),
        YOUTUBE("YouTube")
    }

    private val _searchSource = MutableStateFlow(SearchSourceFilter.ALL)
    val searchSource: StateFlow<SearchSourceFilter> = _searchSource.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _onlineSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val onlineSearchResults: StateFlow<List<Song>> = _onlineSearchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cachedSongs: StateFlow<List<Song>> = repository.cachedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastPlayedSongs: StateFlow<List<Song>> = repository.lastPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val librarySavedSongs: StateFlow<List<Song>> = repository.librarySavedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<String>> = repository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recommendations = MutableStateFlow<Map<String, List<Song>>>(emptyMap())
    val recommendations: StateFlow<Map<String, List<Song>>> = _recommendations.asStateFlow()

    // 6 New Discover Sections: Suggestions powered by JioSaavn
    // 1. Trending Songs (from JioSaavn)
    private val _trendingSongs = MutableStateFlow<List<Song>>(DiscoverContentProvider.getInitialTrendingSongs())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()

    // 2. Categories (Romance, Workout, Pop, 90's, Chill, Best of Years)
    private val _categories = MutableStateFlow<List<CategoryPlaylist>>(DiscoverContentProvider.getInitialCategories())
    val categories: StateFlow<List<CategoryPlaylist>> = _categories.asStateFlow()

    // 3. Top Songs (by Language, Category, Type, Artist, Country)
    private val _selectedTopSongsDimension = MutableStateFlow(TopSongsDimension.LANGUAGE)
    val selectedTopSongsDimension: StateFlow<TopSongsDimension> = _selectedTopSongsDimension.asStateFlow()

    private val _topSongsFilters = MutableStateFlow<List<TopSongFilter>>(DiscoverContentProvider.getTopSongsFilters(TopSongsDimension.LANGUAGE))
    val topSongsFilters: StateFlow<List<TopSongFilter>> = _topSongsFilters.asStateFlow()

    private val _selectedTopSongFilter = MutableStateFlow<TopSongFilter?>(_topSongsFilters.value.firstOrNull())
    val selectedTopSongFilter: StateFlow<TopSongFilter?> = _selectedTopSongFilter.asStateFlow()

    private val _topSongsList = MutableStateFlow<List<Song>>(_topSongsFilters.value.firstOrNull()?.initialSongs ?: emptyList())
    val topSongsList: StateFlow<List<Song>> = _topSongsList.asStateFlow()

    // 4. New Releases (Grouped by Movie / Album)
    private val _movieReleases = MutableStateFlow<List<MovieReleasePlaylist>>(DiscoverContentProvider.getInitialMovieReleases())
    val movieReleases: StateFlow<List<MovieReleasePlaylist>> = _movieReleases.asStateFlow()

    // 5. Recommended Artists (Songs playlist from artists)
    private val _recommendedArtists = MutableStateFlow<List<RecommendedArtist>>(DiscoverContentProvider.getInitialRecommendedArtists())
    val recommendedArtists: StateFlow<List<RecommendedArtist>> = _recommendedArtists.asStateFlow()

    // 6. Top Mix (Songs/Playlists based on Genre of songs)
    private val _topMixes = MutableStateFlow<List<TopMixPlaylist>>(DiscoverContentProvider.getInitialTopMixes())
    val topMixes: StateFlow<List<TopMixPlaylist>> = _topMixes.asStateFlow()

    // Discover Refresh States
    private val _isDiscoverRefreshing = MutableStateFlow(false)
    val isDiscoverRefreshing: StateFlow<Boolean> = _isDiscoverRefreshing.asStateFlow()

    private val _lastDiscoverRefreshedTime = MutableStateFlow(System.currentTimeMillis())
    val lastDiscoverRefreshedTime: StateFlow<Long> = _lastDiscoverRefreshedTime.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.AUTO)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Player states
    val currentSong: StateFlow<Song?> = playerController.currentSong
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val isAutoPlay: StateFlow<Boolean> = playerController.isAutoPlay
    val nextSong: StateFlow<Song?> = playerController.nextSong
    val upcomingQueue: StateFlow<List<Song>> = playerController.upcomingQueue

    private val _autoPlayMode = MutableStateFlow(AutoPlayMode.SMART_MIX)
    val autoPlayMode: StateFlow<AutoPlayMode> = _autoPlayMode.asStateFlow()
    val autoPlaySuggestion: StateFlow<AutoPlaySuggestion?> = playerController.autoPlaySuggestion
    val autoPlaySuggestionsList: StateFlow<List<AutoPlaySuggestion>> = playerController.autoPlaySuggestionsList

    val currentPositionMs: StateFlow<Long> = playerController.currentPositionMs
    val durationMs: StateFlow<Long> = playerController.durationMs
    val repeatMode: StateFlow<RepeatMode> = playerController.repeatMode
    val isShuffle: StateFlow<Boolean> = playerController.isShuffle
    val playbackSpeed: StateFlow<Float> = playerController.playbackSpeed
    val isLowBatteryMode: StateFlow<Boolean> = playerController.isLowBatteryMode
    val equalizerState: StateFlow<EqualizerState> = playerController.equalizerState
    val isBuffering: StateFlow<Boolean> = playerController.isBuffering

    init {
        viewModelScope.launch {
            repository.seedInitialOpenMusicCatalogIfNeeded()
            // Scan offline folder on app open to refresh songs/content
            repository.scanOfflineFolder()
        }

        viewModelScope.launch {
            allSongs.collect { songs ->
                if (songs.isNotEmpty()) {
                    _recommendations.value = repository.getRecommendations(songs)
                }
            }
        }

        playerController.onSongCompletedListener = { song ->
            viewModelScope.launch {
                repository.recordPlay(song)
                // Cache to be cleared after song playing is complete so app won't take too much memory
                repository.clearTempPlaybackCacheForSong(song)
            }
        }

        playerController.onSongUpdatedListener = { updatedSong ->
            viewModelScope.launch {
                repository.insertSong(updatedSong)
            }
        }

        playerController.autoPlaySuggestionProvider = { current, queue ->
            repository.getAutoPlaySuggestion(current, queue, _autoPlayMode.value)
        }
        playerController.autoPlaySuggestionsProvider = { current, queue ->
            repository.getAutoPlaySuggestions(current, queue, _autoPlayMode.value, limit = 8)
        }

        // 1. Auto-fetch and update all Discover sections whenever app opens
        viewModelScope.launch {
            delay(1000)
            refreshAllDiscoverSections(isUserTriggered = false)
        }

        // 2. Periodic background auto-fetcher: keeps all Discover sections fresh as time goes by
        viewModelScope.launch {
            while (isActive) {
                delay(10 * 60 * 1000L) // 10 minutes interval
                Log.d(TAG, "Periodic Discover auto-fetcher triggered")
                refreshAllDiscoverSections(isUserTriggered = false)
            }
        }
    }

    fun setSearchSource(source: SearchSourceFilter) {
        _searchSource.value = source
        if (_searchQuery.value.isNotBlank()) {
            searchOnline(_searchQuery.value, saveToHistory = false)
        }
    }

    fun searchOnline(query: String, saveToHistory: Boolean = false) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        if (trimmed.isBlank()) {
            _onlineSearchResults.value = emptyList()
            _isSearching.value = false
            _searchError.value = null
            return
        }

        if (saveToHistory && trimmed.length >= 2) {
            viewModelScope.launch {
                repository.addSearchKeyword(trimmed)
            }
        }

        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            try {
                val results = when (_searchSource.value) {
                    SearchSourceFilter.ALL -> repository.searchAllOnline(trimmed)
                    SearchSourceFilter.JIOSAAVN -> repository.searchJioSaavn(trimmed)
                    SearchSourceFilter.YOUTUBE_MUSIC -> repository.searchYouTubeMusic(trimmed)
                    SearchSourceFilter.YOUTUBE -> repository.searchYouTube(trimmed)
                }
                _onlineSearchResults.value = results
                if (results.isEmpty()) {
                    _searchError.value = "No songs found for \"$trimmed\" on ${_searchSource.value.displayName}. Try another search term."
                }
            } catch (e: Exception) {
                _searchError.value = "Search error: ${e.localizedMessage ?: "Network issue"}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun deleteSearchHistoryKeyword(keyword: String) {
        viewModelScope.launch {
            repository.deleteSearchKeyword(keyword)
        }
    }

    fun clearAllSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            repository.insertSong(song)
            repository.recordPlay(song)
        }
        val currentQueue = if (_onlineSearchResults.value.any { it.id == song.id }) {
            _onlineSearchResults.value
        } else if (allSongs.value.any { it.id == song.id }) {
            allSongs.value
        } else {
            listOf(song) + allSongs.value.filter { it.id != song.id }
        }
        val index = currentQueue.indexOfFirst { it.id == song.id }
        if (index != -1) {
            playerController.playQueue(currentQueue, index)
        } else {
            playerController.playQueue(listOf(song) + allSongs.value.filter { it.id != song.id }, 0)
        }
        // Start foreground notification service
        try {
            MuesicPlaybackService.startService(
                getApplication(),
                song.title,
                "${song.artist} • ${song.audioQuality}",
                true
            )
        } catch (e: Exception) {
            // Foreground service starting in background check
        }
    }

    fun playPlaylist(playlist: Playlist, songs: List<Song>) {
        if (songs.isNotEmpty()) {
            playerController.playQueue(songs, 0)
            viewModelScope.launch {
                repository.recordPlay(songs[0])
            }
        }
    }

    fun playSongsQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        val selected = songs[safeIndex]
        viewModelScope.launch {
            repository.insertSongs(songs)
            repository.recordPlay(selected)
        }
        playerController.playQueue(songs, safeIndex)
        try {
            MuesicPlaybackService.startService(
                getApplication(),
                selected.title,
                "${selected.artist} • ${selected.audioQuality}",
                true
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun selectTopSongsDimension(dimension: TopSongsDimension) {
        _selectedTopSongsDimension.value = dimension
        val filters = DiscoverContentProvider.getTopSongsFilters(dimension)
        _topSongsFilters.value = filters
        val first = filters.firstOrNull()
        _selectedTopSongFilter.value = first
        _topSongsList.value = first?.initialSongs ?: emptyList()
        first?.let { fetchTopSongsForFilter(it) }
    }

    fun selectTopSongFilter(filter: TopSongFilter) {
        _selectedTopSongFilter.value = filter
        _topSongsList.value = filter.initialSongs
        fetchTopSongsForFilter(filter)
    }

    private fun fetchTopSongsForFilter(filter: TopSongFilter) {
        viewModelScope.launch {
            try {
                val liveSongs = DiscoverContentProvider.fetchFreshJioSaavnTracks(filter.query, filter.initialSongs)
                if (liveSongs.isNotEmpty() && _selectedTopSongFilter.value?.id == filter.id) {
                    _topSongsList.value = liveSongs
                }
            } catch (e: Exception) {
                // Keep initial fallback
            }
        }
    }

    /**
     * User swipe-to-refresh or explicit refresh trigger for Discover page
     */
    fun refreshDiscover(isUserTriggered: Boolean = true) {
        viewModelScope.launch {
            refreshAllDiscoverSections(isUserTriggered = isUserTriggered)
        }
    }

    /**
     * Comprehensive Discover section updater:
     * 1. Trending Songs (from JioSaavn)
     * 2. Categories (Romance, Workout, Pop, 90's, Chill, Best of Years)
     * 3. Top Songs (by Language, Category, Type, Artist, Country)
     * 4. New Releases (Grouped by Movie / Album)
     * 5. Recommended Artists (Song playlists from artists)
     * 6. Top Mix (Genre-based mixes & playlists)
     */
    private suspend fun refreshAllDiscoverSections(isUserTriggered: Boolean) {
        if (_isDiscoverRefreshing.value && isUserTriggered) return
        if (isUserTriggered) {
            _isDiscoverRefreshing.value = true
        }
        try {
            Log.d(TAG, "Starting Discover sections refresh (isUserTriggered=$isUserTriggered)...")

            // 1. High-priority visible sections: Trending, Selected Top Songs, New Movie Releases
            val trendingDeferred = viewModelScope.async(Dispatchers.IO) {
                DiscoverContentProvider.refreshTrendingSongs(_trendingSongs.value)
            }
            val topSongsDeferred = viewModelScope.async(Dispatchers.IO) {
                _selectedTopSongFilter.value?.let { filter ->
                    DiscoverContentProvider.refreshTopSongsFilter(filter)
                }
            }
            val movieReleasesDeferred = viewModelScope.async(Dispatchers.IO) {
                DiscoverContentProvider.refreshMovieReleases(_movieReleases.value)
            }

            val freshTrending = trendingDeferred.await()
            if (freshTrending.isNotEmpty()) {
                _trendingSongs.value = freshTrending
            }

            val freshTopSongs = topSongsDeferred.await()
            if (!freshTopSongs.isNullOrEmpty()) {
                _topSongsList.value = freshTopSongs
            }

            val freshMovies = movieReleasesDeferred.await()
            if (freshMovies.isNotEmpty()) {
                _movieReleases.value = freshMovies
            }

            // Immediately clear refreshing state if user-triggered so the UI returns to normal quickly
            if (isUserTriggered) {
                _isDiscoverRefreshing.value = false
            }

            // 2. Concurrently refresh secondary background sections: Categories, Mixes, Artists
            val categoriesDeferred = viewModelScope.async(Dispatchers.IO) {
                DiscoverContentProvider.refreshCategories(_categories.value)
            }
            val topMixesDeferred = viewModelScope.async(Dispatchers.IO) {
                DiscoverContentProvider.refreshTopMixes(_topMixes.value)
            }
            val artistsDeferred = viewModelScope.async(Dispatchers.IO) {
                DiscoverContentProvider.refreshRecommendedArtists(_recommendedArtists.value)
            }

            val freshCategories = categoriesDeferred.await()
            if (freshCategories.isNotEmpty()) {
                _categories.value = freshCategories
            }

            val freshMixes = topMixesDeferred.await()
            if (freshMixes.isNotEmpty()) {
                _topMixes.value = freshMixes
            }

            val freshArtists = artistsDeferred.await()
            if (freshArtists.isNotEmpty()) {
                _recommendedArtists.value = freshArtists
            }

            _lastDiscoverRefreshedTime.value = System.currentTimeMillis()
            Log.d(TAG, "Discover sections refreshed successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Error during Discover sections refresh: ${e.message}")
        } finally {
            _isDiscoverRefreshing.value = false
        }
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
        currentSong.value?.let { song ->
            try {
                MuesicPlaybackService.startService(
                    getApplication(),
                    song.title,
                    "${song.artist} • ${song.audioQuality}",
                    playerController.isPlaying.value
                )
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun skipToNext() = playerController.skipToNext()

    fun skipToPrevious() = playerController.skipToPrevious()

    fun toggleShuffle() = playerController.toggleShuffle()

    fun cycleRepeatMode() = playerController.cycleRepeatMode()

    fun setPlaybackSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)

    fun toggleLowBatteryMode() = playerController.toggleLowBatteryMode()

    fun updateEqualizer(state: EqualizerState) = playerController.updateEqualizer(state)

    fun applyEqualizerPreset(presetName: String) = playerController.applyPreset(presetName)

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleFavorite(song: Song) {
        val newFav = !song.isFavorite
        val updated = song.copy(isFavorite = newFav)
        playerController.updateCurrentSongState(updated)
        viewModelScope.launch {
            repository.insertSong(updated)
            repository.toggleFavorite(song.id)
        }
    }

    fun cacheSong(song: Song) {
        val updated = song.copy(isCached = true)
        playerController.updateCurrentSongState(updated)
        viewModelScope.launch {
            repository.insertSong(song)
            repository.cacheSong(song)
        }
    }

    fun toggleAutoPlay() = playerController.toggleAutoPlay()

    fun setAutoPlay(enabled: Boolean) = playerController.setAutoPlay(enabled)

    fun setAutoPlayMode(mode: AutoPlayMode) {
        _autoPlayMode.value = mode
        playerController.refreshAutoPlaySuggestion()
    }

    fun refreshAutoPlaySuggestion() {
        playerController.refreshAutoPlaySuggestion()
    }

    fun playAutoPlaySuggestion(song: Song) {
        playerController.playAutoPlaySuggestion(song)
    }

    fun playFromUpcomingQueue(song: Song) {
        playerController.playFromUpcomingQueue(song)
    }

    fun removeFromUpcomingQueue(song: Song) {
        playerController.removeFromUpcomingQueue(song)
    }

    fun addToQueue(song: Song) {
        playerController.addToQueue(song)
    }

    fun pause() = playerController.pause()

    fun resume() = playerController.resume()

    fun createPlaylist(title: String, description: String, isCollaborative: Boolean) {
        viewModelScope.launch {
            repository.createPlaylist(title, description, isCollaborative)
        }
    }

    fun createPlaylistAndAddSong(title: String, description: String, isCollaborative: Boolean, song: Song) {
        viewModelScope.launch {
            val newPlaylistId = repository.createPlaylist(title, description, isCollaborative)
            repository.insertSong(song)
            repository.addSongToPlaylist(newPlaylistId, song.id)
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            repository.insertSong(song)
            repository.addSongToPlaylist(playlistId, song.id)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return repository.getSongsForPlaylist(playlistId)
    }

    suspend fun exportJson(): String = repository.exportSongsToJson()

    suspend fun importJson(json: String): com.example.data.repository.ImportResult =
        repository.importSongsFromJson(json)

    suspend fun exportZip(): java.io.File = repository.createEncryptedBackupFile()

    suspend fun importZip(zipFile: java.io.File): com.example.data.repository.ImportResult =
        repository.restoreFromBackupFile(zipFile)

    val settingsManager = repository.settingsManager

    fun refreshOfflineFolder(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.scanOfflineFolder()
            onComplete(count)
        }
    }

    fun removeDownloadedSong(song: Song) {
        val updated = song.copy(isCached = false, localFilePath = null)
        playerController.updateCurrentSongState(updated)
        viewModelScope.launch {
            repository.removeDownloadedSong(song)
        }
    }

    suspend fun exportConfigAndPlaylists(): java.io.File = repository.exportConfigAndPlaylists()

    suspend fun importConfigAndPlaylists(json: String): com.example.data.repository.ImportResult =
        repository.importConfigAndPlaylists(json)

    suspend fun createAppUpdateBackup(): java.io.File = repository.createAppUpdateBackup()

    suspend fun restoreFromAppUpdateBackup(json: String): com.example.data.repository.ImportResult =
        repository.restoreFromAppUpdateBackup(json)

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
