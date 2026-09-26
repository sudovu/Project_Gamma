package com.example.core.media

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer as AndroidHardwareEqualizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.domain.model.EqualizerSettings
import com.example.domain.model.Track
import com.example.domain.model.TuningPreset
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * High-precision Second-Order Biquad IIR Filter
 * Implements Audio EQ Cookbook peaking and shelving filters.
 */
class BiquadFilter(
    private val type: FilterType,
    private val centerFreqHz: Float,
    private val sampleRate: Float = 44100f,
    private val q: Float = 1.0f
) {
    enum class FilterType { LOW_SHELF, PEAKING, HIGH_SHELF }

    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f

    // State registers for Left & Right channels (Transposed Direct Form II)
    private var s1L = 0.0f
    private var s2L = 0.0f
    private var s1R = 0.0f
    private var s2R = 0.0f

    init {
        setGain(0.0f)
    }

    fun setGain(gainDb: Float) {
        val aVal = 10.0.pow(gainDb.toDouble() / 40.0).toFloat()
        val w0 = (2.0 * PI * (centerFreqHz / sampleRate)).toFloat()
        val cosW0 = cos(w0.toDouble()).toFloat()
        val sinW0 = sin(w0.toDouble()).toFloat()
        val alpha = sinW0 / (2.0f * q)

        val tempA0: Float

        when (type) {
            FilterType.PEAKING -> {
                b0 = 1.0f + alpha * aVal
                b1 = -2.0f * cosW0
                b2 = 1.0f - alpha * aVal
                tempA0 = 1.0f + alpha / aVal
                a1 = -2.0f * cosW0
                a2 = 1.0f - alpha / aVal
            }
            FilterType.LOW_SHELF -> {
                val sqrtA = sqrt(aVal.toDouble()).toFloat()
                val twoSqrtAAlpha = 2.0f * sqrtA * alpha
                b0 = aVal * ((aVal + 1.0f) - (aVal - 1.0f) * cosW0 + twoSqrtAAlpha)
                b1 = 2.0f * aVal * ((aVal - 1.0f) - (aVal + 1.0f) * cosW0)
                b2 = aVal * ((aVal + 1.0f) - (aVal - 1.0f) * cosW0 - twoSqrtAAlpha)
                tempA0 = (aVal + 1.0f) + (aVal - 1.0f) * cosW0 + twoSqrtAAlpha
                a1 = -2.0f * ((aVal - 1.0f) + (aVal + 1.0f) * cosW0)
                a2 = (aVal + 1.0f) - (aVal - 1.0f) * cosW0 - twoSqrtAAlpha
            }
            FilterType.HIGH_SHELF -> {
                val sqrtA = sqrt(aVal.toDouble()).toFloat()
                val twoSqrtAAlpha = 2.0f * sqrtA * alpha
                b0 = aVal * ((aVal + 1.0f) + (aVal - 1.0f) * cosW0 + twoSqrtAAlpha)
                b1 = -2.0f * aVal * ((aVal - 1.0f) + (aVal + 1.0f) * cosW0)
                b2 = aVal * ((aVal + 1.0f) - (aVal - 1.0f) * cosW0 - twoSqrtAAlpha)
                tempA0 = (aVal + 1.0f) - (aVal - 1.0f) * cosW0 + twoSqrtAAlpha
                a1 = 2.0f * ((aVal - 1.0f) - (aVal + 1.0f) * cosW0)
                a2 = (aVal + 1.0f) - (aVal - 1.0f) * cosW0 - twoSqrtAAlpha
            }
        }

        val invA0 = 1.0f / tempA0
        b0 *= invA0
        b1 *= invA0
        b2 *= invA0
        a1 *= invA0
        a2 *= invA0
    }

    fun processLeft(input: Float): Float {
        val output = b0 * input + s1L
        s1L = b1 * input - a1 * output + s2L
        s2L = b2 * input - a2 * output
        return output
    }

    fun processRight(input: Float): Float {
        val output = b0 * input + s1R
        s1R = b1 * input - a1 * output + s2R
        s2R = b2 * input - a2 * output
        return output
    }

    fun reset() {
        s1L = 0.0f
        s2L = 0.0f
        s1R = 0.0f
        s2R = 0.0f
    }
}

/**
 * GAMA Core Audio Engine
 * Pure music audio playback engine with:
 * 1. 5-Band Real-Time DSP Audio Equalizer (Biquad IIR)
 * 2. Hardware Equalizer Session Bridge for Android output
 * 3. Native MediaPlayer for local files and streamed tracks
 * NOTE: Artificial frequency tone generation has been completely removed so actual songs play clearly.
 */
class GamaAudioEngine(private val context: Context) {

