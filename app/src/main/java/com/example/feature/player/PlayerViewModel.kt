package com.example.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.media.PlaybackManager
import com.example.data.provider.LrcLibLyricsProvider
import com.example.data.provider.TrackLyrics
import com.example.data.repository.MusicRepository
import com.example.domain.model.PlaybackState
import com.example.domain.model.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PlayerSheetType {
    NONE,
    QUEUE,
    SPECS,
    EQUALIZER,
    SLEEP_TIMER,
    SPEED,
    LYRICS
}

enum class PlayerStyle {
    CYBER_NEON,
    FLUID_GLASS
}

sealed interface LyricsUiState {
    data object Loading : LyricsUiState
    data class Success(val lyrics: TrackLyrics) : LyricsUiState
    data object Empty : LyricsUiState
    data class Error(val message: String) : LyricsUiState
}

data class PlayerUiState(
    val playbackState: PlaybackState = PlaybackState(),
    val isFavorite: Boolean = false,
    val sheetType: PlayerSheetType = PlayerSheetType.NONE,
    val playerStyle: PlayerStyle = PlayerStyle.FLUID_GLASS
)

class PlayerViewModel(
    val playbackManager: PlaybackManager,
    private val repository: MusicRepository
) : ViewModel() {

    private val _playerStyle = MutableStateFlow(PlayerStyle.FLUID_GLASS)
    val playerStyle: StateFlow<PlayerStyle> = _playerStyle.asStateFlow()

    private val _lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Empty)
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    private val _lyricsOffsetMs = MutableStateFlow(0L)
    val lyricsOffsetMs: StateFlow<Long> = _lyricsOffsetMs.asStateFlow()

    private var lyricsFetchJob: Job? = null
    private var lastFetchedTrackId: String? = null

    init {
        viewModelScope.launch {
            playbackManager.playbackState
                .map { it.currentTrack }
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collect { track ->
                    _lyricsOffsetMs.value = 0L
                    if (track != null) {
                        fetchLyricsForTrack(track)
                    } else {
                        _lyricsState.value = LyricsUiState.Empty
                        lastFetchedTrackId = null
                    }
                }
        }
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackManager.playbackState,
        repository.getFavorites(),
        _playerStyle
    ) { state, favorites, style ->
        val track = state.currentTrack
        val isFav = if (track != null) favorites.any { it.id == track.id } else false
        PlayerUiState(
            playbackState = state,
            isFavorite = isFav,
            playerStyle = style
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState()
    )

    fun togglePlayerStyle() {
        _playerStyle.value = if (_playerStyle.value == PlayerStyle.CYBER_NEON) {
            PlayerStyle.FLUID_GLASS
        } else {
            PlayerStyle.CYBER_NEON
        }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        _lyricsOffsetMs.value = (_lyricsOffsetMs.value + deltaMs).coerceIn(-15000L, 15000L)
    }

    fun resetLyricsOffset() {
        _lyricsOffsetMs.value = 0L
    }

    fun retryFetchLyrics() {
        val current = playbackManager.playbackState.value.currentTrack ?: return
        lastFetchedTrackId = null
        _lyricsOffsetMs.value = 0L
        fetchLyricsForTrack(current)
    }

    private fun fetchLyricsForTrack(track: Track) {
        if (track.id == lastFetchedTrackId && _lyricsState.value is LyricsUiState.Success) {
            return
        }
        lastFetchedTrackId = track.id
        lyricsFetchJob?.cancel()
        lyricsFetchJob = viewModelScope.launch {
            _lyricsState.value = LyricsUiState.Loading
            try {
                val lyrics = LrcLibLyricsProvider.getLyrics(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    durationMs = track.durationSeconds * 1000L
                )
                if (lyrics != null && (lyrics.lines.isNotEmpty() || lyrics.plainLyrics != null)) {
                    _lyricsState.value = LyricsUiState.Success(lyrics)
                } else {
                    _lyricsState.value = LyricsUiState.Empty
                }
            } catch (e: Exception) {
                _lyricsState.value = LyricsUiState.Error(e.message ?: "Failed to load lyrics")
            }
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()

    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)

    fun skipNext() = playbackManager.skipNext()

    fun skipPrevious() = playbackManager.skipPrevious()

    fun toggleShuffle() = playbackManager.toggleShuffle()

    fun cycleRepeatMode() = playbackManager.cycleRepeatMode()

    fun removeFromQueue(index: Int) = playbackManager.removeFromQueue(index)

    fun clearQueue() = playbackManager.clearQueue()

    fun toggleSmartQueue(enabled: Boolean? = null) = playbackManager.toggleSmartQueue(enabled)

    fun appendSmartRecommendations(count: Int = 3) = playbackManager.appendSmartRecommendations(count)

    fun setEqualizerBand(index: Int, gainDb: Float) = playbackManager.setEqualizerBand(index, gainDb)

    fun setTuningPreset(preset: com.example.domain.model.TuningPreset) = playbackManager.setTuningPreset(preset)

    fun toggleEqualizer(enabled: Boolean? = null) = playbackManager.toggleEqualizer(enabled)

    fun setMasterGain(gain: Float) = playbackManager.setMasterGain(gain)

    fun jumpToQueueIndex(index: Int) {
        val q = playbackManager.playbackState.value.queue
        if (index in q.indices) {
            playbackManager.playTrack(q[index], q)
        }
    }

    fun toggleFavorite() {
        val current = playbackManager.playbackState.value.currentTrack ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current)
        }
    }

    fun downloadCurrentTrack() {
        val current = playbackManager.playbackState.value.currentTrack ?: return
        playbackManager.downloadTrack(current)
    }

    fun downloadTrack(track: Track) {
        playbackManager.downloadTrack(track)
    }

    fun seekBy(deltaMs: Long) = playbackManager.seekBy(deltaMs)

    fun setPlaybackSpeed(speed: Float) = playbackManager.setPlaybackSpeed(speed)

    fun startSleepTimer(minutes: Int, endOfTrack: Boolean = false) =
        playbackManager.startSleepTimer(minutes, endOfTrack)

    fun cancelSleepTimer() = playbackManager.cancelSleepTimer()

    fun extendSleepTimer(extraMinutes: Int = 15) = playbackManager.extendSleepTimer(extraMinutes)

    fun reorderQueue(fromIndex: Int, toIndex: Int) = playbackManager.reorderQueue(fromIndex, toIndex)

    companion object {
        fun provideFactory(
            playbackManager: PlaybackManager,
            repository: MusicRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(playbackManager, repository) as T
            }
        }
    }
}
