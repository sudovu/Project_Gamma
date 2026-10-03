package com.example.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.provider.CuratedFrequencies
import com.example.domain.model.Track
import com.example.ui.theme.GammaDivider
import com.example.ui.theme.GammaGlowCyan
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaSurfaceHighlight
import com.example.ui.theme.GammaTextMuted
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary

enum class ImportPlatform(val label: String, val iconColor: Color) {
    ALL("All", Color(0xFF00F5D4)),
    SPOTIFY("Spotify", Color(0xFF1DB954)),
    YOUTUBE("YouTube Music", Color(0xFFFF0000)),
    APPLE("Apple Music", Color(0xFFFC3C44)),
    LINK("Direct Link", Color(0xFF9D4EDD))
}

data class RelatablePlaylist(
    val title: String,
    val platform: ImportPlatform,
    val description: String,
    val coverUrl: String,
    val tracks: List<Track>
)

@Composable
fun GammaImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImportConfirmed: (name: String, tracks: List<Track>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf(ImportPlatform.ALL) }
    var directLinkUrl by remember { mutableStateOf("") }
    var directLinkTitle by remember { mutableStateOf("") }

    val allRelatablePlaylists = remember {
        val tracks = CuratedFrequencies.allTracks
        listOf(
            RelatablePlaylist(
                title = "Today's Top Hits (Spotify)",
                platform = ImportPlatform.SPOTIFY,
                description = "Hottest global tracks streamed across Spotify today",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
                tracks = tracks.take(6)
            ),
            RelatablePlaylist(
                title = "Spotify Rock & Metal Classics",
                platform = ImportPlatform.SPOTIFY,
                description = "Legendary energetic anthems and high-gain rock soundwaves",
                coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=300",
                tracks = tracks.filter { it.genre.contains("Rock", true) || it.genre.contains("Metal", true) }.ifEmpty { tracks.take(5) }
            ),
            RelatablePlaylist(
                title = "Spotify Chill Lofi Beats",
                platform = ImportPlatform.SPOTIFY,
                description = "Calm lo-fi study & relaxation beats from Spotify playlists",
                coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=300",
                tracks = tracks.filter { it.frequencyHz in listOf(432, 528) }.ifEmpty { tracks.take(4) }
            ),
            RelatablePlaylist(
                title = "YouTube Music Trending Waves",
                platform = ImportPlatform.YOUTUBE,
                description = "Viral trending tracks and chart toppers from YouTube Music",
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
                tracks = tracks.takeLast(6)
            ),
            RelatablePlaylist(
                title = "YouTube Miraculous Ambient Frequencies",
                platform = ImportPlatform.YOUTUBE,
                description = "Sacred universal harmonic vibrations & deep meditation",
                coverUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=300",
                tracks = tracks.filter { it.frequencyHz == 432 }.ifEmpty { tracks.take(4) }
            ),
            RelatablePlaylist(
                title = "YouTube Transformation & Harmony",
                platform = ImportPlatform.YOUTUBE,
                description = "Healing frequency soundscapes recorded for rejuvenation",
                coverUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=300",
                tracks = tracks.filter { it.frequencyHz == 528 }.ifEmpty { tracks.take(4) }
            ),
            RelatablePlaylist(
                title = "Apple Music Pop Essentials",
                platform = ImportPlatform.APPLE,
                description = "Definitive pop hits curated from Apple Music playlists",
                coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300",
                tracks = tracks.filter { it.genre.contains("Pop", true) }.ifEmpty { tracks.take(5) }
            ),
            RelatablePlaylist(
                title = "Apple Music Spatial Audio Focus",
                platform = ImportPlatform.APPLE,
                description = "Immersive multi-layered acoustic and electronic clarity",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
                tracks = tracks.take(5)
            ),
            RelatablePlaylist(
                title = "Gym & Workout Pump Energy",
                platform = ImportPlatform.SPOTIFY,
                description = "High-octane gym motivation & workout soundtrack",
                coverUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=300",
                tracks = tracks.take(6)
            ),
            RelatablePlaylist(
                title = "Deep Sleep & Delta Resonances",
                platform = ImportPlatform.YOUTUBE,
                description = "Subtle binaural delta tones for restorative restful sleep",
                coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=300",
                tracks = tracks.filter { it.frequencyHz == 432 || it.frequencyHz == 528 }.ifEmpty { tracks.take(4) }
            )
        )
    }

    val filteredPlaylists = remember(searchQuery, selectedPlatform) {
        allRelatablePlaylists.filter { playlist ->
            val matchesPlatform = selectedPlatform == ImportPlatform.ALL || playlist.platform == selectedPlatform
            val matchesQuery = searchQuery.isBlank() ||
                    playlist.title.contains(searchQuery, ignoreCase = true) ||
                    playlist.description.contains(searchQuery, ignoreCase = true) ||
                    playlist.platform.label.contains(searchQuery, ignoreCase = true)
            matchesPlatform && matchesQuery
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, GammaGlowCyan, RoundedCornerShape(24.dp)),
            color = GammaSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GammaPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = GammaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Import Playlists",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = GammaTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GammaTextMuted
                        )
                    }
                }

                Text(
                    text = "Search relatable available playlists across Spotify, YouTube Music, and Apple Music to import with one tap.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GammaTextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Platform Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ImportPlatform.entries) { platform ->
                        val isSelected = selectedPlatform == platform
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) platform.iconColor.copy(alpha = 0.2f)
                                    else GammaSurfaceHighlight
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) platform.iconColor else GammaDivider,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedPlatform = platform }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("import_tab_${platform.name.lowercase()}")
                        ) {
                            Text(
                                text = platform.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) platform.iconColor else GammaTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedPlatform == ImportPlatform.LINK) {
                    // Direct link custom input
                    OutlinedTextField(
                        value = directLinkTitle,
                        onValueChange = { directLinkTitle = it },
                        label = { Text("Playlist Name (Optional)") },
                        placeholder = { Text("e.g. My Custom Soundwaves") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GammaPrimary,
                            unfocusedBorderColor = GammaDivider,
                            focusedTextColor = GammaTextPrimary,
                            unfocusedTextColor = GammaTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = directLinkUrl,
                        onValueChange = { directLinkUrl = it },
                        label = { Text("Paste Playlist Link") },
                        placeholder = { Text("https://open.spotify.com/... or https://music.youtube.com/...") },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, tint = GammaPrimary)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GammaPrimary,
                            unfocusedBorderColor = GammaDivider,
                            focusedTextColor = GammaTextPrimary,
                            unfocusedTextColor = GammaTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val name = directLinkTitle.trim().ifEmpty { "Imported Playlist" }
                            val matched = CuratedFrequencies.allTracks.take(5)
                            onImportConfirmed(name, matched)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GammaPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Import From Link", color = GammaSurfaceElevated, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Search Bar for relatable playlists
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search relatable playlists (e.g. Gym, Chill, Rock...)") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = GammaPrimary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = GammaTextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GammaPrimary,
                            unfocusedBorderColor = GammaDivider,
                            focusedTextColor = GammaTextPrimary,
                            unfocusedTextColor = GammaTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "AVAILABLE RELATABLE PLAYLISTS (${filteredPlaylists.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = GammaTextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredPlaylists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No relatable playlists found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = GammaTextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .testTag("relatable_playlists_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredPlaylists, key = { it.title }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(GammaSurfaceHighlight)
                                        .clickable {
                                            onImportConfirmed(item.title, item.tracks)
                                            onDismiss()
                                        }
                                        .padding(10.dp)
                                        .testTag("import_item_${item.title.lowercase().replace(" ", "_")}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GammaArtwork(
                                        url = item.coverUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(46.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(item.platform.iconColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = item.platform.label,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = item.platform.iconColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${item.tracks.size} tracks",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GammaTextMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GammaTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GammaTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            onImportConfirmed(item.title, item.tracks)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GammaPrimary.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Import",
                                            tint = GammaPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "Import",
                                            color = GammaPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
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
}
