package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.Playlist
import com.example.data.entity.Song
import com.example.data.model.CategoryPlaylist
import com.example.data.model.DiscoverPlaylistDetail
import com.example.data.model.MovieReleasePlaylist
import com.example.data.model.RecommendedArtist
import com.example.data.model.TopMixPlaylist
import com.example.data.model.TopSongFilter
import com.example.data.model.TopSongsDimension
import com.example.ui.components.AudioVisualizerWave
import com.example.ui.components.DiscoverPlaylistSheet
import com.example.ui.components.GlassCard
import com.example.ui.components.HifiBadge
import com.example.ui.components.PrivacyShieldBadge
import com.example.ui.theme.EmeraldHifi
import com.example.ui.theme.LocalAppThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recommendations: Map<String, List<Song>> = emptyMap(),
    playlists: List<Playlist> = emptyList(),
    lastPlayedSongs: List<Song> = emptyList(),
    currentSong: Song?,
    isPlaying: Boolean,
    isLowBatteryMode: Boolean = false,
    trendingSongs: List<Song> = emptyList(),
    categories: List<CategoryPlaylist> = emptyList(),
    selectedTopSongsDimension: TopSongsDimension = TopSongsDimension.LANGUAGE,
    topSongsFilters: List<TopSongFilter> = emptyList(),
    selectedTopSongFilter: TopSongFilter? = null,
    topSongsList: List<Song> = emptyList(),
    onSelectTopSongsDimension: (TopSongsDimension) -> Unit = {},
    onSelectTopSongFilter: (TopSongFilter) -> Unit = {},
    movieReleases: List<MovieReleasePlaylist> = emptyList(),
    recommendedArtists: List<RecommendedArtist> = emptyList(),
    topMixes: List<TopMixPlaylist> = emptyList(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    lastRefreshedTime: Long = 0L,
    onPlaySongsQueue: (List<Song>, Int) -> Unit = { _, _ -> },
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (Playlist) -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onToggleLowBattery: () -> Unit = {},
    onAddToPlaylist: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppThemeColors.current
    var activeDetailSheet by remember { mutableStateOf<DiscoverPlaylistDetail?>(null) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("discover_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Top Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.ic_muesic_logo),
                                contentDescription = "Muesic Logo",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(appTheme.accent, appTheme.accentSecondary)
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MUESIC",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = appTheme.accent,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "Your music, your device, your privacy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appTheme.textSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Low Battery Mode Pill
                            Surface(
                                onClick = onToggleLowBattery,
                                shape = RoundedCornerShape(50),
                                color = if (isLowBatteryMode) Color(0x3310B981) else appTheme.pillBackground,
                                modifier = Modifier.testTag("low_battery_toggle_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isLowBatteryMode) Icons.Default.Bolt else Icons.Default.BatteryChargingFull,
                                        contentDescription = "Battery Mode",
                                        tint = if (isLowBatteryMode) EmeraldHifi else appTheme.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isLowBatteryMode) "ECO" else "Hi-Fi",
                                        color = if (isLowBatteryMode) EmeraldHifi else appTheme.textSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // Equalizer button
                            IconButton(
                                onClick = onOpenEqualizer,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (appTheme.isLight) Color(0x2200796B) else Color(0x2200F5D4))
                                    .testTag("home_equalizer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Equalizer",
                                    tint = appTheme.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrivacyShieldBadge()

                        // Auto-sync status indicator pill
                        Surface(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(50),
                            color = appTheme.pillBackground,
                            modifier = Modifier.testTag("discover_autosync_status_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isRefreshing) Color(0xFFFFA000) else EmeraldHifi)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isRefreshing) "Updating feed..." else "Auto-sync active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = appTheme.textSecondary
                                )
                            }
                        }
                    }
                }
            }

        // =========================================================================
        // SECTION: LAST PLAYED (3xn matrix view) - At the top for quick access
        // =========================================================================
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = appTheme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LAST PLAYED",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                            color = appTheme.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (lastPlayedSongs.isNotEmpty()) {
                        Text(
                            text = "${lastPlayedSongs.size} tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = appTheme.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val songsToShow = if (lastPlayedSongs.isNotEmpty()) {
                    lastPlayedSongs.distinctBy { it.id }
                } else {
                    trendingSongs.take(9)
                }

                if (songsToShow.isNotEmpty()) {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(182.dp)
                            .testTag("last_played_matrix_grid"),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(count = songsToShow.size, key = { index -> "${songsToShow[index].id}_$index" }) { index ->
                            val song = songsToShow[index]
                            LastPlayedSongGridItem(
                                song = song,
                                isCurrentlyPlaying = currentSong?.id == song.id && isPlaying,
                                onClick = { onSongClick(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 1: TRENDING SONGS (Source: JioSaavn)
        // =========================================================================
        if (trendingSongs.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TRENDING SONGS",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                                color = appTheme.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = appTheme.accent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "JioSaavn 320kbps",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                                color = appTheme.accent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(trendingSongs, key = { "trend_${it.id}" }) { song ->
                            DiscoverSongCard(
                                song = song,
                                isCurrentlyPlaying = currentSong?.id == song.id && isPlaying,
                                onClick = { onSongClick(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                testTag = "trending_song_${song.id}"
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 2: CATEGORIES (Romance, Workout, Pop, 90's, Chill, Best of Years)
        // =========================================================================
        if (categories.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = appTheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CATEGORIES & PLAYLISTS",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                                color = appTheme.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${categories.size} Vibes",
                            style = MaterialTheme.typography.labelSmall,
                            color = appTheme.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(categories, key = { it.id }) { cat ->
                            CategoryCard(
                                category = cat,
                                onClick = {
                                    activeDetailSheet = DiscoverPlaylistDetail(
                                        title = cat.title,
                                        subtitle = cat.description,
                                        artworkUrl = cat.coverArtworkUrl ?: cat.songs.firstOrNull()?.artworkUrl,
                                        gradientColors = cat.gradientColors,
                                        songs = cat.songs,
                                        sourceBadge = "JioSaavn Curated"
                                    )
                                },
                                onPlayDirect = {
                                    if (cat.songs.isNotEmpty()) {
                                        onPlaySongsQueue(cat.songs, 0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 3: TOP SONGS (Language, Category, Type, Artist, Country)
        // =========================================================================
        item {
            Column(modifier = Modifier.padding(top = 22.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = appTheme.accentSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TOP SONGS",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                            color = appTheme.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Filtered Charts",
                        style = MaterialTheme.typography.labelSmall,
                        color = appTheme.accent
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dimension Switcher (Language, Category, Type, Artist, Country)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TopSongsDimension.values()) { dimension ->
                        val isSelected = dimension == selectedTopSongsDimension
                        Surface(
                            onClick = { onSelectTopSongsDimension(dimension) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) appTheme.accent else appTheme.pillBackground,
                            modifier = Modifier.testTag("top_dimension_${dimension.name.lowercase()}")
                        ) {
                            Text(
                                text = dimension.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else appTheme.textSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sub-filter options (e.g. Hindi, English, Punjabi...)
                if (topSongsFilters.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(topSongsFilters, key = { it.id }) { filter ->
                            val isSelected = filter.id == selectedTopSongFilter?.id
                            Surface(
                                onClick = { onSelectTopSongFilter(filter) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) appTheme.accent.copy(alpha = 0.2f) else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, appTheme.accent) else null,
                                modifier = Modifier.testTag("top_filter_${filter.id}")
                            ) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) appTheme.accent else appTheme.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Top Songs for this filter
                if (topSongsList.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(topSongsList, key = { "top_${it.id}" }) { song ->
                            DiscoverSongCard(
                                song = song,
                                isCurrentlyPlaying = currentSong?.id == song.id && isPlaying,
                                onClick = { onSongClick(song) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                testTag = "top_song_${song.id}"
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 4: NEW RELEASES (Grouped by Movie / Album)
        // =========================================================================
        if (movieReleases.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = appTheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NEW RELEASES (BY MOVIE)",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                                color = appTheme.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Soundtracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = appTheme.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(movieReleases, key = { it.id }) { movie ->
                            MovieReleaseCard(
                                movie = movie,
                                onClick = {
                                    activeDetailSheet = DiscoverPlaylistDetail(
                                        title = "${movie.movieName} Soundtrack",
                                        subtitle = "${movie.composer} • ${movie.year}",
                                        artworkUrl = movie.posterUrl,
                                        songs = movie.songs,
                                        sourceBadge = "JioSaavn Movie Release"
                                    )
                                },
                                onPlayDirect = {
                                    if (movie.songs.isNotEmpty()) {
                                        onPlaySongsQueue(movie.songs, 0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 5: RECOMMENDED ARTISTS (Songs Playlist from Artists)
        // =========================================================================
        if (recommendedArtists.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = appTheme.accentSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECOMMENDED ARTISTS",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                                color = appTheme.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Artist Playlists",
                            style = MaterialTheme.typography.labelSmall,
                            color = appTheme.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recommendedArtists, key = { it.id }) { artist ->
                            RecommendedArtistItem(
                                artist = artist,
                                onClick = {
                                    activeDetailSheet = DiscoverPlaylistDetail(
                                        title = "${artist.name} Essentials",
                                        subtitle = artist.role,
                                        artworkUrl = artist.avatarUrl,
                                        songs = artist.songs,
                                        sourceBadge = "JioSaavn Artist Mix"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 6: TOP MIX (Songs/Playlists based on Genre of Songs)
        // =========================================================================
        if (topMixes.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = appTheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TOP MIXES (BY GENRE)",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                                color = appTheme.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Curated Mixes",
                            style = MaterialTheme.typography.labelSmall,
                            color = appTheme.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topMixes, key = { it.id }) { mix ->
                            TopMixCard(
                                mix = mix,
                                onClick = {
                                    activeDetailSheet = DiscoverPlaylistDetail(
                                        title = mix.genreName,
                                        subtitle = mix.subtitle,
                                        artworkUrl = mix.coverArtworkUrl ?: mix.songs.firstOrNull()?.artworkUrl,
                                        gradientColors = mix.gradientColors,
                                        songs = mix.songs,
                                        sourceBadge = "JioSaavn Top Mix"
                                    )
                                },
                                onPlayDirect = {
                                    if (mix.songs.isNotEmpty()) {
                                        onPlaySongsQueue(mix.songs, 0)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // Modal sheet for viewing any Discover playlist (Category, Movie, Artist, Mix)
    activeDetailSheet?.let { detail ->
        DiscoverPlaylistSheet(
            detail = detail,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onSongClick = onSongClick,
            onPlayAll = { songs -> onPlaySongsQueue(songs, 0) },
            onShufflePlay = { songs -> onPlaySongsQueue(songs, 0) },
            onAddToPlaylist = onAddToPlaylist,
            onDismiss = { activeDetailSheet = null }
        )
    }
}

// -----------------------------------------------------------------------------
// Component: DiscoverSongCard (50x50 thumbnail matching search design)
// -----------------------------------------------------------------------------
@Composable
fun DiscoverSongCard(
    song: Song,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    testTag: String = "discover_song_${song.id}"
) {
    val appTheme = LocalAppThemeColors.current
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .width(235.dp)
            .height(64.dp)
            .testTag(testTag),
        onClick = onClick,
        contentPadding = PaddingValues(6.dp),
        backgroundColor = if (isCurrentlyPlaying) {
            if (appTheme.isLight) Color(0x2800796B) else Color(0x3300F5D4)
        } else {
            appTheme.cardGlass
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (appTheme.isLight) Color(0x180F172A) else Color(0x3314192B)),
                contentAlignment = Alignment.Center
            ) {
                if (!song.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = song.artworkUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = appTheme.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioVisualizerWave(isPlaying = true, barCount = 4, maxHeight = 18.dp, barWidth = 2.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentlyPlaying) appTheme.accent else appTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = appTheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                HifiBadge(quality = song.audioQuality)
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = appTheme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = appTheme.accent)
                        },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        }
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: CategoryCard
// -----------------------------------------------------------------------------
@Composable
fun CategoryCard(
    category: CategoryPlaylist,
    onClick: () -> Unit,
    onPlayDirect: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current
    val artUrl = category.coverArtworkUrl ?: category.songs.firstOrNull()?.artworkUrl

    Surface(
        modifier = Modifier
            .width(170.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("category_card_${category.id}"),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!artUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artUrl,
                    contentDescription = category.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                category.gradientColors.map { Color(it) }
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${category.songs.size} tracks",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    IconButton(
                        onClick = onPlayDirect,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Category",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: MovieReleaseCard
// -----------------------------------------------------------------------------
@Composable
fun MovieReleaseCard(
    movie: MovieReleasePlaylist,
    onClick: () -> Unit,
    onPlayDirect: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current

    Surface(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
            .testTag("movie_release_${movie.id}"),
        shape = RoundedCornerShape(12.dp),
        color = appTheme.cardGlass
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(appTheme.pillBackground)
            ) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.movieName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = onPlayDirect,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(appTheme.accent)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Movie Tracks",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = movie.movieName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = appTheme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${movie.composer} • ${movie.year}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = appTheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component: RecommendedArtistItem
// -----------------------------------------------------------------------------
@Composable
fun RecommendedArtistItem(
    artist: RecommendedArtist,
    onClick: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current

    Column(
        modifier = Modifier
            .width(86.dp)
            .clickable(onClick = onClick)
            .testTag("artist_${artist.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(2.dp, appTheme.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = artist.avatarUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = appTheme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "Playlist",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = appTheme.accentSecondary
        )
    }
}

// -----------------------------------------------------------------------------
// Component: TopMixCard
// -----------------------------------------------------------------------------
@Composable
fun TopMixCard(
    mix: TopMixPlaylist,
    onClick: () -> Unit,
    onPlayDirect: () -> Unit
) {
    val artUrl = mix.coverArtworkUrl ?: mix.songs.firstOrNull()?.artworkUrl

    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(105.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("top_mix_${mix.id}"),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!artUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artUrl,
                    contentDescription = mix.genreName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.50f),
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(mix.gradientColors.map { Color(it) })
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = mix.genreName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mix.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${mix.songs.size} tracks",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    IconButton(
                        onClick = onPlayDirect,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Mix",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: LastPlayedSongGridItem
// -----------------------------------------------------------------------------
@Composable
fun LastPlayedSongGridItem(
    song: Song,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit = {}
) {
    val appTheme = LocalAppThemeColors.current
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .width(235.dp)
            .height(52.dp)
            .clickable(onClick = onClick)
            .testTag("last_played_item_${song.id}"),
        contentPadding = PaddingValues(4.dp),
        backgroundColor = if (isCurrentlyPlaying) {
            if (appTheme.isLight) Color(0x2800796B) else Color(0x3300F5D4)
        } else {
            appTheme.cardGlass
        },
        borderBrush = if (isCurrentlyPlaying) {
            Brush.linearGradient(listOf(appTheme.accent, appTheme.accent))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (appTheme.isLight) Color(0x180F172A) else Color(0x3314192B)),
                contentAlignment = Alignment.Center
            ) {
                if (!song.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = song.artworkUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = appTheme.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioVisualizerWave(isPlaying = true, barCount = 4, maxHeight = 18.dp, barWidth = 2.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 2.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentlyPlaying) appTheme.accent else appTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = appTheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = appTheme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = appTheme.accent)
                        },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        }
                    )
                }
            }
        }
    }
}
