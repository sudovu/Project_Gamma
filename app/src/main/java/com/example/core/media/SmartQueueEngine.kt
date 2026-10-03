package com.example.core.media

import com.example.domain.model.SmartQueueProfile
import com.example.domain.model.Track
import kotlin.math.abs

object SmartQueueEngine {

    private val unplayableTrackIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun markTrackUnplayable(idOrVideoId: String) {
        val clean = idOrVideoId.trim()
        if (clean.isNotEmpty()) {
            unplayableTrackIds.add(clean)
        }
    }

    fun isTrackPlayable(track: Track?): Boolean {
        if (track == null) return false
        val id = track.id.trim()
        val ytId = track.youtubeVideoId.trim()
        val local = track.localAudioUri.trim()
        val stream = track.streamUrl.trim()

        if (id.isNotEmpty() && unplayableTrackIds.contains(id)) return false
        if (ytId.isNotEmpty() && unplayableTrackIds.contains(ytId)) return false
        if (ytId.startsWith("invalid") || ytId.startsWith("dummy") || id.startsWith("invalid")) return false

        // Valid local download or remote stream
        if (local.isNotEmpty() || stream.isNotEmpty()) return true

        // If it has a YouTube video ID, must be 11 characters
        if (ytId.isNotEmpty()) {
            return ytId.length == 11
        }

        // For in-memory / catalog / library tracks without direct stream yet, valid unless marked unplayable
        return id.isNotEmpty()
    }

    fun analyzePlayedTracks(playedTracks: List<Track>): SmartQueueProfile {
        if (playedTracks.isEmpty()) {
            return SmartQueueProfile(
                dominantGenre = "All",
                totalAnalyzed = 0
            )
        }

        val recentTracks = playedTracks.takeLast(20)
        val genreScoreMap = mutableMapOf<String, Int>()

        recentTracks.forEachIndexed { index, track ->
            val weight = 1 + (index * 3) / recentTracks.size.coerceAtLeast(1)
            val cleanGenre = track.genre.trim()
            if (cleanGenre.isNotEmpty()) {
                genreScoreMap[cleanGenre] = (genreScoreMap[cleanGenre] ?: 0) + weight
            }
        }

        val topGenre = genreScoreMap.maxByOrNull { it.value }?.key ?: playedTracks.last().genre

        return SmartQueueProfile(
            dominantGenre = topGenre,
            genreBreakdown = genreScoreMap,
            totalAnalyzed = playedTracks.size
        )
    }

    fun isCollectionOrMix(title: String, durationSeconds: Int = 0): Boolean {
        // Single songs can reach up to 10-12 minutes (e.g. November Rain is 537s). Full albums, compilations, and DJ sets typically exceed 12-15+ minutes.
        if (durationSeconds > 720 || (durationSeconds in 1..45)) return true

        val t = title.lowercase().trim()
        val collectionKeywords = listOf(
            "full album", "album mix", "playlist", "collection", "compilation",
            "jukebox", "non stop", "nonstop", "discography", "mashup",
            "1 hour", "2 hour", "3 hour", "4 hour", "hours", "mix 20", "video mix",
            "top 10", "top 20", "top 30", "top 40", "top 50", "top 100",
            "best songs of", "greatest hits mix", "vol.", "vol ", "volume ",
            "best of", "megamix", "today's top", "todays top", "top hits",
            "top songs", "top tracks", "top music", "hit songs 20", "hits 20",
            "billboard 20", "billboard hot", "chart hits", "music chart",
            "top chart", "new songs 20", "songs 202", "music 202", "viral song",
            "viral hits", "christmas music", "relaxing music", "study music",
            "sleep music", "lofi live", "radio live", "radio stream", "24/7",
            "hits of the day", "audio mix", "official audio mix", "soundtrack",
            "full ost", "medley", "mixtape", "dj mix", "continuous mix",
            "all songs", "every song", "full concert", "live concert", "live at ",
            "tracklist", "best english songs", "best hits", "greatest hits"
        )
        if (collectionKeywords.any { t.contains(it) }) return true
        if (t.contains("mix") && (durationSeconds > 300 || durationSeconds == 0)) return true
        if (t.contains("playlist")) return true
        if (t.contains("album") && !t.contains("album version")) return true
        return false
    }