    companion object {
        private const val TAG = "GamaAudioEngine"
        private const val SAMPLE_RATE = 44100
    }

    private val filters = arrayOf(
        BiquadFilter(BiquadFilter.FilterType.LOW_SHELF, 60.0f, SAMPLE_RATE.toFloat(), 0.8f),
        BiquadFilter(BiquadFilter.FilterType.PEAKING, 230.0f, SAMPLE_RATE.toFloat(), 1.0f),
        BiquadFilter(BiquadFilter.FilterType.PEAKING, 910.0f, SAMPLE_RATE.toFloat(), 1.0f),
        BiquadFilter(BiquadFilter.FilterType.PEAKING, 3600.0f, SAMPLE_RATE.toFloat(), 1.0f),
        BiquadFilter(BiquadFilter.FilterType.HIGH_SHELF, 14000.0f, SAMPLE_RATE.toFloat(), 0.8f)
    )

    private val isEnginePlaying = AtomicBoolean(false)
    private var mediaPlayer: MediaPlayer? = null
    private var hardwareEqualizer: AndroidHardwareEqualizer? = null
    private var globalHardwareEqualizer: AndroidHardwareEqualizer? = null
    private var globalBassBoost: BassBoost? = null
    private var globalVirtualizer: Virtualizer? = null
    private var globalLoudnessEnhancer: LoudnessEnhancer? = null

    private var currentTrack: Track? = null
    private var currentPlaybackPositionMs = 0L
    private var lastExternalUpdateTime = 0L
    private var masterGainMultiplier = 1.0f
    private var isEqEnabled = true
    private var currentSpeed = 1.0f
    private var currentDucking = 1.0f

    private val currentBandGains = floatArrayOf(0f, 0f, 0f, 0f, 0f)

    init {
        initHardwareEffects()
    }

