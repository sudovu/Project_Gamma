package com.example.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.media.PlaybackManager
import com.example.data.repository.MusicRepository
import com.example.data.provider.CuratedFrequencies
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab(val title: String) {
    FAVORITES("Favorites"),
    DOWNLOADED("Downloaded"),
    DISCOVERY("Discovery"),
    UPLOADS("Uploads"),
    PLAYLISTS("Playlists"),
    HISTORY("Played")
}

enum class PlaylistFilter(val title: String) {
    ALL("All Playlists"),
    MOST_PLAYED("Most Played"),
    LEAST_PLAYED("Least Played")
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.FAVORITES,
    val selectedPlaylistFilter: PlaylistFilter = PlaylistFilter.ALL,
    val favorites: List<Track> = emptyList(),
    val downloaded: List<Track> = emptyList(),
    val discoveryHistory: List<Track> = emptyList(),
    val uploads: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val history: List<Track> = emptyList(),
    val mostPlayed: List<Track> = emptyList(),
    val leastPlayed: List<Track> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val downloadedIds: Set<String> = emptySet(),
    val downloadingTrackId: String? = null
)

private data class LibraryData(
    val favorites: List<Track>,
    val discoveryHistory: List<Track>,
    val uploads: List<Track>,
    val playlists: List<Playlist>,
    val history: List<Track>,
    val mostPlayed: List<Track> = emptyList(),
    val leastPlayed: List<Track> = emptyList()
)

private data class LibraryDataWithDownloads(
    val base: LibraryData,
    val downloaded: List<Track>,
    val downloadedIds: Set<String>,
    val downloadingTrackId: String?
)

class LibraryViewModel(
    private val repository: MusicRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.FAVORITES)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    private val _selectedPlaylistFilter = MutableStateFlow(PlaylistFilter.ALL)
    val selectedPlaylistFilter: StateFlow<PlaylistFilter> = _selectedPlaylistFilter.asStateFlow()

    private val _playStatsData = combine(
        repository.getMostPlayedTracks(),
        repository.getLeastPlayedTracks()
    ) { most, least ->
        Pair(most, least)
    }

    private val _coreData = combine(
        repository.getFavorites(),
        repository.getDiscoveryHistory(),
        repository.getUploadedTracks(),
        repository.getUserPlaylists(),
        repository.getRecentTracks()
    ) { favs, disc, uploads, pls, hist ->
        LibraryData(favs, disc, uploads, pls, hist)
    }

    private val _baseData = combine(_coreData, _playStatsData) { core, (most, least) ->
        core.copy(mostPlayed = most, leastPlayed = least)
    }

    private val _dataState = combine(
        _baseData,
        playbackManager.musicDownloader.downloadStatuses
    ) { base, statuses ->
        val allKnown = (CuratedFrequencies.allTracks + base.discoveryHistory + base.uploads + base.favorites + base.history + base.mostPlayed + base.leastPlayed).distinctBy { it.id }
        val downloaded = allKnown.filter { track ->
            track.localAudioUri.isNotBlank() ||
            statuses[track.id] is com.example.core.download.DownloadStatus.Completed ||
            playbackManager.isTrackDownloaded(track.id)
        }
        val downloadedIds = downloaded.map { it.id }.toSet()
        val downloadingTrackId = statuses.entries.firstOrNull { it.value is com.example.core.download.DownloadStatus.Downloading }?.key
        LibraryDataWithDownloads(base, downloaded, downloadedIds, downloadingTrackId)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        _selectedTab,
        _selectedPlaylistFilter,
        _dataState
    ) { tab, filter, data ->
        LibraryUiState(
            selectedTab = tab,
            selectedPlaylistFilter = filter,
            favorites = data.base.favorites,
            downloaded = data.downloaded,
            discoveryHistory = data.base.discoveryHistory,
            uploads = data.base.uploads,
            playlists = data.base.playlists,
            history = data.base.history,
            mostPlayed = data.base.mostPlayed,
            leastPlayed = data.base.leastPlayed,
            favoriteIds = data.base.favorites.map { it.id }.toSet(),
            downloadedIds = data.downloadedIds,
            downloadingTrackId = data.downloadingTrackId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun selectPlaylistFilter(filter: PlaylistFilter) {
        _selectedPlaylistFilter.value = filter
    }

    fun playTrack(track: Track, queue: List<Track>) {
        playbackManager.playTrack(track, queue)
        viewModelScope.launch {
            repository.recordRecent(track)
        }
    }

    fun playAll(tracks: List<Track>, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val queue = if (shuffle) tracks.shuffled() else tracks
        playbackManager.playTrack(queue.first(), queue)
        viewModelScope.launch {
            repository.recordRecent(queue.first())
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track)
        }
    }

    fun downloadTrack(track: Track) {
        playbackManager.downloadTrack(track)
    }

    fun deleteDownload(trackId: String) {
        playbackManager.musicDownloader.deleteDownload(trackId)
    }

    fun uploadTrack(
        title: String,
        artist: String,
        frequencyHz: Int,
        genre: String,
        youtubeUrlOrId: String,
        localAudioUri: String
    ) {
        viewModelScope.launch {
            repository.uploadTrack(
                title = title,
                artist = artist,
                frequencyHz = frequencyHz,
                genre = genre,
                youtubeUrlOrId = youtubeUrlOrId,
                localAudioUri = localAudioUri
            )
            _selectedTab.value = LibraryTab.UPLOADS
        }
    }

    fun deleteUploadedTrack(id: String) {
        viewModelScope.launch {
            repository.deleteUploadedTrack(id)
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                repository.createPlaylist(name.trim(), description.trim())
            }
        }
    }

    fun importPlaylist(name: String, tracks: List<Track>) {
        if (name.isNotBlank()) {
            viewModelScope.launch {
                val playlistId = repository.createPlaylist(name.trim(), "Imported playlist • ${tracks.size} tracks")
                tracks.forEach { track ->
                    repository.addTrackToPlaylist(playlistId, track)
                }
                _selectedTab.value = LibraryTab.PLAYLISTS
            }
        }
    }

    fun downloadPlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.getPlaylist(playlistId).collect { result ->
                result.onSuccess { playlist ->
                    playlist.tracks.forEach { track ->
                        playbackManager.downloadTrack(track)
                    }
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearDiscoveryHistory() {
        viewModelScope.launch {
            repository.clearDiscoveryHistory()
        }
    }

    companion object {
        fun provideFactory(
            repository: MusicRepository,
            playbackManager: PlaybackManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(repository, playbackManager) as T
            }
        }
    }
}
