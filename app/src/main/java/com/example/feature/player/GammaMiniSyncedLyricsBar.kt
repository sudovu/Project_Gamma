package com.example.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GammaGlowCyan
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary

@Composable
fun GammaMiniSyncedLyricsBar(
    lyricsState: LyricsUiState,
    playbackPositionMs: Long,
    lyricsOffsetMs: Long = 0L,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyricsState !is LyricsUiState.Success || lyricsState.lyrics.lines.isEmpty()) {
        return
    }

    val lines = lyricsState.lyrics.lines
    val effectivePositionMs = (playbackPositionMs + lyricsOffsetMs).coerceAtLeast(0L)
    val currentLineIndex = remember(effectivePositionMs, lines) {
        val idx = lines.indexOfLast { it.timeMs <= effectivePositionMs }
        if (idx >= 0) idx else 0
    }
    val currentLineText = lines.getOrNull(currentLineIndex)?.text ?: ""
    val nextLineText = lines.getOrNull(currentLineIndex + 1)?.text

    if (currentLineText.isBlank() && nextLineText.isNullOrBlank()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x22FFFFFF))
            .border(1.dp, Color(0x3300FFFF).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("mini_synced_lyrics_bar"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = GammaGlowCyan,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Line 1: Active lyric line (bold, glowing white)
                AnimatedContent(
                    targetState = currentLineText,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { height -> height / 2 })
                            .togetherWith(fadeOut() + slideOutVertically { height -> -height / 2 })
                    },
                    label = "ActiveLyricScroll"
                ) { lineText ->
                    Text(
                        text = lineText.ifBlank { "♪ ♪ ♪" },
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Line 2: Upcoming next lyric line (preview / sing-along ahead)
                if (!nextLineText.isNullOrBlank()) {
                    AnimatedContent(
                        targetState = nextLineText,
                        transitionSpec = {
                            fadeIn().togetherWith(fadeOut())
                        },
                        label = "UpcomingLyricFade"
                    ) { nextText ->
                        Text(
                            text = nextText,
                            color = Color(0x99FFFFFF),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
