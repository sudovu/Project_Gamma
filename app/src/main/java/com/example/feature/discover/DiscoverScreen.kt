package com.example.feature.discover

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.data.provider.YouTubeProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.core.media.SmartQueueEngine
import com.example.core.ui.GammaDailyChartInfoDialog
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.GammaAddToPlaylistDialog
import com.example.core.ui.GammaAlbumCard
import com.example.core.ui.GammaArtistCard
import com.example.core.ui.GammaErrorState
import com.example.core.ui.GammaFilterChip
import com.example.core.ui.GammaInternetAccessDialog
import com.example.core.ui.GammaInternetBanner
import com.example.core.ui.GammaLoadingSkeleton
import com.example.core.ui.GammaPlaylistCard
import com.example.core.ui.GammaQuickPickCard
import com.example.core.ui.GammaSectionHeader
import com.example.core.ui.GammaTrackRow
import com.example.core.ui.GammaUploadMusicDialog
import com.example.core.taste.TastePreferenceManager
import com.example.core.util.NetworkMonitor
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.ui.theme.GammaAuraBrush
import com.example.ui.theme.GammaBackground
import com.example.ui.theme.GammaGlowCyan
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaSecondary
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaSurfaceHighlight
import com.example.ui.theme.GammaTextMuted
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    currentPlayingTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (Track, List<Track>) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    youtubeProvider: YouTubeProvider? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val tasteManager = remember { TastePreferenceManager.getInstance(context) }
    val isSpotifyLinked by tasteManager.isSpotifyLinked.collectAsStateWithLifecycle()
    val isGoogleLinked by tasteManager.isGoogleLinked.collectAsStateWithLifecycle()
    val spotifyTastes by tasteManager.spotifyTastes.collectAsStateWithLifecycle()
    val youtubeTastes by tasteManager.youtubeTastes.collectAsStateWithLifecycle()
    val liveSpotifyTracks by tasteManager.spotifyTracks.collectAsStateWithLifecycle()
    val liveGoogleTracks by tasteManager.googleTracks.collectAsStateWithLifecycle()
    val isSpotifySyncing by tasteManager.isSpotifySyncing.collectAsStateWithLifecycle()
    val isGoogleSyncing by tasteManager.isGoogleSyncing.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showUploadDialog by remember { mutableStateOf(false) }
    var showInternetDialog by remember { mutableStateOf(false) }

    if (showUploadDialog) {
        GammaUploadMusicDialog(
            onDismiss = { showUploadDialog = false },
            onUploadConfirmed = { title, artist, freq, genre, yt, uri ->
                viewModel.uploadTrack(title, artist, freq, genre, yt, uri)
            }
        )
    }

    if (showInternetDialog) {
        GammaInternetAccessDialog(
            onDismiss = { showInternetDialog = false },
            onRetry = { viewModel.loadFeed() }
        )
    }

    var trackForPlaylist by remember { mutableStateOf<Track?>(null) }
    var selectedChartTrackForInfo by remember { mutableStateOf<Track?>(null) }
    var showAllTrending by remember { mutableStateOf(false) }
    val userPlaylists by viewModel.userPlaylists.collectAsStateWithLifecycle()

    if (trackForPlaylist != null) {
        val selected = trackForPlaylist!!
        GammaAddToPlaylistDialog(
            track = selected,
            userPlaylists = userPlaylists,
            onDismiss = { trackForPlaylist = null },
            onAddToPlaylist = { playlistId ->
                viewModel.addTrackToPlaylist(playlistId, selected)
            },
            onCreatePlaylistAndAdd = { name ->
                viewModel.createPlaylistAndAddTrack(name, selected)
            }
        )
    }

    if (selectedChartTrackForInfo != null) {
        val selectedInfoTrack = selectedChartTrackForInfo!!
        GammaDailyChartInfoDialog(
            track = selectedInfoTrack,
            onDismiss = { selectedChartTrackForInfo = null },
            onPlayTrack = { trk ->
                val current = (uiState as? DiscoverUiState.Success)?.feed?.trendingTracks ?: emptyList()
                onTrackClick(trk, current)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GammaAuraBrush)
    ) {
        when (val state = uiState) {
            is DiscoverUiState.Loading -> {
                GammaLoadingSkeleton()
            }
            is DiscoverUiState.Error -> {
                GammaErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadFeed() }
                )
            }
            is DiscoverUiState.Success -> {
                val feed = state.feed
                val displayedTracks = if (state.selectedMood == "All") {
                    feed.trendingTracks
                } else {
                    feed.moodPlaylists[state.selectedMood] ?: feed.trendingTracks
                }

                val spotifyTracks = remember(feed, isSpotifyLinked, liveSpotifyTracks) {
                    tasteManager.getSpotifyAdaptedRecommendations(feed.quickPicks + feed.trendingTracks)
                }
                val ytTracks = remember(feed, isGoogleLinked, liveGoogleTracks) {
                    tasteManager.getYouTubeAdaptedRecommendations(feed.quickPicks + feed.trendingTracks)
                }

                val pullRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh(tasteManager, youtubeProvider) },
                    state = pullRefreshState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = GammaSurfaceElevated,
                            color = GammaPrimary
                        )
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("discover_list"),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                    // Top Bar / Header with Upload Action and Network Status
                    item {
                        DiscoverHeader(
                            isOnline = isOnline,
                            onUploadClick = { showUploadDialog = true },
                            onNetworkClick = { showInternetDialog = true }
                        )
                    }

                    // Offline internet prompt banner if disconnected
                    item {
                        GammaInternetBanner(
                            isOnline = isOnline,
                            onOpenSettings = { NetworkMonitor.openNetworkSettings(context) }
                        )
                    }

                    // Mood & Genre separate selector sections
                    item {
                        MoodAndGenreSection(
                            selectedMood = state.selectedMood,
                            onMoodSelect = { viewModel.selectMood(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Shuffle All Recommended Action Bar (Combines each genre seamlessly without duplicates)
                    item {
                        val allGenreCombinedTracks: List<Track> = remember(feed, spotifyTracks, ytTracks) {
                            val genreMap = mutableMapOf<String, MutableList<Track>>()

                            // 1. Every genre playlist from moodPlaylists (Hip-Hop, Rock & Metal, Pop Hits, Classics, etc.)
                            feed.moodPlaylists.forEach { (genre, list) ->
                                if (list.isNotEmpty()) {
                                    genreMap.getOrPut(genre) { mutableListOf() }.addAll(list)
                                }
                            }

                            // 2. Taste recommendations
                            if (spotifyTracks.isNotEmpty()) {
                                genreMap.getOrPut("Spotify Taste") { mutableListOf() }.addAll(spotifyTracks)
                            }
                            if (ytTracks.isNotEmpty()) {
                                genreMap.getOrPut("YouTube Taste") { mutableListOf() }.addAll(ytTracks)
                            }

                            // 3. Quick Picks & Trending
                            feed.quickPicks.forEach { t ->
                                val g = t.genre.ifBlank { "Trending" }
                                genreMap.getOrPut(g) { mutableListOf() }.add(t)
                            }
                            feed.trendingTracks.forEach { t ->
                                val g = t.genre.ifBlank { "Trending" }
                                genreMap.getOrPut(g) { mutableListOf() }.add(t)
                            }

                            // 4. Featured Playlists & Uploads
                            feed.featuredPlaylists.forEach { pl ->
                                pl.tracks.forEach { t ->
                                    val g = t.genre.ifBlank { "Featured" }
                                    genreMap.getOrPut(g) { mutableListOf() }.add(t)
                                }
                            }
                            feed.uploadedTracks.forEach { t ->
                                genreMap.getOrPut("Uploaded") { mutableListOf() }.add(t)
                            }

                            // Interleave across every genre for rich multi-genre diversity, then strictly deduplicate
                            val interleaved = mutableListOf<Track>()
                            val iterators = genreMap.values.map { it.shuffled().iterator() }.toMutableList()
                            var hasMore = true
                            while (hasMore) {
                                hasMore = false
                                for (it in iterators) {
                                    if (it.hasNext()) {
                                        interleaved.add(it.next())
                                        hasMore = true
                                    }
                                }
                            }
                            SmartQueueEngine.deduplicateTracks(interleaved)
                        }

                        if (allGenreCombinedTracks.isNotEmpty()) {
                            val activePool: List<Track> = remember(state.selectedMood, allGenreCombinedTracks, feed) {
                                if (state.selectedMood == "All") {
                                    allGenreCombinedTracks
                                } else {
                                    val moodList = feed.moodPlaylists[state.selectedMood]
                                    if (!moodList.isNullOrEmpty()) {
                                        SmartQueueEngine.deduplicateTracks(moodList)
                                    } else {
                                        allGenreCombinedTracks
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (state.selectedMood == "All") "All-Genre Combined Stream" else "${state.selectedMood} Mix",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GammaTextSecondary
                                )

                                Surface(
                                    onClick = {
                                        if (activePool.isNotEmpty()) {
                                            viewModel.playShuffled(activePool)
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = GammaPrimary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, GammaPrimary.copy(alpha = 0.35f)),
                                    modifier = Modifier.testTag("shuffle_all_recommended_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Shuffle All",
                                            tint = GammaPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (state.selectedMood == "All") "Shuffle All Genres (${activePool.size})" else "Shuffle ${state.selectedMood} (${activePool.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GammaPrimary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    if (state.selectedMood == "All") {
                        // --- FULL ALL-GENRE HOME DASHBOARD ---
                        // Dedicated Uploaded Signals Section (Discoverable Music!)
                        if (feed.uploadedTracks.isNotEmpty()) {
                            item {
                                GammaSectionHeader(
                                    category = "Your Transmissions",
                                    title = "Uploaded Signals (${feed.uploadedTracks.size})"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.testTag("uploaded_tracks_row")
                                ) {
                                    items(feed.uploadedTracks, key = { "up_disc_" + it.id }) { track ->
                                        val isFav = state.favoriteIds.contains(track.id)
                                        GammaQuickPickCard(
                                            track = track.copy(isFavorite = isFav),
                                            onClick = { onTrackClick(track, feed.uploadedTracks) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Quick Picks
                        if (feed.quickPicks.isNotEmpty()) {
                            item {
                                GammaSectionHeader(
                                    category = "Trending Now",
                                    title = "Quick Picks",
                                    actionText = "Shuffle",
                                    onActionClick = {
                                        if (feed.quickPicks.isNotEmpty()) {
                                            viewModel.playShuffled(feed.quickPicks)
                                        }
                                    }
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.testTag("quick_picks_row")
                                ) {
                                    items(feed.quickPicks, key = { it.id }) { track ->
                                        GammaQuickPickCard(
                                            track = track,
                                            onClick = { onTrackClick(track, feed.quickPicks) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Continue Listening (Recent)
                        if (state.recentTracks.isNotEmpty()) {
                            item {
                                GammaSectionHeader(
                                    category = "Your Orbit",
                                    title = "Continue Listening"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.testTag("recent_tracks_row")
                                ) {
                                    items(state.recentTracks.take(5), key = { it.id }) { track ->
                                        val isFav = state.favoriteIds.contains(track.id)
                                        GammaQuickPickCard(
                                            track = track.copy(isFavorite = isFav),
                                            onClick = { onTrackClick(track, state.recentTracks) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Curated Playlists
                        if (feed.featuredPlaylists.isNotEmpty()) {
                            item {
                                GammaSectionHeader(
                                    category = "Curated Soundwaves",
                                    title = "Featured Playlists"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.testTag("featured_playlists_row")
                                ) {
                                    items(feed.featuredPlaylists, key = { it.id }) { playlist ->
                                        GammaPlaylistCard(
                                            playlist = playlist,
                                            onClick = { onPlaylistClick(playlist.id) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Trending Signals (Daily Top 100 Chart) with direct Shuffle & Info buttons
                        val trendingCount = if (showAllTrending) displayedTracks.size else 12.coerceAtMost(displayedTracks.size)
                        item {
                            GammaSectionHeader(
                                category = "Daily Top 100 Chart • Updated for Today",
                                title = "Trending Music (${displayedTracks.size} Songs)",
                                actionText = "Shuffle",
                                onActionClick = {
                                    if (displayedTracks.isNotEmpty()) {
                                        viewModel.playShuffled(displayedTracks)
                                    }
                                }
                            )
                        }

                        items(displayedTracks.take(trendingCount), key = { it.id }) { track ->
                            val isFav = state.favoriteIds.contains(track.id)
                            val isCurr = track.id == currentPlayingTrackId
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                GammaTrackRow(
                                    track = track.copy(isFavorite = isFav),
                                    isCurrentTrack = isCurr,
                                    isPlaying = isPlaying && isCurr,
                                    onClick = { onTrackClick(track, displayedTracks) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(track) },
                                    onInfoClick = { selectedChartTrackForInfo = track },
                                    onMoreOptionsClick = { trackForPlaylist = track }
                                )
                            }
                        }

                        if (displayedTracks.size > 12) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { showAllTrending = !showAllTrending },
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, GammaPrimary.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = if (showAllTrending) "Show Top 12 Songs" else "View All Top 100 Daily Songs (${displayedTracks.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GammaPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Featured Albums
                        if (feed.featuredAlbums.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                GammaSectionHeader(
                                    category = "Curated Releases",
                                    title = "Featured Albums"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(feed.featuredAlbums, key = { it.id }) { album ->
                                        GammaAlbumCard(
                                            album = album,
                                            onClick = { onAlbumClick(album.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Featured Artists
                        if (feed.featuredArtists.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                GammaSectionHeader(
                                    category = "Orbital Creators",
                                    title = "Featured Artists"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(feed.featuredArtists, key = { it.id }) { artist ->
                                        GammaArtistCard(
                                            artist = artist,
                                            onClick = { onArtistClick(artist.id) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // --- DEDICATED GENRE SCREEN (Hip-Hop, Rock & Metal, Pop Hits, Classics) ---
                        // 1. Featured Picks for this genre (horizontal quick picks)
                        if (displayedTracks.isNotEmpty()) {
                            item {
                                GammaSectionHeader(
                                    category = "Vibe Frequency",
                                    title = "${state.selectedMood} Anthems"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.testTag("genre_quick_picks_row")
                                ) {
                                    items(displayedTracks.take(6), key = { "genre_pick_${it.id}" }) { track ->
                                        val isFav = state.favoriteIds.contains(track.id)
                                        GammaQuickPickCard(
                                            track = track.copy(isFavorite = isFav),
                                            onClick = { onTrackClick(track, displayedTracks) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // 2. Complete tracklist for this genre
                        item {
                            GammaSectionHeader(
                                category = "Unlimited Stream",
                                title = "${state.selectedMood} Radio"
                            )
                        }

                        items(displayedTracks, key = { it.id }) { track ->
                            val isFav = state.favoriteIds.contains(track.id)
                            val isCurr = track.id == currentPlayingTrackId
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                GammaTrackRow(
                                    track = track.copy(isFavorite = isFav),
                                    isCurrentTrack = isCurr,
                                    isPlaying = isPlaying && isCurr,
                                    onClick = { onTrackClick(track, displayedTracks) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(track) }
                                )
                            }
                        }

                        // 3. Matching Playlists for this genre
                        val matchingPlaylists = feed.featuredPlaylists.filter {
                            it.title.contains(state.selectedMood, ignoreCase = true) ||
                            it.description.contains(state.selectedMood, ignoreCase = true)
                        }
                        if (matchingPlaylists.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                GammaSectionHeader(
                                    category = "Genre Playlists",
                                    title = "${state.selectedMood} Soundwaves"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(matchingPlaylists, key = { it.id }) { playlist ->
                                        GammaPlaylistCard(
                                            playlist = playlist,
                                            onClick = { onPlaylistClick(playlist.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Matching Albums for this genre
                        val matchingAlbums = feed.featuredAlbums.filter {
                            it.genre.contains(state.selectedMood, ignoreCase = true) ||
                            it.title.contains(state.selectedMood, ignoreCase = true)
                        }
                        if (matchingAlbums.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                GammaSectionHeader(
                                    category = "Curated Albums",
                                    title = "${state.selectedMood} Releases"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(matchingAlbums, key = { it.id }) { album ->
                                        GammaAlbumCard(
                                            album = album,
                                            onClick = { onAlbumClick(album.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Matching Artists for this genre
                        val matchingArtists = feed.featuredArtists.filter { artist ->
                            artist.genres.any { it.contains(state.selectedMood, ignoreCase = true) }
                        }
                        if (matchingArtists.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                GammaSectionHeader(
                                    category = "Genre Creators",
                                    title = "${state.selectedMood} Artists"
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(matchingArtists, key = { it.id }) { artist ->
                                        GammaArtistCard(
                                            artist = artist,
                                            onClick = { onArtistClick(artist.id) }
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
}

@Composable
private fun DiscoverHeader(
    isOnline: Boolean,
    onUploadClick: () -> Unit,
    onNetworkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "GAMA",
                    style = MaterialTheme.typography.displayMedium.copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GammaPrimary, GammaSecondary)
                        )
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(GammaPrimary, CircleShape)
                )
            }
            Text(
                text = "Your personal music space",
                style = MaterialTheme.typography.bodyMedium,
                color = GammaTextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Upload button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GammaPrimary)
                    .clickable(onClick = onUploadClick)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("upload_music_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload Music",
                        tint = GammaSurfaceElevated,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Upload",
                        style = MaterialTheme.typography.labelSmall,
                        color = GammaSurfaceElevated
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodAndGenreSection(
    selectedMood: String,
    onMoodSelect: (String) -> Unit
) {
    val moods = listOf(
        "All",
        "⚡ Energize",
        "🧘 Relax",
        "🌙 Sleep",
        "🎯 Focus",
        "🔥 Workout",
        "🎉 Party",
        "💖 Romance",
        "☕ Morning Chill"
    )

    val genres = listOf(
        "Hip-Hop",
        "Rock & Metal",
        "Pop Hits",
        "Cyberpunk & Synthwave",
        "Nu Metal & Alt-Rock",
        "Electronic & EDM",
        "432Hz & Ambient",
        "Lo-Fi & Chill",
        "Acoustic & Folk",
        "R&B & Soul",
        "Phonk & Drift",
        "Classics",
        "Orchestral & Cinematic"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: Moods & Vibes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MOODS & VIBES",
                style = MaterialTheme.typography.labelSmall,
                color = GammaSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                fontSize = 10.5.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("moods_lazy_row")
        ) {
            items(moods) { mood ->
                GammaFilterChip(
                    text = mood,
                    selected = mood == selectedMood,
                    onClick = { onMoodSelect(mood) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Music Genres
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MUSIC GENRES",
                style = MaterialTheme.typography.labelSmall,
                color = GammaGlowCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                fontSize = 10.5.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("genres_lazy_row")
        ) {
            items(genres) { genre ->
                GammaFilterChip(
                    text = genre,
                    selected = genre == selectedMood,
                    onClick = { onMoodSelect(genre) }
                )
            }
        }
    }
}