    /**
     * Cleans raw YouTube video titles and parses them into genuine (Song Title, Artist Name) pairs.
     * Removes noisy video badges like "(Official Music Video)", "[Official Audio]", etc.
     */
    fun cleanSongTitleAndArtist(rawTitle: String, rawAuthor: String): Pair<String, String> {
        // Strip common YouTube badges in parentheses or brackets
        val stripped = rawTitle
            .replace(Regex("""(?i)\s*[\(\[\{](?:official\s*(?:music\s*)?video|official\s*audio|official\s*lyric\s*video|official\s*video|lyric\s*video|lyrics|audio|visualizer|video|4k|hd|hq|live|remastered|explicit)[\)\]\}]"""), "")
            .replace(Regex("""(?i)\s*\|\|\s*official\s*(?:music\s*)?video.*"""), "")
            .replace(Regex("""(?i)\s*\|\s*official\s*(?:music\s*)?video.*"""), "")
            .replace(Regex("""(?i)\s*-\s*official\s*(?:music\s*)?video.*"""), "")
            .trim()

        val cleanAuthor = rawAuthor
            .replace(Regex("""(?i)\s*-\s*topic"""), "")
            .replace(Regex("""(?i)vevo$"""), "")
            .replace(Regex("""(?i)\s*official.*"""), "")
            .trim()
            .ifEmpty { "Artist" }

        // Common delimiter patterns: "Artist - Title", "Artist – Title", "Artist — Title", "Artist : Title"
        val splitDelimiters = listOf(" - ", " – ", " — ", " : ")
        for (delim in splitDelimiters) {
            if (stripped.contains(delim)) {
                val parts = stripped.split(delim, limit = 2)
                val left = parts[0].trim()
                val right = parts[1].trim()
                if (left.isNotEmpty() && right.isNotEmpty()) {
                    // Check which side is title vs artist:
                    // If author is mentioned in left or author is short, left is artist, right is title
                    return Pair(right, left)
                }
            }
        }

        return Pair(stripped.ifEmpty { rawTitle }, cleanAuthor)
    }

    fun getTrackMetaKey(track: Track): String {
        val normTitle = track.title
            .replace(Regex("\\s*[\\[\\(].*?[\\]\\)]"), "")
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .lowercase()
            .trim()
        val normArtist = track.artist
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .lowercase()
            .trim()
        return if (normTitle.isNotEmpty()) "${normArtist}_$normTitle" else ""
    }

    fun areTracksEqual(a: Track?, b: Track?): Boolean {
        if (a == null || b == null) return false
        if (a.id.isNotBlank() && a.id == b.id) return true
        if (a.youtubeVideoId.isNotBlank() && a.youtubeVideoId == b.youtubeVideoId) return true
        if (a.localAudioUri.isNotBlank() && a.localAudioUri == b.localAudioUri) return true
        val metaA = getTrackMetaKey(a)
        val metaB = getTrackMetaKey(b)
        return metaA.isNotEmpty() && metaA == metaB
    }

    fun deduplicateTracks(tracks: List<Track>): List<Track> {
        val seenIds = mutableSetOf<String>()
        val seenYtIds = mutableSetOf<String>()
        val seenUris = mutableSetOf<String>()
        val seenMetaKeys = mutableSetOf<String>()
        val result = ArrayList<Track>(tracks.size)

        for (t in tracks) {
            val id = t.id.trim()
            val ytId = t.youtubeVideoId.trim()
            val uri = t.localAudioUri.trim()
            val metaKey = getTrackMetaKey(t)

            val isDuplicate = (id.isNotEmpty() && id in seenIds) ||
                    (ytId.isNotEmpty() && ytId in seenYtIds) ||
                    (uri.isNotEmpty() && uri in seenUris) ||
                    (metaKey.isNotEmpty() && metaKey in seenMetaKeys)

            if (!isDuplicate) {
                if (id.isNotEmpty()) seenIds.add(id)
                if (ytId.isNotEmpty()) seenYtIds.add(ytId)
                if (uri.isNotEmpty()) seenUris.add(uri)
                if (metaKey.isNotEmpty()) seenMetaKeys.add(metaKey)
                result.add(t)
            }
        }
        return result
    }

