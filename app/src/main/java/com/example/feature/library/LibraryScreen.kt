package com.example.feature.library

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.GammaArtwork
import com.example.core.ui.GammaEmptyState
import com.example.core.ui.GammaFilterChip
import com.example.core.ui.GammaImportPlaylistDialog
import com.example.core.ui.GammaPrimaryButton
import com.example.core.ui.GammaSecondaryButton
import com.example.core.ui.GammaTrackRow
import com.example.core.ui.GammaUploadMusicDialog
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.GammaAuraBrush
import com.example.ui.theme.GammaBackground
import com.example.ui.theme.GammaDivider
import com.example.ui.theme.GammaGlowCyan
import com.example.ui.theme.GammaGlowViolet
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaSurface
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaTextMuted
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    currentPlayingTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlaylistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    if (showUploadDialog) {
        GammaUploadMusicDialog(
            onDismiss = { showUploadDialog = false },
            onUploadConfirmed = { title, artist, freq, genre, yt, uri ->
                viewModel.uploadTrack(title, artist, freq, genre, yt, uri)
            }
        )
    }

    if (showImportDialog) {
        GammaImportPlaylistDialog(
            onDismiss = { showImportDialog = false },
            onImportConfirmed = { name, tracks ->
                viewModel.importPlaylist(name, tracks)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GammaAuraBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Orbit",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GammaTextPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Import Playlist Button (Spotify, YouTube Music, Apple Music)
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(GammaSurfaceElevated, CircleShape)
                            .border(1.dp, GammaDivider, CircleShape)
                            .testTag("library_import_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Import Playlist",
                            tint = GammaPrimary
                        )
                    }

                    // Upload Music Button
                    IconButton(
                        onClick = { showUploadDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(GammaPrimary, CircleShape)
                            .testTag("library_upload_music_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload Music",
                            tint = GammaSurfaceElevated
                        )
                    }

                    if (uiState.selectedTab == LibraryTab.PLAYLISTS) {
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(GammaSurfaceElevated, CircleShape)
                                .testTag("create_playlist_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Playlist",
                                tint = GammaPrimary
                            )
                        }
                    } else if (uiState.selectedTab == LibraryTab.DISCOVERY && uiState.discoveryHistory.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearDiscoveryHistory() },
                            modifier = Modifier.testTag("clear_discovery_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Discovery History",
                                tint = GammaTextMuted
                            )
                        }
                    } else if (uiState.selectedTab == LibraryTab.HISTORY && uiState.history.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = GammaTextMuted
                            )
                        }
                    }
                }
            }

            // 2-Column Echo-Music Quick Action Cards Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Starred / Favorites
                EchoLibraryCard(
                    title = "Liked Tracks",
                    count = uiState.favorites.size,
                    icon = Icons.Default.Favorite,
                    iconTint = Color(0xFFFF4081),
                    isSelected = uiState.selectedTab == LibraryTab.FAVORITES,
                    onClick = { viewModel.selectTab(LibraryTab.FAVORITES) },
                    modifier = Modifier.weight(1f)
                )

                // Card 2: Offline Downloads
                EchoLibraryCard(
                    title = "Downloaded",
                    count = uiState.downloaded.size,
                    icon = Icons.Default.DownloadDone,
                    iconTint = GammaPrimary,
                    isSelected = uiState.selectedTab == LibraryTab.DOWNLOADED,
                    onClick = { viewModel.selectTab(LibraryTab.DOWNLOADED) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 3: Tuned Uploads & Local
                EchoLibraryCard(
                    title = "Uploaded Audio",
                    count = uiState.uploads.size,
                    icon = Icons.Default.Sensors,
                    iconTint = GammaGlowCyan,
                    isSelected = uiState.selectedTab == LibraryTab.UPLOADS,
                    onClick = { viewModel.selectTab(LibraryTab.UPLOADS) },
                    modifier = Modifier.weight(1f)
                )

                // Card 4: Playlists
                EchoLibraryCard(
                    title = "Playlists",
                    count = uiState.playlists.size,
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    iconTint = GammaGlowViolet,
                    isSelected = uiState.selectedTab == LibraryTab.PLAYLISTS,
                    onClick = { viewModel.selectTab(LibraryTab.PLAYLISTS) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab selector chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LibraryTab.entries) { tab ->
                    val count = when (tab) {
                        LibraryTab.FAVORITES -> uiState.favorites.size
                        LibraryTab.DOWNLOADED -> uiState.downloaded.size
                        LibraryTab.DISCOVERY -> uiState.discoveryHistory.size
                        LibraryTab.UPLOADS -> uiState.uploads.size
                        LibraryTab.PLAYLISTS -> uiState.playlists.size
                        LibraryTab.HISTORY -> uiState.history.size
                    }
                    GammaFilterChip(
                        text = "${tab.title} ($count)",
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab contents
            when (uiState.selectedTab) {
                LibraryTab.FAVORITES -> {
                    if (uiState.favorites.isEmpty()) {
                        GammaEmptyState(
                            title = "No Starred Signals",
                            message = "Tap the heart on any frequency in Discover or Search to store it in your orbit.",
                            icon = Icons.Default.Favorite
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("favorites_list"),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GammaPrimaryButton(
                                        text = "Play All",
                                        icon = Icons.Default.PlayArrow,
                                        onClick = { viewModel.playAll(uiState.favorites, shuffle = false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    GammaSecondaryButton(
                                        text = "Shuffle",
                                        icon = Icons.Default.Shuffle,
                                        onClick = { viewModel.playAll(uiState.favorites, shuffle = true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(uiState.favorites, key = { it.id }) { track ->
                                val isCurr = track.id == currentPlayingTrackId
                                val isDown = uiState.downloadedIds.contains(track.id) || track.isDownloaded || track.localAudioUri.isNotBlank()
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    GammaTrackRow(
                                        track = track.copy(isFavorite = true, isDownloaded = isDown),
                                        isCurrentTrack = isCurr,
                                        isPlaying = isPlaying && isCurr,
                                        isDownloaded = isDown,
                                        isDownloading = uiState.downloadingTrackId == track.id,
                                        onClick = { onTrackClick(track, uiState.favorites) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                        onDownloadClick = { viewModel.downloadTrack(track) }
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.DOWNLOADED -> {
                    if (uiState.downloaded.isEmpty()) {
                        GammaEmptyState(
                            title = "No Downloaded Signals",
                            message = "Download tracks to listen offline with full hardware equalizer DSP.",
                            icon = Icons.Default.DownloadDone
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("downloaded_list"),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GammaPrimaryButton(
                                        text = "Play All (${uiState.downloaded.size})",
                                        icon = Icons.Default.PlayArrow,
                                        onClick = { viewModel.playAll(uiState.downloaded, shuffle = false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    GammaSecondaryButton(
                                        text = "Shuffle",
                                        icon = Icons.Default.Shuffle,
                                        onClick = { viewModel.playAll(uiState.downloaded, shuffle = true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(uiState.downloaded, key = { "dl_" + it.id }) { track ->
                                val isCurr = track.id == currentPlayingTrackId
                                val isFav = uiState.favoriteIds.contains(track.id)
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    GammaTrackRow(
                                        track = track.copy(isFavorite = isFav, isDownloaded = true),
                                        isCurrentTrack = isCurr,
                                        isPlaying = isPlaying && isCurr,
                                        isDownloaded = true,
                                        isDownloading = uiState.downloadingTrackId == track.id,
                                        onClick = { onTrackClick(track, uiState.downloaded) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                        onDownloadClick = { viewModel.downloadTrack(track) }
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.DISCOVERY -> {
                    if (uiState.discoveryHistory.isEmpty()) {
                        GammaEmptyState(
                            title = "No Discovery Cache",
                            message = "Tracks and frequencies you explore in Discover and Search are automatically cached in Room for offline access.",
                            icon = Icons.Default.Sensors
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("discovery_history_list"),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GammaPrimaryButton(
                                        text = "Play All",
                                        icon = Icons.Default.PlayArrow,
                                        onClick = { viewModel.playAll(uiState.discoveryHistory, shuffle = false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    GammaSecondaryButton(
                                        text = "Shuffle",
                                        icon = Icons.Default.Shuffle,
                                        onClick = { viewModel.playAll(uiState.discoveryHistory, shuffle = true) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(uiState.discoveryHistory, key = { "disc_" + it.id }) { track ->
                                val isFav = uiState.favoriteIds.contains(track.id)
                                val isCurr = track.id == currentPlayingTrackId
                                val isDown = uiState.downloadedIds.contains(track.id) || track.isDownloaded || track.localAudioUri.isNotBlank()
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    GammaTrackRow(
                                        track = track.copy(isFavorite = isFav, isDownloaded = isDown),
                                        isCurrentTrack = isCurr,
                                        isPlaying = isPlaying && isCurr,
                                        isDownloaded = isDown,
                                        isDownloading = uiState.downloadingTrackId == track.id,
                                        onClick = { onTrackClick(track, uiState.discoveryHistory) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                        onDownloadClick = { viewModel.downloadTrack(track) }
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.UPLOADS -> {
                    if (uiState.uploads.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GammaEmptyState(
                                title = "No Uploaded Signals",
                                message = "Upload your audio files, personal frequencies, or YouTube music to make them discoverable across the app.",
                                icon = Icons.Default.CloudUpload
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GammaPrimaryButton(
                                text = "Upload Music",
                                icon = Icons.Default.CloudUpload,
                                onClick = { showUploadDialog = true }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("uploads_list"),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GammaPrimaryButton(
                                        text = "Play All",
                                        icon = Icons.Default.PlayArrow,
                                        onClick = { viewModel.playAll(uiState.uploads, shuffle = false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    GammaSecondaryButton(
                                        text = "Upload More",
                                        icon = Icons.Default.Add,
                                        onClick = { showUploadDialog = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(uiState.uploads, key = { "up_lib_" + it.id }) { track ->
                                val isFav = uiState.favoriteIds.contains(track.id)
                                val isCurr = track.id == currentPlayingTrackId
                                val isDown = uiState.downloadedIds.contains(track.id) || track.isDownloaded || track.localAudioUri.isNotBlank()
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    GammaTrackRow(
                                        track = track.copy(isFavorite = isFav, isDownloaded = isDown),
                                        isCurrentTrack = isCurr,
                                        isPlaying = isPlaying && isCurr,
                                        isDownloaded = isDown,
                                        isDownloading = uiState.downloadingTrackId == track.id,
                                        onClick = { onTrackClick(track, uiState.uploads) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                        onDownloadClick = { viewModel.downloadTrack(track) }
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.PLAYLISTS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sub-filter tabs: All Playlists, Most Played, Least Played
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GammaFilterChip(
                                text = "All (${uiState.playlists.size})",
                                selected = uiState.selectedPlaylistFilter == PlaylistFilter.ALL,
                                onClick = { viewModel.selectPlaylistFilter(PlaylistFilter.ALL) }
                            )
                            GammaFilterChip(
                                text = "🔥 Most Played (${uiState.mostPlayed.size})",
                                selected = uiState.selectedPlaylistFilter == PlaylistFilter.MOST_PLAYED,
                                onClick = { viewModel.selectPlaylistFilter(PlaylistFilter.MOST_PLAYED) }
                            )
                            GammaFilterChip(
                                text = "❄️ Least Played (${uiState.leastPlayed.size})",
                                selected = uiState.selectedPlaylistFilter == PlaylistFilter.LEAST_PLAYED,
                                onClick = { viewModel.selectPlaylistFilter(PlaylistFilter.LEAST_PLAYED) }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        when (uiState.selectedPlaylistFilter) {
                            PlaylistFilter.MOST_PLAYED -> {
                                if (uiState.mostPlayed.isEmpty()) {
                                    GammaEmptyState(
                                        title = "No Frequency Records",
                                        message = "As you listen to songs in GAMA, your most played tracks will automatically appear here.",
                                        icon = Icons.Default.GraphicEq
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("most_played_list"),
                                        contentPadding = PaddingValues(bottom = 120.dp)
                                    ) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                GammaPrimaryButton(
                                                    text = "Play All (${uiState.mostPlayed.size})",
                                                    icon = Icons.Default.PlayArrow,
                                                    onClick = { viewModel.playAll(uiState.mostPlayed, shuffle = false) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                GammaSecondaryButton(
                                                    text = "Shuffle",
                                                    icon = Icons.Default.Shuffle,
                                                    onClick = { viewModel.playAll(uiState.mostPlayed, shuffle = true) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        items(uiState.mostPlayed, key = { "most_" + it.id }) { track ->
                                            val isCurr = track.id == currentPlayingTrackId
                                            val isFav = uiState.favoriteIds.contains(track.id)
                                            val isDown = uiState.downloadedIds.contains(track.id) || track.isDownloaded || track.localAudioUri.isNotBlank()
                                            Box(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            ) {
                                                GammaTrackRow(
                                                    track = track.copy(isFavorite = isFav, isDownloaded = isDown),
                                                    isCurrentTrack = isCurr,
                                                    isPlaying = isPlaying && isCurr,
                                                    isDownloaded = isDown,
                                                    isDownloading = uiState.downloadingTrackId == track.id,
                                                    onClick = { onTrackClick(track, uiState.mostPlayed) },
                                                    onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                                    onDownloadClick = { viewModel.downloadTrack(track) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            PlaylistFilter.LEAST_PLAYED -> {
                                if (uiState.leastPlayed.isEmpty()) {
                                    GammaEmptyState(
                                        title = "No Frequency Records",
                                        message = "As you listen to songs in GAMA, your least played tracks and rare gems will automatically appear here.",
                                        icon = Icons.Default.History
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("least_played_list"),
                                        contentPadding = PaddingValues(bottom = 120.dp)
                                    ) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                GammaPrimaryButton(
                                                    text = "Play All (${uiState.leastPlayed.size})",
                                                    icon = Icons.Default.PlayArrow,
                                                    onClick = { viewModel.playAll(uiState.leastPlayed, shuffle = false) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                GammaSecondaryButton(
                                                    text = "Shuffle",
                                                    icon = Icons.Default.Shuffle,
                                                    onClick = { viewModel.playAll(uiState.leastPlayed, shuffle = true) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        items(uiState.leastPlayed, key = { "least_" + it.id }) { track ->
                                            val isCurr = track.id == currentPlayingTrackId
                                            val isFav = uiState.favoriteIds.contains(track.id)
                                            val isDown = uiState.downloadedIds.contains(track.id) || track.isDownloaded || track.localAudioUri.isNotBlank()
                                            Box(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            ) {
                                                GammaTrackRow(
                                                    track = track.copy(isFavorite = isFav, isDownloaded = isDown),
                                                    isCurrentTrack = isCurr,
                                                    isPlaying = isPlaying && isCurr,
                                                    isDownloaded = isDown,
                                                    isDownloading = uiState.downloadingTrackId == track.id,
                                                    onClick = { onTrackClick(track, uiState.leastPlayed) },
                                                    onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                                    onDownloadClick = { viewModel.downloadTrack(track) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            PlaylistFilter.ALL -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("user_playlists_list"),
                                    contentPadding = PaddingValues(bottom = 120.dp)
                                ) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            GammaPrimaryButton(
                                                text = "Create Playlist",
                                                icon = Icons.Default.Add,
                                                onClick = { showCreateDialog = true },
                                                modifier = Modifier.weight(1f)
                                            )
                                            GammaSecondaryButton(
                                                text = "Import Playlist",
                                                icon = Icons.Default.CloudDownload,
                                                onClick = { showImportDialog = true },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    // Automatic Smart Playlists: Most Played & Least Played
                                    item {
                                        SmartPlaylistCard(
                                            title = "🔥 Most Played",
                                            description = "Automatically generated based on listening frequency",
                                            trackCount = uiState.mostPlayed.size,
                                            iconTint = Color(0xFFFF5722),
                                            onClick = { viewModel.selectPlaylistFilter(PlaylistFilter.MOST_PLAYED) },
                                            onPlayClick = if (uiState.mostPlayed.isNotEmpty()) {
                                                { viewModel.playAll(uiState.mostPlayed, shuffle = false) }
                                            } else null,
                                            onShuffleClick = if (uiState.mostPlayed.isNotEmpty()) {
                                                { viewModel.playAll(uiState.mostPlayed, shuffle = true) }
                                            } else null
                                        )
                                    }

                                    item {
                                        SmartPlaylistCard(
                                            title = "❄️ Least Played",
                                            description = "Rarely played songs and undiscovered gems",
                                            trackCount = uiState.leastPlayed.size,
                                            iconTint = GammaGlowCyan,
                                            onClick = { viewModel.selectPlaylistFilter(PlaylistFilter.LEAST_PLAYED) },
                                            onPlayClick = if (uiState.leastPlayed.isNotEmpty()) {
                                                { viewModel.playAll(uiState.leastPlayed, shuffle = false) }
                                            } else null,
                                            onShuffleClick = if (uiState.leastPlayed.isNotEmpty()) {
                                                { viewModel.playAll(uiState.leastPlayed, shuffle = true) }
                                            } else null
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    if (uiState.playlists.isEmpty()) {
                                        item {
                                            GammaEmptyState(
                                                title = "Your Soundwaves",
                                                message = "Create custom frequency compilations for focus, meditation, or travel.",
                                                icon = Icons.AutoMirrored.Filled.QueueMusic
                                            )
                                        }
                                    } else {
                                        items(uiState.playlists, key = { it.id }) { playlist ->
                                            UserPlaylistRow(
                                                playlist = playlist,
                                                onClick = { onPlaylistClick(playlist.id) },
                                                onDownloadClick = { viewModel.downloadPlaylist(playlist.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.HISTORY -> {
                    if (uiState.history.isEmpty()) {
                        GammaEmptyState(
                            title = "Clear Orbit",
                            message = "Signals you listen to will appear here so you can replay any past journey.",
                            icon = Icons.Default.History
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("history_list"),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            items(uiState.history, key = { "${it.id}_${it.durationSeconds}" }) { track ->
                                val isFav = uiState.favoriteIds.contains(track.id)
                                val isCurr = track.id == currentPlayingTrackId
                                val isDown = uiState.downloadedIds.contains(track.id) || track.isDownloaded || track.localAudioUri.isNotBlank()
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    GammaTrackRow(
                                        track = track.copy(isFavorite = isFav, isDownloaded = isDown),
                                        isCurrentTrack = isCurr,
                                        isPlaying = isPlaying && isCurr,
                                        isDownloaded = isDown,
                                        isDownloading = uiState.downloadingTrackId == track.id,
                                        onClick = { onTrackClick(track, uiState.history) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                        onDownloadClick = { viewModel.downloadTrack(track) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = GammaSurfaceElevated,
            title = {
                Text(
                    text = "New Frequency Wave",
                    style = MaterialTheme.typography.titleLarge,
                    color = GammaTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GammaTextPrimary,
                            unfocusedTextColor = GammaTextPrimary,
                            focusedBorderColor = GammaPrimary,
                            unfocusedBorderColor = GammaDivider
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("playlist_name_input")
                    )

                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GammaTextPrimary,
                            unfocusedTextColor = GammaTextPrimary,
                            focusedBorderColor = GammaPrimary,
                            unfocusedBorderColor = GammaDivider
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName, newPlaylistDesc)
                            newPlaylistName = ""
                            newPlaylistDesc = ""
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_save_playlist_button")
                ) {
                    Text("Create", color = GammaPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = GammaTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun UserPlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDownloadClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GammaSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GammaArtwork(
            url = playlist.artworkUrl,
            contentDescription = "${playlist.title} cover",
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                color = GammaTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = playlist.description.ifEmpty { "Personal soundwave playlist" },
                style = MaterialTheme.typography.bodyMedium,
                color = GammaTextSecondary
            )
        }

        if (onDownloadClick != null) {
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.testTag("download_playlist_${playlist.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download entire playlist",
                    tint = GammaPrimary
                )
            }
        }
    }
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    description: String,
    trackCount: Int,
    iconTint: Color,
    onClick: () -> Unit,
    onPlayClick: (() -> Unit)? = null,
    onShuffleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GammaSurfaceElevated)
            .border(1.dp, GammaDivider.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.15f))
                .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GammaTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = GammaTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$trackCount songs • Automatic Smart Playlist",
                style = MaterialTheme.typography.labelSmall,
                color = iconTint
            )
        }

        if (trackCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onPlayClick != null) {
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(GammaPrimary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Smart Playlist",
                            tint = GammaBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (onShuffleClick != null) {
                    IconButton(
                        onClick = onShuffleClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(GammaPrimary.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle Smart Playlist",
                            tint = GammaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EchoLibraryCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) GammaSurfaceElevated else GammaSurface
    val border = if (isSelected) iconTint.copy(alpha = 0.7f) else GammaDivider.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("library_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f))
                    .border(0.5.dp, iconTint.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = GammaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$count tracks",
                    style = MaterialTheme.typography.labelSmall,
                    color = GammaTextMuted
                )
            }
        }
    }
}