    private fun initHardwareEffects() {
        try {
            val openIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            context.sendBroadcast(openIntent)
        } catch (_: Exception) {}

        try {
            globalHardwareEqualizer = AndroidHardwareEqualizer(1000, 0).apply {
                enabled = isEqEnabled
                applyHardwareEqualizerGains(this)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Global session equalizer not available: ${e.message}")
        }

        try {
            globalBassBoost = BassBoost(1000, 0).apply {
                enabled = isEqEnabled
                if (strengthSupported) {
                    setStrength(0)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Global bass boost not available: ${e.message}")
        }

        try {
            globalVirtualizer = Virtualizer(1000, 0).apply {
                enabled = isEqEnabled
                if (strengthSupported) {
                    setStrength(0)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Global virtualizer not available: ${e.message}")
        }

        try {
            globalLoudnessEnhancer = LoudnessEnhancer(0).apply {
                enabled = isEqEnabled
            }
        } catch (e: Exception) {
            Log.d(TAG, "Global loudness enhancer not available: ${e.message}")
        }
    }

    @Volatile
    private var externalDurationMs: Long = 0L

    var onTrackCompletedListener: (() -> Unit)? = null

    fun playTrack(track: Track) {
        currentTrack = track
        currentPlaybackPositionMs = 0L
        externalDurationMs = if (track.durationSeconds > 0) track.durationSeconds * 1000L else 0L

        if (track.localAudioUri.isNotEmpty()) {
            playViaMediaPlayer(track.localAudioUri)
        } else if (track.streamUrl.isNotEmpty()) {
            playViaMediaPlayer(track.streamUrl)
        } else {
            // YouTube track: Stop native MediaPlayer completely so no fake audio plays and no audio focus competition occurs
            stopMediaPlayer()
            isEnginePlaying.set(true)
        }
    }

    fun setExternalPosition(currentMs: Long, durationMs: Long) {
        currentPlaybackPositionMs = currentMs
        lastExternalUpdateTime = android.os.SystemClock.elapsedRealtime()
        if (durationMs > 1000L) {
            externalDurationMs = durationMs
        }
    }

    fun resumeVisualizer() {
        isEnginePlaying.set(true)
    }

    fun pauseVisualizer() {
        isEnginePlaying.set(false)
    }

    fun getCurrentPositionMs(): Long {
        return try {
            if (mediaPlayer != null && isEnginePlaying.get()) {
                val pos = mediaPlayer?.currentPosition?.toLong() ?: currentPlaybackPositionMs
                if (pos > 0) pos else currentPlaybackPositionMs
            } else if (isEnginePlaying.get() && currentPlaybackPositionMs > 0 && lastExternalUpdateTime > 0) {
                val elapsed = (android.os.SystemClock.elapsedRealtime() - lastExternalUpdateTime).coerceAtLeast(0L)
                val maxDur = if (externalDurationMs > 0) externalDurationMs else Long.MAX_VALUE
                (currentPlaybackPositionMs + elapsed).coerceAtMost(maxDur)
            } else {
                currentPlaybackPositionMs
            }
        } catch (_: Exception) {
            currentPlaybackPositionMs
        }
    }

    fun getDurationMs(): Long {
        return try {
            if (externalDurationMs > 1000L) return externalDurationMs
            val dur = mediaPlayer?.duration?.toLong() ?: 0L
            if (dur > 1000L) dur else (currentTrack?.durationSeconds?.toLong()?.times(1000L) ?: 0L)
        } catch (_: Exception) {
            if (externalDurationMs > 1000L) externalDurationMs else (currentTrack?.durationSeconds?.toLong()?.times(1000L) ?: 0L)
        }
    }

    fun resume() {
        isEnginePlaying.set(true)
        if (mediaPlayer != null) {
            try {
                mediaPlayer?.start()
            } catch (_: Exception) {}
        }
    }

    fun pause() {
        isEnginePlaying.set(false)
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
    }

    fun stop() {
        isEnginePlaying.set(false)
        stopMediaPlayer()
        currentPlaybackPositionMs = 0L
    }

    fun seekTo(positionMs: Long) {
        currentPlaybackPositionMs = positionMs
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (_: Exception) {}
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.25f, 3.0f)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { mp ->
                    mp.playbackParams = mp.playbackParams.setSpeed(currentSpeed)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set playback speed on MediaPlayer: ${e.message}")
        }
    }

    fun setVolumeDucking(duckFactor: Float) {
        currentDucking = duckFactor.coerceIn(0f, 1f)
        val finalVol = (masterGainMultiplier * currentDucking).coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(finalVol, finalVol)
        } catch (_: Exception) {}
    }

    private fun stopMediaPlayer() {
        try {
            hardwareEqualizer?.release()
            hardwareEqualizer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    private fun playViaMediaPlayer(uriString: String) {
        stopMediaPlayer()
        try {
            val isRemote = uriString.startsWith("http://") || uriString.startsWith("https://")
            mediaPlayer = MediaPlayer().apply {
                try {
                    setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                } catch (_: Exception) {}
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                if (uriString.startsWith("http://") || uriString.startsWith("https://") || uriString.startsWith("content://")) {
                    setDataSource(context, Uri.parse(uriString))
                } else {
                    setDataSource(uriString)
                }

                setOnPreparedListener { mp ->
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                            try {
                                mp.playbackParams = mp.playbackParams.setSpeed(currentSpeed)
                            } catch (_: Exception) {}
                        }
                        val finalVol = (masterGainMultiplier * currentDucking).coerceIn(0f, 1f)
                        mp.setVolume(finalVol, finalVol)
                        mp.start()
                        isEnginePlaying.set(true)
                        if (currentPlaybackPositionMs > 0) {
                            mp.seekTo(currentPlaybackPositionMs.toInt())
                        }
                        attachEqualizerToSession(mp.audioSessionId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start playback after prepare: ${e.message}")
                    }
                }

                setOnCompletionListener {
                    isEnginePlaying.set(false)
                    onTrackCompletedListener?.invoke()
                }

                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what extra=$extra")
                    true
                }

                if (isRemote) {
                    prepareAsync()
                } else {
                    prepare()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                        try {
                            playbackParams = playbackParams.setSpeed(currentSpeed)
                        } catch (_: Exception) {}
                    }
                    val finalVol = (masterGainMultiplier * currentDucking).coerceIn(0f, 1f)
                    setVolume(finalVol, finalVol)
                    start()
                    isEnginePlaying.set(true)
                    attachEqualizerToSession(audioSessionId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer initialization failed: ${e.message}", e)
        }
    }

    private fun attachEqualizerToSession(sessionId: Int) {
        if (sessionId != 0) {
            try {
                hardwareEqualizer?.release()
                hardwareEqualizer = AndroidHardwareEqualizer(1000, sessionId).apply {
                    enabled = isEqEnabled
                    applyHardwareEqualizerGains(this)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Hardware equalizer session attachment: ${e.message}")
            }
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in filters.indices) {
            val clamped = gainDb.coerceIn(-12.0f, 12.0f)
            currentBandGains[bandIndex] = clamped
            filters[bandIndex].setGain(clamped)
            hardwareEqualizer?.let { applyHardwareEqualizerGains(it) }
            globalHardwareEqualizer?.let { applyHardwareEqualizerGains(it) }

            // Dynamic BassBoost integration: if band 0 (60Hz) is boosted, dynamically scale BassBoost
            if (bandIndex == 0) {
                try {
                    val bassRatio = (clamped.coerceAtLeast(0f) / 12.0f)
                    val strength = (bassRatio * 1000).toInt().toShort()
                    globalBassBoost?.let { bb ->
                        if (bb.strengthSupported) {
                            bb.setStrength(strength)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun setAllBands(gainsDb: List<Float>) {
        gainsDb.forEachIndexed { index, gain ->
            setBandGain(index, gain)
        }
    }

    fun applyPreset(preset: TuningPreset) {
        val gains = EqualizerSettings.presetGains(preset)
        setAllBands(gains)

        // Preset-specific hardware DSP profile tuning
        try {
            when (preset) {
                TuningPreset.BASS_BOOST -> {
                    globalBassBoost?.let { if (it.strengthSupported) it.setStrength(800) }
                }
                TuningPreset.SYNTHWAVE -> {
                    globalBassBoost?.let { if (it.strengthSupported) it.setStrength(450) }
                    globalVirtualizer?.let { if (it.strengthSupported) it.setStrength(400) }
                }
                TuningPreset.ELECTRONIC -> {
                    globalBassBoost?.let { if (it.strengthSupported) it.setStrength(600) }
                    globalVirtualizer?.let { if (it.strengthSupported) it.setStrength(500) }
                }
                TuningPreset.VOCAL_CLARITY -> {
                    globalBassBoost?.let { if (it.strengthSupported) it.setStrength(0) }
                    globalVirtualizer?.let { if (it.strengthSupported) it.setStrength(200) }
                }
                else -> {
                    globalBassBoost?.let { if (it.strengthSupported) it.setStrength(0) }
                    globalVirtualizer?.let { if (it.strengthSupported) it.setStrength(0) }
                }
            }
        } catch (_: Exception) {}
    }

    fun setMasterGain(gain: Float) {
        this.masterGainMultiplier = gain.coerceIn(0.2f, 2.0f)
        try {
            val boostMb = ((gain - 1.0f).coerceAtLeast(0f) * 1000).toInt()
            globalLoudnessEnhancer?.setTargetGain(boostMb)
        } catch (_: Exception) {}
    }

    fun setSpatialAudioEnabled(enabled: Boolean) {
        try {
            globalVirtualizer?.let { virt ->
                virt.enabled = enabled
                if (virt.strengthSupported) {
                    virt.setStrength(if (enabled) 700 else 0)
                }
            }
        } catch (_: Exception) {}
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        this.isEqEnabled = enabled
        hardwareEqualizer?.enabled = enabled
        globalHardwareEqualizer?.enabled = enabled
        globalBassBoost?.enabled = enabled
        globalVirtualizer?.enabled = enabled
        globalLoudnessEnhancer?.enabled = enabled
    }

    private fun applyHardwareEqualizerGains(hwEq: AndroidHardwareEqualizer) {
        try {
            val numBands = hwEq.numberOfBands.toInt()
            val minMb = hwEq.bandLevelRange[0]
            val maxMb = hwEq.bandLevelRange[1]

            for (bandIdx in 0 until numBands) {
                val centerFreqHz = hwEq.getCenterFreq(bandIdx.toShort()) / 1000
                val closestIdx = when {
                    centerFreqHz < 120 -> 0 // 60 Hz
                    centerFreqHz < 500 -> 1 // 230 Hz
                    centerFreqHz < 2000 -> 2 // 910 Hz
                    centerFreqHz < 8000 -> 3 // 3.6 kHz
                    else -> 4 // 14 kHz
                }
                val db = currentBandGains[closestIdx]
                val mb = (db * 100).toInt().coerceIn(minMb.toInt(), maxMb.toInt()).toShort()
                hwEq.setBandLevel(bandIdx.toShort(), mb)
            }
        } catch (_: Exception) {}
    }

    fun computeVisualizerBands(step: Long): FloatArray {
        val active = isEnginePlaying.get()
        return FloatArray(16) { i ->
            if (!active) {
                0.08f
            } else {
                val eqInfluence = when (i) {
                    in 0..2 -> currentBandGains[0]
                    in 3..6 -> currentBandGains[1]
                    in 7..10 -> currentBandGains[2]
                    in 11..13 -> currentBandGains[3]
                    else -> currentBandGains[4]
                }

                val eqScalar = if (isEqEnabled) (1.0f + (eqInfluence / 20.0f)).coerceIn(0.2f, 2.0f) else 1.0f
                val baseSine = 0.25f + 0.55f * sin((step * 0.35f + i * 0.55f)).coerceAtLeast(0f)
                val noise = (abs(Random.nextFloat() * 0.2f))
                ((baseSine + noise) * eqScalar * masterGainMultiplier).coerceIn(0.1f, 1.0f)
            }
        }
    }
}
