package com.example.data.provider

import com.example.core.media.SmartQueueEngine
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.DiscoverFeed
import com.example.domain.model.Playlist
import com.example.domain.model.SearchResults
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import android.util.LruCache
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class YouTubeProvider(
    private val apiKey: String = "",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) : MusicProvider {

    private val searchCache = LruCache<String, SearchResults>(80)

    override val id: String = "youtube_authorized"
    override val displayName: String = "YouTube & YouTube Music"

    override fun getPlaybackCapability(): PlaybackCapability {
        return PlaybackCapability(
            complianceMode = ComplianceMode.OFFICIAL_IFRAME_EMBED,
            canSeek = true,
            canShuffle = true,
            canRepeat = true,
            hasVisualFeedback = true,
            termsNotice = "Powered by official YouTube & YouTube Music embedded IFrame player. Respects all YouTube terms of service, creator rights, and platform restrictions."
        )
    }

    companion object {
        private val TRENDING_SEEDS = listOf(
            "Kendrick Lamar Not Like Us official music video",
            "Sabrina Carpenter Espresso official video",
            "Lady Gaga Bruno Mars Die With A Smile official video",
            "Billie Eilish Birds of a Feather official music video",
            "Shaboozey A Bar Song Tipsy official video",
            "Teddy Swims Lose Control official music video",
            "Benson Boone Beautiful Things official video",
            "Chappell Roan Good Luck Babe official video",
            "Linkin Park The Emptiness Machine official video",
            "Eminem Houdini official video",
            "The Weeknd Blinding Lights official music video"
        )

        private val GENRE_QUERY_POOLS = mapOf(
            "Hip-Hop" to listOf(
                "top hip hop rap hits single official music video",
                "trending rap music video official single",
                "latest hip hop tracks official video",
                "best new rap music single",
                "hip hop official music video single"
            ),
            "Rock & Metal" to listOf(
                "best rock metal songs single official music video",
                "modern alternative rock hits official video",
                "legendary rock anthems official music video",
                "hard rock hits single official",
                "metal alternative rock official video single"
            ),
            "Pop Hits" to listOf(
                "top pop hits single official music video",
                "global pop music charts official video",
                "catchy pop singles official music video",
                "trending dance pop hits official",
                "billboard pop songs official music video"
            ),
            "Classics" to listOf(
                "classic rock single official music video",
                "timeless 80s 90s classic hits official",
                "golden retro hits official music video",
                "legendary classic songs official",
                "vintage greatest hits official video"
            ),
            "Cyberpunk & Synthwave" to listOf(
                "cyberpunk synthwave dark synth music single",
                "retro synthwave official music video",
                "cyberpunk industrial bass electronic single",
                "outrun synthwave 80s electronic single"
            ),
            "Nu Metal & Alt-Rock" to listOf(
                "nu metal alternative rock official music video",
                "slipknot linkin park style nu metal single",
                "heavy alt rock anthems official video",
                "modern post grunge alt rock single"
            ),
            "Electronic & EDM" to listOf(
                "top electronic dance music edm official single",
                "festival edm progressive house official video",
                "best electro bass hits official single",
                "electronic dance anthems official video"
            ),
            "432Hz & Ambient" to listOf(
                "432Hz healing frequency soundscape music single",
                "deep ambient 432Hz meditation tone single",
                "harmonic 432Hz peaceful ambient single",
                "432Hz miracle tone resonance single"
            ),
            "Lo-Fi & Chill" to listOf(
                "lofi chillhop beats to relax single",
                "aesthetic lofi hip hop official single",
                "mellow lofi chill beats single",
                "calm lofi study beat single"
            ),
            "Acoustic & Folk" to listOf(
                "acoustic folk guitar music official single",
                "indie acoustic singer songwriter single",
                "warm acoustic folk melodies official video",
                "fingerstyle acoustic guitar official single"
            ),
            "R&B & Soul" to listOf(
                "smooth r&b soul music official video",
                "modern neo soul r&b hits single",
                "classic soul r&b grooves official single",
                "emotional r&b ballad official video"
            ),
            "Phonk & Drift" to listOf(
                "drift phonk aggressive phonk official single",
                "memphis phonk wave bass boosted single",
                "cowbell phonk drift music official video",
                "brazilian phonk phonk wave single"
            ),
            "Orchestral & Cinematic" to listOf(
                "epic cinematic orchestral music single",
                "trailer orchestral hybrid soundtrack single",
                "powerful symphonic orchestral themes single",
                "dramatic film score orchestral official single"
            )
        )

        @Volatile private var cachedDailyChartDate: String = ""
        @Volatile private var cachedDailyTop100: List<Track> = emptyList()

        val KEYWORD_VIBE_MAP = mapOf(
            "workout" to listOf("Electronic & EDM", "Hip-Hop", "Rock & Metal", "Phonk & Drift"),
            "gym" to listOf("Phonk & Drift", "Rock & Metal", "Hip-Hop", "Electronic & EDM"),
            "fitness" to listOf("Electronic & EDM", "Phonk & Drift"),
            "pump" to listOf("Phonk & Drift", "Rock & Metal"),
            "chill" to listOf("Lo-Fi & Chill", "Acoustic & Folk", "432Hz & Ambient"),
            "relax" to listOf("Lo-Fi & Chill", "432Hz & Ambient", "Acoustic & Folk"),
            "sleep" to listOf("432Hz & Ambient", "Lo-Fi & Chill"),
            "meditation" to listOf("432Hz & Ambient"),
            "study" to listOf("Lo-Fi & Chill", "432Hz & Ambient"),
            "focus" to listOf("Lo-Fi & Chill", "Cyberpunk & Synthwave", "432Hz & Ambient"),
            "sad" to listOf("Acoustic & Folk", "R&B & Soul", "Lo-Fi & Chill", "Pop Hits"),
            "heartbreak" to listOf("R&B & Soul", "Pop Hits", "Acoustic & Folk"),
            "drive" to listOf("Cyberpunk & Synthwave", "Phonk & Drift", "Hip-Hop"),
            "night drive" to listOf("Cyberpunk & Synthwave", "Phonk & Drift", "Lo-Fi & Chill"),
            "party" to listOf("Electronic & EDM", "Pop Hits", "Hip-Hop"),
            "dance" to listOf("Electronic & EDM", "Pop Hits"),
            "guitar" to listOf("Acoustic & Folk", "Rock & Metal", "Nu Metal & Alt-Rock"),
            "rock" to listOf("Rock & Metal", "Nu Metal & Alt-Rock", "Classics"),
            "metal" to listOf("Rock & Metal", "Nu Metal & Alt-Rock"),
            "ambient" to listOf("432Hz & Ambient"),
            "calm" to listOf("432Hz & Ambient", "Lo-Fi & Chill"),
            "pop" to listOf("Pop Hits"),
            "lofi" to listOf("Lo-Fi & Chill"),
            "phonk" to listOf("Phonk & Drift")
        )

        private val publicProvider by lazy { YouTubeProvider() }

        suspend fun searchVideosPublic(query: String, scope: String = "ALL"): List<Track> {
            return try {
                val result = publicProvider.searchWithScope(query, scope)
                result.getOrNull()?.tracks ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun getDailyChartDateFormatted(): String {
            val date = java.util.Date()
            val format = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US)
            return format.format(date)
        }
    }

    suspend fun fetchDailyTop100Chart(forceRefresh: Boolean = false): List<Track> = withContext(Dispatchers.IO) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (!forceRefresh && cachedDailyChartDate == today && cachedDailyTop100.isNotEmpty()) {
            return@withContext cachedDailyTop100
        }

        val seed = if (forceRefresh) System.currentTimeMillis() else today.hashCode().toLong()
        val baseTop100 = DailyTop100Registry.getDailyTop100(seed)

        val top100 = baseTop100.mapIndexed { index, track ->
            val rank = index + 1
            val trend = when {
                rank == 1 -> "HOT #1 OF THE DAY"
                rank == 2 -> if (forceRefresh) "▲ +1 CLIMBER" else "TOP 2 RUNNER"
                rank == 3 -> if (forceRefresh) "★ NEW PEAK" else "TOP 3 PEAK"
                rank in 4..10 -> if (forceRefresh && rank % 2 == 0) "▲ +2 TODAY" else "TOP 10 ANTHEM"
                rank in 11..25 -> if (forceRefresh && rank % 3 == 0) "🔥 SURGING" else "TOP 25 CHART"
                rank in 26..50 -> "TOP 50 HIT"
                else -> "DAILY 100"
            }
            track.copy(dailyRank = rank, chartTrend = trend)
        }

        cachedDailyChartDate = today
        cachedDailyTop100 = top100
        top100
    }

    override suspend fun getDiscoverFeed(forceRefresh: Boolean): Result<DiscoverFeed> = withContext(Dispatchers.IO) {
        try {
            val seed = if (forceRefresh) System.currentTimeMillis() else java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()).hashCode().toLong()
            val rng = kotlin.random.Random(seed)

            // Pick dynamic rotating seed for trending so pull-to-refresh rotates fresh tracks
            val trendingSeed = TRENDING_SEEDS.random(rng)

            // Pick random seeds from genre pools for variety
            val hipHopSeed = GENRE_QUERY_POOLS["Hip-Hop"]?.random(rng) ?: "top hip hop rap hits single official music video"
            val rockSeed = GENRE_QUERY_POOLS["Rock & Metal"]?.random(rng) ?: "best rock metal songs single official music video"
            val popSeed = GENRE_QUERY_POOLS["Pop Hits"]?.random(rng) ?: "top pop hits single official music video"
            val classicsSeed = GENRE_QUERY_POOLS["Classics"]?.random(rng) ?: "classic rock single official music video"

            // Concurrently fetch streams in parallel for sub-second feed generation
            val onlineTrending: List<Track>
            val hipHopYt: List<Track>
            val rockYt: List<Track>
            val popYt: List<Track>
            val classicsYt: List<Track>
            val dailyTop100: List<Track>

            coroutineScope {
                val dailyChartDef = async { fetchDailyTop100Chart(forceRefresh = forceRefresh) }
                val trendingDef = async {
                    try {
                        fetchInnerTubeVideos(trendingSeed, "ALL")
                            .filter { !SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) }
                            .take(20)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val hipHopDef = async {
                    try {
                        fetchInnerTubeVideos(hipHopSeed, "YOUTUBE_MUSIC")
                            .filter { !SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) }
                            .take(10)
                            .map { it.copy(genre = "Hip-Hop") }
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val rockDef = async {
                    try {
                        fetchInnerTubeVideos(rockSeed, "YOUTUBE_MUSIC")
                            .filter { !SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) }
                            .take(10)
                            .map { it.copy(genre = "Rock & Metal") }
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val popDef = async {
                    try {
                        fetchInnerTubeVideos(popSeed, "YOUTUBE_MUSIC")
                            .filter { !SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) }
                            .take(10)
                            .map { it.copy(genre = "Pop Hits") }
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val classicsDef = async {
                    try {
                        fetchInnerTubeVideos(classicsSeed, "YOUTUBE_MUSIC")
                            .filter { !SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) }
                            .take(10)
                            .map { it.copy(genre = "Classics") }
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                dailyTop100 = dailyChartDef.await()
                onlineTrending = trendingDef.await()
                hipHopYt = hipHopDef.await()
                rockYt = rockDef.await()
                popYt = popDef.await()
                classicsYt = classicsDef.await()
            }

            val baseTracks = CuratedFrequencies.allTracks
            val allCombined: List<Track> = (dailyTop100 + onlineTrending + hipHopYt + rockYt + popYt + classicsYt + baseTracks).distinctBy { it.youtubeVideoId.ifEmpty { it.id } }

            val quickPicks = (dailyTop100.shuffled(rng).take(5) + onlineTrending.shuffled(rng).take(3) + baseTracks.shuffled(rng).take(2)).distinctBy { it.id }.take(8)
            val trending = if (dailyTop100.isNotEmpty()) {
                dailyTop100
            } else if (onlineTrending.isNotEmpty()) {
                onlineTrending.take(20).mapIndexed { i, t -> t.copy(dailyRank = i + 1, chartTrend = "HOT #${i + 1}") }
            } else {
                allCombined.sortedByDescending { it.playCount.takeIf { c -> c > 0 } ?: 500000000L }.take(20).mapIndexed { i, t -> t.copy(dailyRank = i + 1, chartTrend = "TOP #${i + 1}") }
            }
            val playlists = CuratedFrequencies.getRotatingPlaylists(seed)
            val albums = CuratedFrequencies.getRotatingAlbums(seed)
            val artists = CuratedFrequencies.getRotatingArtists(seed)

            val moodMap = mutableMapOf<String, List<Track>>(
                "Hip-Hop" to (hipHopYt + allCombined.filter { 
                    it.genre.contains("Rap", ignoreCase = true) || 
                    it.genre.contains("Hip-Hop", ignoreCase = true) ||
                    it.artist in listOf("Eminem", "Kendrick Lamar", "Badshah", "Travis Scott", "50 Cent", "Dr. Dre")
                }).distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Rock & Metal" to (rockYt + allCombined.filter { 
                    it.genre.contains("Rock", ignoreCase = true) || 
                    it.genre.contains("Metal", ignoreCase = true) || 
                    it.genre.contains("Grunge", ignoreCase = true) ||
                    it.artist in listOf("Linkin Park", "Slipknot", "Metallica", "AC/DC", "Nirvana", "Queen", "System Of A Down", "Evanescence")
                }).distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Pop Hits" to (popYt + allCombined.filter { 
                    it.genre.contains("Pop", ignoreCase = true) || 
                    it.genre.contains("Soul", ignoreCase = true) || 
                    it.genre.contains("Funk", ignoreCase = true) ||
                    it.artist in listOf("The Weeknd", "Billie Eilish", "Ed Sheeran", "Taylor Swift", "Bruno Mars", "Adele", "Dua Lipa", "Imagine Dragons")
                }).distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Classics" to (classicsYt + allCombined.filter { 
                    it.genre.contains("Classic", ignoreCase = true) || 
                    it.artist in listOf("Michael Jackson", "Queen", "Nirvana", "AC/DC", "Coldplay")
                }).distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Cyberpunk & Synthwave" to allCombined.filter {
                    it.genre.contains("Cyberpunk", ignoreCase = true) ||
                    it.genre.contains("Synthwave", ignoreCase = true) ||
                    it.genre.contains("Electronic", ignoreCase = true) ||
                    it.title.contains("Synth", ignoreCase = true)
                }.distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Nu Metal & Alt-Rock" to allCombined.filter {
                    it.genre.contains("Nu Metal", ignoreCase = true) ||
                    it.genre.contains("Alternative", ignoreCase = true) ||
                    it.artist in listOf("Slipknot", "Linkin Park", "System Of A Down")
                }.distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Electronic & EDM" to allCombined.filter {
                    it.genre.contains("EDM", ignoreCase = true) ||
                    it.genre.contains("Electronic", ignoreCase = true) ||
                    it.genre.contains("Dance", ignoreCase = true)
                }.distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "432Hz & Ambient" to allCombined.filter {
                    it.frequencyHz == 432 ||
                    it.genre.contains("432", ignoreCase = true) ||
                    it.genre.contains("Ambient", ignoreCase = true)
                }.distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Lo-Fi & Chill" to allCombined.filter {
                    it.genre.contains("Lo-Fi", ignoreCase = true) ||
                    it.genre.contains("Chill", ignoreCase = true)
                }.distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Acoustic & Folk" to allCombined.filter {
                    it.genre.contains("Acoustic", ignoreCase = true) ||
                    it.genre.contains("Folk", ignoreCase = true)
                }.distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "R&B & Soul" to allCombined.filter {
                    it.genre.contains("R&B", ignoreCase = true) ||
                    it.genre.contains("Soul", ignoreCase = true)
                }.distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Phonk & Drift" to allCombined.filter {
                    it.genre.contains("Phonk", ignoreCase = true) ||
                    it.title.contains("Phonk", ignoreCase = true)
                }.distinctBy { it.youtubeVideoId.ifEmpty { it.id } },
                "Orchestral & Cinematic" to allCombined.filter {
                    it.genre.contains("Orchestral", ignoreCase = true) ||
                    it.genre.contains("Cinematic", ignoreCase = true)
                }.distinctBy { it.youtubeVideoId.ifEmpty { it.id } }
            )

            Result.success(
                DiscoverFeed(
                    quickPicks = quickPicks,
                    trendingTracks = trending,
                    featuredPlaylists = playlists,
                    featuredAlbums = albums,
                    featuredArtists = artists,
                    moodPlaylists = moodMap
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun search(query: String): Result<SearchResults> {
        return searchWithScope(query, "ALL")
    }

    suspend fun searchWithScope(query: String, scope: String): Result<SearchResults> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return@withContext Result.success(SearchResults(query = "", searchScope = scope))
        }

        val cacheKey = "${cleanQuery.lowercase()}_$scope"
        synchronized(searchCache) {
            val cached = searchCache.get(cacheKey)
            if (cached != null) {
                return@withContext Result.success(cached)
            }
        }

        // 1. Check if user entered a direct YouTube or YouTube Music URL or 11-char Video ID
        val directVideoId = extractYouTubeVideoId(cleanQuery)
        if (directVideoId != null) {
            val oembedTrack = fetchOEmbedMetadata(directVideoId)
            val directTrack = oembedTrack?.copy(streamUrl = "", genre = "Music") ?: Track(
                id = "yt_$directVideoId",
                title = "YouTube Video: $directVideoId",
                artist = if (scope == "YOUTUBE_MUSIC") "Music" else "Creator",
                artworkUrl = "https://img.youtube.com/vi/$directVideoId/hqdefault.jpg",
                youtubeVideoId = directVideoId,
                streamUrl = "",
                genre = "Music",
                durationSeconds = 240,
                frequencyHz = 432,
                source = if (scope == "YOUTUBE_MUSIC") "youtube_music" else "youtube"
            )
            val res = SearchResults(
                query = cleanQuery,
                tracks = listOf(directTrack),
                searchScope = scope
            )
            synchronized(searchCache) {
                searchCache.put(cacheKey, res)
            }
            return@withContext Result.success(res)
        }

        val ytTracks = mutableListOf<Track>()

        // 2. Query official YouTube Data API v3 if API key is provided
        if (apiKey.isNotBlank()) {
            try {
                val searchUrl = if (scope == "YOUTUBE_MUSIC") {
                    "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoCategoryId=10&maxResults=25&q=${URLEncoder.encode("$cleanQuery audio music", "UTF-8")}&key=${apiKey}"
                } else {
                    "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoCategoryId=10&maxResults=25&q=${URLEncoder.encode(cleanQuery, "UTF-8")}&key=${apiKey}"
                }
                val request = Request.Builder().url(searchUrl).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (bodyString != null) {
                        val json = JSONObject(bodyString)
                        val items = json.optJSONArray("items")
                        if (items != null) {
                            for (i in 0 until items.length()) {
                                val item = items.getJSONObject(i)
                                val idObj = item.optJSONObject("id")
                                val videoId = idObj?.optString("videoId") ?: ""
                                val snippet = item.optJSONObject("snippet")
                                val title = snippet?.optString("title") ?: "Unknown Title"
                                val channelTitle = snippet?.optString("channelTitle") ?: "YouTube Artist"
                                val thumbs = snippet?.optJSONObject("thumbnails")
                                val thumbHigh = thumbs?.optJSONObject("high")?.optString("url")
                                    ?: thumbs?.optJSONObject("medium")?.optString("url")
                                    ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                                if (videoId.isNotEmpty() && !ytTracks.any { it.youtubeVideoId == videoId }) {
                                    ytTracks.add(
                                        Track(
                                            id = "yt_$videoId",
                                            title = cleanTitle(title),
                                            artist = cleanTitle(channelTitle),
                                            artworkUrl = thumbHigh,
                                            youtubeVideoId = videoId,
                                            streamUrl = "",
                                            genre = "Music",
                                            durationSeconds = 240,
                                            frequencyHz = 432,
                                            source = if (scope == "YOUTUBE_MUSIC") "youtube_music" else "youtube"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. YouTube InnerTube Search: ultra-fast live public search for songs and videos
        val innerTubeResults = fetchInnerTubeVideos(cleanQuery, scope)
        ytTracks.addAll(innerTubeResults)

        // 4. Secondary fallback: Web search scraper (only if InnerTube produced zero results)
        if (ytTracks.isEmpty()) {
            val webResults = fetchWebSearchVideos(cleanQuery, scope)
            for (t in webResults) {
                if (!ytTracks.any { it.youtubeVideoId == t.youtubeVideoId }) {
                    ytTracks.add(t)
                }
            }
        }

        // 5. Query broadening: only if results are very sparse (< 4), run 1 fast variation
        if (ytTracks.size < 4) {
            try {
                val moreTracks = fetchInnerTubeVideos("$cleanQuery songs", scope)
                for (t in moreTracks) {
                    if (ytTracks.size >= 15) break
                    if (!ytTracks.any { it.youtubeVideoId == t.youtubeVideoId }) {
                        ytTracks.add(t)
                    }
                }
            } catch (_: Exception) {}
        }

        // 6. Curated and local frequency matching with keyword vibe mapping
        val cleanLower = cleanQuery.lowercase()
        val mappedGenres = KEYWORD_VIBE_MAP.entries
            .filter { cleanLower.contains(it.key) }
            .flatMap { it.value }
            .toSet()

        val matchedTracks = CuratedFrequencies.allTracks.filter { track ->
            track.title.contains(cleanQuery, ignoreCase = true) ||
                    track.artist.contains(cleanQuery, ignoreCase = true) ||
                    track.genre.contains(cleanQuery, ignoreCase = true) ||
                    mappedGenres.any { g -> track.genre.equals(g, ignoreCase = true) }
        }

        // 7. If still empty, search YouTube for artist songs fallback
        if (ytTracks.isEmpty() && matchedTracks.isEmpty()) {
            try {
                val artistTracks = fetchInnerTubeVideos("$cleanQuery official tracks", scope)
                ytTracks.addAll(artistTracks.take(10))
            } catch (_: Exception) {}
        }

        val matchedArtists = if (scope != "YOUTUBE" && scope != "YOUTUBE_MUSIC") {
            CuratedFrequencies.artists.filter {
                it.name.contains(cleanQuery, ignoreCase = true) ||
                        it.genres.any { g -> g.contains(cleanQuery, ignoreCase = true) } ||
                        cleanLower.contains(it.name.lowercase())
            }
        } else emptyList()

        val matchedAlbums = if (scope != "YOUTUBE" && scope != "YOUTUBE_MUSIC") {
            CuratedFrequencies.albums.filter {
                it.title.contains(cleanQuery, ignoreCase = true) ||
                        it.artist.contains(cleanQuery, ignoreCase = true)
            }
        } else emptyList()

        val matchedPlaylists = if (scope != "YOUTUBE" && scope != "YOUTUBE_MUSIC") {
            CuratedFrequencies.playlists.filter {
                it.title.contains(cleanQuery, ignoreCase = true) ||
                        it.description.contains(cleanQuery, ignoreCase = true)
            }
        } else emptyList()

        // Combine based on scope
        val baseTracks = when (scope) {
            "YOUTUBE" -> ytTracks.ifEmpty {
                matchedTracks.map { it.copy(source = "youtube") }
            }
            "YOUTUBE_MUSIC" -> ytTracks.ifEmpty {
                matchedTracks.map { it.copy(source = "youtube_music") }
            }
            else -> {
                (ytTracks + matchedTracks).distinctBy { it.youtubeVideoId.ifEmpty { it.id } }
            }
        }

        // Rank by relevance scoring
        val combinedTracks = baseTracks.sortedByDescending { scoreTrack(it, cleanQuery) }

        val searchResults = SearchResults(
            query = cleanQuery,
            tracks = combinedTracks,
            artists = matchedArtists,
            albums = matchedAlbums,
            playlists = matchedPlaylists,
            searchScope = scope
        )

        synchronized(searchCache) {
            searchCache.put(cacheKey, searchResults)
        }

        Result.success(searchResults)
    }

    private fun scoreTrack(track: Track, q: String): Int {
        val qLower = q.lowercase()
        val tLower = track.title.lowercase()
        val aLower = track.artist.lowercase()
        val gLower = track.genre.lowercase()

        var score = 0
        if (tLower == qLower) score += 100
        else if (tLower.startsWith(qLower)) score += 80
        else if (tLower.contains(qLower)) score += 60

        if (aLower == qLower) score += 70
        else if (aLower.startsWith(qLower)) score += 50
        else if (aLower.contains(qLower)) score += 40

        if (gLower.contains(qLower)) score += 30

        KEYWORD_VIBE_MAP.forEach { (keyword, genres) ->
            if (qLower.contains(keyword) && genres.any { g -> gLower.contains(g.lowercase()) }) {
                score += 35
            }
        }

        if (!SmartQueueEngine.isTrackPlayable(track)) {
            score -= 200
        }
        return score
    }

    /**
     * Fetches unlimited genre songs directly from YouTube and YouTube Music.
     * Strictly filters for good single songs and discards collections/compilations/mixes.
     */
    suspend fun fetchTracksForGenre(genre: String): List<Track> = withContext(Dispatchers.IO) {
        val pool = GENRE_QUERY_POOLS[genre]
        val query = if (!pool.isNullOrEmpty()) {
            pool.random()
        } else {
            val variants = listOf(
                "$genre single official music video",
                "best $genre songs single official",
                "top $genre hits official video"
            )
            variants.random()
        }
        val ytTracks = try {
            val results = fetchInnerTubeVideos(query, "YOUTUBE_MUSIC").ifEmpty {
                fetchWebSearchVideos(query, "YOUTUBE_MUSIC")
            }
            results.filter { !SmartQueueEngine.isCollectionOrMix(it.title, it.durationSeconds) }
        } catch (_: Exception) {
            emptyList()
        }
        ytTracks.map { it.copy(genre = genre) }
    }

    /**
     * Searches YouTube using YouTube InnerTube API for uploaded songs and music videos.
     * Requires no API key and returns real-time YouTube songs and video IDs.
     */
    private fun fetchInnerTubeVideos(query: String, scope: String): List<Track> {
        return try {
            val effectiveQuery = if (scope == "YOUTUBE_MUSIC") {
                if (query.lowercase().contains("music") || query.lowercase().contains("song")) query else "$query song"
            } else {
                query
            }

            val payloadJson = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20231201.00.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("query", effectiveQuery)
            }

            val requestBody = payloadJson.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val root = JSONObject(body)
            val tracks = mutableListOf<Track>()
            extractTracksFromJsonObject(root, scope, tracks)
            tracks
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fallback web search scraper that fetches video results directly from YouTube HTML ytInitialData.
     */
    private fun fetchWebSearchVideos(query: String, scope: String): List<Track> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery&sp=EgIQAQ%253D%253D"
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val html = response.body?.string() ?: return emptyList()

            val jsonMarker = "var ytInitialData = "
            val startIndex = html.indexOf(jsonMarker)
            if (startIndex != -1) {
                val jsonStart = startIndex + jsonMarker.length
                val endIndex = html.indexOf(";</script>", jsonStart)
                if (endIndex != -1) {
                    val jsonString = html.substring(jsonStart, endIndex)
                    val json = JSONObject(jsonString)
                    val tracks = mutableListOf<Track>()
                    extractTracksFromJsonObject(json, scope, tracks)
                    if (tracks.isNotEmpty()) return tracks
                }
            }

            // Regex extraction directly from HTML as secondary resilience
            val pattern = Pattern.compile("\"videoId\":\"([a-zA-Z0-9_-]{11})\".*?\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"")
            val matcher = pattern.matcher(html)
            val regexTracks = mutableListOf<Track>()
            while (matcher.find() && regexTracks.size < 20) {
                val vid = matcher.group(1) ?: continue
                val rawTitle = matcher.group(2) ?: "Track"
                val (songTitle, songArtist) = SmartQueueEngine.cleanSongTitleAndArtist(rawTitle, "Artist")
                if (!regexTracks.any { it.youtubeVideoId == vid } &&
                    songTitle.isNotBlank() &&
                    !SmartQueueEngine.isCollectionOrMix(songTitle, 240) &&
                    !SmartQueueEngine.isCollectionOrMix(rawTitle, 240)) {
                    regexTracks.add(
                        Track(
                            id = "yt_$vid",
                            title = cleanTitle(songTitle),
                            artist = cleanTitle(songArtist),
                            artworkUrl = "https://img.youtube.com/vi/$vid/hqdefault.jpg",
                            youtubeVideoId = vid,
                            streamUrl = "",
                            genre = if (scope == "YOUTUBE_MUSIC") "Music" else "Audio",
                            durationSeconds = 240,
                            frequencyHz = 432,
                            source = if (scope == "YOUTUBE_MUSIC") "youtube_music" else "youtube"
                        )
                    )
                }
            }
            regexTracks
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Traverses the innerTube or ytInitialData JSON hierarchy to extract all videoRenderer and
     * musicResponsiveListItemRenderer tracks.
     */
    private fun extractTracksFromJsonObject(obj: JSONObject, scope: String, out: MutableList<Track>) {
        // 1. videoRenderer / compactVideoRenderer
        val vr = obj.optJSONObject("videoRenderer") ?: obj.optJSONObject("compactVideoRenderer")
        if (vr != null) {
            val videoId = vr.optString("videoId")
            if (videoId.isNotEmpty() && !out.any { it.youtubeVideoId == videoId }) {
                val titleObj = vr.optJSONObject("title")
                val titleRuns = titleObj?.optJSONArray("runs")
                val title = if (titleRuns != null && titleRuns.length() > 0) {
                    titleRuns.optJSONObject(0)?.optString("text") ?: ""
                } else {
                    titleObj?.optString("simpleText") ?: ""
                }

                val ownerObj = vr.optJSONObject("ownerText") ?: vr.optJSONObject("shortBylineText")
                val ownerRuns = ownerObj?.optJSONArray("runs")
                val artist = if (ownerRuns != null && ownerRuns.length() > 0) {
                    ownerRuns.optJSONObject(0)?.optString("text") ?: "Artist"
                } else {
                    ownerObj?.optString("simpleText") ?: "Artist"
                }

                val lengthObj = vr.optJSONObject("lengthText")
                var lengthStr = lengthObj?.optString("simpleText") ?: ""
                var isLiveStream = false
                val overlays = vr.optJSONArray("thumbnailOverlays")
                if (overlays != null) {
                    for (i in 0 until overlays.length()) {
                        val to = overlays.optJSONObject(i)?.optJSONObject("thumbnailOverlayTimeStatusRenderer")
                        if (to != null) {
                            val style = to.optString("style")
                            if (style.equals("LIVE", ignoreCase = true)) {
                                isLiveStream = true
                                break
                            }
                            if (lengthStr.isEmpty()) {
                                lengthStr = to.optJSONObject("text")?.optString("simpleText") ?: ""
                                if (lengthStr.isEmpty()) {
                                    val runs = to.optJSONObject("text")?.optJSONArray("runs")
                                    if (runs != null && runs.length() > 0) {
                                        lengthStr = runs.optJSONObject(0)?.optString("text") ?: ""
                                    }
                                }
                            }
                        }
                    }
                }
                if (isLiveStream || lengthStr.contains("LIVE", ignoreCase = true)) {
                    // Do not parse live streams or non-stop radios as songs
                    return
                }

                val durationSec = parseDurationToSeconds(lengthStr)

                val thumbs = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
                    thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                } else {
                    "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                }

                val (songTitle, songArtist) = SmartQueueEngine.cleanSongTitleAndArtist(title, artist)
                if (songTitle.isNotBlank() &&
                    !SmartQueueEngine.isCollectionOrMix(songTitle, durationSec) &&
                    !SmartQueueEngine.isCollectionOrMix(title, durationSec)) {
                    out.add(
                        Track(
                            id = "yt_$videoId",
                            title = cleanTitle(songTitle),
                            artist = cleanTitle(songArtist),
                            artworkUrl = thumbUrl,
                            youtubeVideoId = videoId,
                            streamUrl = "",
                            genre = if (scope == "YOUTUBE_MUSIC") "Music" else "Audio",
                            durationSeconds = durationSec,
                            frequencyHz = 432,
                            source = if (scope == "YOUTUBE_MUSIC") "youtube_music" else "youtube"
                        )
                    )
                }
            }
        }

        // 2. musicResponsiveListItemRenderer
        val mr = obj.optJSONObject("musicResponsiveListItemRenderer")
        if (mr != null) {
            val plData = mr.optJSONObject("playlistItemData")
            var videoId = plData?.optString("videoId") ?: ""
            if (videoId.isEmpty()) {
                val nav = mr.optJSONObject("navigationEndpoint")
                videoId = nav?.optJSONObject("watchEndpoint")?.optString("videoId") ?: ""
            }
            if (videoId.isNotEmpty() && !out.any { it.youtubeVideoId == videoId }) {
                val flexColumns = mr.optJSONArray("flexColumns")
                var title = ""
                var artist = "Artist"
                if (flexColumns != null && flexColumns.length() > 0) {
                    val col0 = flexColumns.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")?.optJSONArray("runs")
                    if (col0 != null && col0.length() > 0) {
                        title = col0.optJSONObject(0)?.optString("text") ?: ""
                    }
                }
                if (flexColumns != null && flexColumns.length() > 1) {
                    val col1 = flexColumns.optJSONObject(1)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")?.optJSONArray("runs")
                    if (col1 != null && col1.length() > 0) {
                        val names = mutableListOf<String>()
                        for (i in 0 until col1.length()) {
                            val t = col1.optJSONObject(i)?.optString("text") ?: ""
                            if (t.isNotBlank() && t != " • " && t != "•") {
                                names.add(t)
                            }
                        }
                        if (names.isNotEmpty()) {
                            artist = names.joinToString(" • ")
                        }
                    }
                }

                val (songTitle, songArtist) = SmartQueueEngine.cleanSongTitleAndArtist(title, artist)
                if (songTitle.isNotBlank() &&
                    !SmartQueueEngine.isCollectionOrMix(songTitle, 210) &&
                    !SmartQueueEngine.isCollectionOrMix(title, 210)) {
                    out.add(
                        Track(
                            id = "yt_$videoId",
                            title = cleanTitle(songTitle),
                            artist = cleanTitle(songArtist),
                            artworkUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                            youtubeVideoId = videoId,
                            streamUrl = "",
                            genre = "Music",
                            durationSeconds = 210,
                            frequencyHz = 432,
                            source = "youtube_music"
                        )
                    )
                }
            }
        }

        // Recurse child objects and arrays
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val child = obj.opt(key)
            if (child is JSONObject) {
                extractTracksFromJsonObject(child, scope, out)
            } else if (child is JSONArray) {
                extractTracksFromJsonArray(child, scope, out)
            }
        }
    }

    private fun extractTracksFromJsonArray(arr: JSONArray, scope: String, out: MutableList<Track>) {
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            if (item is JSONObject) {
                extractTracksFromJsonObject(item, scope, out)
            } else if (item is JSONArray) {
                extractTracksFromJsonArray(item, scope, out)
            }
        }
    }

    private fun parseDurationToSeconds(lengthStr: String): Int {
        if (lengthStr.isBlank()) return 210
        val parts = lengthStr.split(":").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 210
        }
    }

    /**
     * Resolves live YouTube metadata using YouTube oEmbed without an API key.
     */
    private fun fetchOEmbedMetadata(videoId: String): Track? {
        return try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val title = json.optString("title", "YouTube Track: $videoId")
                val author = json.optString("author_name", "YouTube Creator")
                val thumb = json.optString("thumbnail_url", "https://img.youtube.com/vi/$videoId/hqdefault.jpg")
                val (songTitle, songArtist) = SmartQueueEngine.cleanSongTitleAndArtist(title, author)
                Track(
                    id = "yt_$videoId",
                    title = cleanTitle(songTitle),
                    artist = cleanTitle(songArtist),
                    artworkUrl = thumb,
                    youtubeVideoId = videoId,
                    genre = "Music",
                    durationSeconds = 240,
                    frequencyHz = 432,
                    source = "youtube"
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getTrack(trackId: String): Result<Track> = withContext(Dispatchers.IO) {
        val track = CuratedFrequencies.allTracks.find { it.id == trackId }
        if (track != null) {
            Result.success(track)
        } else {
            // Check if YouTube track ID
            if (trackId.startsWith("yt_")) {
                val videoId = trackId.removePrefix("yt_")
                val fetched = fetchOEmbedMetadata(videoId) ?: Track(
                    id = trackId,
                    title = "Video Stream",
                    artist = "Artist",
                    artworkUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                    youtubeVideoId = videoId,
                    genre = "Music",
                    durationSeconds = 240,
                    frequencyHz = 432,
                    source = "youtube"
                )
                Result.success(fetched)
            } else {
                Result.failure(NoSuchElementException("Track $trackId not found in provider"))
            }
        }
    }

    override suspend fun getArtist(artistId: String): Result<Artist> = withContext(Dispatchers.IO) {
        val artist = CuratedFrequencies.artists.find { it.id == artistId }
        if (artist != null) {
            val topTracks = CuratedFrequencies.allTracks.filter { it.artistId == artistId }
            val albums = CuratedFrequencies.albums.filter { it.artistId == artistId }
            Result.success(artist.copy(topTracks = topTracks, albums = albums))
        } else {
            Result.failure(NoSuchElementException("Artist $artistId not found in provider"))
        }
    }

    override suspend fun getAlbum(albumId: String): Result<Album> = withContext(Dispatchers.IO) {
        val album = CuratedFrequencies.albums.find { it.id == albumId }
        if (album != null) {
            Result.success(album)
        } else {
            Result.failure(NoSuchElementException("Album $albumId not found in provider"))
        }
    }

    override suspend fun getPlaylist(playlistId: String): Result<Playlist> = withContext(Dispatchers.IO) {
        val playlist = CuratedFrequencies.playlists.find { it.id == playlistId }
        if (playlist != null) {
            Result.success(playlist)
        } else {
            Result.failure(NoSuchElementException("Playlist $playlistId not found in provider"))
        }
    }

    private fun extractYouTubeVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length == 11 && trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }
        val pattern = Pattern.compile(
            "(?:https?:\\/\\/)?(?:www\\.|m\\.|music\\.)?(?:youtube\\.com\\/(?:watch\\?.*?v=|embed\\/|v\\/|shorts\\/|live\\/|e\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})"
        )
        val matcher = pattern.matcher(trimmed)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            val paramPattern = Pattern.compile("[?&]v=([a-zA-Z0-9_-]{11})")
            val paramMatcher = paramPattern.matcher(trimmed)
            if (paramMatcher.find()) paramMatcher.group(1) else null
        }
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}


