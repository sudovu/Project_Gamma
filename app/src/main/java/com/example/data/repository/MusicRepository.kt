package com.example.data.repository

import com.example.data.local.DiscoveryCacheDao
import com.example.data.local.DiscoveredTrackEntity
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteTrackEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.data.local.RecentPlaybackDao
import com.example.data.local.RecentPlaybackEntity
import com.example.data.local.SearchHistoryDao
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.TrackPlayStatDao
import com.example.data.local.TrackPlayStatEntity
import com.example.data.local.UploadedTrackDao
import com.example.data.local.UploadedTrackEntity
import com.example.data.provider.CuratedFrequencies
import com.example.data.provider.DailyTop100Registry
import com.example.data.provider.MusicProvider
import com.example.data.provider.YouTubeProvider
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.DiscoverFeed
import com.example.domain.model.Playlist
import com.example.domain.model.SearchResults
import com.example.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.regex.Pattern

class MusicRepository(
    private val provider: MusicProvider,
    private val favoriteDao: FavoriteDao,
    private val recentPlaybackDao: RecentPlaybackDao,
    private val playlistDao: PlaylistDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val uploadedTrackDao: UploadedTrackDao,
    private val discoveryCacheDao: DiscoveryCacheDao,
    private val trackPlayStatDao: TrackPlayStatDao? = null
) {

    fun getDiscoverFeed(): Flow<Result<DiscoverFeed>> = combine(
        flow {
            // Fast Start: Emit cached discovery feed or curated feed immediately (<50ms) so user never waits for UI
            val cachedTracks = try {
                discoveryCacheDao.getAllDiscoveredSync()
            } catch (_: Exception) {
                emptyList()
            }

            if (cachedTracks.isNotEmpty()) {
                val cachedFeed = buildFeedFromCached(cachedTracks)
                emit(Result.success(cachedFeed))
            } else {
                val initialFeed = buildCuratedInitialFeed()
                try {
                    cacheFeedTracks(initialFeed)
                } catch (_: Exception) {}
                emit(Result.success(initialFeed))
            }

            // Fresh online fetch from provider in background
            val providerResult = try {
                provider.getDiscoverFeed()
            } catch (e: Exception) {
                Result.failure(e)
            }

            if (providerResult.isSuccess) {
                val feed = providerResult.getOrThrow()
                // Cache discovered music metadata into Room for offline browsing
                try {
                    cacheFeedTracks(feed)
                } catch (_: Exception) {}
                emit(providerResult)
            }
        },
        getUploadedTracks()
    ) { providerResult, uploaded ->
        providerResult.map { feed ->
            feed.copy(
                uploadedTracks = uploaded,
                quickPicks = if (uploaded.isNotEmpty()) {
                    (uploaded + feed.quickPicks).distinctBy { it.id }
                } else feed.quickPicks
            )
        }
    }

    suspend fun refreshDiscoverFeed(): Result<DiscoverFeed> {
        val providerResult = try {
            provider.getDiscoverFeed(forceRefresh = true)
        } catch (e: Exception) {
            Result.failure(e)
        }

        return if (providerResult.isSuccess) {
            val feed = providerResult.getOrThrow()
            try {
                cacheFeedTracks(feed)
            } catch (_: Exception) {}
            val uploaded = try {
                uploadedTrackDao.getAllUploaded().first().map { it.toDomain() }
            } catch (_: Exception) {
                emptyList()
            }
            Result.success(
                feed.copy(
                    uploadedTracks = uploaded,
                    quickPicks = if (uploaded.isNotEmpty()) {
                        (uploaded + feed.quickPicks).distinctBy { it.id }
                    } else feed.quickPicks
                )
            )
        } else {
            val cachedTracks = try {
                discoveryCacheDao.getAllDiscoveredSync()
            } catch (_: Exception) {
                emptyList()
            }
            if (cachedTracks.isNotEmpty()) {
                Result.success(buildFeedFromCached(cachedTracks))
            } else {
                providerResult
            }
        }
    }

    private fun buildCuratedInitialFeed(): DiscoverFeed {
        val dailyChart: List<Track> = DailyTop100Registry.getDailyTop100()
        val all: List<Track> = CuratedFrequencies.allTracks
        val moodMap = mapOf(
            "Hip-Hop" to (dailyChart.filter { it.genre == "Hip-Hop" } + all.filter { it.genre.contains("Rap", ignoreCase = true) || it.genre.contains("Hip-Hop", ignoreCase = true) }).distinctBy { it.id },
            "Rock & Metal" to (dailyChart.filter { it.genre == "Rock & Metal" } + all.filter { it.genre.contains("Rock", ignoreCase = true) || it.genre.contains("Metal", ignoreCase = true) }).distinctBy { it.id },
            "Pop Hits" to (dailyChart.filter { it.genre == "Pop Hits" } + all.filter { it.genre.contains("Pop", ignoreCase = true) }).distinctBy { it.id },
            "Classics" to (dailyChart.filter { it.genre == "Classics" } + all.filter { it.genre.contains("Classic", ignoreCase = true) }).distinctBy { it.id }
        )
        return DiscoverFeed(
            quickPicks = dailyChart.take(8),
            trendingTracks = dailyChart,
            featuredPlaylists = CuratedFrequencies.getRotatingPlaylists(),
            featuredAlbums = CuratedFrequencies.getRotatingAlbums(),
            featuredArtists = CuratedFrequencies.getRotatingArtists(),
            moodPlaylists = moodMap
        )
    }

    fun search(query: String): Flow<Result<SearchResults>> {
        return searchWithScope(query, "ALL")
    }

    fun searchWithScope(query: String, scope: com.example.feature.search.SearchScope): Flow<Result<SearchResults>> {
        return searchWithScope(query, scope.name)
    }

    fun searchWithScope(query: String, scope: String = "ALL"): Flow<Result<SearchResults>> = flow {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            emit(Result.success(SearchResults(query = "", searchScope = scope)))
            return@flow
        }

        // 1. Search uploaded tracks
        val uploadedMatches = if (scope == "ALL" || scope == "UPLOADED") {
            try {
                uploadedTrackDao.searchUploaded(cleanQuery).first().map { it.toDomain() }
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        // 2. Search provider (YouTube / YouTube Music / Curated)
        val providerResult = if (scope != "UPLOADED") {
            try {
                if (provider is YouTubeProvider) {
                    provider.searchWithScope(cleanQuery, scope)
                } else {
                    provider.search(cleanQuery)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.success(SearchResults(query = cleanQuery, searchScope = scope))
        }

        // 3. If online search succeeds, cache tracks; if provider failed (offline), search cached discovery metadata!
        val effectiveProviderResult = if (providerResult.isSuccess) {
            val res = providerResult.getOrThrow()
            try {
                val toCache = res.tracks.map { it.toDiscoveredEntity(category = "Search") }
                if (toCache.isNotEmpty()) {
                    discoveryCacheDao.insertAllDiscovered(toCache)
                }
            } catch (_: Exception) {}
            providerResult
        } else {
            val cachedMatches = try {
                discoveryCacheDao.searchCached(cleanQuery).map { it.toDomain() }
            } catch (_: Exception) {
                emptyList()
            }
            Result.success(
                SearchResults(
                    query = cleanQuery,
                    searchScope = scope,
                    tracks = cachedMatches
                )
            )
        }

        val combinedResult = effectiveProviderResult.map { results ->
            results.copy(
                query = cleanQuery,
                searchScope = scope,
                uploadedTracks = uploadedMatches,
                tracks = if (scope == "UPLOADED") {
                    uploadedMatches
                } else {
                    (uploadedMatches + results.tracks).distinctBy { it.id }
                }
            )
        }

        emit(combinedResult)
    }

    /**
     * Discovery History cached in Room.
     * Enables users to browse their complete discovery history even without an active internet connection.
     */
    fun getDiscoveryHistory(): Flow<List<Track>> {
        return discoveryCacheDao.getDiscoveryHistory().map { entities ->
            entities.map { it.toDomain() }.filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }
        }
    }

    suspend fun clearDiscoveryHistory() {
        discoveryCacheDao.clearDiscoveryCache()
    }

    suspend fun deleteDiscoveryTrack(trackId: String) {
        discoveryCacheDao.deleteDiscovered(trackId)
    }

    suspend fun cacheDiscoveredTrack(track: Track, category: String = "Discover") {
        discoveryCacheDao.insertDiscovered(track.toDiscoveredEntity(category))
    }

    private suspend fun cacheFeedTracks(feed: DiscoverFeed) {
        // Clean up obsolete/ambient track cache entries from previous versions
        try {
            val obsoleteIds = listOf(
                "trk_sol_01", "trk_sol_02", "trk_vek_01", "trk_vek_02",
                "trk_lofi_01", "trk_lofi_02", "trk_aeth_01", "trk_aeth_02",
                "trk_kin_01", "trk_kin_02"
            )
            obsoleteIds.forEach { discoveryCacheDao.deleteDiscovered(it) }
        } catch (_: Exception) {}

        val entities = mutableListOf<DiscoveredTrackEntity>()
        feed.trendingTracks.filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }.forEach { track ->
            entities.add(track.toDiscoveredEntity(category = "Trending"))
        }
        feed.quickPicks.filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }.forEach { track ->
            entities.add(track.toDiscoveredEntity(category = "Quick Pick"))
        }
        feed.moodPlaylists.forEach { (mood, tracks) ->
            tracks.filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }.forEach { track ->
                entities.add(track.toDiscoveredEntity(category = mood))
            }
        }
        if (entities.isNotEmpty()) {
            discoveryCacheDao.insertAllDiscovered(entities.distinctBy { it.trackId })
        }
    }

    private fun buildFeedFromCached(entities: List<DiscoveredTrackEntity>): DiscoverFeed {
        val allTracks = entities
            .map { it.toDomain() }
            .filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }
        val quickPicks = entities
            .filter { it.category == "Quick Pick" }
            .map { it.toDomain() }
            .filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }
            .ifEmpty { allTracks.take(6) }
        val trending = entities
            .filter { it.category == "Trending" }
            .map { it.toDomain() }
            .filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }
            .ifEmpty { allTracks }

        val moodMap = mutableMapOf<String, List<Track>>()
        val moods = listOf("Hip-Hop", "Rock & Metal", "Pop Hits", "Classics")
        moods.forEach { mood ->
            val moodTracks = entities
                .filter { it.category == mood || it.genre.contains(mood, ignoreCase = true) }
                .map { it.toDomain() }
                .filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }
            if (moodTracks.isNotEmpty()) {
                moodMap[mood] = moodTracks
            } else {
                moodMap[mood] = allTracks.take(5)
            }
        }

        return DiscoverFeed(
            trendingTracks = trending,
            quickPicks = quickPicks,
            moodPlaylists = moodMap,
            featuredAlbums = emptyList(),
            featuredArtists = emptyList(),
            featuredPlaylists = emptyList(),
            uploadedTracks = emptyList()
        )
    }

    fun getUploadedTracks(): Flow<List<Track>> {
        return uploadedTrackDao.getAllUploaded().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getDownloadedTracks(): Flow<List<Track>> = combine(
        getUploadedTracks(),
        discoveryCacheDao.getAllDiscovered()
    ) { uploaded: List<Track>, discovered: List<DiscoveredTrackEntity> ->
        val fromDiscovered = discovered.map { it.toDomain() }
        val all = (CuratedFrequencies.allTracks + fromDiscovered + uploaded)
            .distinctBy { it.id }
            .filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }
        all.filter { it.localAudioUri.isNotBlank() || it.isDownloaded }
    }

    suspend fun uploadTrack(
        title: String,
        artist: String,
        durationSeconds: Int = 200,
        frequencyHz: Int = 432,
        genre: String = "Personal Frequency",
        youtubeUrlOrId: String = "",
        localAudioUri: String = "",
        artworkUrl: String = ""
    ): Track {
        val id = "upload_" + UUID.randomUUID().toString().take(8)
        val cleanVideoId = extractVideoId(youtubeUrlOrId)
        val defaultCover = when (frequencyHz) {
            432 -> "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80"
            528 -> "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80"
            639 -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80"
            else -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
        }
        val finalArtwork = if (artworkUrl.isNotBlank()) artworkUrl else if (cleanVideoId.isNotEmpty()) "https://img.youtube.com/vi/$cleanVideoId/hqdefault.jpg" else defaultCover

        val entity = UploadedTrackEntity(
            id = id,
            title = title.trim(),
            artist = artist.trim().ifEmpty { "Community Creator" },
            durationSeconds = durationSeconds.coerceAtLeast(30),
            artworkUrl = finalArtwork,
            youtubeVideoId = cleanVideoId,
            localAudioUri = localAudioUri,
            genre = genre.trim().ifEmpty { "Custom Orbit" },
            frequencyHz = frequencyHz
        )

        uploadedTrackDao.insertUploaded(entity)
        return entity.toDomain()
    }

    suspend fun deleteUploadedTrack(id: String) {
        uploadedTrackDao.deleteUploaded(id)
    }

    fun getArtist(artistId: String): Flow<Result<Artist>> = flow {
        val result = provider.getArtist(artistId)
        emit(result)
    }

    fun getAlbum(albumId: String): Flow<Result<Album>> = flow {
        val result = provider.getAlbum(albumId)
        emit(result)
    }

    fun getPlaylist(playlistId: String): Flow<Result<Playlist>> = flow {
        val local = playlistDao.getPlaylistById(playlistId)
        if (local != null) {
            val items = playlistDao.getItemsForPlaylistSync(playlistId)
            val tracks = items.map { item ->
                Track(
                    id = item.trackId,
                    title = item.title,
                    artist = item.artist,
                    albumTitle = item.albumTitle,
                    durationSeconds = item.durationSeconds,
                    artworkUrl = item.artworkUrl,
                    youtubeVideoId = item.youtubeVideoId
                )
            }
            emit(
                Result.success(
                    Playlist(
                        id = local.id,
                        title = local.name,
                        description = local.description,
                        artworkUrl = local.coverUrl,
                        trackCount = tracks.size,
                        isUserCreated = true,
                        tracks = tracks
                    )
                )
            )
        } else {
            val result = provider.getPlaylist(playlistId)
            emit(result)
        }
    }

    fun getFavorites(): Flow<List<Track>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { entity ->
                Track(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    artistId = entity.artistId,
                    albumTitle = entity.albumTitle,
                    albumId = entity.albumId,
                    durationSeconds = entity.durationSeconds,
                    artworkUrl = entity.artworkUrl,
                    youtubeVideoId = entity.youtubeVideoId,
                    genre = entity.genre,
                    isFavorite = true
                )
            }
        }
    }

    fun isFavorite(trackId: String): Flow<Boolean> {
        return favoriteDao.isFavorite(trackId)
    }

    suspend fun toggleFavorite(track: Track) {
        val isFav = favoriteDao.isFavoriteDirect(track.id)
        if (isFav) {
            favoriteDao.deleteFavorite(track.id)
        } else {
            favoriteDao.insertFavorite(
                FavoriteTrackEntity(
                    id = track.id,
                    title = track.title,
                    artist = track.artist,
                    artistId = track.artistId,
                    albumTitle = track.albumTitle,
                    albumId = track.albumId,
                    durationSeconds = track.durationSeconds,
                    artworkUrl = track.artworkUrl,
                    youtubeVideoId = track.youtubeVideoId,
                    genre = track.genre
                )
            )
        }
    }

    fun getRecentTracks(): Flow<List<Track>> {
        return recentPlaybackDao.getRecent().map { entities ->
            entities.map { entity ->
                Track(
                    id = entity.trackId,
                    title = entity.title,
                    artist = entity.artist,
                    albumTitle = entity.albumTitle,
                    durationSeconds = entity.durationSeconds,
                    artworkUrl = entity.artworkUrl,
                    youtubeVideoId = entity.youtubeVideoId
                )
            }
        }
    }

    suspend fun recordRecent(track: Track) {
        recentPlaybackDao.recordRecent(
            RecentPlaybackEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                albumTitle = track.albumTitle,
                durationSeconds = track.durationSeconds,
                artworkUrl = track.artworkUrl,
                youtubeVideoId = track.youtubeVideoId
            )
        )
        try {
            trackPlayStatDao?.recordPlay(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                albumTitle = track.albumTitle,
                artworkUrl = track.artworkUrl,
                youtubeVideoId = track.youtubeVideoId,
                durationSeconds = track.durationSeconds,
                genre = track.genre
            )
        } catch (_: Exception) {}
    }

    suspend fun recordPlayback(track: Track) {
        recordRecent(track)
    }

    fun getMostPlayedTracks(limit: Int = 100): Flow<List<Track>> {
        return trackPlayStatDao?.getMostPlayed(limit)?.map { entities ->
            entities.map { entity ->
                Track(
                    id = entity.trackId,
                    title = entity.title,
                    artist = entity.artist,
                    albumTitle = entity.albumTitle,
                    artworkUrl = entity.artworkUrl,
                    youtubeVideoId = entity.youtubeVideoId,
                    durationSeconds = entity.durationSeconds,
                    genre = entity.genre,
                    playCount = entity.playCount.toLong(),
                    frequencyHz = entity.frequencyHz
                )
            }
        } ?: flowOf(emptyList())
    }

    fun getLeastPlayedTracks(limit: Int = 100): Flow<List<Track>> {
        return trackPlayStatDao?.getLeastPlayed(limit)?.map { entities ->
            entities.map { entity ->
                Track(
                    id = entity.trackId,
                    title = entity.title,
                    artist = entity.artist,
                    albumTitle = entity.albumTitle,
                    artworkUrl = entity.artworkUrl,
                    youtubeVideoId = entity.youtubeVideoId,
                    durationSeconds = entity.durationSeconds,
                    genre = entity.genre,
                    playCount = entity.playCount.toLong(),
                    frequencyHz = entity.frequencyHz
                )
            }
        } ?: flowOf(emptyList())
    }

    fun getRecentHistory(): Flow<List<Track>> = getRecentTracks()

    suspend fun clearRecentHistory() {
        clearHistory()
    }

    suspend fun clearHistory() {
        recentPlaybackDao.clearAllRecent()
    }

    fun getUserPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                Playlist(
                    id = entity.id,
                    title = entity.name,
                    description = entity.description,
                    artworkUrl = entity.coverUrl,
                    isUserCreated = true
                )
            }
        }
    }

    suspend fun createPlaylist(name: String, description: String = ""): String {
        val id = "pl_user_" + UUID.randomUUID().toString().take(8)
        playlistDao.insertPlaylist(
            PlaylistEntity(
                id = id,
                name = name,
                description = description,
                coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80"
            )
        )
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        val currentCount = playlistDao.getItemsForPlaylistSync(playlistId).size
        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                albumTitle = track.albumTitle,
                durationSeconds = track.durationSeconds,
                artworkUrl = track.artworkUrl,
                youtubeVideoId = track.youtubeVideoId,
                position = currentCount
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removePlaylistItem(playlistId, trackId)
    }

    fun getSearchHistory(): Flow<List<String>> {
        return searchHistoryDao.getSearchHistory().map { list ->
            list.map { it.query }
        }
    }

    suspend fun addSearchQuery(query: String) {
        if (query.isNotBlank()) {
            searchHistoryDao.insertSearch(SearchHistoryEntity(query = query.trim()))
        }
    }

    suspend fun deleteSearchQuery(query: String) {
        searchHistoryDao.deleteSearch(query)
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearSearchHistory()
    }

    private fun extractVideoId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.length == 11 && trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return trimmed
        }
        val pattern = Pattern.compile(
            "(?:https?:\\/\\/)?(?:www\\.|m\\.|music\\.)?(?:youtube\\.com\\/(?:[^\\/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})"
        )
        val matcher = pattern.matcher(trimmed)
        return if (matcher.find()) {
            matcher.group(1) ?: ""
        } else ""
    }

    private fun UploadedTrackEntity.toDomain(): Track {
        return Track(
            id = this.id,
            title = this.title,
            artist = this.artist,
            durationSeconds = this.durationSeconds,
            artworkUrl = this.artworkUrl,
            youtubeVideoId = this.youtubeVideoId,
            localAudioUri = this.localAudioUri,
            genre = this.genre,
            frequencyHz = this.frequencyHz,
            isUploaded = true,
            source = "uploaded"
        )
    }

    private fun DiscoveredTrackEntity.toDomain(): Track {
        return Track(
            id = this.trackId,
            title = this.title,
            artist = this.artist,
            artistId = this.artistId,
            albumTitle = this.albumTitle,
            albumId = this.albumId,
            durationSeconds = this.durationSeconds,
            artworkUrl = this.artworkUrl,
            youtubeVideoId = this.youtubeVideoId,
            genre = this.genre,
            frequencyHz = this.frequencyHz,
            source = this.source
        )
    }

    private fun Track.toDiscoveredEntity(category: String): DiscoveredTrackEntity {
        return DiscoveredTrackEntity(
            trackId = this.id,
            title = this.title,
            artist = this.artist,
            artistId = this.artistId,
            albumTitle = this.albumTitle,
            albumId = this.albumId,
            durationSeconds = this.durationSeconds,
            artworkUrl = this.artworkUrl,
            youtubeVideoId = this.youtubeVideoId,
            genre = this.genre,
            frequencyHz = this.frequencyHz,
            category = category,
            source = this.source,
            discoveredAt = System.currentTimeMillis()
        )
    }

    private fun isLegacyMockTrack(id: String, title: String, artist: String): Boolean {
        val obsoleteIds = setOf(
            "trk_sol_01", "trk_sol_02", "trk_vek_01", "trk_vek_02",
            "trk_lofi_01", "trk_lofi_02", "trk_aeth_01", "trk_aeth_02",
            "trk_kin_01", "trk_kin_02"
        )
        if (id in obsoleteIds) return true
        val t = title.lowercase()
        val a = artist.lowercase()
        if (t.contains("hyper-velocity") || t.contains("cybernetic velocity") || t.contains("solar flare") || t.contains("synaptic discharge") || t.contains("ultraviolet aurora")) return true
        if (a.contains("kinetic pulse") || a.contains("vektor orbit") || a.contains("solaris resonance") || a.contains("aether void")) return true
        return false
    }

    suspend fun getAllCandidateTracks(): List<Track> {
        val base = CuratedFrequencies.allTracks
        val cached = try {
            discoveryCacheDao.getAllDiscoveredSync().map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
        val uploaded = try {
            uploadedTrackDao.getAllUploadedSync().map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
        return (base + cached + uploaded)
            .filterNot { isLegacyMockTrack(it.id, it.title, it.artist) }
            .distinctBy { it.id }
    }

    suspend fun getTracksForGenreFromYouTube(genre: String): List<Track> {
        val ytTracks = if (provider is YouTubeProvider) {
            provider.fetchTracksForGenre(genre)
        } else {
            val query = "$genre hits songs"
            searchWithScope(query, "YOUTUBE_MUSIC").first().getOrNull()?.tracks ?: emptyList()
        }
        val taggedTracks = ytTracks.map { it.copy(genre = genre) }
        if (taggedTracks.isNotEmpty()) {
            try {
                val entities = taggedTracks.map { track ->
                    DiscoveredTrackEntity(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        albumTitle = track.albumTitle,
                        artworkUrl = track.artworkUrl,
                        durationSeconds = track.durationSeconds,
                        genre = track.genre,
                        frequencyHz = track.frequencyHz,
                        category = genre,
                        source = track.source,
                        youtubeVideoId = track.youtubeVideoId
                    )
                }
                discoveryCacheDao.insertAllDiscovered(entities)
            } catch (_: Exception) {}
        }
        return taggedTracks
    }
}
