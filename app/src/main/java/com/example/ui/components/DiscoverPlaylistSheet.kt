package com.example.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.entity.Song
import com.example.data.model.DiscoverPlaylistDetail
import com.example.ui.theme.LocalAppThemeColors

/**
 * Clean, modern pop-up screen displaying full playlist details, artwork,
 * Play All / Shuffle buttons (placed neatly after details without superimposition),
 * and a top-right close option.
 */
@Composable
fun DiscoverPlaylistSheet(
    detail: DiscoverPlaylistDetail,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val appTheme = LocalAppThemeColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("discover_playlist_screen"),
            color = appTheme.backgroundPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(detail.gradientColors.firstOrNull() ?: 0xFF00F5D4).copy(alpha = 0.25f),
                                appTheme.backgroundPrimary,
                                appTheme.backgroundPrimary
                            )
                        )
                    )
            ) {
                // 1. Top Bar with Close Option on Top Right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Curation Source Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = appTheme.accent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = detail.sourceBadge.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.1.sp
                            ),
                            color = appTheme.accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Close option on top right
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(appTheme.pillBackground)
                            .testTag("close_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = appTheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 2. Playlist Details Section (Artwork + Title + Subtitle + Tracks)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val effectiveArtworkUrl = detail.artworkUrl?.takeIf { it.isNotBlank() }
                            ?: detail.songs.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl

                        if (!effectiveArtworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = effectiveArtworkUrl,
                                contentDescription = detail.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            detail.gradientColors.map { Color(it) }
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = detail.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = appTheme.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (detail.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = detail.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appTheme.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${detail.songs.size} tracks available",
                                style = MaterialTheme.typography.labelMedium,
                                color = appTheme.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Play All & Shuffle Buttons (Properly sequenced after details - not superimposed, moderate height)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(detail.songs) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("playlist_play_all_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = appTheme.accent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = { onShufflePlay(detail.songs.shuffled()) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("playlist_shuffle_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = appTheme.textPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = appTheme.accent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Songs List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(detail.songs, key = { index, s -> "${s.id}_$index" }) { index, song ->
                        val isCurrent = currentSong?.id == song.id
                        val isPlayingNow = isCurrent && isPlaying

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSongClick(song) }
                                .testTag("sheet_song_${song.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) appTheme.accent.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrent) appTheme.accent else appTheme.textMuted,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.width(26.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(appTheme.pillBackground),
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
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (isPlayingNow) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0x88000000)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AudioVisualizerWave(
                                                isPlaying = true,
                                                barCount = 4,
                                                maxHeight = 16.dp,
                                                barWidth = 2.dp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrent) appTheme.accent else appTheme.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = appTheme.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { onAddToPlaylist(song) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAdd,
                                        contentDescription = "Add to playlist",
                                        tint = appTheme.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
