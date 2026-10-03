package com.example.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.GammaAlbumCard
import com.example.core.ui.GammaArtistCard
import com.example.core.ui.GammaEmptyState
import com.example.core.ui.GammaErrorState
import com.example.core.ui.GammaFilterChip
import com.example.core.ui.GammaInternetAccessDialog
import com.example.core.ui.GammaInternetBanner
import com.example.core.ui.GammaLoadingSkeleton
import com.example.core.ui.GammaPlaylistCard
import com.example.core.ui.GammaSearchField
import com.example.core.ui.GammaSectionHeader
import com.example.core.ui.GammaTrackRow
import com.example.core.util.NetworkMonitor
import com.example.domain.model.Track
import com.example.ui.theme.GammaAuraBrush
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaTextMuted
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    currentPlayingTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchScope by viewModel.searchScope.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showInternetDialog by remember { mutableStateOf(false) }

    if (showInternetDialog) {
        GammaInternetAccessDialog(
            onDismiss = { showInternetDialog = false },
            onRetry = {
                if (query.isNotBlank()) viewModel.submitSearch(query)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GammaAuraBrush)
    ) {
        // Search bar header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search Songs & Artists",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GammaTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            GammaSearchField(
                query = query,
                onQueryChange = { viewModel.onQueryChange(it) },
                onSearch = {
                    if (!isOnline && (searchScope == SearchScope.YOUTUBE || searchScope == SearchScope.YOUTUBE_MUSIC)) {
                        showInternetDialog = true
                    }
                    viewModel.submitSearch(query)
                },
                placeholder = "Search any song name or YouTube video..."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search Scope Selector (All Signals, YouTube, YouTube Music, Uploaded)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("search_scope_row")
            ) {
                items(SearchScope.entries) { scope ->
                    GammaFilterChip(
                        text = scope.displayName,
                        selected = scope == searchScope,
                        onClick = {
                            if (!isOnline && (scope == SearchScope.YOUTUBE || scope == SearchScope.YOUTUBE_MUSIC)) {
                                showInternetDialog = true
                            }
                            viewModel.setSearchScope(scope)
                        }
                    )
                }
            }
        }

        // Offline notice banner if offline and browsing YouTube or online scopes
        GammaInternetBanner(
            isOnline = isOnline,
            onOpenSettings = { NetworkMonitor.openNetworkSettings(context) }
        )

        // Content
        when (val state = uiState) {
            is SearchUiState.Idle -> {
                if (searchHistory.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT SEARCHES",
                                style = MaterialTheme.typography.labelSmall,
                                color = GammaPrimary
                            )
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GammaTextMuted,
                                modifier = Modifier
                                    .clickable { viewModel.clearHistory() }
                                    .padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchHistory.forEach { histItem ->
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(GammaSurfaceElevated)
                                        .clickable { viewModel.submitSearch(histItem) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = GammaTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = histItem,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GammaTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteHistoryItem(histItem) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = GammaTextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    GammaEmptyState(
                        title = "Tune Your Signal",
                        message = "Search YouTube, YouTube Music, or your uploaded tracks to discover frequencies.",
                        icon = Icons.Default.Search
                    )
                }
            }

            is SearchUiState.Loading -> {
                GammaLoadingSkeleton()
            }

            is SearchUiState.Error -> {
                GammaErrorState(
                    message = state.message,
                    onRetry = { viewModel.submitSearch(query) }
                )
            }

            is SearchUiState.Success -> {
                val results = state.results

                if (results.isEmpty) {
                    EmptySearchArtistFallback(
                        query = query,
                        searchScopeDisplayName = searchScope.displayName,
                        onSearchAsArtist = {
                            if (searchScope == SearchScope.UPLOADED) {
                                viewModel.setSearchScope(SearchScope.ALL)
                            }
                            viewModel.searchByArtist(it)
                        },
                        onKeywordClick = {
                            if (searchScope == SearchScope.UPLOADED) {
                                viewModel.setSearchScope(SearchScope.ALL)
                            }
                            viewModel.submitSearch(it)
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_list"),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        // Uploaded Tracks first if present!
                        if (results.uploadedTracks.isNotEmpty()) {
                            item {
                                GammaSectionHeader(
                                    category = "Your Transmissions",
                                    title = "Uploaded Music (${results.uploadedTracks.size})"
                                )
                            }
                            items(results.uploadedTracks, key = { "up_" + it.id }) { track ->
                                val isFav = state.favoriteIds.contains(track.id)
                                val isCurr = track.id == currentPlayingTrackId
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    GammaTrackRow(
                                        track = track.copy(isFavorite = isFav),
                                        isCurrentTrack = isCurr,
                                        isPlaying = isPlaying && isCurr,
                                        onClick = {
                                            viewModel.submitSearch(query)
                                            onTrackClick(track, results.uploadedTracks)
                                        },
                                        onFavoriteToggle = { viewModel.toggleFavorite(track) }
                                    )
                                }
                            }
                        }

                        // Artists
                        if (results.artists.isNotEmpty()) {
                            item {
                                GammaSectionHeader(
                                    category = "Matched Creators",
                                    title = "Artists"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(results.artists, key = { it.id }) { artist ->
                                        GammaArtistCard(
                                            artist = artist,
                                            onClick = { onArtistClick(artist.id) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // YouTube / YouTube Music & Provider Tracks
                        if (results.tracks.isNotEmpty()) {
                            val nonUploadedTracks = results.tracks.filter { !it.isUploaded }
                            if (nonUploadedTracks.isNotEmpty()) {
                                item {
                                    GammaSectionHeader(
                                        category = "Search Results",
                                        title = "Tracks"
                                    )
                                }
                                items(nonUploadedTracks, key = { it.id }) { track ->
                                    val isFav = state.favoriteIds.contains(track.id)
                                    val isCurr = track.id == currentPlayingTrackId
                                    Box(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    ) {
                                        GammaTrackRow(
                                            track = track.copy(isFavorite = isFav),
                                            isCurrentTrack = isCurr,
                                            isPlaying = isPlaying && isCurr,
                                            onClick = {
                                                viewModel.submitSearch(query)
                                                onTrackClick(track, nonUploadedTracks)
                                            },
                                            onFavoriteToggle = { viewModel.toggleFavorite(track) }
                                        )
                                    }
                                }
                            }
                        }

                        // Albums
                        if (results.albums.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                GammaSectionHeader(
                                    category = "Matched Releases",
                                    title = "Albums"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(results.albums, key = { it.id }) { album ->
                                        GammaAlbumCard(
                                            album = album,
                                            onClick = { onAlbumClick(album.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Playlists
                        if (results.playlists.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                GammaSectionHeader(
                                    category = "Matched Soundwaves",
                                    title = "Playlists"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(results.playlists, key = { it.id }) { playlist ->
                                        GammaPlaylistCard(
                                            playlist = playlist,
                                            onClick = { onPlaylistClick(playlist.id) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptySearchArtistFallback(
    query: String,
    searchScopeDisplayName: String,
    onSearchAsArtist: (String) -> Unit,
    onKeywordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val vibeKeywords = listOf(
        "Workout", "Gym Pump", "Lo-Fi Chill", "Phonk Drift",
        "Deep Calm", "Top Hits", "90s Rock", "Sad Vibes",
        "Cyberpunk", "Acoustic Folk"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(GammaPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GammaPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No direct song found for \"$query\"",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = GammaTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Didn't find that exact song in $searchScopeDisplayName? Try searching as an artist name to discover all their tracks & releases.",
            style = MaterialTheme.typography.bodyMedium,
            color = GammaTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSearchAsArtist(query) },
            colors = ButtonDefaults.buttonColors(
                containerColor = GammaPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("btn_search_as_artist")
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Search \"$query\" as Artist",
                color = Color.Black,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.Black)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "RELATED VIBE KEYWORDS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = GammaTextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            vibeKeywords.forEach { keyword ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(GammaSurfaceElevated)
                        .clickable { onKeywordClick(keyword) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = keyword,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GammaTextPrimary
                    )
                }
            }
        }
    }
}
