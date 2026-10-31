package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.Song
import com.example.ui.MusicViewModel
import com.example.ui.components.AudioVisualizerWave
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.HifiBadge
import com.example.ui.theme.EmeraldHifi
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.NeonPink
import java.util.Locale

@Composable
fun SearchScreen(
    allSongs: List<Song>,
    onlineSearchResults: List<Song>,
    isSearching: Boolean,
    searchError: String?,
    activeSource: MusicViewModel.SearchSourceFilter,
    searchHistory: List<String> = emptyList(),
    currentSong: Song?,
    isPlaying: Boolean,
    onSearch: (String) -> Unit,
    onExecuteSearch: ((String) -> Unit)? = null,
    onSelectSource: (MusicViewModel.SearchSourceFilter) -> Unit,
    onDeleteHistoryItem: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onCacheSong: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppThemeColors.current
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Filtered songs to display
    val displayedSongs = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        onlineSearchResults
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "SEARCH & STREAM",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = appTheme.textPrimary,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Stream millions of songs from JioSaavn, YouTube Music & YouTube • Zero API keys or login required",
                    style = MaterialTheme.typography.bodySmall,
                    color = appTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Source Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("source_filter_row")
                ) {
                    item {
                        GlassPill(
                            text = "All Sources",
                            isSelected = activeSource == MusicViewModel.SearchSourceFilter.ALL,
                            onClick = { onSelectSource(MusicViewModel.SearchSourceFilter.ALL) },
                            modifier = Modifier.testTag("source_filter_ALL")
                        )
                    }
                    item {
                        GlassPill(
                            text = "JioSaavn 320k",
                            isSelected = activeSource == MusicViewModel.SearchSourceFilter.JIOSAAVN,
                            onClick = { onSelectSource(MusicViewModel.SearchSourceFilter.JIOSAAVN) },
                            modifier = Modifier.testTag("source_filter_JIOSAAVN")
                        )
                    }
                    item {
                        GlassPill(
                            text = "YouTube Music",
                            isSelected = activeSource == MusicViewModel.SearchSourceFilter.YOUTUBE_MUSIC,
                            onClick = { onSelectSource(MusicViewModel.SearchSourceFilter.YOUTUBE_MUSIC) },
                            modifier = Modifier.testTag("source_filter_YOUTUBE_MUSIC")
                        )
                    }
                    item {
                        GlassPill(
                            text = "YouTube",
                            isSelected = activeSource == MusicViewModel.SearchSourceFilter.YOUTUBE,
                            onClick = { onSelectSource(MusicViewModel.SearchSourceFilter.YOUTUBE) },
                            modifier = Modifier.testTag("source_filter_YOUTUBE")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length >= 3) {
                            onSearch(it)
                        }
                    },
                    placeholder = {
                        Text(
                            when (activeSource) {
                                MusicViewModel.SearchSourceFilter.ALL -> "Search JioSaavn, YouTube Music, YouTube (e.g. Kesariya, Starboy, Faded)..."
                                MusicViewModel.SearchSourceFilter.JIOSAAVN -> "Search JioSaavn 320kbps catalog..."
                                MusicViewModel.SearchSourceFilter.YOUTUBE_MUSIC -> "Search YouTube Music catalog..."
                                MusicViewModel.SearchSourceFilter.YOUTUBE -> "Search YouTube songs & audio..."
                            },
                            color = appTheme.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = appTheme.accent
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    onSearch("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = appTheme.textSecondary
                                    )
                                }
                            }
                            IconButton(onClick = {
                                val trimmed = searchQuery.trim()
                                if (trimmed.isNotEmpty()) {
                                    keyboardController?.hide()
                                    if (onExecuteSearch != null) {
                                        onExecuteSearch(trimmed)
                                    } else {
                                        onSearch(trimmed)
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Execute Search",
                                    tint = appTheme.accent
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val trimmed = searchQuery.trim()
                            if (trimmed.isNotEmpty()) {
                                keyboardController?.hide()
                                if (onExecuteSearch != null) {
                                    onExecuteSearch(trimmed)
                                } else {
                                    onSearch(trimmed)
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = appTheme.accent,
                        unfocusedBorderColor = appTheme.cardBorder,
                        focusedContainerColor = if (appTheme.isLight) Color(0xFFFFFFFF) else Color(0x5514192B),
                        unfocusedContainerColor = if (appTheme.isLight) Color(0xF5FFFFFF) else Color(0x3314192B),
                        cursorColor = appTheme.accent,
                        focusedTextColor = appTheme.textPrimary,
                        unfocusedTextColor = appTheme.textPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field")
                )

                // Status row (only when search query is entered)
                if (searchQuery.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSearching) {
                                "Searching JioSaavn..."
                            } else {
                                "${displayedSongs.size} tracks found"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSearching) appTheme.accent else appTheme.textSecondary
                        )
                        Text(
                            text = "Direct JioSaavn HD Stream",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = EmeraldHifi
                        )
                    }
                }
            }
        }

        // Searching Spinner Card
        if (isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = appTheme.accent,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Fetching high-fidelity audio streams...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = appTheme.textPrimary
                        )
                        Text(
                            text = "Connecting directly to JioSaavn & YouTube Music",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textSecondary
                        )
                    }
                }
            }
        }

        // Search Error Card
        if (!isSearching && searchError != null && displayedSongs.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Could Not Fetch Tracks",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonPink,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = searchError,
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        IconButton(onClick = { (onExecuteSearch ?: onSearch).invoke(searchQuery) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = appTheme.accent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", color = appTheme.accent, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Search History Section (Only search keywords, else nothing required)
        if (searchQuery.isBlank() && !isSearching) {
            if (searchHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SEARCH HISTORY",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = appTheme.textPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        TextButton(
                            onClick = onClearHistory,
                            modifier = Modifier.testTag("clear_search_history_btn")
                        ) {
                            Text(
                                text = "Clear all",
                                style = MaterialTheme.typography.labelMedium,
                                color = appTheme.accent
                            )
                        }
                    }
                }

                items(count = searchHistory.size, key = { index -> "${searchHistory[index]}_$index" }) { index ->
                    val keyword = searchHistory[index]
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem()
                            .testTag("history_item_$keyword"),
                        contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        onClick = {
                            searchQuery = keyword
                            keyboardController?.hide()
                            (onExecuteSearch ?: onSearch).invoke(keyword)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = appTheme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = keyword,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = appTheme.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteHistoryItem(keyword) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_history_$keyword")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove $keyword",
                                    tint = appTheme.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (appTheme.isLight) Color(0x2200796B) else Color(0x1A00F5D4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = appTheme.accent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Search History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = appTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Search keywords will appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textSecondary
                        )
                    }
                }
            }
        }

        // When search query is entered but no tracks found
        if (searchQuery.isNotBlank() && !isSearching && displayedSongs.isEmpty() && searchError == null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (appTheme.isLight) Color(0x2200796B) else Color(0x2200F5D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = appTheme.accent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No songs found for '$searchQuery'",
                        style = MaterialTheme.typography.titleMedium,
                        color = appTheme.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try searching with artist name or title keyword (e.g. Coldplay, Believer, Arijit)",
                        style = MaterialTheme.typography.bodySmall,
                        color = appTheme.textSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // Song Results List
        if (!isSearching && displayedSongs.isNotEmpty()) {
            items(count = displayedSongs.size, key = { index -> "${displayedSongs[index].id}_$index" }) { index ->
                val song = displayedSongs[index]
                val isCurrent = currentSong?.id == song.id

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .testTag("search_song_card_${song.id}"),
                    contentPadding = PaddingValues(10.dp),
                    onClick = { onSongClick(song) },
                    backgroundColor = if (isCurrent && isPlaying) {
                        if (appTheme.isLight) Color(0x2800796B) else Color(0x3300F5D4)
                    } else {
                        appTheme.cardGlass
                    },
                    borderBrush = if (isCurrent) {
                        Brush.linearGradient(listOf(appTheme.accent, appTheme.accent))
                    } else {
                        null
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artwork Thumbnail
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
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

                            // Visualizer Wave overlay if playing
                            if (isCurrent && isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x88000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AudioVisualizerWave(
                                        isPlaying = true,
                                        barCount = 5,
                                        maxHeight = 22.dp,
                                        barWidth = 2.5.dp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) appTheme.accent else appTheme.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${song.artist} • ${formatDuration(song.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = appTheme.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Source Badge
                                val (sourceBadgeText, sourceBadgeColor) = when {
                                    song.source.contains("YouTube Music", ignoreCase = true) -> "YT Music" to NeonPink
                                    song.source.contains("YouTube", ignoreCase = true) -> "YouTube" to Color(0xFFFF5252)
                                    else -> "JioSaavn • 320k" to EmeraldHifi
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(sourceBadgeColor.copy(alpha = 0.2f))
                                        .border(0.8.dp, sourceBadgeColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = sourceBadgeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = sourceBadgeColor
                                    )
                                }

                                HifiBadge(quality = song.audioQuality)
                            }
                        }

                        // Add to Playlist
                        IconButton(
                            onClick = { onAddToPlaylist(song) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = appTheme.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Save Offline
                        IconButton(
                            onClick = { onCacheSong(song) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (song.isCached) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                                contentDescription = if (song.isCached) "Saved Offline" else "Save Offline",
                                tint = if (song.isCached) EmeraldHifi else appTheme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Favorite
                        IconButton(
                            onClick = { onToggleFavorite(song) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song.isFavorite) NeonPink else appTheme.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSec = durationMs / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}
