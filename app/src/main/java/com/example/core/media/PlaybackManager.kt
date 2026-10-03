package com.example.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.example.data.provider.CuratedFrequencies
import com.example.data.provider.YouTubeProvider
import com.example.domain.model.EqualizerSettings
import com.example.domain.model.PlaybackState
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import com.example.domain.model.TuningPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface YouTubePlayerBridge {
    fun loadVideo(videoId: String)
    fun playVideo()
    fun pauseVideo()
    fun seekToSeconds(seconds: Float)
    fun setPlaybackRate(rate: Float)
    fun setVolumePercent(volume: Int)
    fun applyEqualizer(bands: List<Float>, masterGain: Float, isEnabled: Boolean)
}

class PlaybackManager(private val context: Context) {

    companion object {
        var activeInstance: PlaybackManager? = null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioEngine = GamaAudioEngine(context)
    val musicDownloader = com.example.core.download.MusicDownloader(context)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var playerBridge: YouTubePlayerBridge? = null
    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var originalQueueBeforeShuffle: List<Track> = emptyList()

    private var candidateProvider: (suspend () -> List<Track>)? = null
    private var cachedCandidates: List<Track> = CuratedFrequencies.allTracks
    private var isTransitioningTrack = false

    // Audio Focus listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val current = _playbackState.value.currentTrack
                val isYouTube = current?.let { it.youtubeVideoId.isNotEmpty() && it.localAudioUri.isEmpty() } ?: false
                if (!isYouTube) {
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Audio focus restored
            }
        }
    }

    private var focusRequest: AudioFocusRequest? = null
    var onTrackStartedListener: ((Track) -> Unit)? = null

    init {
        activeInstance = this
        audioEngine.onTrackCompletedListener = {
            scope.launch { onTrackFinished() }
        }
        startProgressTicker()
    }

    fun setCandidateProvider(provider: suspend () -> List<Track>) {
        this.candidateProvider = provider
        scope.launch {
            try {
                val list = provider()
                if (list.isNotEmpty()) {
                    val obsolete = setOf("trk_sol_01", "trk_sol_02", "trk_vek_01", "trk_vek_02", "trk_lofi_01", "trk_lofi_02", "trk_aeth_01", "trk_aeth_02", "trk_kin_01", "trk_kin_02")
                    cachedCandidates = (CuratedFrequencies.allTracks + list)
                        .distinctBy { it.id }
                        .filterNot { it.id in obsolete || it.title.contains("Hyper-Velocity", ignoreCase = true) || it.artist.contains("Kinetic Pulse", ignoreCase = true) }
                        .filterNot { SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) }
                }
            } catch (_: Exception) {}
        }
    }

    fun getAvailableCandidates(): List<Track> {
        val pool = if (cachedCandidates.isNotEmpty()) cachedCandidates else CuratedFrequencies.allTracks
        return pool.filter { !SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) && SmartQueueEngine.isTrackPlayable(it) }
    }

    fun attachBridge(bridge: YouTubePlayerBridge) {
        this.playerBridge = bridge
        val eq = _playbackState.value.equalizerSettings
        bridge.applyEqualizer(
            bands = eq.bands.map { it.gainDb },
            masterGain = eq.masterGain,
            isEnabled = eq.isEnabled
        )
        _playbackState.value.currentTrack?.let { track ->
            if (track.youtubeVideoId.isNotEmpty()) {
                bridge.loadVideo(track.youtubeVideoId)
                if (_playbackState.value.isPlaying) {
                    bridge.playVideo()
                }
            }
        }
    }

    fun detachBridge() {
        this.playerBridge = null
    }

    fun playTrack(track: Track, newQueue: List<Track> = emptyList()) {
        val downloadedFile = musicDownloader.getDownloadedFile(track.id)
        val playableTrack = if (downloadedFile != null) {
            track.copy(localAudioUri = downloadedFile.absolutePath, isDownloaded = true)
        } else {
            track.copy(isDownloaded = musicDownloader.isTrackDownloaded(track.id))
        }

        val queue = if (newQueue.isNotEmpty()) newQueue else listOf(playableTrack)
        val index = queue.indexOfFirst { it.id == playableTrack.id }.coerceAtLeast(0)

        val isYouTube = playableTrack.youtubeVideoId.isNotEmpty() && playableTrack.localAudioUri.isEmpty()
        if (!isYouTube) {
            requestAudioFocus()
        }

        val updatedHistory = (_playbackState.value.playedTracksHistory + playableTrack).takeLast(30)
        val profile = SmartQueueEngine.analyzePlayedTracks(updatedHistory)

        val isSingleOrTiny = queue.size <= 2
        val shouldAutoAppend = _playbackState.value.isSmartQueueEnabled && ((queue.size - 1 - index <= 2) || isSingleOrTiny)
        val finalQueue = if (shouldAutoAppend) {
            val countNeeded = if (isSingleOrTiny) 8 else 4
            val recommendations = SmartQueueEngine.generateGenreRecommendations(
                targetTrack = playableTrack,
                playedTracks = updatedHistory,
                candidatePool = getAvailableCandidates(),
                currentQueue = queue,
                count = countNeeded
            )
            SmartQueueEngine.deduplicateTracks(queue + recommendations)
        } else {
            SmartQueueEngine.deduplicateTracks(queue)
        }

        isTransitioningTrack = true
        val trackDurationMs = (playableTrack.durationSeconds * 1000L).coerceAtLeast(1000L)
        audioEngine.setExternalPosition(0L, trackDurationMs)

        _playbackState.update { current ->
            current.copy(
                currentTrack = playableTrack,
                queue = finalQueue,
                queueIndex = index,
                isPlaying = true,
                isBuffering = false,
                positionMs = 0L,
                durationMs = trackDurationMs,
                errorMessage = null,
                isAudioOnlyMode = false,
                isVideoUnavailable = false,
                playedTracksHistory = updatedHistory,
                smartQueueProfile = profile
            )
        }

        // Play via Native GamaAudioEngine (handles local downloaded files or stops MediaPlayer for YouTube)
        audioEngine.setPlaybackSpeed(_playbackState.value.playbackSpeed)
        audioEngine.playTrack(playableTrack)

        if (isYouTube) {
            playerBridge?.loadVideo(playableTrack.youtubeVideoId)
            playerBridge?.setPlaybackRate(_playbackState.value.playbackSpeed)
            playerBridge?.playVideo()
        } else {
            playerBridge?.pauseVideo()
        }

        GamaPlaybackService.start(
            context = context,
            title = playableTrack.title,
            artist = playableTrack.artist,
            isPlaying = true,
            artUrl = playableTrack.artworkUrl
        )

        onTrackStartedListener?.invoke(playableTrack)
    }

    fun playQueue(queue: List<Track>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, queue.lastIndex)
        playTrack(queue[validIndex], queue)
    }

    fun playQueueShuffled(queue: List<Track>, startTrack: Track? = null) {
        if (queue.isEmpty()) return
        val playableOnly = queue.filter { SmartQueueEngine.isTrackPlayable(it) }
        val pool = if (playableOnly.isNotEmpty()) playableOnly else queue
        val deduped = SmartQueueEngine.deduplicateTracks(pool)
        if (deduped.isEmpty()) return
        val shuffled = deduped.shuffled()
        val firstTrack = if (startTrack != null && SmartQueueEngine.isTrackPlayable(startTrack)) startTrack else shuffled.first()
        val remaining = shuffled.filter { !SmartQueueEngine.areTracksEqual(it, firstTrack) }
        val finalQueue = listOf(firstTrack) + remaining

        originalQueueBeforeShuffle = deduped
        playTrack(firstTrack, finalQueue)
        _playbackState.update { it.copy(isShuffle = true) }
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        val track = _playbackState.value.currentTrack
        if (track == null) {
            val q = _playbackState.value.queue
            if (q.isNotEmpty()) {
                playTrack(q[0], q)
            }
            return
        }

        val isYouTube = track.youtubeVideoId.isNotEmpty() && track.localAudioUri.isEmpty()
        if (!isYouTube) {
            requestAudioFocus()
        }
        _playbackState.update { it.copy(isPlaying = true, isBuffering = false) }
        if (track.localAudioUri.isNotEmpty() || track.streamUrl.isNotEmpty()) {
            audioEngine.resume()
        } else if (isYouTube) {
            audioEngine.resumeVisualizer()
            playerBridge?.playVideo()
        }

        GamaPlaybackService.update(
            context = context,
            title = track.title,
            artist = track.artist,
            isPlaying = true,
            artUrl = track.artworkUrl
        )
    }

    fun pause() {
        _playbackState.update { it.copy(isPlaying = false) }
        audioEngine.pause()
        playerBridge?.pauseVideo()

        _playbackState.value.currentTrack?.let { track ->
            GamaPlaybackService.update(
                context = context,
                title = track.title,
                artist = track.artist,
                isPlaying = false,
                artUrl = track.artworkUrl
            )
        }
    }

    fun stopPlayback() {
        _playbackState.update {
            it.copy(
                isPlaying = false,
                currentTrack = null,
                positionMs = 0L
            )
        }
        audioEngine.stop()
        playerBridge?.pauseVideo()
        GamaPlaybackService.stop(context)
    }

    fun seekTo(positionMs: Long) {
        val track = _playbackState.value.currentTrack
        val duration = _playbackState.value.durationMs
        val clamped = positionMs.coerceIn(0L, duration.coerceAtLeast(1000L))
        isTransitioningTrack = false
        _playbackState.update { it.copy(positionMs = clamped) }
        if (track != null && (track.localAudioUri.isNotEmpty() || track.streamUrl.isNotEmpty())) {
            audioEngine.seekTo(clamped)
        } else {
            audioEngine.setExternalPosition(clamped, duration)
            playerBridge?.seekToSeconds(clamped / 1000f)
        }
    }

    fun seekBy(deltaMs: Long) {
        val current = _playbackState.value.positionMs
        seekTo(current + deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 3.0f)
        _playbackState.update { it.copy(playbackSpeed = clamped) }
        audioEngine.setPlaybackSpeed(clamped)
        playerBridge?.setPlaybackRate(clamped)
    }

    // ==========================================
    // SLEEP TIMER CONTROLS
    // ==========================================

    fun startSleepTimer(minutes: Int, endOfTrack: Boolean = false) {
        cancelSleepTimer()
        if (endOfTrack) {
            _playbackState.update {
                it.copy(
                    isSleepTimerActive = true,
                    isSleepTimerEndOfTrack = true,
                    sleepTimerRemainingSeconds = null,
                    sleepTimerInitialSeconds = null
                )
            }
            return
        }

        if (minutes <= 0) return
        val totalSeconds = minutes * 60L
        _playbackState.update {
            it.copy(
                isSleepTimerActive = true,
                isSleepTimerEndOfTrack = false,
                sleepTimerRemainingSeconds = totalSeconds,
                sleepTimerInitialSeconds = totalSeconds
            )
        }
        audioEngine.setVolumeDucking(1.0f)

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _playbackState.update { it.copy(sleepTimerRemainingSeconds = remaining) }

                // In final 30 seconds, smoothly duck volume for a gentle fade out
                if (remaining in 1..30) {
                    val duckFactor = remaining / 30f
                    audioEngine.setVolumeDucking(duckFactor)
                    val ytVol = (100 * duckFactor).toInt()
                    playerBridge?.setVolumePercent(ytVol)
                }
            }
            if (isActive) {
                pause()
                audioEngine.setVolumeDucking(1.0f)
                playerBridge?.setVolumePercent(100)
                _playbackState.update {
                    it.copy(
                        isSleepTimerActive = false,
                        isSleepTimerEndOfTrack = false,
                        sleepTimerRemainingSeconds = null,
                        sleepTimerInitialSeconds = null
                    )
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        audioEngine.setVolumeDucking(1.0f)
        playerBridge?.setVolumePercent(100)
        _playbackState.update {
            it.copy(
                isSleepTimerActive = false,
                isSleepTimerEndOfTrack = false,
                sleepTimerRemainingSeconds = null,
                sleepTimerInitialSeconds = null
            )
        }
    }

    fun extendSleepTimer(extraMinutes: Int = 15) {
        val currentRemaining = _playbackState.value.sleepTimerRemainingSeconds ?: 0L
        val newTotal = (currentRemaining + extraMinutes * 60L).coerceAtLeast(extraMinutes * 60L)
        startSleepTimer(minutes = ((newTotal + 59L) / 60L).toInt(), endOfTrack = false)
    }

    private fun onTrackFinished() {
        val state = _playbackState.value
        if (state.isSleepTimerActive && state.isSleepTimerEndOfTrack) {
            pause()
            cancelSleepTimer()
            return
        }
        skipNext()
    }

    fun skipNext() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        if (state.repeatMode == RepeatMode.ONE) {
            seekTo(0)
            play()
            return
        }

        // Advance to next track, skipping over collections/mixes to guarantee a good single song
        var nextIndex = state.queueIndex + 1
        while (nextIndex < queue.size && SmartQueueEngine.isCollectionOrMix(queue[nextIndex].title, queue[nextIndex].durationSeconds)) {
            nextIndex++
        }

        if (nextIndex < queue.size) {
            playTrack(queue[nextIndex], queue)
            if (state.isSmartQueueEnabled && (queue.size - 1 - nextIndex <= 2)) {
                appendSmartRecommendations(count = 5)
            }
        } else {
            // Auto-append more tracks related to current genre so playlist NEVER ENDS!
            val singlePool = getAvailableCandidates().filter { !SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) }
            val recommendations = SmartQueueEngine.generateGenreRecommendations(
                targetTrack = state.currentTrack,
                playedTracks = state.playedTracksHistory,
                candidatePool = singlePool,
                currentQueue = queue,
                count = 5
            )
            val updatedQueue = SmartQueueEngine.deduplicateTracks(queue + recommendations)
            var finalNextIndex = state.queueIndex + 1
            while (finalNextIndex < updatedQueue.size && SmartQueueEngine.isCollectionOrMix(updatedQueue[finalNextIndex].title, updatedQueue[finalNextIndex].durationSeconds)) {
                finalNextIndex++
            }
            if (finalNextIndex < updatedQueue.size) {
                playTrack(updatedQueue[finalNextIndex], updatedQueue)
            } else if (state.repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
                val nextT = if (state.isShuffle) queue.shuffled().first() else queue[0]
                playTrack(nextT, queue)
            }
        }
    }

    fun skipPrevious() {
        val state = _playbackState.value
        if (state.positionMs > 3000L) {
            seekTo(0)
            return
        }

        val queue = state.queue
        if (queue.isEmpty()) return

        val prevIndex = state.queueIndex - 1
        if (prevIndex >= 0) {
            playTrack(queue[prevIndex], queue)
        } else {
            seekTo(0)
        }
    }

    fun toggleShuffle() {
        val state = _playbackState.value
        val currentTrack = state.currentTrack ?: return
        val currentQueue = state.queue

        if (!state.isShuffle) {
            originalQueueBeforeShuffle = currentQueue
            val playableQueue = currentQueue.filter { SmartQueueEngine.isTrackPlayable(it) }
            val pool = if (playableQueue.isNotEmpty()) playableQueue else currentQueue
            val remaining = pool.filter { it.id != currentTrack.id }.shuffled()
            val newQueue = listOf(currentTrack) + remaining
            _playbackState.update {
                it.copy(isShuffle = true, queue = newQueue, queueIndex = 0)
            }
        } else {
            val restoredQueue = if (originalQueueBeforeShuffle.isNotEmpty()) originalQueueBeforeShuffle else currentQueue
            val newIndex = restoredQueue.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0)
            _playbackState.update {
                it.copy(isShuffle = false, queue = restoredQueue, queueIndex = newIndex)
            }
        }
    }

    fun cycleRepeatMode() {
        val current = _playbackState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.update { it.copy(repeatMode = next) }
    }

    fun addToQueue(track: Track) {
        _playbackState.update { state ->
            val updatedQueue = state.queue + track
            state.copy(queue = updatedQueue)
        }
    }

    fun removeFromQueue(index: Int) {
        _playbackState.update { state ->
            if (index in state.queue.indices) {
                val updatedQueue = state.queue.toMutableList().apply { removeAt(index) }
                val newIndex = if (index < state.queueIndex) state.queueIndex - 1 else state.queueIndex
                state.copy(queue = updatedQueue, queueIndex = newIndex.coerceAtLeast(0))
            } else {
                state
            }
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        _playbackState.update { state ->
            if (fromIndex in state.queue.indices && toIndex in state.queue.indices) {
                val updated = state.queue.toMutableList()
                val item = updated.removeAt(fromIndex)
                updated.add(toIndex, item)
                val newCurrent = updated.indexOfFirst { it.id == state.currentTrack?.id }.coerceAtLeast(0)
                state.copy(queue = updated, queueIndex = newCurrent)
            } else {
                state
            }
        }
    }

    fun clearQueue() {
        val current = _playbackState.value.currentTrack
        _playbackState.update { state ->
            state.copy(queue = if (current != null) listOf(current) else emptyList(), queueIndex = 0)
        }
    }

    fun toggleSmartQueue(enabled: Boolean? = null) {
        val target = enabled ?: !_playbackState.value.isSmartQueueEnabled
        _playbackState.update { it.copy(isSmartQueueEnabled = target) }
        if (target) {
            val state = _playbackState.value
            val remaining = state.queue.size - 1 - state.queueIndex
            if (remaining <= 1) {
                appendSmartRecommendations(count = 3)
            }
        }
    }

    fun appendSmartRecommendations(count: Int = 5): List<Track> {
        val state = _playbackState.value
        val history = if (state.playedTracksHistory.isNotEmpty()) {
            state.playedTracksHistory
        } else {
            listOfNotNull(state.currentTrack)
        }

        val singlePool = getAvailableCandidates().filter { !SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) }
        val recommendations = SmartQueueEngine.generateGenreRecommendations(
            targetTrack = state.currentTrack,
            playedTracks = history,
            candidatePool = singlePool,
            currentQueue = state.queue,
            count = count
        )

        if (recommendations.isNotEmpty()) {
            _playbackState.update { current ->
                val newQueue = SmartQueueEngine.deduplicateTracks(current.queue + recommendations)
                current.copy(queue = newQueue)
            }
        }
        return recommendations
    }

    // ==========================================
    // EQUALIZER & TUNING CONTROLS
    // ==========================================

    fun setEqualizerBand(index: Int, gainDb: Float) {
        audioEngine.setBandGain(index, gainDb)
        _playbackState.update { current ->
            val currentSettings = current.equalizerSettings
            val updatedBands = currentSettings.bands.map { band ->
                if (band.index == index) band.copy(gainDb = gainDb) else band
            }
            val newSettings = currentSettings.copy(
                bands = updatedBands,
                currentPreset = TuningPreset.CUSTOM
            )
            playerBridge?.applyEqualizer(
                bands = updatedBands.map { it.gainDb },
                masterGain = newSettings.masterGain,
                isEnabled = newSettings.isEnabled
            )
            current.copy(equalizerSettings = newSettings)
        }
    }

    fun setTuningPreset(preset: TuningPreset) {
        audioEngine.applyPreset(preset)
        val gains = EqualizerSettings.presetGains(preset)
        _playbackState.update { current ->
            val currentSettings = current.equalizerSettings
            val updatedBands = currentSettings.bands.mapIndexed { i, band ->
                band.copy(gainDb = gains.getOrElse(i) { 0f })
            }
            val newSettings = currentSettings.copy(
                currentPreset = preset,
                bands = updatedBands
            )
            playerBridge?.applyEqualizer(
                bands = updatedBands.map { it.gainDb },
                masterGain = newSettings.masterGain,
                isEnabled = newSettings.isEnabled
            )
            current.copy(equalizerSettings = newSettings)
        }
    }

    fun toggleEqualizer(enabled: Boolean? = null) {
        val target = enabled ?: !_playbackState.value.equalizerSettings.isEnabled
        audioEngine.setEqualizerEnabled(target)
        _playbackState.update { current ->
            val newSettings = current.equalizerSettings.copy(isEnabled = target)
            playerBridge?.applyEqualizer(
                bands = newSettings.bands.map { it.gainDb },
                masterGain = newSettings.masterGain,
                isEnabled = target
            )
            current.copy(equalizerSettings = newSettings)
        }
    }

    fun setMasterGain(gain: Float) {
        audioEngine.setMasterGain(gain)
        _playbackState.update { current ->
            val newSettings = current.equalizerSettings.copy(masterGain = gain)
            playerBridge?.applyEqualizer(
                bands = newSettings.bands.map { it.gainDb },
                masterGain = gain,
                isEnabled = newSettings.isEnabled
            )
            current.copy(equalizerSettings = newSettings)
        }
    }

    fun setSpatialAudioEnabled(enabled: Boolean) {
        audioEngine.setSpatialAudioEnabled(enabled)
    }

    // JavaScript Bridge callbacks from YouTube
    fun onBridgePlayerStateChange(stateInt: Int) {
        when (stateInt) {
            1 -> { // Playing
                isTransitioningTrack = false
                _playbackState.update { it.copy(isPlaying = true, isBuffering = false) }
                audioEngine.resumeVisualizer()
                _playbackState.value.currentTrack?.let {
                    GamaPlaybackService.update(context, it.title, it.artist, true, it.artworkUrl)
                }
            }
            2 -> { // Paused
                _playbackState.update { it.copy(isPlaying = false, isBuffering = false) }
                audioEngine.pauseVisualizer()
                _playbackState.value.currentTrack?.let {
                    GamaPlaybackService.update(context, it.title, it.artist, false, it.artworkUrl)
                }
            }
            3 -> { // Buffering
                _playbackState.update { it.copy(isBuffering = true) }
            }
            0 -> { // Ended
                onTrackFinished()
            }
        }
    }

    fun onBridgeTimeUpdate(currentTimeSec: Float, durationSec: Float) {
        if (isTransitioningTrack) {
            if (currentTimeSec <= 1.5f && currentTimeSec >= 0f) {
                isTransitioningTrack = false
            } else if (currentTimeSec > 3f && _playbackState.value.positionMs == 0L) {
                // If direct seek or fast start occurs
                isTransitioningTrack = false
            }
        }
        // Zero reset glitch from unstarted/hidden webview must be ignored if playback already progressed
        if (currentTimeSec <= 0f && durationSec <= 0f && _playbackState.value.positionMs > 0L) {
            return
        }
        val posMs = (currentTimeSec.coerceAtLeast(0f) * 1000f).toLong()
        val durMs = if (durationSec > 0f) (durationSec * 1000f).toLong() else _playbackState.value.durationMs
        audioEngine.setExternalPosition(posMs, durMs)
        _playbackState.update { state ->
            state.copy(positionMs = posMs, durationMs = durMs)
        }
    }

    fun onBridgeError(errorMsg: String) {
        _playbackState.update { it.copy(isBuffering = false, errorMessage = errorMsg) }
    }

    fun onBridgeErrorWithCode(errorCode: Int) {
        val current = _playbackState.value.currentTrack
        android.util.Log.w("PlaybackManager", "onBridgeErrorWithCode: code=$errorCode, track=${current?.title}")

        val isUnavailable = errorCode in listOf(2, 5, 100, 101, 150)
        if (isUnavailable && current != null) {
            _playbackState.update { it.copy(isVideoUnavailable = true, isAudioOnlyMode = true) }
            playerBridge?.pauseVideo()

            // 1. If local audio exists, play immediately via native engine
            if (current.localAudioUri.isNotEmpty()) {
                audioEngine.playTrack(current)
                _playbackState.update { it.copy(isPlaying = true, isBuffering = false, isVideoUnavailable = true, isAudioOnlyMode = true) }
                return
            }

            // 2. Official music video blocked from embedding (101/150): fallback to audio / topic release
            scope.launch {
                val candidateAudio = try {
                    val query = "${current.artist} ${current.title} audio"
                    val results = YouTubeProvider.searchVideosPublic(query, "YOUTUBE_MUSIC")
                    results.firstOrNull {
                        it.youtubeVideoId.isNotEmpty() &&
                        it.youtubeVideoId != current.youtubeVideoId &&
                        SmartQueueEngine.isTrackPlayable(it)
                    }
                } catch (_: Exception) {
                    null
                }

                if (candidateAudio != null) {
                    val updatedTrack = current.copy(
                        youtubeVideoId = candidateAudio.youtubeVideoId,
                        streamUrl = candidateAudio.streamUrl
                    )
                    _playbackState.update { state ->
                        val updatedQueue = state.queue.map { if (it.id == current.id) updatedTrack else it }
                        state.copy(
                            currentTrack = updatedTrack,
                            queue = updatedQueue,
                            isAudioOnlyMode = true,
                            isVideoUnavailable = true
                        )
                    }
                    playerBridge?.loadVideo(candidateAudio.youtubeVideoId)
                    playerBridge?.playVideo()
                } else if (_playbackState.value.isShuffle) {
                    // Mark as unplayable so shuffle and queue engine never recommend it again
                    SmartQueueEngine.markTrackUnplayable(current.id)
                    if (current.youtubeVideoId.isNotEmpty()) {
                        SmartQueueEngine.markTrackUnplayable(current.youtubeVideoId)
                    }
                    skipNext()
                }
            }
        } else {
            _playbackState.update { it.copy(isBuffering = false, errorMessage = "Playback error ($errorCode)") }
        }
    }

    fun downloadTrack(track: Track) {
        scope.launch {
            _playbackState.update {
                it.copy(
                    isDownloading = true,
                    downloadingTrackId = track.id,
                    downloadProgress = 0.05f
                )
            }
            val result = musicDownloader.downloadTrack(track) { progress ->
                _playbackState.update { it.copy(downloadProgress = progress) }
            }
            if (result.isSuccess) {
                val downloadedFile = result.getOrThrow()
                val updatedTrack = track.copy(
                    localAudioUri = downloadedFile.absolutePath,
                    isDownloaded = true
                )
                _playbackState.update { state ->
                    val updatedQueue = state.queue.map { if (it.id == track.id) updatedTrack else it }
                    val current = if (state.currentTrack?.id == track.id) updatedTrack else state.currentTrack
                    state.copy(
                        currentTrack = current,
                        queue = updatedQueue,
                        isDownloading = false,
                        downloadingTrackId = null,
                        downloadProgress = 1.0f
                    )
                }
            } else {
                _playbackState.update {
                    it.copy(
                        isDownloading = false,
                        downloadingTrackId = null,
                        errorMessage = "Download error: " + (result.exceptionOrNull()?.message ?: "failed")
                    )
                }
            }
        }
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return musicDownloader.isTrackDownloaded(trackId)
    }

    fun getDownloadedFile(trackId: String): java.io.File? {
        return musicDownloader.getDownloadedFile(trackId)
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun startProgressTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var step = 0L
            while (isActive) {
                delay(120)
                step += 1

                val state = _playbackState.value
                if (state.isPlaying && !state.isBuffering) {
                    val bands = audioEngine.computeVisualizerBands(step)
                    val isYouTube = state.currentTrack?.let { it.youtubeVideoId.isNotEmpty() && it.localAudioUri.isEmpty() } ?: false
                    val newPos = if (isYouTube) {
                        if (isTransitioningTrack) {
                            0L
                        } else {
                            val enginePos = audioEngine.getCurrentPositionMs()
                            if (enginePos >= 0L) enginePos else state.positionMs
                        }
                    } else {
                        val enginePos = audioEngine.getCurrentPositionMs()
                        if (enginePos > 0L) enginePos else (state.positionMs + 120L).coerceAtMost(state.durationMs.coerceAtLeast(1000L))
                    }
                    val engineDur = audioEngine.getDurationMs()
                    val dur = if (isYouTube) {
                        if (state.durationMs > 1000L) state.durationMs else engineDur
                    } else {
                        if (engineDur > 1000L) engineDur else state.durationMs
                    }

                    _playbackState.update {
                        it.copy(
                            positionMs = newPos,
                            durationMs = dur,
                            visualizerBands = bands
                        )
                    }

                    if (!isTransitioningTrack && dur > 3000L && newPos >= dur) {
                        onTrackFinished()
                    }
                } else {
                    if (state.visualizerBands.any { it > 0.15f }) {
                        val decayed = FloatArray(16) { i ->
                            (state.visualizerBands[i] * 0.85f).coerceAtLeast(0.1f)
                        }
                        _playbackState.update { it.copy(visualizerBands = decayed) }
                    }
                }
            }
        }
    }
}
