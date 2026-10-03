package com.example.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.media.PlaybackManager
import com.example.core.util.NetworkMonitor
import com.example.data.repository.MusicRepository
import com.example.domain.model.DiscoverFeed
import com.example.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Success(
        val feed: DiscoverFeed,
        val recentTracks: List<Track>,
        val favoriteIds: Set<String>,
        val selectedMood: String = "All"
    ) : DiscoverUiState
    data class Error(val message: String) : DiscoverUiState
}

class DiscoverViewModel(
    private val repository: MusicRepository,
    private val playbackManager: PlaybackManager,
    private val networkMonitor: NetworkMonitor? = null
) : ViewModel() {

    private val _selectedMood = MutableStateFlow("All")
    val selectedMood: StateFlow<String> = _selectedMood.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor?.isOnline
        ?: MutableStateFlow(true).asStateFlow()

    private val _rawState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)

    val uiState: StateFlow<DiscoverUiState> = combine(
        _rawState,
        repository.getFavorites(),
        repository.getRecentTracks(),
        _selectedMood
    ) { raw, favorites, recent, mood ->
        when (raw) {
            is DiscoverUiState.Success -> {
                val favIds = favorites.map { it.id }.toSet()
                raw.copy(
                    favoriteIds = favIds,
                    recentTracks = recent,
                    selectedMood = mood
                )
            }
            else -> raw
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiscoverUiState.Loading
    )

    init {
        loadFeed()
    }

    fun loadFeed() {
        _rawState.value = DiscoverUiState.Loading
        viewModelScope.launch {
            repository.getDiscoverFeed().collect { result ->
                result.fold(
                    onSuccess = { feed ->
                        _rawState.value = DiscoverUiState.Success(
                            feed = feed,
                            recentTracks = emptyList(),
                            favoriteIds = emptySet()
                        )
                    },
                    onFailure = { error ->
                        _rawState.value = DiscoverUiState.Error(
                            error.message ?: "Failed to load music feed"
                        )
                    }
                )
            }
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh(
        tasteManager: com.example.core.taste.TastePreferenceManager? = null,
        youtubeProvider: com.example.data.provider.YouTubeProvider? = null
    ) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                kotlinx.coroutines.withTimeoutOrNull(6000L) {
                    val currentMood = _selectedMood.value
                    if (currentMood == "All") {
                        val result = repository.refreshDiscoverFeed()
                        result.fold(
                            onSuccess = { feed ->
                                _rawState.value = DiscoverUiState.Success(
                                    feed = feed,
                                    recentTracks = emptyList(),
                                    favoriteIds = emptySet(),
                                    selectedMood = "All"
                                )
                            },
                            onFailure = { error ->
                                _rawState.value = DiscoverUiState.Error(
                                    error.message ?: "Failed to refresh music feed"
                                )
                            }
                        )
                        if (youtubeProvider != null && tasteManager != null) {
                            launch {
                                try {
                                    tasteManager.syncLiveTastes(youtubeProvider)
                                } catch (_: Exception) {}
                            }
                        }
                    } else {
                        // Force refresh genre stream from YouTube
                        val cleanQuery = currentMood.replace(Regex("""^[^\w\s&]+"""), "").trim()
                        val genreYt = repository.getTracksForGenreFromYouTube(cleanQuery.ifEmpty { currentMood })
                        if (genreYt.isNotEmpty()) {
                            val current = _rawState.value
                            if (current is DiscoverUiState.Success) {
                                val updatedMoodPlaylists = current.feed.moodPlaylists.toMutableMap()
                                updatedMoodPlaylists[currentMood] = genreYt.distinctBy { it.youtubeVideoId.ifEmpty { it.id } }
                                _rawState.value = current.copy(
                                    feed = current.feed.copy(moodPlaylists = updatedMoodPlaylists)
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private val _loadedGenres = mutableSetOf<String>()

    fun selectMood(mood: String) {
        _selectedMood.value = mood
        if (mood != "All" && !_loadedGenres.contains(mood)) {
            _loadedGenres.add(mood)
            viewModelScope.launch {
                try {
                    val cleanQuery = mood.replace(Regex("""^[^\w\s&]+"""), "").trim()
                    val genreYt = repository.getTracksForGenreFromYouTube(cleanQuery.ifEmpty { mood })
                    if (genreYt.isNotEmpty()) {
                        val current = _rawState.value
                        if (current is DiscoverUiState.Success) {
                            val existingMoodList = current.feed.moodPlaylists[mood] ?: emptyList()
                            val mergedList = (genreYt + existingMoodList).distinctBy { it.youtubeVideoId.ifEmpty { it.id } }
                            val updatedMoodPlaylists = current.feed.moodPlaylists.toMutableMap()
                            updatedMoodPlaylists[mood] = mergedList
                            _rawState.value = current.copy(
                                feed = current.feed.copy(moodPlaylists = updatedMoodPlaylists)
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
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
            val uploaded = repository.uploadTrack(
                title = title,
                artist = artist,
                frequencyHz = frequencyHz,
                genre = genre,
                youtubeUrlOrId = youtubeUrlOrId,
                localAudioUri = localAudioUri
            )
            // Immediately refresh feed so the uploaded track is 100% discoverable
            loadFeed()
        }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        playbackManager.playTrack(track, queue)
        viewModelScope.launch {
            repository.recordRecent(track)
        }
    }

    fun playShuffled(queue: List<Track>) {
        val playableOnly = queue.filter { com.example.core.media.SmartQueueEngine.isTrackPlayable(it) }
        playbackManager.playQueueShuffled(if (playableOnly.isNotEmpty()) playableOnly else queue)
        playbackManager.playbackState.value.currentTrack?.let { track ->
            viewModelScope.launch {
                repository.recordRecent(track)
            }
        }
    }

    fun playAll(queue: List<Track>, shuffle: Boolean = false) {
        if (shuffle) {
            playShuffled(queue)
        } else {
            val playableOnly = queue.filter { com.example.core.media.SmartQueueEngine.isTrackPlayable(it) }
            val list = if (playableOnly.isNotEmpty()) playableOnly else queue
            if (list.isNotEmpty()) {
                playbackManager.playQueue(list, startIndex = 0)
                playbackManager.playbackState.value.currentTrack?.let { track ->
                    viewModelScope.launch {
                        repository.recordRecent(track)
                    }
                }
            }
        }
    }

    val userPlaylists: StateFlow<List<com.example.domain.model.Playlist>> = repository.getUserPlaylists().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun createPlaylistAndAddTrack(name: String, track: Track) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name, "Personal soundwave playlist")
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            repository.toggleFavorite(track)
        }
    }

    companion object {
        fun provideFactory(
            repository: MusicRepository,
            playbackManager: PlaybackManager,
            networkMonitor: NetworkMonitor? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DiscoverViewModel(repository, playbackManager, networkMonitor) as T
            }
        }
    }
}