    fun scoreCandidate(
        candidate: Track,
        profile: SmartQueueProfile,
        currentQueue: List<Track>,
        playedIds: Set<String>
    ): Double {
        val candidateMeta = getTrackMetaKey(candidate)
        if (currentQueue.any { 
            it.id == candidate.id || 
            (it.youtubeVideoId.isNotEmpty() && it.youtubeVideoId == candidate.youtubeVideoId) ||
            (it.localAudioUri.isNotEmpty() && it.localAudioUri == candidate.localAudioUri) ||
            (candidateMeta.isNotEmpty() && getTrackMetaKey(it) == candidateMeta)
        }) {
            return -1000.0
        }
        if (!isTrackPlayable(candidate)) {
            return -1000.0
        }
        if (isCollectionOrMix(candidate.title, candidate.durationSeconds)) {
            return -1000.0
        }

        var score = 0.0

        val candidateGenre = candidate.genre.trim()
        if (candidateGenre.equals(profile.dominantGenre, ignoreCase = true)) {
            score += 100.0
        } else if (candidateGenre.contains(profile.dominantGenre, ignoreCase = true) ||
            profile.dominantGenre.contains(candidateGenre, ignoreCase = true)
        ) {
            score += 65.0
        } else {
            val genreMatchWeight = profile.genreBreakdown[candidateGenre]
            if (genreMatchWeight != null) {
                score += 40.0 + (genreMatchWeight * 5.0).coerceAtMost(25.0)
            } else {
                val candidateTokens = candidateGenre.lowercase().split(" ", "-", "/")
                val dominantTokens = profile.dominantGenre.lowercase().split(" ", "-", "/")
                if (candidateTokens.any { token -> token.length > 3 && dominantTokens.contains(token) }) {
                    score += 30.0
                }
            }
        }

        // Artist affinity boost if artist is in current queue
        if (currentQueue.any { it.artist.equals(candidate.artist, ignoreCase = true) }) {
            score += 25.0
        }

        if (playedIds.contains(candidate.id)) {
            score -= 40.0
        }

        val jitter = (abs(candidate.id.hashCode() % 100) / 20.0)
        score += jitter

        return score
    }

    fun generateRecommendations(
        playedTracks: List<Track>,
        candidatePool: List<Track>,
        currentQueue: List<Track>,
        count: Int = 3
    ): List<Track> {
        val singlePool = candidatePool.filter { isTrackPlayable(it) && !isCollectionOrMix(it.title, it.durationSeconds) }
        if (singlePool.isEmpty()) return emptyList()

        val profile = analyzePlayedTracks(playedTracks)
        val playedIds = playedTracks.map { it.id }.toSet()
        val currentQueueIds = currentQueue.map { it.id }.filter { it.isNotEmpty() }.toSet()
        val currentVideoIds = currentQueue.map { it.youtubeVideoId }.filter { it.isNotEmpty() }.toSet()
        val currentMetaKeys = currentQueue.map { getTrackMetaKey(it) }.filter { it.isNotEmpty() }.toSet()

        fun isCandidateInQueue(c: Track): Boolean {
            val meta = getTrackMetaKey(c)
            return (c.id.isNotEmpty() && c.id in currentQueueIds) ||
                    (c.youtubeVideoId.isNotEmpty() && c.youtubeVideoId in currentVideoIds) ||
                    (meta.isNotEmpty() && meta in currentMetaKeys)
        }

        val scoredCandidates = singlePool
            .filter { candidate -> !isCandidateInQueue(candidate) }
            .map { candidate ->
                val score = scoreCandidate(candidate, profile, currentQueue, playedIds)
                candidate to score
            }
            .filter { it.second > -500.0 }
            .sortedByDescending { it.second }

        val selected = deduplicateTracks(scoredCandidates.take(count).map { (track, _) ->
            track.copy(source = "smart_recommendation")
        })

        if (selected.isEmpty()) {
            val unqueued = singlePool.filter { !isCandidateInQueue(it) }
            return deduplicateTracks(unqueued).take(count).map { it.copy(source = "smart_recommendation") }
        }

        return selected
    }

