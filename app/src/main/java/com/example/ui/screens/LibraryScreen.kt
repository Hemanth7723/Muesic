package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Playlist
import com.example.data.entity.Song
import com.example.ui.components.AudioVisualizerWave
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.components.HifiBadge
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldHifi
import com.example.ui.theme.LocalAppThemeColors
import com.example.ui.theme.NeonPink
import java.util.Locale

enum class LibraryTab {
    OFFLINE, PLAYLISTS
}

@Composable
fun LibraryScreen(
    allSongs: List<Song>,
    cachedSongs: List<Song>,
    favoriteSongs: List<Song>,
    playlists: List<Playlist>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onCacheSong: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit,
    onOpenImportSync: () -> Unit,
    onOpenBackupVault: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onRefreshOfflineFolder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppThemeColors.current
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(LibraryTab.OFFLINE) }

    val displayedSongs = when (selectedTab) {
        LibraryTab.OFFLINE -> cachedSongs
        LibraryTab.PLAYLISTS -> emptyList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Library Header & Actions
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
                    Column {
                        Text(
                            text = "YOUR LIBRARY",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = appTheme.textPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "${cachedSongs.size} Offline Songs • ${playlists.size} Playlists",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.accent
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onRefreshOfflineFolder,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x2210B981))
                                .testTag("refresh_offline_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan & Refresh Folder",
                                tint = EmeraldHifi,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenImportSync,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (appTheme.isLight) Color(0x2200796B) else Color(0x2200F5D4))
                                .testTag("import_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Import / Sync",
                                tint = appTheme.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenBackupVault,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x227928CA))
                                .testTag("backup_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Vault Backup",
                                tint = ElectricViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Offline, Playlists
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassPill(
                        text = "Offline (${cachedSongs.size})",
                        isSelected = selectedTab == LibraryTab.OFFLINE,
                        onClick = { selectedTab = LibraryTab.OFFLINE },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = EmeraldHifi,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                    GlassPill(
                        text = "Playlists (${playlists.size})",
                        isSelected = selectedTab == LibraryTab.PLAYLISTS,
                        onClick = { selectedTab = LibraryTab.PLAYLISTS }
                    )
                }

                if (selectedTab == LibraryTab.PLAYLISTS) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onCreatePlaylist,
                        colors = ButtonDefaults.buttonColors(containerColor = appTheme.accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_playlist_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = if (appTheme.isLight) Color.White else Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Custom / Collaborative Playlist", color = if (appTheme.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Playlists View
        if (selectedTab == LibraryTab.PLAYLISTS) {
            items(count = playlists.size, key = { index -> "${playlists[index].id}_$index" }) { index ->
                val playlist = playlists[index]
                PlaylistItemRow(
                    playlist = playlist,
                    modifier = Modifier.animateItem(),
                    onClick = { onPlaylistClick(playlist) },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Muesic Playlist: ${playlist.title}")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🎶 Join my collaborative playlist on Muesic!\nPlaylist: \"${playlist.title}\"\nPairing Code: ${playlist.shareCode}\nContributors: ${playlist.contributorNames}\nPrivate & Local-first 🎧"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
                    }
                )
            }
        } else {
            // Songs View (All, Offline, Favorites)
            if (displayedSongs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = appTheme.textMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Offline Songs",
                            style = MaterialTheme.typography.titleMedium,
                            color = appTheme.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Downloaded songs and imported storage tracks will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = appTheme.textMuted
                        )
                    }
                }
            } else {
                items(count = displayedSongs.size, key = { index -> "${displayedSongs[index].id}_$index" }) { index ->
                    val song = displayedSongs[index]
                    SongItemRow(
                        song = song,
                        isCurrentlyPlaying = currentSong?.id == song.id && isPlaying,
                        modifier = Modifier.animateItem(),
                        onSongClick = { onSongClick(song) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onCacheSong = { onCacheSong(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SongItemRow(
    song: Song,
    isCurrentlyPlaying: Boolean,
    onSongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCacheSong: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppThemeColors.current

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        contentPadding = PaddingValues(12.dp),
        onClick = onSongClick,
        backgroundColor = if (isCurrentlyPlaying) {
            if (appTheme.isLight) Color(0x2200796B) else Color(0x3300F5D4)
        } else {
            appTheme.surfaceGlass
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Wave / Artwork Thumbnail (50dp size)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (appTheme.isLight) Color(0x15000000) else Color(0x3314192B)),
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
                        AudioVisualizerWave(
                            isPlaying = true,
                            barCount = 6,
                            maxHeight = 22.dp,
                            barWidth = 2.5.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentlyPlaying) appTheme.accent else appTheme.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${song.artist} • ${formatDuration(song.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = appTheme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                HifiBadge(quality = song.audioQuality)
            }

            // Add to Playlist
            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = appTheme.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Offline Cache Indicator / Trigger
            IconButton(
                onClick = onCacheSong,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (song.isCached) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                    contentDescription = if (song.isCached) "Cached" else "Download",
                    tint = if (song.isCached) EmeraldHifi else appTheme.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Favorite
            IconButton(
                onClick = onToggleFavorite,
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

@Composable
fun PlaylistItemRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppThemeColors.current

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (playlist.isCollaborative) listOf(ElectricViolet, NeonPink) else listOf(Color(0xFF0D9488), appTheme.accent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playlist.isCollaborative) Icons.Default.Group else Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = appTheme.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playlist.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = appTheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlist.isCollaborative) {
                    Text(
                        text = "Collab Key: ${playlist.shareCode} • Contributors: ${playlist.contributorNames}",
                        style = MaterialTheme.typography.labelSmall,
                        color = appTheme.accent
                    )
                }
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Playlist",
                    tint = appTheme.accent,
                    modifier = Modifier.size(20.dp)
                )
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
