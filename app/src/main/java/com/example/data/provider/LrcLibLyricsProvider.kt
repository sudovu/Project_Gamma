package com.example.data.provider

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class TrackLyrics(
    val trackId: String,
    val title: String,
    val artist: String,
    val lines: List<LyricLine>,
    val plainLyrics: String? = null,
    val source: String = "LrcLib"
)

object LrcLibLyricsProvider {
    private const val BASE_URL = "https://lrclib.net"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val TITLE_CLEANUP_REGEXES = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE)
    )

    private val ARTIST_SEPARATORS = listOf(
        " & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with "
    )

    private val LRC_LINE_REGEX = Regex("""\[(\d{1,2}):(\d{2})\.(\d{2,3})\](.*)""")

    fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in TITLE_CLEANUP_REGEXES) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (sep in ARTIST_SEPARATORS) {
            if (cleaned.contains(sep, ignoreCase = true)) {
                cleaned = cleaned.split(sep, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrcText.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank() && !trimmed.startsWith("[offset:", ignoreCase = true)) {
                val match = LRC_LINE_REGEX.matchEntire(trimmed)
                if (match != null) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fractionStr = match.groupValues[3]
                    val fractionVal = fractionStr.toLongOrNull() ?: 0L
                    val millis = if (fractionStr.length == 2) fractionVal * 10 else fractionVal
                    val totalMs = minutes * 60_000L + seconds * 1_000L + millis
                    val text = match.groupValues[4].trim()
                    if (text.isNotBlank()) {
                        lines.add(LyricLine(timeMs = totalMs, text = text))
                    }
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, TrackLyrics>()
    @Volatile private var diskCacheDir: java.io.File? = null

    fun initCache(cacheDir: java.io.File) {
        diskCacheDir = java.io.File(cacheDir, "lyrics").apply { mkdirs() }
    }

    fun getCachedLyrics(trackId: String): TrackLyrics? {
        memoryCache[trackId]?.let { return it }
        val dir = diskCacheDir ?: return null
        val file = java.io.File(dir, "${trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.json")
        if (file.exists()) {
            try {
                val json = JSONObject(file.readText())
                val lyrics = parseTrackLyricsFromJson(trackId, json)
                if (lyrics != null) {
                    memoryCache[trackId] = lyrics
                    return lyrics
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun saveToCache(trackLyrics: TrackLyrics) {
        memoryCache[trackLyrics.trackId] = trackLyrics
        val dir = diskCacheDir ?: return
        try {
            val safeFileName = "${trackLyrics.trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.json"
            val file = java.io.File(dir, safeFileName)
            val json = JSONObject().apply {
                put("trackId", trackLyrics.trackId)
                put("title", trackLyrics.title)
                put("artist", trackLyrics.artist)
                put("source", trackLyrics.source)
                put("plainLyrics", trackLyrics.plainLyrics ?: "")
                val linesArray = JSONArray()
                trackLyrics.lines.forEach { line ->
                    val lineObj = JSONObject().apply {
                        put("timeMs", line.timeMs)
                        put("text", line.text)
                    }
                    linesArray.put(lineObj)
                }
                put("lines", linesArray)
            }
            file.writeText(json.toString())
        } catch (_: Exception) {}
    }

    private fun parseTrackLyricsFromJson(trackId: String, json: JSONObject): TrackLyrics? {
        val lines = mutableListOf<LyricLine>()
        val linesArray = json.optJSONArray("lines")
        if (linesArray != null) {
            for (i in 0 until linesArray.length()) {
                val obj = linesArray.getJSONObject(i)
                lines.add(LyricLine(timeMs = obj.getLong("timeMs"), text = obj.getString("text")))
            }
        }
        val plain = json.optString("plainLyrics", "").ifBlank { null }
        if (lines.isEmpty() && plain == null) return null
        return TrackLyrics(
            trackId = trackId,
            title = json.optString("title", ""),
            artist = json.optString("artist", ""),
            lines = lines,
            plainLyrics = plain,
            source = json.optString("source", "LrcLib (Cached)")
        )
    }

    suspend fun getLyrics(
        trackId: String,
        title: String,
        artist: String,
        durationMs: Long = 0L
    ): TrackLyrics? = withContext(Dispatchers.IO) {
        // Fast-path: check offline cache (in-memory & disk)
        getCachedLyrics(trackId)?.let { return@withContext it }

        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val durationSec = if (durationMs > 0) (durationMs / 1000).toInt() else 0

        // Strategy 1: /api/get with track_name and artist_name
        val exactLyrics = fetchApiGet(cleanedTitle, cleanedArtist, durationSec)
        if (exactLyrics != null) {
            val result = processLyricsResult(trackId, title, artist, exactLyrics)
            saveToCache(result)
            return@withContext result
        }

        // Strategy 2: /api/search with combined query
        val searchResults = fetchApiSearch("$cleanedArtist $cleanedTitle")
        if (searchResults.isNotEmpty()) {
            val best = findBestMatch(searchResults, cleanedTitle, cleanedArtist, durationSec)
            if (best != null) {
                val result = processLyricsResult(trackId, title, artist, best)
                saveToCache(result)
                return@withContext result
            }
        }

        // Strategy 3: /api/search with just title
        if (cleanedTitle.isNotBlank()) {
            val titleResults = fetchApiSearch(cleanedTitle)
            if (titleResults.isNotEmpty()) {
                val best = findBestMatch(titleResults, cleanedTitle, cleanedArtist, durationSec)
                if (best != null) {
                    val result = processLyricsResult(trackId, title, artist, best)
                    saveToCache(result)
                    return@withContext result
                }
            }
        }

        // Strategy 4: Fallback to original raw title if different
        if (cleanedTitle != title.trim()) {
            val rawResults = fetchApiSearch(title.trim())
            val best = rawResults.firstOrNull { it.has("syncedLyrics") || it.has("plainLyrics") }
            if (best != null) {
                val result = processLyricsResult(trackId, title, artist, best)
                saveToCache(result)
                return@withContext result
            }
        }

        null
    }

    private fun processLyricsResult(
        trackId: String,
        title: String,
        artist: String,
        json: JSONObject
    ): TrackLyrics {
        val synced = json.optString("syncedLyrics", "").trim()
        val plain = json.optString("plainLyrics", "").trim()
        val lines = if (synced.isNotBlank()) parseLrc(synced) else emptyList()

        return TrackLyrics(
            trackId = trackId,
            title = json.optString("trackName", title),
            artist = json.optString("artistName", artist),
            lines = lines,
            plainLyrics = plain.ifBlank { null },
            source = "LrcLib"
        )
    }

    private fun findBestMatch(
        results: List<JSONObject>,
        targetTitle: String,
        targetArtist: String,
        durationSec: Int
    ): JSONObject? {
        val withLyrics = results.filter {
            it.optString("syncedLyrics").isNotBlank() || it.optString("plainLyrics").isNotBlank()
        }
        if (withLyrics.isEmpty()) return null

        // Prefer one with synced lyrics and matching duration if available
        if (durationSec > 0) {
            val durationMatch = withLyrics.firstOrNull { obj ->
                val d = obj.optInt("duration", 0)
                d > 0 && kotlin.math.abs(d - durationSec) <= 5 && obj.optString("syncedLyrics").isNotBlank()
            }
            if (durationMatch != null) return durationMatch
        }

        // Prefer one with synced lyrics
        val syncedMatch = withLyrics.firstOrNull { it.optString("syncedLyrics").isNotBlank() }
        if (syncedMatch != null) return syncedMatch

        return withLyrics.firstOrNull()
    }

    private fun fetchApiGet(title: String, artist: String, durationSec: Int): JSONObject? {
        return try {
            val urlBuilder = Uri.parse("$BASE_URL/api/get").buildUpon()
                .appendQueryParameter("track_name", title)
                .appendQueryParameter("artist_name", artist)

            if (durationSec > 0) {
                urlBuilder.appendQueryParameter("duration", durationSec.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build().toString())
                .header("User-Agent", "GAMA-Music/2.2.1 (contact@gautambhuwan.com.np)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    JSONObject(body)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchApiSearch(query: String): List<JSONObject> {
        return try {
            val url = Uri.parse("$BASE_URL/api/search").buildUpon()
                .appendQueryParameter("q", query)
                .build()
                .toString()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GAMA-Music/2.2.1 (contact@gautambhuwan.com.np)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return emptyList()
                    val jsonArray = JSONArray(body)
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until jsonArray.length()) {
                        list.add(jsonArray.getJSONObject(i))
                    }
                    list
                } else {
                    emptyList()
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
