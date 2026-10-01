package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Playlist
import com.example.data.entity.Song
import com.example.ui.screens.SongItemRow
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalAppThemeColors

@Composable
fun PlaylistDetailDialog(
    playlist: Playlist,
    songs: List<Song>,
    availableSongs: List<Song> = emptyList(),
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onAddSong: (Song) -> Unit = {},
    onRemoveSong: (Song) -> Unit = {},
    onDeletePlaylist: (Playlist) -> Unit = {},
    onToggleFavorite: (Song) -> Unit,
    onCacheSong: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current
    val context = LocalContext.current
    var showAddSongsDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("playlist_detail_dialog"),
            color = appTheme.backgroundPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            if (appTheme.isLight) {
                                listOf(appTheme.backgroundSecondary, appTheme.backgroundPrimary)
                            } else {
                                listOf(Color(0xFF1E1738), appTheme.backgroundPrimary, appTheme.backgroundPrimary)
                            }
                        )
                    )
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Actions: Share & Delete
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Muesic: ${playlist.title}")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "🎧 Check out this playlist on Muesic:\n\"${playlist.title}\" (${songs.size} tracks)\nPairing Key: ${playlist.shareCode}\nPrivate & Local-first 🎧"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Playlist"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = appTheme.accent)
                        }

                        IconButton(
                            onClick = {
                                onDeletePlaylist(playlist)
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Playlist", tint = Color(0xFFEF4444))
                        }
                    }

                    // Top Right: Close Option
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(appTheme.pillBackground)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = appTheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Playlist Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = appTheme.textPrimary
                    )
                    if (playlist.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playlist.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = appTheme.textSecondary
                        )
                    }

                    if (playlist.isCollaborative) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x337928CA))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Collab Key: ${playlist.shareCode} • Contributors: ${playlist.contributorNames}",
                                style = MaterialTheme.typography.labelSmall,
                                color = appTheme.accent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(containerColor = appTheme.accent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("play_all_playlist_button"),
                            enabled = songs.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (appTheme.isLight) Color.White else Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All (${songs.size})", color = if (appTheme.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showAddSongsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_songs_to_playlist_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = appTheme.accent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Songs", color = appTheme.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Song List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (songs.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No tracks in this playlist yet.", color = appTheme.textSecondary)
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { showAddSongsDialog = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = appTheme.accent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Songs Now", color = appTheme.accent)
                                }
                            }
                        }
                    } else {
                        items(songs) { song ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SongItemRow(
                                        song = song,
                                        isCurrentlyPlaying = currentSong?.id == song.id && isPlaying,
                                        onSongClick = { onSongClick(song) },
                                        onToggleFavorite = { onToggleFavorite(song) },
                                        onCacheSong = { onCacheSong(song) }
                                    )
                                }

                                IconButton(
                                    onClick = { onRemoveSong(song) },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove song",
                                        tint = appTheme.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSongsDialog) {
        AddSongsToPlaylistDialog(
            playlist = playlist,
            availableSongs = availableSongs,
            existingSongIds = songs.map { it.id }.toSet(),
            onAddSong = { song ->
                onAddSong(song)
            },
            onDismiss = { showAddSongsDialog = false }
        )
    }
}