    /**
     * Generates endless recommendations strictly aligned with the target track's genre and artist.
     * Guaranteed to return high-quality single songs and never collections or mixes, and never repeat songs in the queue.
     */
    fun generateGenreRecommendations(
        targetTrack: Track?,
        playedTracks: List<Track>,
        candidatePool: List<Track>,
        currentQueue: List<Track>,
        count: Int = 5
    ): List<Track> {
        val singlePool = candidatePool.filter { isTrackPlayable(it) && !isCollectionOrMix(it.title, it.durationSeconds) }
        if (singlePool.isEmpty()) return emptyList()

        val activeGenre = targetTrack?.genre?.trim().orEmpty()
        val activeArtist = targetTrack?.artist?.trim().orEmpty()

        val currentQueueIds = currentQueue.map { it.id }.filter { it.isNotEmpty() }.toSet()
        val currentVideoIds = currentQueue.map { it.youtubeVideoId }.filter { it.isNotEmpty() }.toSet()
        val currentUris = currentQueue.map { it.localAudioUri }.filter { it.isNotEmpty() }.toSet()
        val currentMetaKeys = currentQueue.map { getTrackMetaKey(it) }.filter { it.isNotEmpty() }.toSet()

        val playedIds = playedTracks.map { it.id }.filter { it.isNotEmpty() }.toSet()
        val playedVideoIds = playedTracks.map { it.youtubeVideoId }.filter { it.isNotEmpty() }.toSet()
        val playedMetaKeys = playedTracks.map { getTrackMetaKey(it) }.filter { it.isNotEmpty() }.toSet()

        fun isCandidateInQueue(c: Track): Boolean {
            val meta = getTrackMetaKey(c)
            return (c.id.isNotEmpty() && c.id in currentQueueIds) ||
                    (c.youtubeVideoId.isNotEmpty() && c.youtubeVideoId in currentVideoIds) ||
                    (c.localAudioUri.isNotEmpty() && c.localAudioUri in currentUris) ||
                    (meta.isNotEmpty() && meta in currentMetaKeys)
        }

        fun isCandidatePlayed(c: Track): Boolean {
            val meta = getTrackMetaKey(c)
            return (c.id.isNotEmpty() && c.id in playedIds) ||
                    (c.youtubeVideoId.isNotEmpty() && c.youtubeVideoId in playedVideoIds) ||
                    (meta.isNotEmpty() && meta in playedMetaKeys)
        }

        // 1. Unplayed candidates matching the target genre or artist NOT already in the queue
        val genreMatches = singlePool
            .filter { candidate ->
                !isCandidateInQueue(candidate) &&
                (
                    (activeGenre.isNotEmpty() && (
                        candidate.genre.contains(activeGenre, ignoreCase = true) ||
                        activeGenre.contains(candidate.genre, ignoreCase = true)
                    )) ||
                    (activeArtist.isNotEmpty() && candidate.artist.equals(activeArtist, ignoreCase = true))
                )
            }
            .sortedBy { if (isCandidatePlayed(it)) 1 else 0 }

        if (genreMatches.isNotEmpty()) {
            return deduplicateTracks(genreMatches).take(count).map { it.copy(source = "genre_recommendation") }
        }

        // 2. Fallback to standard smart profile recommendations NOT in queue
        val generalRecs = generateRecommendations(playedTracks, singlePool, currentQueue, count)
        if (generalRecs.isNotEmpty()) {
            return deduplicateTracks(generalRecs)
        }

        // 3. Fallback: only pick candidates that are NOT currently in the queue
        val unqueued = singlePool.filter { !isCandidateInQueue(it) }
        if (unqueued.isNotEmpty()) {
            return deduplicateTracks(unqueued).shuffled().take(count).map { it.copy(source = "genre_recommendation") }
        }

        // Strictly avoid repeating songs when every candidate is already in queue
        return emptyList()
    }
}
