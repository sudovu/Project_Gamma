package com.example.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PlaybackState
import com.example.ui.theme.GammaBackground
import com.example.ui.theme.GammaDivider
import com.example.ui.theme.GammaGlowCyan
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaSurfaceGlass
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GammaMiniPlayer(
    playbackState: PlaybackState,
    onExpandClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack

    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        if (track == null) return@AnimatedVisibility

        val coroutineScope = rememberCoroutineScope()
        val offsetX = remember { Animatable(0f) }
        var totalDrag by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(track.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDrag = 0f
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (totalDrag < -100f) {
                                    // Swiped Left -> Skip Next
                                    onNextClick()
                                } else if (totalDrag > 100f) {
                                    // Swiped Right -> Skip Previous
                                    onPreviousClick()
                                }
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount * 0.7f)
                            }
                        }
                    )
                }
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(GammaSurfaceElevated.copy(alpha = 0.96f))
                .border(1.dp, GammaDivider.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                .clickable(onClick = onExpandClick)
                .testTag("mini_player")
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork with rounded capsule corners & glow border
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GammaGlowCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        GammaArtwork(
                            url = track.artworkUrl,
                            contentDescription = "${track.title} artwork",
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Track details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = GammaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GammaTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (track.frequencyHz != 440) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x3300FFFF))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${track.frequencyHz}Hz",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GammaGlowCyan
                                    )
                                }
                            }
                        }
                    }

                    // Mini equalizer visualizer bars
                    MiniEqualizerBars(isPlaying = playbackState.isPlaying)

                    Spacer(modifier = Modifier.width(6.dp))

                    // Play/Pause button (smooth pill button)
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(38.dp)
                            .background(GammaPrimary, CircleShape)
                            .testTag("mini_play_pause_button")
                    ) {
                        if (playbackState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = GammaBackground,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = GammaBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Next track button
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("mini_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next track",
                            tint = GammaTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Progress Bar line at the bottom
                LinearProgressIndicator(
                    progress = { playbackState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = GammaPrimary,
                    trackColor = GammaDivider.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun MiniEqualizerBars(isPlaying: Boolean) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = if (isPlaying) listOf(14.dp, 20.dp, 10.dp) else listOf(4.dp, 4.dp, 4.dp)
        heights.forEachIndexed { _, h ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(2.5.dp)
                    .height(h)
                    .clip(CircleShape)
                    .background(if (isPlaying) GammaPrimary else GammaTextSecondary.copy(alpha = 0.5f))
            )
        }
    }
}
