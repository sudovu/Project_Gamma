package com.example.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.provider.TrackLyrics
import com.example.domain.model.Track
import com.example.ui.theme.GammaBackground
import com.example.ui.theme.GammaDivider
import com.example.ui.theme.GammaGlowCyan
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaSurfaceGlass
import com.example.ui.theme.GammaTextMuted
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GammaSyncedLyricsSheet(
    track: Track?,
    lyricsState: LyricsUiState,
    playbackPositionMs: Long,
    lyricsOffsetMs: Long = 0L,
    onAdjustOffset: ((Long) -> Unit)? = null,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GammaBackground.copy(alpha = 0.96f),
        dragHandle = null,
        modifier = modifier
            .fillMaxSize()
            .testTag("lyrics_modal_sheet")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fluid blurred artwork background (inspired by Echo Music / Apple Music)
            if (track != null && track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .alpha(0.28f)
                )
            }

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header with Track details, LrcLib badge, and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = GammaGlowCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE LYRICS",
                                style = MaterialTheme.typography.labelSmall,
                                color = GammaGlowCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3300FFFF))
                                    .border(0.5.dp, GammaGlowCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LrcLib",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = track?.title ?: "Unknown Track",
                            style = MaterialTheme.typography.titleMedium,
                            color = GammaTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = track?.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = GammaTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x28FFFFFF), CircleShape)
                            .border(1.dp, Color(0x35FFFFFF), CircleShape)
                            .testTag("lyrics_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Content area
                when (lyricsState) {
                    is LyricsUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("lyrics_loading"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = GammaPrimary,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Syncing with LrcLib database...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GammaTextSecondary
                                )
                            }
                        }
                    }

                    is LyricsUiState.Empty, is LyricsUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("lyrics_empty"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color(0x20FFFFFF), CircleShape)
                                        .border(1.dp, Color(0x30FFFFFF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = GammaTextMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No synchronized lyrics found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GammaTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Instrumental tracks or rare recordings might not have timestamped lyrics yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GammaTextMuted,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0x28FFFFFF))
                                        .border(1.dp, GammaPrimary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                        .clickable(onClick = onRetry)
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = GammaPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Retry Search",
                                        color = GammaPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    is LyricsUiState.Success -> {
                        val lyrics = lyricsState.lyrics
                        if (lyrics.lines.isNotEmpty()) {
                            val listState = rememberLazyListState()

                            // Identify current line index based on effective playback position with offset
                            val effectivePositionMs = (playbackPositionMs + lyricsOffsetMs).coerceAtLeast(0L)
                            val activeIndex = remember(effectivePositionMs, lyrics.lines) {
                                val idx = lyrics.lines.indexOfLast { it.timeMs <= effectivePositionMs }
                                if (idx >= 0) idx else 0
                            }

                            // Auto-scroll to current line
                            LaunchedEffect(activeIndex) {
                                if (lyrics.lines.isNotEmpty()) {
                                    val targetScrollIndex = (activeIndex - 2).coerceAtLeast(0)
                                    listState.animateScrollToItem(targetScrollIndex)
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("lyrics_lines_list"),
                                contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                itemsIndexed(lyrics.lines) { index, line ->
                                    val isActive = index == activeIndex
                                    val isUpcoming = index > activeIndex

                                    val textColor by animateColorAsState(
                                        targetValue = when {
                                            isActive -> Color.White
                                            isUpcoming -> Color(0x99FFFFFF)
                                            else -> Color(0x55FFFFFF)
                                        },
                                        label = "lyricTextColor"
                                    )

                                    val scale by animateFloatAsState(
                                        targetValue = if (isActive) 1.04f else 1.0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "lyricScale"
                                    )

                                    // Word-by-word progressive karaoke highlighting
                                    val nextLine = lyrics.lines.getOrNull(index + 1)
                                    val lineDurationMs = ((nextLine?.timeMs ?: (line.timeMs + 4000L)) - line.timeMs).coerceAtLeast(800L)
                                    val elapsedInLine = (effectivePositionMs - line.timeMs).coerceAtLeast(0L)
                                    val lineProgress = (elapsedInLine.toFloat() / lineDurationMs.toFloat()).coerceIn(0f, 1f)
                                    val words = remember(line.text) { line.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() } }
                                    val activeWordIdx = (lineProgress * words.size).toInt().coerceIn(0, (words.size - 1).coerceAtLeast(0))

                                    val lyricDisplay = remember(line.text, activeWordIdx, isActive) {
                                        if (!isActive || words.isEmpty()) {
                                            AnnotatedString(line.text)
                                        } else {
                                            buildAnnotatedString {
                                                words.forEachIndexed { wIdx, word ->
                                                    if (wIdx > 0) append(" ")
                                                    when {
                                                        wIdx < activeWordIdx -> {
                                                            // Sung word: bright bold white
                                                            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                                                append(word)
                                                            }
                                                        }
                                                        wIdx == activeWordIdx -> {
                                                            // Actively sung word: vivid cyan glow
                                                            withStyle(
                                                                SpanStyle(
                                                                    color = GammaGlowCyan,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    shadow = Shadow(color = GammaGlowCyan.copy(alpha = 0.8f), blurRadius = 8f)
                                                                )
                                                            ) {
                                                                append(word)
                                                            }
                                                        }
                                                        else -> {
                                                            // Upcoming word: soft translucent white
                                                            withStyle(SpanStyle(color = Color(0x88FFFFFF), fontWeight = FontWeight.Medium)) {
                                                                append(word)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .scale(scale)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSeekTo(line.timeMs) }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                            .testTag(if (isActive) "active_lyric_line" else "lyric_line")
                                    ) {
                                        Column {
                                            Text(
                                                text = lyricDisplay,
                                                fontSize = if (isActive) 23.sp else 18.sp,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                color = textColor,
                                                lineHeight = if (isActive) 32.sp else 26.sp
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 36.dp, height = 3.5.dp)
                                                        .clip(CircleShape)
                                                        .background(GammaGlowCyan)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (lyrics.plainLyrics != null) {
                            // Plain lyrics view
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp),
                                contentPadding = PaddingValues(bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(lyrics.plainLyrics.lines().filter { it.isNotBlank() }) { _, line ->
                                    Text(
                                        text = line,
                                        fontSize = 18.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        lineHeight = 26.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Sync Calibration Capsule (Echo Music Best-Feature Integration)
            if (lyricsState is LyricsUiState.Success && lyricsState.lyrics.lines.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xEE141923))
                            .border(
                                1.dp,
                                if (lyricsOffsetMs != 0L) GammaGlowCyan else Color(0x33FFFFFF),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "-2s",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAdjustOffset?.invoke(-2000L) }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                                .testTag("lyrics_offset_minus_2s")
                        )

                        Text(
                            text = "-0.5s",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAdjustOffset?.invoke(-500L) }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                                .testTag("lyrics_offset_minus")
                        )

                        val offsetSeconds = lyricsOffsetMs / 1000.0
                        val offsetText = if (lyricsOffsetMs == 0L) "Sync: 0.0s" else if (lyricsOffsetMs > 0) "Sync: +${String.format(java.util.Locale.US, "%.1f", offsetSeconds)}s" else "Sync: ${String.format(java.util.Locale.US, "%.1f", offsetSeconds)}s"
                        Text(
                            text = offsetText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (lyricsOffsetMs != 0L) GammaGlowCyan else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAdjustOffset?.invoke(-lyricsOffsetMs) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("lyrics_offset_reset")
                        )

                        Text(
                            text = "+0.5s",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAdjustOffset?.invoke(500L) }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                                .testTag("lyrics_offset_plus")
                        )

                        Text(
                            text = "+2s",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAdjustOffset?.invoke(2000L) }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                                .testTag("lyrics_offset_plus_2s")
                        )
                    }
                }
            }
        }
    }
}
