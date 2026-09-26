package com.example.feature.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.core.ui.GammaCurvedWaveformSeekbar
import com.example.core.ui.GammaGestureOverlay
import com.example.data.provider.TrackDetailsInfo
import com.example.data.provider.TrackInfoProvider
import com.example.domain.model.TuningPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.GammaArtwork
import com.example.core.ui.GammaWaveformVisualizer
import com.example.domain.model.RepeatMode as DomainRepeatMode
import com.example.domain.model.Track
import com.example.ui.theme.GammaAuraBrush
import com.example.ui.theme.GammaBackground
import com.example.ui.theme.GammaDivider
import com.example.ui.theme.GammaFrequencyBrush
import com.example.ui.theme.GammaGlowCyan
import com.example.ui.theme.GammaGlowViolet
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaPrimaryVariant
import com.example.ui.theme.GammaSecondary
import com.example.ui.theme.GammaSurface
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaSurfaceHighlight
import com.example.ui.theme.GammaTextMuted
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GammaPlayerScreen(
    viewModel: PlayerViewModel,
    onCollapseClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVideoClosed: Boolean = false,
    onReopenVideo: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val lyricsOffsetMs by viewModel.lyricsOffsetMs.collectAsStateWithLifecycle()
    val playback = uiState.playbackState
    val track = playback.currentTrack

    var showLyricsSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSpecsSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showCustomSleepDialog by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf(20) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val specsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val equalizerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sleepTimerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var originalSpeedBeforeBoost by remember { mutableFloatStateOf(1.0f) }

    var trackDetails by remember { mutableStateOf<TrackDetailsInfo?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    LaunchedEffect(track?.id, showSpecsSheet) {
        if (showSpecsSheet && track != null) {
            isLoadingDetails = true
            trackDetails = withContext(Dispatchers.IO) {
                TrackInfoProvider.resolveTrackDetails(track)
            }
            isLoadingDetails = false
        }
    }

    // Breathing artwork pulse when playing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GammaBackground)
    ) {
        // Fluid blurred artwork background (Echo-Music / Apple Music style)
        if (uiState.playerStyle == PlayerStyle.FLUID_GLASS && track != null && track.artworkUrl.isNotBlank()) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(65.dp)
                    .alpha(0.38f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
        } else {
            // Deep ambient glow for Cyber Neon mode
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GammaGlowViolet, GammaBackground),
                            radius = 1200f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation & Frequency Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapseClick,
                    modifier = Modifier.testTag("player_collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse player",
                        tint = GammaTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Frequency badge or Sleep Timer countdown pill
                if (playback.isSleepTimerActive) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GammaSurfaceElevated)
                            .border(1.dp, GammaPrimary, CircleShape)
                            .clickable { showSleepTimerSheet = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("sleep_timer_active_badge")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Sleep timer active",
                                tint = GammaPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SLEEP: ${playback.formattedSleepTimer}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GammaPrimary
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GammaSurfaceElevated)
                            .border(1.dp, GammaGlowCyan, CircleShape)
                            .clickable { showSpecsSheet = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isAudioOnly = playback.isAudioOnlyMode || playback.isVideoUnavailable
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isAudioOnly) GammaGlowCyan else GammaPrimary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAudioOnly) "${track?.genre ?: "Music"} • AUDIO-ONLY STREAM" else "${track?.genre ?: "Music"} • NOW PLAYING",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAudioOnly) GammaGlowCyan else GammaPrimary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Player Style Switcher: Fluid Glass (Echo Music) vs Cyber Neon
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x28FFFFFF))
                            .border(1.dp, Color(0x35FFFFFF), CircleShape)
                            .clickable { viewModel.togglePlayerStyle() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("player_style_toggle_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Toggle Player Style",
                                tint = if (uiState.playerStyle == PlayerStyle.FLUID_GLASS) GammaGlowCyan else GammaPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.playerStyle == PlayerStyle.FLUID_GLASS) "Fluid" else "Cyber",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { showSpecsSheet = true },
                        modifier = Modifier.testTag("player_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Track specs",
                            tint = GammaTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hero Artwork or YouTube Video Area with Gesture Overlay
            val isYouTubePlaying = track?.let { it.youtubeVideoId.isNotEmpty() && it.localAudioUri.isEmpty() } ?: false
            val isAudioOnly = playback.isAudioOnlyMode || playback.isVideoUnavailable
            GammaGestureOverlay(
                onSkipBackward5 = { viewModel.seekBy(-5000L) },
                onSkipForward5 = { viewModel.seekBy(5000L) },
                onContinuousRewind = { viewModel.seekBy(-2000L) },
                onFastForwardStart = {
                    originalSpeedBeforeBoost = playback.playbackSpeed
                    viewModel.setPlaybackSpeed(2.0f)
                },
                onFastForwardEnd = {
                    viewModel.setPlaybackSpeed(originalSpeedBeforeBoost)
                },
                onSingleTap = {
                    viewModel.togglePlayPause()
                },
                modifier = Modifier
                    .size(width = 350.dp, height = 224.dp)
                    .scale(if (playback.isPlaying) pulseScale else 1.0f)
            ) {
                if (!isYouTubePlaying || isVideoClosed || isAudioOnly) {
                    // Background aura ring
                    Box(
                        modifier = Modifier
                            .size(width = 336.dp, height = 210.dp)
                            .background(GammaGlowCyan.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    )

                    GammaArtwork(
                        url = track?.artworkUrl ?: "",
                        contentDescription = "${track?.title} cover",
                        modifier = Modifier.size(214.dp),
                        shape = RoundedCornerShape(20.dp),
                        hasGlowBorder = true
                    )

                    if (isAudioOnly) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .border(1.dp, GammaGlowCyan, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("audio_only_badge")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Audio-Only Mode",
                                    tint = GammaGlowCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Audio-Only Mode • Video unavailable",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GammaGlowCyan,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else if (isYouTubePlaying && isVideoClosed) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .border(1.dp, GammaPrimary, RoundedCornerShape(16.dp))
                                .clickable { onReopenVideo() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("reopen_video_badge")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch Video",
                                    tint = GammaPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Watch Video",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GammaPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Visual Frequency Equalizer Bars
            GammaWaveformVisualizer(
                bands = playback.visualizerBands,
                isPlaying = playback.isPlaying,
                modifier = Modifier.padding(horizontal = 12.dp),
                height = 40.dp
            )

            // Track Title & Favorite Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "No Signal Active",
                        style = MaterialTheme.typography.displayMedium,
                        color = GammaTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track?.artist ?: "GAMMA Frequency Hub",
                        style = MaterialTheme.typography.titleMedium,
                        color = GammaTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Download Button
                    val isDownloading = playback.isDownloading && playback.downloadingTrackId == track?.id
                    val isDownloaded = track?.isDownloaded == true || (track != null && viewModel.playbackManager.isTrackDownloaded(track.id))
                    IconButton(
                        onClick = { viewModel.downloadCurrentTrack() },
                        modifier = Modifier.testTag("player_download_button")
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { playback.downloadProgress },
                                modifier = Modifier.size(24.dp),
                                color = GammaPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Default.Download,
                                contentDescription = if (isDownloaded) "Downloaded locally" else "Download locally",
                                tint = if (isDownloaded) GammaPrimary else GammaTextMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier.testTag("player_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (uiState.isFavorite) GammaPrimary else GammaTextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Mini Synced Karaoke Lyrics Preview (tap to open full lyrics sheet)
            GammaMiniSyncedLyricsBar(
                lyricsState = lyricsState,
                playbackPositionMs = playback.positionMs,
                lyricsOffsetMs = lyricsOffsetMs,
                onClick = { showLyricsSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Curved Waveform Seekbar
            GammaCurvedWaveformSeekbar(
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                onSeek = { targetMs ->
                    viewModel.seekTo(targetMs)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            // Control Deck (Shuffle, Prev, Hero Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.testTag("player_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playback.isShuffle) GammaPrimary else GammaTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Skip Previous
                IconButton(
                    onClick = { viewModel.skipPrevious() },
                    modifier = Modifier.testTag("player_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous signal",
                        tint = GammaTextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Hero Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GammaFrequencyBrush)
                        .clickable { viewModel.togglePlayPause() }
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (playback.isBuffering) {
                        CircularProgressIndicator(
                            color = GammaBackground,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playback.isPlaying) "Pause" else "Play",
                            tint = GammaBackground,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Skip Next
                IconButton(
                    onClick = { viewModel.skipNext() },
                    modifier = Modifier.testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next signal",
                        tint = GammaTextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode
                IconButton(
                    onClick = { viewModel.cycleRepeatMode() },
                    modifier = Modifier.testTag("player_repeat_button")
                ) {
                    val (icon, tint) = when (playback.repeatMode) {
                        DomainRepeatMode.OFF -> Pair(Icons.Default.Repeat, GammaTextMuted)
                        DomainRepeatMode.ALL -> Pair(Icons.Default.Repeat, GammaPrimary)
                        DomainRepeatMode.ONE -> Pair(Icons.Default.RepeatOne, GammaPrimary)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat mode",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Actions (Lyrics, Smart Queue, Equalizer, Speed, Sleep Timer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Synced Lyrics Button (LrcLib)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (lyricsState is LyricsUiState.Success) Color(0x3300FFFF) else GammaSurface)
                        .border(
                            width = 1.dp,
                            color = if (lyricsState is LyricsUiState.Success) GammaGlowCyan else GammaDivider,
                            shape = CircleShape
                        )
                        .clickable { showLyricsSheet = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("open_lyrics_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Live Synced Lyrics",
                            tint = if (lyricsState is LyricsUiState.Success) GammaGlowCyan else GammaTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (lyricsState is LyricsUiState.Success) "Lyrics (Live)" else "Lyrics",
                            style = MaterialTheme.typography.bodySmall,
                            color = GammaTextPrimary
                        )
                    }
                }

                // Smart Queue Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (playback.isSmartQueueEnabled) GammaSurfaceElevated else GammaSurface)
                        .border(
                            width = 1.dp,
                            brush = if (playback.isSmartQueueEnabled) GammaFrequencyBrush else Brush.linearGradient(listOf(GammaDivider, GammaDivider)),
                            shape = CircleShape
                        )
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("open_queue_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (playback.isSmartQueueEnabled) Icons.Default.AutoAwesome else Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = if (playback.isSmartQueueEnabled) GammaPrimary else GammaTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (playback.isSmartQueueEnabled) "Smart (${playback.queue.size})" else "Queue (${playback.queue.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = GammaTextPrimary
                        )
                    }
                }

                // Equalizer Tuning Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (playback.equalizerSettings.isEnabled) GammaSurfaceElevated else GammaSurface)
                        .border(
                            width = 1.dp,
                            brush = if (playback.equalizerSettings.isEnabled) GammaFrequencyBrush else Brush.linearGradient(listOf(GammaDivider, GammaDivider)),
                            shape = CircleShape
                        )
                        .clickable { showEqualizerSheet = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("open_equalizer_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Equalizer Tuning",
                            tint = if (playback.equalizerSettings.isEnabled) GammaPrimary else GammaTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "EQ: ${playback.equalizerSettings.currentPreset.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GammaTextPrimary
                        )
                    }
                }

                // Playback Speed Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (playback.playbackSpeed != 1.0f) GammaSurfaceElevated else GammaSurface)
                        .border(
                            width = 1.dp,
                            brush = if (playback.playbackSpeed != 1.0f) GammaFrequencyBrush else Brush.linearGradient(listOf(GammaDivider, GammaDivider)),
                            shape = CircleShape
                        )
                        .clickable { showSpeedSheet = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("open_speed_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Playback speed",
                            tint = if (playback.playbackSpeed != 1.0f) GammaPrimary else GammaTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "%.2gx".format(playback.playbackSpeed),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (playback.playbackSpeed != 1.0f) GammaPrimary else GammaTextPrimary
                        )
                    }
                }

                // Sleep Timer Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (playback.isSleepTimerActive) GammaSurfaceElevated else GammaSurface)
                        .border(
                            width = 1.dp,
                            brush = if (playback.isSleepTimerActive) GammaFrequencyBrush else Brush.linearGradient(listOf(GammaDivider, GammaDivider)),
                            shape = CircleShape
                        )
                        .clickable { showSleepTimerSheet = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("open_sleep_timer_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep timer",
                            tint = if (playback.isSleepTimerActive) GammaPrimary else GammaTextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (playback.isSleepTimerActive) playback.formattedSleepTimer else "Sleep",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (playback.isSleepTimerActive) FontWeight.Bold else FontWeight.Normal),
                            color = if (playback.isSleepTimerActive) GammaPrimary else GammaTextPrimary
                        )
                    }
                }
            }
        }
    }

    // Synced Karaoke Lyrics Bottom Sheet (LrcLib integration)
    if (showLyricsSheet) {
        val lyricsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        GammaSyncedLyricsSheet(
            track = track,
            lyricsState = lyricsState,
            playbackPositionMs = playback.positionMs,
            lyricsOffsetMs = lyricsOffsetMs,
            onAdjustOffset = viewModel::adjustLyricsOffset,
            sheetState = lyricsSheetState,
            onDismiss = { showLyricsSheet = false },
            onSeekTo = { targetMs -> viewModel.seekTo(targetMs) },
            onRetry = { viewModel.retryFetchLyrics() }
        )
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = queueSheetState,
            containerColor = GammaSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GammaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Queue",
                            style = MaterialTheme.typography.titleLarge,
                            color = GammaTextPrimary
                        )
                    }
                    Text(
                        text = "Clear Queue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GammaPrimary,
                        modifier = Modifier
                            .clickable { viewModel.clearQueue() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Smart Queue Mode & Resonance Affinity Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GammaSurfaceHighlight)
                        .border(1.dp, GammaFrequencyBrush, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Append Recommendations",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = GammaTextPrimary
                                )
                                Text(
                                    text = if (playback.isSmartQueueEnabled) {
                                        "Matches your session: ${playback.smartQueueProfile.displaySummary}"
                                    } else {
                                        "Smart queue paused"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GammaTextSecondary
                                )
                            }
                            Switch(
                                checked = playback.isSmartQueueEnabled,
                                onCheckedChange = { viewModel.toggleSmartQueue(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GammaPrimary,
                                    checkedTrackColor = GammaPrimaryVariant
                                )
                            )
                        }

                        if (playback.isSmartQueueEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GammaPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = playback.smartQueueProfile.dominantGenre,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GammaPrimary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GammaSecondary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${playback.smartQueueProfile.totalAnalyzed} Tracks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GammaSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GammaPrimary)
                                        .clickable { viewModel.appendSmartRecommendations(3) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = GammaBackground,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+ Append 3",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GammaBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    itemsIndexed(playback.queue) { index, queueTrack ->
                        val isCurr = index == playback.queueIndex

                        // Header separating current track and Up Next queue
                        if (index == playback.queueIndex + 1) {
                            Text(
                                text = "UP NEXT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = GammaPrimary,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurr) GammaSurfaceHighlight else Color.Transparent)
                                .clickable { viewModel.jumpToQueueIndex(index) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCurr) "▶" else "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal),
                                color = if (isCurr) GammaPrimary else GammaTextMuted,
                                modifier = Modifier.width(22.dp)
                            )
                            GammaArtwork(
                                url = queueTrack.artworkUrl,
                                contentDescription = null,
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = queueTrack.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCurr) GammaPrimary else GammaTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = queueTrack.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GammaTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (queueTrack.source == "smart_recommendation") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "✦ Auto-Rec",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GammaPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = queueTrack.genre,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GammaTextMuted
                                    )
                                }
                            }

                            // Reorder Controls (Move Up / Down)
                            if (index > 0) {
                                IconButton(
                                    onClick = { viewModel.reorderQueue(index, index - 1) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Move up",
                                        tint = GammaTextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (index < playback.queue.lastIndex) {
                                IconButton(
                                    onClick = { viewModel.reorderQueue(index, index + 1) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Move down",
                                        tint = GammaTextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.removeFromQueue(index) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove from queue",
                                    tint = GammaTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Specs Bottom Sheet (Track Information, Songwriters, Artist Bio & Technical Audio Specs)
    if (showSpecsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpecsSheet = false },
            sheetState = specsSheetState,
            containerColor = GammaSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GammaPrimary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = GammaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Track & Artist Information",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GammaTextPrimary
                            )
                            Text(
                                text = "Credits • Songwriters • Audio Architecture",
                                style = MaterialTheme.typography.labelSmall,
                                color = GammaTextSecondary
                            )
                        }
                    }

                    TextButton(onClick = { showSpecsSheet = false }) {
                        Text("Done", color = GammaPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // Track Artwork Banner & Quick Details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GammaBackground)
                        .border(1.dp, GammaDivider.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GammaGlowCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        GammaArtwork(
                            url = track?.artworkUrl.orEmpty(),
                            contentDescription = track?.title,
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: "Unknown Track",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = GammaTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track?.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GammaTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GammaPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${track?.frequencyHz ?: 432}Hz Solfeggio",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GammaPrimary
                                )
                            }
                            if (!trackDetails?.releaseYear.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GammaSurfaceHighlight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = trackDetails?.releaseYear.orEmpty(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GammaTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 1: Songwriters & Production Credits
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SONG CREDITS & WRITERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = GammaSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GammaBackground)
                            .border(1.dp, GammaDivider.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SpecRow(
                                "Written By",
                                trackDetails?.songwriters ?: "${track?.artist ?: "Unknown"} (Songwriter)"
                            )
                            if (!trackDetails?.producers.isNullOrBlank()) {
                                SpecRow("Producer(s)", trackDetails?.producers.orEmpty())
                            }
                            SpecRow(
                                "Album / Collection",
                                trackDetails?.album ?: track?.albumTitle?.ifBlank { "Original Single" } ?: "Single"
                            )
                            if (!trackDetails?.releaseYear.isNullOrBlank()) {
                                SpecRow("Release Date", trackDetails?.releaseYear.orEmpty())
                            }
                            SpecRow(
                                "Genre Classification",
                                trackDetails?.genre ?: track?.genre ?: "Contemporary Music"
                            )
                        }
                    }
                }

                // Section 2: Song Story & Description
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ABOUT THIS SONG",
                        style = MaterialTheme.typography.labelSmall,
                        color = GammaPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GammaBackground)
                            .border(1.dp, GammaDivider.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        if (isLoadingDetails) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = GammaPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Gathering songwriting and encyclopedic overview...", style = MaterialTheme.typography.bodySmall, color = GammaTextMuted)
                            }
                        } else {
                            val descText = trackDetails?.description
                                ?: "“${track?.title}” by ${track?.artist} is streamed in harmonic fidelity on GAMA. Explore live synchronized lyrics, real-time 5-band DSP equalization, and solfeggio frequency resonance."
                            Text(
                                text = descText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GammaTextPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // Section 3: Artist Biography (if available)
                if (!trackDetails?.artistBio.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ARTIST PROFILE • ${track?.artist?.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GammaGlowCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(GammaBackground)
                                .border(1.dp, GammaDivider.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                        ) {
                            Text(
                                text = trackDetails?.artistBio.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = GammaTextPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // Section 4: Technical Acoustic & Audio Architecture Specs
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AUDIO ARCHITECTURE & DSP SPECS",
                        style = MaterialTheme.typography.labelSmall,
                        color = GammaTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GammaBackground)
                            .border(1.dp, GammaDivider.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SpecRow("Harmonic Tuning", "${track?.frequencyHz ?: 432}Hz Natural Solfeggio")
                            SpecRow("DSP Equalizer Engine", "5-Band IIR Biquad Filters")
                            SpecRow("Audio Quality Profile", "256 kbps High-Definition AAC/Opus")
                            SpecRow("Playback Mechanism", "YouTube Authorized IFrame Embed")
                            if (!trackDetails?.viewCount.isNullOrBlank()) {
                                SpecRow("Global Video Streams", trackDetails?.viewCount.orEmpty())
                            }
                            SpecRow("Media ID", track?.youtubeVideoId?.ifBlank { track.id } ?: "local_track")
                        }
                    }
                }
            }
        }
    }

    // Equalizer Bottom Sheet
    if (showEqualizerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEqualizerSheet = false },
            sheetState = equalizerSheetState,
            containerColor = GammaSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Header with EQ bypass switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = GammaPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "GAMA Equalizer & Tuning",
                                style = MaterialTheme.typography.titleLarge,
                                color = GammaTextPrimary
                            )
                            Text(
                                text = "5-Band Real-Time DSP Audio Processing",
                                style = MaterialTheme.typography.bodySmall,
                                color = GammaTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = playback.equalizerSettings.isEnabled,
                        onCheckedChange = { viewModel.toggleEqualizer(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GammaPrimary,
                            checkedTrackColor = GammaPrimaryVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets Carousel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TUNING PRESETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = GammaTextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Active: ${playback.equalizerSettings.currentPreset.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GammaPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TuningPreset.values()) { preset ->
                        val isSelected = playback.equalizerSettings.currentPreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) GammaPrimary else GammaSurfaceHighlight)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) GammaPrimary else GammaDivider,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.setTuningPreset(preset) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) GammaBackground else GammaTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5 Equalizer Bands
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GammaBackground)
                        .border(1.dp, GammaFrequencyBrush, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playback.equalizerSettings.bands.forEach { band ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = band.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GammaTextPrimary
                                    )
                                    val sign = if (band.gainDb > 0f) "+" else ""
                                    Text(
                                        text = "$sign%.1f dB".format(band.gainDb),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (band.gainDb != 0f) GammaPrimary else GammaTextMuted
                                    )
                                }
                                Slider(
                                    value = band.gainDb,
                                    onValueChange = { newDb ->
                                        viewModel.setEqualizerBand(band.index, newDb)
                                    },
                                    valueRange = -12f..12f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GammaPrimary,
                                        activeTrackColor = GammaPrimary,
                                        inactiveTrackColor = GammaSurfaceHighlight
                                    ),
                                    enabled = playback.equalizerSettings.isEnabled
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Master Pre-Amp Gain
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Master Tuning Gain",
                        style = MaterialTheme.typography.bodySmall,
                        color = GammaTextSecondary
                    )
                    Text(
                        text = "%.1fx".format(playback.equalizerSettings.masterGain),
                        style = MaterialTheme.typography.labelSmall,
                        color = GammaSecondary
                    )
                }
                Slider(
                    value = playback.equalizerSettings.masterGain,
                    onValueChange = { viewModel.setMasterGain(it) },
                    valueRange = 0.5f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = GammaSecondary,
                        activeTrackColor = GammaSecondary,
                        inactiveTrackColor = GammaSurfaceHighlight
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }

    // Sleep Timer Bottom Sheet
    if (showSleepTimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepTimerSheet = false },
            sheetState = sleepTimerSheetState,
            containerColor = GammaSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = GammaPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sleep Timer",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GammaTextPrimary
                        )
                    }

                    if (playback.isSleepTimerActive) {
                        Text(
                            text = "Turn Off",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = GammaPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.cancelSleepTimer()
                                    showSleepTimerSheet = false
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Timer Status Card
                if (playback.isSleepTimerActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GammaBackground)
                            .border(1.5.dp, GammaFrequencyBrush, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "AUDIO WILL PAUSE IN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = GammaTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playback.formattedSleepTimer,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 36.sp
                                ),
                                color = GammaPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.extendSleepTimer(15) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GammaSurfaceHighlight),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+15 Mins", color = GammaPrimary, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.cancelSleepTimer() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GammaSurfaceHighlight),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancel", color = GammaTextSecondary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                Text(
                    text = "SET SLEEP DURATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = GammaTextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val presets = listOf(
                    Pair("15 Minutes", 15),
                    Pair("30 Minutes", 30),
                    Pair("45 Minutes", 45),
                    Pair("60 Minutes", 60)
                )

                presets.forEach { (label, minutes) ->
                    val isSelected = playback.isSleepTimerActive && !playback.isSleepTimerEndOfTrack && (playback.sleepTimerInitialSeconds == minutes * 60L)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GammaSurfaceHighlight else Color.Transparent)
                            .clickable {
                                viewModel.startSleepTimer(minutes, endOfTrack = false)
                                showSleepTimerSheet = false
                            }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) GammaPrimary else GammaTextPrimary
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = GammaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // End of track option
                val isEndOfTrackSelected = playback.isSleepTimerActive && playback.isSleepTimerEndOfTrack
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isEndOfTrackSelected) GammaSurfaceHighlight else Color.Transparent)
                        .clickable {
                            viewModel.startSleepTimer(0, endOfTrack = true)
                            showSleepTimerSheet = false
                        }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "End of Current Track",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isEndOfTrackSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isEndOfTrackSelected) GammaPrimary else GammaTextPrimary
                        )
                        Text(
                            text = "Pauses when this song finishes",
                            style = MaterialTheme.typography.bodySmall,
                            color = GammaTextSecondary
                        )
                    }
                    if (isEndOfTrackSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = GammaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Custom Duration Option
                val isCustomSelected = playback.isSleepTimerActive && !playback.isSleepTimerEndOfTrack &&
                    presets.none { it.second * 60L == playback.sleepTimerInitialSeconds }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCustomSelected) GammaSurfaceHighlight else Color.Transparent)
                        .clickable {
                            showSleepTimerSheet = false
                            showCustomSleepDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .testTag("sleep_timer_custom_option"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCustomSelected && playback.sleepTimerInitialSeconds != null) {
                                "Custom (${playback.sleepTimerInitialSeconds!! / 60} min)"
                            } else {
                                "Custom Duration..."
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isCustomSelected) GammaPrimary else GammaTextPrimary
                        )
                        Text(
                            text = "Choose any time from 1 to 180 minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = GammaTextSecondary
                        )
                    }
                    if (isCustomSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = GammaPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = GammaTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "🌙 Music volume will gently fade out over the final 30 seconds before pausing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = GammaTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp, start = 8.dp, end = 8.dp)
                )
            }
        }
    }

    // Custom Sleep Timer Dialog
    if (showCustomSleepDialog) {
        AlertDialog(
            onDismissRequest = { showCustomSleepDialog = false },
            containerColor = GammaSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = GammaPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Custom Sleep Timer",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = GammaTextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$customMinutes min",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 40.sp
                        ),
                        color = GammaPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Audio will gently fade out and pause",
                        style = MaterialTheme.typography.bodySmall,
                        color = GammaTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = customMinutes.toFloat(),
                        onValueChange = { customMinutes = it.toInt().coerceIn(1, 180) },
                        valueRange = 1f..180f,
                        steps = 178,
                        colors = SliderDefaults.colors(
                            thumbColor = GammaPrimary,
                            activeTrackColor = GammaPrimary,
                            inactiveTrackColor = GammaSurfaceHighlight
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { customMinutes = (customMinutes - 5).coerceAtLeast(1) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-5m", color = GammaPrimary)
                        }
                        OutlinedButton(
                            onClick = { customMinutes = (customMinutes + 5).coerceAtMost(180) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+5m", color = GammaPrimary)
                        }
                        OutlinedButton(
                            onClick = { customMinutes = (customMinutes + 15).coerceAtMost(180) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+15m", color = GammaPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startSleepTimer(customMinutes, endOfTrack = false)
                        showCustomSleepDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GammaPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Start Timer", color = GammaBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomSleepDialog = false }) {
                    Text("Cancel", color = GammaTextSecondary)
                }
            }
        )
    }

    // Playback Speed Bottom Sheet
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = speedSheetState,
            containerColor = GammaSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = GammaPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = GammaTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val speedOptions = listOf(
                    Pair(0.5f, "0.5x (Slow study)"),
                    Pair(0.75f, "0.75x (Slowed & reverb tempo)"),
                    Pair(0.9f, "0.9x (Subtle chill)"),
                    Pair(1.0f, "1.0x (Standard normal)"),
                    Pair(1.25f, "1.25x (Brisk tempo)"),
                    Pair(1.5f, "1.5x (Fast pace)"),
                    Pair(2.0f, "2.0x (Double speed)")
                )

                speedOptions.forEach { (speed, label) ->
                    val isSelected = kotlin.math.abs(playback.playbackSpeed - speed) < 0.05f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) GammaSurfaceHighlight else Color.Transparent)
                            .clickable {
                                viewModel.setPlaybackSpeed(speed)
                                showSpeedSheet = false
                            }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) GammaPrimary else GammaTextPrimary
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = GammaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = GammaTextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = GammaPrimary)
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
