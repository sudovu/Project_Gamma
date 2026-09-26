package com.example.data.provider

import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class TrackDetailsInfo(
    val title: String,
    val artist: String,
    val description: String? = null,
    val songwriters: String? = null,
    val producers: String? = null,
    val releaseYear: String? = null,
    val album: String? = null,
    val artistBio: String? = null,
    val viewCount: String? = null,
    val likeCount: String? = null,
    val frequencyHz: Int = 432,
    val genre: String = "Music",
    val youtubeVideoId: String = ""
)

object TrackInfoProvider {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private val cache = ConcurrentHashMap<String, TrackDetailsInfo>()

    suspend fun resolveTrackDetails(track: Track): TrackDetailsInfo = withContext(Dispatchers.IO) {
        cache[track.id]?.let { return@withContext it }

        var description: String? = null
        var songwriters: String? = null
        var producers: String? = null
        var releaseYear: String? = null
        var album: String? = track.albumTitle.ifBlank { null }
        var artistBio: String? = null
        var viewCount: String? = null
        var likeCount: String? = null

        // 1. Query Wikipedia for authentic encyclopedic song story, background, and songwriters
        try {
            val query = "${LrcLibLyricsProvider.cleanTitle(track.title)} ${LrcLibLyricsProvider.cleanArtist(track.artist)} song"
            val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
                    URLEncoder.encode(query, "UTF-8") + "&format=json&utf8="
            val searchReq = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "GAMA-Player/2.4 (info@gautambhuwan.com.np)")
                .build()

            val searchResp = client.newCall(searchReq).execute()
            if (searchResp.isSuccessful) {
                val bodyStr = searchResp.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val searchArr = json.optJSONObject("query")?.optJSONArray("search")
                if (searchArr != null && searchArr.length() > 0) {
                    val pageTitle = searchArr.getJSONObject(0).optString("title")
                    if (pageTitle.isNotBlank()) {
                        val summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                                URLEncoder.encode(pageTitle, "UTF-8")
                        val summaryReq = Request.Builder()
                            .url(summaryUrl)
                            .header("User-Agent", "GAMA-Player/2.4 (info@gautambhuwan.com.np)")
                            .build()
                        val summaryResp = client.newCall(summaryReq).execute()
                        if (summaryResp.isSuccessful) {
                            val sumJson = JSONObject(summaryResp.body?.string().orEmpty())
                            val extract = sumJson.optString("extract").trim()
                            if (extract.isNotBlank() && !extract.contains("may refer to", ignoreCase = true)) {
                                description = extract

                                // Extract songwriters if mentioned in text (e.g., "written by X, Y and Z")
                                val writerRegex = Regex("""(?:written|composed)\s+by\s+([^.]+?)(?:\.\s|\s+along|\s+with|\s+and\s+produced)""", RegexOption.IGNORE_CASE)
                                val writerMatch = writerRegex.find(extract)
                                if (writerMatch != null) {
                                    val writersFound = writerMatch.groupValues[1].trim()
                                    if (writersFound.length in 3..120) {
                                        songwriters = writersFound
                                    }
                                }

                                // Extract producer if mentioned
                                val prodRegex = Regex("""produced\s+by\s+([^.]+?)(?:\.|\s+and)""", RegexOption.IGNORE_CASE)
                                val prodMatch = prodRegex.find(extract)
                                if (prodMatch != null) {
                                    val prodFound = prodMatch.groupValues[1].trim()
                                    if (prodFound.length in 3..80) {
                                        producers = prodFound
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Query YouTube InnerTube for official record label description & metadata credits
        if (track.youtubeVideoId.isNotBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB")
                            put("clientVersion", "2.20231201.00.00")
                            put("hl", "en")
                            put("gl", "US")
                        })
                    })
                    put("videoId", track.youtubeVideoId)
                }

                val reqBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val ytReq = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/next")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .post(reqBody)
                    .build()

                val ytResp = client.newCall(ytReq).execute()
                if (ytResp.isSuccessful) {
                    val ytJson = JSONObject(ytResp.body?.string().orEmpty())
                    val results = ytJson.optJSONObject("contents")
                        ?.optJSONObject("twoColumnWatchNextResults")
                        ?.optJSONObject("results")
                        ?.optJSONObject("results")
                        ?.optJSONArray("contents")

                    if (results != null) {
                        for (i in 0 until results.length()) {
                            val item = results.optJSONObject(i) ?: continue
                            val secondary = item.optJSONObject("videoSecondaryInfoRenderer")
                            if (secondary != null) {
                                val ytDesc = secondary.optJSONObject("attributedDescription")?.optString("content")
                                    ?: secondary.optJSONObject("description")?.optJSONArray("runs")?.let { runs ->
                                        val sb = StringBuilder()
                                        for (r in 0 until runs.length()) {
                                            sb.append(runs.getJSONObject(r).optString("text"))
                                        }
                                        sb.toString()
                                    }

                                if (!ytDesc.isNullOrBlank()) {
                                    if (description.isNullOrBlank()) {
                                        description = ytDesc.take(450)
                                    }

                                    // Parse structured YouTube Music audio credits
                                    ytDesc.lines().forEach { line ->
                                        val trimmed = line.trim()
                                        when {
                                            trimmed.startsWith("Author, Composer:", ignoreCase = true) ||
                                            trimmed.startsWith("Composer, Lyricist:", ignoreCase = true) ||
                                            trimmed.startsWith("Composer:", ignoreCase = true) ||
                                            trimmed.startsWith("Lyricist:", ignoreCase = true) ||
                                            trimmed.startsWith("Written by:", ignoreCase = true) -> {
                                                val names = trimmed.substringAfter(":").trim()
                                                songwriters = if (songwriters.isNullOrBlank()) names else "$songwriters, $names"
                                            }
                                            trimmed.startsWith("Producer:", ignoreCase = true) ||
                                            trimmed.startsWith("Produced by:", ignoreCase = true) -> {
                                                producers = trimmed.substringAfter(":").trim()
                                            }
                                            trimmed.startsWith("Released on:", ignoreCase = true) -> {
                                                releaseYear = trimmed.substringAfter(":").trim()
                                            }
                                            trimmed.startsWith("℗ ", ignoreCase = true) -> {
                                                if (releaseYear.isNullOrBlank()) {
                                                    val yr = Regex("""\b(19\d\d|20\d\d)\b""").find(trimmed)?.value
                                                    if (yr != null) releaseYear = yr
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val primary = item.optJSONObject("videoPrimaryInfoRenderer")
                            if (primary != null) {
                                val viewStr = primary.optJSONObject("viewCount")
                                    ?.optJSONObject("videoViewCountRenderer")
                                    ?.optJSONObject("viewCount")
                                    ?.optString("simpleText")
                                if (!viewStr.isNullOrBlank()) {
                                    viewCount = viewStr
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Query Artist Biography from Wikipedia if not yet present
        try {
            val cleanArtistName = LrcLibLyricsProvider.cleanArtist(track.artist)
            val artistSearchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
                    URLEncoder.encode("$cleanArtistName musician", "UTF-8") + "&format=json&utf8="
            val aSearchReq = Request.Builder()
                .url(artistSearchUrl)
                .header("User-Agent", "GAMA-Player/2.4 (info@gautambhuwan.com.np)")
                .build()
            val aSearchResp = client.newCall(aSearchReq).execute()
            if (aSearchResp.isSuccessful) {
                val aJson = JSONObject(aSearchResp.body?.string().orEmpty())
                val aArr = aJson.optJSONObject("query")?.optJSONArray("search")
                if (aArr != null && aArr.length() > 0) {
                    val aTitle = aArr.getJSONObject(0).optString("title")
                    val aSumUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + URLEncoder.encode(aTitle, "UTF-8")
                    val aSumResp = client.newCall(Request.Builder().url(aSumUrl).header("User-Agent", "GAMA-Player/2.4 (info@gautambhuwan.com.np)").build()).execute()
                    if (aSumResp.isSuccessful) {
                        val aSumJson = JSONObject(aSumResp.body?.string().orEmpty())
                        val aExtract = aSumJson.optString("extract").trim()
                        if (aExtract.isNotBlank() && !aExtract.contains("may refer to", ignoreCase = true)) {
                            artistBio = aExtract
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val result = TrackDetailsInfo(
            title = track.title,
            artist = track.artist,
            description = description?.ifBlank { null },
            songwriters = songwriters?.ifBlank { null },
            producers = producers?.ifBlank { null },
            releaseYear = releaseYear?.ifBlank { null },
            album = album,
            artistBio = artistBio?.ifBlank { null },
            viewCount = viewCount,
            likeCount = likeCount,
            frequencyHz = track.frequencyHz,
            genre = track.genre.ifBlank { "Music" },
            youtubeVideoId = track.youtubeVideoId
        )

        cache[track.id] = result
        result
    }
}
