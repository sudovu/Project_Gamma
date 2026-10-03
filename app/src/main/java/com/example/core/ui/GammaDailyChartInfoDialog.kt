package com.example.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.YouTubeProvider
import com.example.domain.model.Track
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaSecondary
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaSurfaceHighlight
import com.example.ui.theme.GammaTextMuted
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary

@Composable
fun GammaDailyChartInfoDialog(
    track: Track?,
    onDismiss: () -> Unit,
    onPlayTrack: ((Track) -> Unit)? = null
) {
    val dateStr = YouTubeProvider.getDailyChartDateFormatted()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GammaPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = GammaPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (track != null) "Daily Chart Info" else "Top 100 Songs of the Day",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GammaTextPrimary
                    )
                    Text(
                        text = "Updated Daily • $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = GammaSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (track != null) {
                    // Track Card Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GammaSurfaceHighlight)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GammaArtwork(
                            url = track.artworkUrl,
                            contentDescription = track.title,
                            modifier = Modifier.size(54.dp),
                            hasGlowBorder = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GammaTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GammaTextSecondary,
                                maxLines = 1
                            )
                            if (track.dailyRank > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val rankColor = when (track.dailyRank) {
                                    1 -> Color(0xFFFFD700)
                                    2 -> Color(0xFFC0C0C0)
                                    3 -> Color(0xFFCD7F32)
                                    else -> GammaPrimary
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(rankColor.copy(alpha = 0.2f))
                                        .border(1.dp, rankColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "#${track.dailyRank} • ${track.chartTrend.ifBlank { "DAILY CHART" }}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = rankColor
                                    )
                                }
                            }
                        }
                    }

                    // Key stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GammaSurfaceHighlight.copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Equalizer, contentDescription = null, modifier = Modifier.size(14.dp), tint = GammaSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Genre", style = MaterialTheme.typography.labelSmall, color = GammaTextMuted)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.genre.ifBlank { "Pop Hits" },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = GammaTextPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GammaSurfaceHighlight.copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(14.dp), tint = GammaPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Audio Fidelity", style = MaterialTheme.typography.labelSmall, color = GammaTextMuted)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "HD Mastered",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = GammaPrimary
                                )
                            }
                        }
                    }
                }

                // Daily Auto-Update Info Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GammaPrimary.copy(alpha = 0.08f))
                        .border(1.dp, GammaPrimary.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = GammaPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Everyday Automatic Daily Refresh",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GammaPrimary
                            )
                        }
                        Text(
                            text = "GAMA's neural audio engine automatically fetches and updates the Top 100 songs of the day every 24 hours at midnight. Daily chart ranks are calculated from global streaming velocity and YouTube Music daily charts.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                            color = GammaTextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (track != null && onPlayTrack != null) {
                Button(
                    onClick = {
                        onPlayTrack(track)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GammaPrimary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play Song", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GammaPrimary)
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        },
        dismissButton = {
            if (track != null && onPlayTrack != null) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Close", color = GammaTextSecondary)
                }
            }
        },
        containerColor = GammaSurfaceElevated
    )
}
