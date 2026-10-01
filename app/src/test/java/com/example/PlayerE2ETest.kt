package com.example

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.Song
import com.example.data.repository.AutoPlaySuggestion
import com.example.playback.AudioPlayerController
import com.example.playback.RepeatMode
import com.example.ui.MusicViewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingSheet
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.MuesicTheme
import com.example.util.OriginalSongFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive End-to-End Test Suite certifying player functionality:
 * - Search query across each music source (ALL, JioSaavn 320k, YouTube Music, YouTube)
 * - Source filter switching and search input entry
 * - Song selection from search results triggering playback
 * - Mid-song pause and resume with timestamp preservation
 * - Precise seeking / scrubbing
 * - Skip to next and previous songs in queue
 * - Selection directly from "Up Next" recommendation card
 * - MiniPlayerBar instant controls (play/pause, next)
 * - NowPlayingSheet controls & Up Next selection
 * - Audio source filter original song validation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var playerController: AudioPlayerController
    private lateinit var viewModel: MusicViewModel

    private val sampleTrack1 = Song(
        id = "jiosaavn-101",
        title = "Samajavaragamana",
        artist = "Sid Sriram",
        album = "Ala Vaikunthapurramuloo",
        durationMs = 214000,
        audioQuality = "FLAC Lossless 24-bit / 96kHz",
        streamUrl = "https://example.com/audio/samajavaragamana.mp4",
        genre = "Soundtrack",
        source = "JioSaavn HD"
    )

    private val sampleTrack2 = Song(
        id = "ytm-202",
        title = "Starboy",
        artist = "The Weeknd, Daft Punk",
        album = "Starboy",
        durationMs = 230000,
        audioQuality = "Master Studio Audio",
        streamUrl = "https://example.com/audio/starboy.mp4",
        genre = "Pop",
        source = "YouTube Music"
    )

    private val sampleTrack3 = Song(
        id = "yt-303",
        title = "Kesariya",
        artist = "Arijit Singh, Pritam",
        album = "Brahmastra",
        durationMs = 268000,
        audioQuality = "Direct Audio 320kbps",
        streamUrl = "https://example.com/audio/kesariya.mp4",
        genre = "Bollywood",
        source = "YouTube"
    )

    private val sampleTrack4 = Song(
        id = "cached-404",
        title = "Butta Bomma",
        artist = "Armaan Malik",
        album = "Ala Vaikunthapurramuloo",
        durationMs = 198000,
        audioQuality = "Offline Cache (Lossless)",
        streamUrl = "file:///data/user/0/com.example/cache/butta_bomma.m4a",
        genre = "Pop",
        source = "Offline Cache",
        isCached = true,
        localFilePath = "/data/user/0/com.example/cache/butta_bomma.m4a"
    )

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        playerController = AudioPlayerController(app)
        playerController.simulateMediaPlayerForTesting = true
        viewModel = MusicViewModel(app)
    }

    @Test
    fun testSearchSourceFilterSwitching() {
        // Verify default source is ALL
        assertEquals(MusicViewModel.SearchSourceFilter.ALL, viewModel.searchSource.value)

        // 1. Switch to JioSaavn filter
        viewModel.setSearchSource(MusicViewModel.SearchSourceFilter.JIOSAAVN)
        assertEquals(MusicViewModel.SearchSourceFilter.JIOSAAVN, viewModel.searchSource.value)

        // 2. Switch to YouTube Music filter
        viewModel.setSearchSource(MusicViewModel.SearchSourceFilter.YOUTUBE_MUSIC)
        assertEquals(MusicViewModel.SearchSourceFilter.YOUTUBE_MUSIC, viewModel.searchSource.value)

        // 3. Switch to YouTube filter
        viewModel.setSearchSource(MusicViewModel.SearchSourceFilter.YOUTUBE)
        assertEquals(MusicViewModel.SearchSourceFilter.YOUTUBE, viewModel.searchSource.value)

        // 4. Return to All Sources
        viewModel.setSearchSource(MusicViewModel.SearchSourceFilter.ALL)
        assertEquals(MusicViewModel.SearchSourceFilter.ALL, viewModel.searchSource.value)
    }

    @Test
    fun testOriginalSongFilterVerificationAcrossSources() {
        // Authentic official track
        val authenticSong = Song(
            id = "saavn-orig-1",
            title = "Samajavaragamana",
            artist = "Sid Sriram",
            album = "Ala Vaikunthapurramuloo",
            durationMs = 214000L,
            streamUrl = "https://example.com/audio.mp3",
            source = "JioSaavn HD"
        )
        assertTrue("Official original release must pass filter", OriginalSongFilter.isOriginal(authenticSong, false))

        // Remix / Lofi / Cover tracks must be identified
        val remixTrack = Song(
            id = "remix-1",
            title = "Samajavaragamana (8D Audio Remix)",
            artist = "Sid Sriram",
            album = "Remix Single",
            durationMs = 214000L,
            streamUrl = "https://example.com/remix.mp3",
            source = "YouTube"
        )
        assertFalse("8D Audio remix should be filtered out from original queries", OriginalSongFilter.isOriginal(remixTrack, false))

        val coverTrack = Song(
            id = "cover-1",
            title = "Samajavaragamana Guitar Cover Tutorial",
            artist = "Random Creator",
            album = "Tutorials",
            durationMs = 214000L,
            streamUrl = "https://example.com/cover.mp3",
            source = "YouTube"
        )
        assertFalse("Cover tutorial must be filtered out", OriginalSongFilter.isOriginal(coverTrack, false))
    }

    @Test
    fun testSelectAndPlaySong() {
        val playlist = listOf(sampleTrack1, sampleTrack2, sampleTrack3)

        // Select and play track 1
        playerController.playQueue(playlist, startIndex = 0)

        // Verify current song is set
        val current = playerController.currentSong.value
        assertNotNull("Current song must not be null after selection", current)
        assertEquals(sampleTrack1.id, current?.id)
        assertEquals("Samajavaragamana", current?.title)

        // Verify queue state
        assertEquals(3, playerController.queue.value.size)
        assertEquals(0, playerController.currentIndex.value)

        // Verify next song is primed
        val next = playerController.nextSong.value
        assertNotNull("Next song should be populated", next)
        assertEquals(sampleTrack2.id, next?.id)
    }

    @Test
    fun testPauseAndResumeInMiddleOfSong() {
        val playlist = listOf(sampleTrack2, sampleTrack1)
        playerController.playQueue(playlist, 0)

        // Simulate playback start from player callback
        playerController.youTubeWebPlayer.onPlaybackStateChanged?.invoke(true)
        assertTrue("Playback should be active", playerController.isPlaying.value)

        // Simulate song reaching 1 minute 15 seconds (75,000 ms)
        val middlePositionMs = 75_000L
        playerController.seekTo(middlePositionMs)
        assertEquals(middlePositionMs, playerController.currentPositionMs.value)

        // User toggles play/pause to pause mid-song
        playerController.togglePlayPause()
        assertFalse("Playback should be paused", playerController.isPlaying.value)
        assertEquals("Position must be retained when paused", middlePositionMs, playerController.currentPositionMs.value)

        // User toggles play/pause to resume mid-song
        playerController.togglePlayPause()
        assertTrue("Playback should be resumed", playerController.isPlaying.value)
        assertEquals("Position must remain at middle point when resuming", middlePositionMs, playerController.currentPositionMs.value)
    }

    @Test
    fun testPlaybackStallAutoRecoveryFallback() {
        val playlist = listOf(sampleTrack2)
        playerController.playQueue(playlist, 0)

        // Trigger player embed failure/restriction (e.g., error 150 embed disallowed)
        playerController.youTubeWebPlayer.onPlayerErrorCallback?.invoke(150)

        // Player should handle the error gracefully without crashing
        assertNotNull(playerController.currentSong.value)
    }

    @Test
    fun testJioSaavnMusicSourcePlaybackAndSeeking() {
        // 1. Load JioSaavn track (Native direct stream, FLAC / 320kbps CD Quality)
        playerController.playQueue(listOf(sampleTrack1), startIndex = 0)
        playerController.setPlaybackStateForTesting(true)
        assertTrue(playerController.isPlaying.value)

        assertEquals("JioSaavn HD", playerController.currentSong.value?.source)
        assertEquals("FLAC Lossless 24-bit / 96kHz", playerController.currentSong.value?.audioQuality)
        assertFalse("JioSaavn song must not be classified as YouTube source", playerController.isCurrentSongYouTube())
        assertEquals("https://example.com/audio/samajavaragamana.mp4", playerController.currentSong.value?.streamUrl)

        // 2. Test precise seek on JioSaavn stream
        val seekPos = 95_000L
        playerController.seekTo(seekPos)
        assertEquals(seekPos, playerController.currentPositionMs.value)

        // 3. Test pause and resume on JioSaavn stream
        playerController.togglePlayPause()
        assertFalse(playerController.isPlaying.value)
        assertEquals(seekPos, playerController.currentPositionMs.value)

        playerController.togglePlayPause()
        assertTrue(playerController.isPlaying.value)
        assertEquals(seekPos, playerController.currentPositionMs.value)
    }

    @Test
    fun testYouTubeMusicSourcePlaybackAndSeeking() {
        // 1. Load YouTube Music track
        playerController.playQueue(listOf(sampleTrack2), startIndex = 0)

        assertEquals("YouTube Music", playerController.currentSong.value?.source)
        assertTrue("YouTube Music track must route through YouTube Web Player", playerController.isCurrentSongYouTube())

        // 2. Simulate YouTube Web Player frame state active
        playerController.youTubeWebPlayer.onPlaybackStateChanged?.invoke(true)
        assertTrue(playerController.isPlaying.value)

        // 3. Test precise seek on YouTube Music stream
        val targetSeek = 82_000L
        playerController.seekTo(targetSeek)
        assertEquals(targetSeek, playerController.currentPositionMs.value)

        // 4. Test pause and resume
        playerController.togglePlayPause()
        assertFalse(playerController.isPlaying.value)
        assertEquals(targetSeek, playerController.currentPositionMs.value)

        playerController.togglePlayPause()
        assertTrue(playerController.isPlaying.value)
    }

    @Test
    fun testYouTubeStandardVideoSourcePlaybackAndSeeking() {
        // 1. Load YouTube Standard Video track
        playerController.playQueue(listOf(sampleTrack3), startIndex = 0)

        assertEquals("YouTube", playerController.currentSong.value?.source)
        assertTrue("YouTube standard video track must be identified as YouTube stream", playerController.isCurrentSongYouTube())

        // 2. Verify video ID extraction for standard YouTube format
        val extractedId = playerController.extractYouTubeVideoId(sampleTrack3)
        assertEquals(sampleTrack3.id, extractedId)

        // 3. Test seek on YouTube stream
        val seekMs = 140_000L
        playerController.seekTo(seekMs)
        assertEquals(seekMs, playerController.currentPositionMs.value)
    }

    @Test
    fun testOfflineCachedLocalMusicSourcePlaybackAndSeeking() {
        // 1. Load Offline Cached track
        playerController.playQueue(listOf(sampleTrack4), startIndex = 0)
        playerController.setPlaybackStateForTesting(true)
        assertTrue(playerController.isPlaying.value)

        val active = playerController.currentSong.value
        assertNotNull(active)
        assertTrue("Track must be marked as cached", active?.isCached == true)
        assertEquals("Offline Cache", active?.source)
        assertEquals("/data/user/0/com.example/cache/butta_bomma.m4a", active?.localFilePath)
        assertFalse("Offline cached track should NOT route to YouTube engine", playerController.isCurrentSongYouTube())

        // 2. Test seeking on local cached audio
        playerController.seekTo(60_000L)
        assertEquals(60_000L, playerController.currentPositionMs.value)

        // 3. Test pause and resume on local cached audio
        playerController.togglePlayPause()
        assertFalse(playerController.isPlaying.value)
        assertEquals(60_000L, playerController.currentPositionMs.value)

        playerController.togglePlayPause()
        assertTrue(playerController.isPlaying.value)
        assertEquals(60_000L, playerController.currentPositionMs.value)
    }

    @Test
    fun testSeamlessSourceSwitchingInQueue() {
        // Queue contains 4 distinct music sources: JioSaavn -> YouTube Music -> YouTube -> Offline Cache
        val multiSourceQueue = listOf(sampleTrack1, sampleTrack2, sampleTrack3, sampleTrack4)
        playerController.playQueue(multiSourceQueue, startIndex = 0)

        // Track 1: JioSaavn (Direct native stream)
        assertEquals(sampleTrack1.id, playerController.currentSong.value?.id)
        assertEquals("JioSaavn HD", playerController.currentSong.value?.source)
        assertFalse(playerController.isCurrentSongYouTube())

        // Skip to Track 2: YouTube Music (Transitions seamlessly to YouTube Web Player)
        playerController.skipToNext()
        assertEquals(sampleTrack2.id, playerController.currentSong.value?.id)
        assertEquals("YouTube Music", playerController.currentSong.value?.source)
        assertTrue(playerController.isCurrentSongYouTube())

        // Skip to Track 3: YouTube standard
        playerController.skipToNext()
        assertEquals(sampleTrack3.id, playerController.currentSong.value?.id)
        assertEquals("YouTube", playerController.currentSong.value?.source)
        assertTrue(playerController.isCurrentSongYouTube())

        // Skip to Track 4: Offline Cached track (Transitions seamlessly back to direct local player)
        playerController.skipToNext()
        assertEquals(sampleTrack4.id, playerController.currentSong.value?.id)
        assertEquals("Offline Cache", playerController.currentSong.value?.source)
        assertTrue(playerController.currentSong.value?.isCached == true)
        assertFalse(playerController.isCurrentSongYouTube())

        // Return backwards through the queue
        playerController.skipToPrevious()
        assertEquals(sampleTrack3.id, playerController.currentSong.value?.id)
        playerController.skipToPrevious()
        assertEquals(sampleTrack2.id, playerController.currentSong.value?.id)
        playerController.skipToPrevious()
        assertEquals(sampleTrack1.id, playerController.currentSong.value?.id)
    }

    @Test
    fun testSeekingMidSongScrubber() {
        val playlist = listOf(sampleTrack2)
        playerController.playQueue(playlist, 0)

        // Seek forward to 120 seconds
        val targetSeekMs = 120_000L
        playerController.seekTo(targetSeekMs)
        assertEquals(targetSeekMs, playerController.currentPositionMs.value)

        // Seek backward to 30 seconds
        val targetBackMs = 30_000L
        playerController.seekTo(targetBackMs)
        assertEquals(targetBackMs, playerController.currentPositionMs.value)
    }

    @Test
    fun testSkipToNextAndPreviousSongs() {
        val playlist = listOf(sampleTrack1, sampleTrack2, sampleTrack3)
        playerController.playQueue(playlist, startIndex = 0)

        assertEquals(sampleTrack1.id, playerController.currentSong.value?.id)

        // User clicks Next
        playerController.skipToNext()

        // Verify track skipped to sampleTrack2
        assertEquals(sampleTrack2.id, playerController.currentSong.value?.id)
        assertEquals(1, playerController.currentIndex.value)
        assertEquals(sampleTrack3.id, playerController.nextSong.value?.id)

        // User clicks Next again
        playerController.skipToNext()
        assertEquals(sampleTrack3.id, playerController.currentSong.value?.id)
        assertEquals(2, playerController.currentIndex.value)

        // User clicks Previous
        playerController.skipToPrevious()
        assertEquals(sampleTrack2.id, playerController.currentSong.value?.id)
        assertEquals(1, playerController.currentIndex.value)
    }

    @Test
    fun testSelectFromUpNextSongs() {
        val playlist = listOf(sampleTrack1, sampleTrack2, sampleTrack3)
        playerController.playQueue(playlist, startIndex = 0)

        // Next song is sampleTrack2
        val upcomingTrack = playerController.nextSong.value
        assertNotNull(upcomingTrack)
        assertEquals(sampleTrack2.id, upcomingTrack?.id)

        // User taps directly on the "Up Next" recommendation card
        playerController.playAutoPlaySuggestion(sampleTrack2)

        // Verify current song is immediately switched to the Up Next selection
        assertEquals(sampleTrack2.id, playerController.currentSong.value?.id)
        assertEquals("Starboy", playerController.currentSong.value?.title)
    }

    @Test
    fun testPlaybackControlsRepeatAndShuffle() {
        // Repeat mode cycling: OFF -> ALL -> ONE -> OFF
        assertEquals(RepeatMode.OFF, playerController.repeatMode.value)
        playerController.cycleRepeatMode()
        assertEquals(RepeatMode.ALL, playerController.repeatMode.value)
        playerController.cycleRepeatMode()
        assertEquals(RepeatMode.ONE, playerController.repeatMode.value)
        playerController.cycleRepeatMode()
        assertEquals(RepeatMode.OFF, playerController.repeatMode.value)

        // Shuffle toggling
        assertFalse(playerController.isShuffle.value)
        playerController.toggleShuffle()
        assertTrue(playerController.isShuffle.value)
        playerController.toggleShuffle()
        assertFalse(playerController.isShuffle.value)
    }

    @Test
    fun testComposeUiSearchAndPlayerUserActions() {
        var clickedSong: Song? = null
        var selectedSource by mutableStateOf(MusicViewModel.SearchSourceFilter.ALL)

        composeTestRule.setContent {
            MuesicTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    SearchScreen(
                        allSongs = listOf(sampleTrack1, sampleTrack2),
                        onlineSearchResults = listOf(sampleTrack1, sampleTrack2),
                        isSearching = false,
                        searchError = null,
                        activeSource = selectedSource,
                        searchHistory = listOf("Samajavaragamana", "Starboy"),
                        currentSong = sampleTrack1,
                        isPlaying = true,
                        onSearch = { },
                        onSelectSource = { selectedSource = it },
                        onDeleteHistoryItem = { },
                        onClearHistory = { },
                        onSongClick = { clickedSong = it },
                        onToggleFavorite = { },
                        onCacheSong = { },
                        onAddToPlaylist = { }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify search input field is rendered and responsive
        composeTestRule.onNodeWithTag("search_input_field").assertIsDisplayed()

        // 2. Click on JioSaavn source filter pill
        composeTestRule.onNodeWithTag("source_filter_JIOSAAVN").assertIsDisplayed().performClick()
        assertEquals(MusicViewModel.SearchSourceFilter.JIOSAAVN, selectedSource)

        // 3. Click on YouTube Music source filter pill
        composeTestRule.onNodeWithTag("source_filter_YOUTUBE_MUSIC").assertIsDisplayed().performClick()
        assertEquals(MusicViewModel.SearchSourceFilter.YOUTUBE_MUSIC, selectedSource)

        // 4. Click on YouTube source filter pill
        composeTestRule.onNodeWithTag("source_filter_YOUTUBE").assertIsDisplayed().performClick()
        assertEquals(MusicViewModel.SearchSourceFilter.YOUTUBE, selectedSource)

        // 5. Click on All Sources filter pill
        composeTestRule.onNodeWithTag("source_filter_ALL").assertIsDisplayed().performClick()
        assertEquals(MusicViewModel.SearchSourceFilter.ALL, selectedSource)

        // 6. Enter search text to display results
        composeTestRule.onNodeWithTag("search_input_field").performTextInput("Samajavaragamana")
        composeTestRule.waitForIdle()

        // 7. Click on a song card from search results to trigger playback
        composeTestRule.onNodeWithTag("search_song_card_${sampleTrack1.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performTouchInput { click(Offset(40f, 40f)) }
        composeTestRule.waitForIdle()
        assertNotNull("Song click must be received", clickedSong)
        assertEquals(sampleTrack1.id, clickedSong?.id)
    }

    @Test
    fun testComposeUiMiniPlayerBarUserActions() {
        var isPlayingState by mutableStateOf(true)
        var nextClicked = false
        var barClicked = false

        composeTestRule.setContent {
            MuesicTheme {
                MiniPlayerBar(
                    song = sampleTrack1,
                    isPlaying = isPlayingState,
                    progressMs = 45_000L,
                    durationMs = 214_000L,
                    isBuffering = false,
                    onPlayPause = { isPlayingState = !isPlayingState },
                    onNext = { nextClicked = true },
                    onClick = { barClicked = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify MiniPlayerBar renders and is clickable
        composeTestRule.onNodeWithTag("mini_player_bar").assertIsDisplayed().performClick()
        assertTrue("Bar click must expand full player", barClicked)

        // 2. Click play/pause button on MiniPlayer
        composeTestRule.onNodeWithTag("mini_play_pause_button").assertIsDisplayed().performClick()
        assertFalse("Toggling play/pause pauses active playback", isPlayingState)

        composeTestRule.onNodeWithTag("mini_play_pause_button").assertIsDisplayed().performClick()
        assertTrue("Toggling play/pause resumes active playback", isPlayingState)

        // 3. Click next track on MiniPlayer
        composeTestRule.onNodeWithTag("mini_next_button").assertIsDisplayed().performClick()
        assertTrue("Next track click must be received", nextClicked)
    }

    @Test
    fun testComposeUiNowPlayingSheetUserActions() {
        var isPlayingState by mutableStateOf(true)
        var nextClicked = false
        var prevClicked = false
        var upNextSelected: Song? = null
        var scrubbedPosition = 0L

        composeTestRule.setContent {
            MuesicTheme {
                NowPlayingSheet(
                    song = sampleTrack1,
                    isPlaying = isPlayingState,
                    currentPositionMs = 45_000L,
                    durationMs = 214_000L,
                    repeatMode = RepeatMode.OFF,
                    isShuffle = false,
                    playbackSpeed = 1.0f,
                    isBuffering = false,
                    isAutoPlay = true,
                    nextSong = sampleTrack2,
                    autoPlaySuggestion = AutoPlaySuggestion(sampleTrack2, "Up next in queue", isFromHistory = false),
                    onPlayPause = { isPlayingState = !isPlayingState },
                    onSeek = { scrubbedPosition = it },
                    onNext = { nextClicked = true },
                    onPrevious = { prevClicked = true },
                    onToggleShuffle = { },
                    onCycleRepeat = { },
                    onToggleAutoPlay = { },
                    onPlayNextSong = { upNextSelected = it },
                    onToggleFavorite = { },
                    onCacheSong = { },
                    onOpenEqualizer = { },
                    onDismiss = { }
                )
            }
        }

        // 1. Verify play/pause button in Now Playing Sheet
        composeTestRule.onNodeWithTag("main_play_pause_button", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        assertFalse("Pause click should toggle isPlaying to false", isPlayingState)

        composeTestRule.onNodeWithTag("main_play_pause_button", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        assertTrue("Play click should toggle isPlaying to true", isPlayingState)

        // 2. Verify next button triggers queue skip
        composeTestRule.onNodeWithTag("next_button", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        assertTrue("Next button click must be handled", nextClicked)

        // 3. Verify previous button triggers previous skip
        composeTestRule.onNodeWithTag("prev_button", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        assertTrue("Previous button click must be handled", prevClicked)

        // 4. Verify clicking on the "Up Next" song card selects the recommended song
        composeTestRule.onNodeWithTag("up_next_song_card", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        assertNotNull("Up Next song must be selected", upNextSelected)
        assertEquals(sampleTrack2.id, upNextSelected?.id)
    }

    @Test
    fun testMultiSongUpcomingQueueAutoPlayFlow() {
        // 1. Play initial song and configure multiple upcoming songs in queue
        playerController.playSong(sampleTrack1)
        assertEquals(sampleTrack1.id, playerController.currentSong.value?.id)

        playerController.addToQueue(sampleTrack2)
        playerController.addToQueue(sampleTrack3)
        playerController.addToQueue(sampleTrack4)

        assertEquals(3, playerController.upcomingQueue.value.size)
        assertEquals(sampleTrack2.id, playerController.upcomingQueue.value[0].id)
        assertEquals(sampleTrack3.id, playerController.upcomingQueue.value[1].id)
        assertEquals(sampleTrack4.id, playerController.upcomingQueue.value[2].id)

        // 2. User does not select any song; current song ends or user skips
        playerController.skipToNext()

        // Should automatically play track 2 from the upcoming queue
        assertEquals(sampleTrack2.id, playerController.currentSong.value?.id)
        assertEquals(2, playerController.upcomingQueue.value.size)
        assertEquals(sampleTrack3.id, playerController.upcomingQueue.value[0].id)
        assertEquals(sampleTrack4.id, playerController.upcomingQueue.value[1].id)

        // 3. User again does not select; skip to next
        playerController.skipToNext()
        assertEquals(sampleTrack3.id, playerController.currentSong.value?.id)
        assertEquals(1, playerController.upcomingQueue.value.size)
        assertEquals(sampleTrack4.id, playerController.upcomingQueue.value[0].id)

        // 4. Test remove from upcoming queue
        playerController.removeFromUpcomingQueue(sampleTrack4)
        assertTrue(
            "sampleTrack4 should be removed from upcoming queue",
            playerController.upcomingQueue.value.none { it.id == sampleTrack4.id }
        )
    }

    @Test
    fun testInteractiveUpcomingQueueUserActions() {
        var selectedSong: Song? = null
        var removedSong: Song? = null

        val queueList = listOf(sampleTrack2, sampleTrack3, sampleTrack4)

        composeTestRule.setContent {
            MuesicTheme {
                NowPlayingSheet(
                    song = sampleTrack1,
                    isPlaying = true,
                    currentPositionMs = 30_000L,
                    durationMs = 214_000L,
                    repeatMode = RepeatMode.OFF,
                    isShuffle = false,
                    playbackSpeed = 1.0f,
                    isBuffering = false,
                    isAutoPlay = true,
                    upcomingQueue = queueList,
                    onPlayPause = { },
                    onSeek = { },
                    onNext = { },
                    onPrevious = { },
                    onToggleShuffle = { },
                    onCycleRepeat = { },
                    onToggleAutoPlay = { },
                    onPlayFromUpcomingQueue = { selectedSong = it },
                    onRemoveFromQueue = { removedSong = it },
                    onToggleFavorite = { },
                    onCacheSong = { },
                    onOpenEqualizer = { },
                    onDismiss = { }
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify multiple queue items are present
        composeTestRule.onNodeWithTag("up_next_song_card", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("upcoming_queue_item_${sampleTrack3.id}", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        // 2. Click on the 2nd queue item to play it
        composeTestRule.onNodeWithTag("upcoming_queue_item_${sampleTrack3.id}", useUnmergedTree = true)
            .performClick()
        assertEquals(sampleTrack3.id, selectedSong?.id)
    }

    @Test
    fun testRobustPauseStopsPlaybackImmediatelyAcrossEngines() {
        // 1. Start playback on native stream
        playerController.playSong(sampleTrack1)
        playerController.setPlaybackStateForTesting(true)
        assertTrue(playerController.isPlaying.value)

        // 2. User presses Pause
        playerController.pause()
        assertFalse("isPlaying must immediately become false upon pause", playerController.isPlaying.value)

        // 3. Re-pause call should stay paused
        playerController.pause()
        assertFalse(playerController.isPlaying.value)

        // 4. Switch to YouTube source track
        playerController.playSong(sampleTrack2)
        playerController.setPlaybackStateForTesting(true)
        assertTrue(playerController.isPlaying.value)

        // 5. User presses Pause while YouTube stream is active
        playerController.pause()
        assertFalse("isPlaying must immediately become false on YouTube track pause", playerController.isPlaying.value)
        assertTrue("YouTube player must have isExplicitlyPaused flag set", playerController.youTubeWebPlayer.isExplicitlyPaused)

        // 6. If YouTube webview fires late playing state, it must be rejected while explicitly paused
        playerController.youTubeWebPlayer.onPlaybackStateChanged?.invoke(true)
        // Verify audio player controller maintains pause state
        assertFalse("Player must not resume when paused by user", playerController.isPlaying.value)

        // 7. Resume works as expected
        playerController.resume()
        assertTrue(playerController.isPlaying.value)
        assertFalse(playerController.youTubeWebPlayer.isExplicitlyPaused)
    }
}
