package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.entity.Song
import com.example.ui.theme.LocalAppThemeColors

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    isBuffering: Boolean = false,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppThemeColors.current
    val progressFraction = (progressMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 350, easing = LinearEasing),
        label = "mini_progress_fluid"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mini_player_bar"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle liquid top progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = appTheme.accent,
                trackColor = if (appTheme.isLight) Color(0x180F172A) else Color(0x22FFFFFF)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Soundwave / Artwork avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x337928CA)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!song.artworkUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = song.artworkUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp)
                        )
                        if (isPlaying) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0x77000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                AudioVisualizerWave(
                                    isPlaying = true,
                                    barCount = 5,
                                    maxHeight = 20.dp,
                                    barWidth = 2.dp
                                )
                            }
                        }
                    } else {
                        AudioVisualizerWave(
                            isPlaying = isPlaying,
                            barCount = 7,
                            maxHeight = 24.dp,
                            barWidth = 2.5.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = appTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.audioQuality}",
                        style = MaterialTheme.typography.bodySmall,
                        color = appTheme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controls
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_play_pause_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (appTheme.isLight) Color(0x2200796B) else Color(0x3300F5D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = appTheme.accent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = {
                                    (scaleIn(tween(180)) + fadeIn(tween(180)))
                                        .togetherWith(scaleOut(tween(140)) + fadeOut(tween(140)))
                                },
                                label = "mini_play_pause_anim"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = appTheme.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Next",
                        tint = appTheme.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
