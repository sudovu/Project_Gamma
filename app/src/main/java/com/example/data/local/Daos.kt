package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :trackId)")
    fun isFavorite(trackId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :trackId)")
    suspend fun isFavoriteDirect(trackId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE id = :trackId")
    suspend fun deleteFavorite(trackId: String)
}

@Dao
interface RecentPlaybackDao {
    @Query("SELECT * FROM recent_playback ORDER BY playedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<RecentPlaybackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(entity: RecentPlaybackEntity)

    @Query("DELETE FROM recent_playback WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: String)

    @Transaction
    suspend fun recordRecent(entity: RecentPlaybackEntity) {
        deleteByTrackId(entity.trackId)
        insertRecent(entity)
    }

    @Query("DELETE FROM recent_playback")
    suspend fun clearAllRecent()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getItemsForPlaylist(playlistId: String): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getItemsForPlaylistSync(playlistId: String): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removePlaylistItem(playlistId: String, trackId: String)

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    fun getItemCount(playlistId: String): Flow<Int>
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun getSearchHistory(limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}

@Dao
interface UserPreferenceDao {
    @Query("SELECT value FROM user_preferences WHERE `key` = :key")
    fun getPreference(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(entity: UserPreferenceEntity)
}

@Dao
interface UploadedTrackDao {
    @Query("SELECT * FROM uploaded_tracks ORDER BY uploadedAt DESC")
    fun getAllUploaded(): Flow<List<UploadedTrackEntity>>

    @Query("SELECT * FROM uploaded_tracks ORDER BY uploadedAt DESC")
    suspend fun getAllUploadedSync(): List<UploadedTrackEntity>

    @Query("SELECT * FROM uploaded_tracks WHERE id = :id")
    suspend fun getUploadedById(id: String): UploadedTrackEntity?

    @Query("SELECT * FROM uploaded_tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%' ORDER BY uploadedAt DESC")
    fun searchUploaded(query: String): Flow<List<UploadedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploaded(track: UploadedTrackEntity)

    @Query("DELETE FROM uploaded_tracks WHERE id = :id")
    suspend fun deleteUploaded(id: String)
}

@Dao
interface DiscoveryCacheDao {
    @Query("SELECT * FROM discovered_tracks ORDER BY discoveredAt DESC")
    fun getDiscoveryHistory(): Flow<List<DiscoveredTrackEntity>>

    fun getAllDiscovered(): Flow<List<DiscoveredTrackEntity>> = getDiscoveryHistory()

    @Query("SELECT * FROM discovered_tracks ORDER BY discoveredAt DESC LIMIT :limit")
    fun getRecentDiscovered(limit: Int = 50): Flow<List<DiscoveredTrackEntity>>

    @Query("SELECT * FROM discovered_tracks ORDER BY discoveredAt DESC")
    suspend fun getAllDiscoveredSync(): List<DiscoveredTrackEntity>

    @Query("SELECT * FROM discovered_tracks WHERE category = :category ORDER BY discoveredAt DESC")
    suspend fun getDiscoveredByCategory(category: String): List<DiscoveredTrackEntity>

    @Query("SELECT * FROM discovered_tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%' ORDER BY discoveredAt DESC")
    suspend fun searchCached(query: String): List<DiscoveredTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscovered(track: DiscoveredTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDiscovered(tracks: List<DiscoveredTrackEntity>)

    @Query("DELETE FROM discovered_tracks WHERE trackId = :trackId")
    suspend fun deleteDiscovered(trackId: String)

    @Query("DELETE FROM discovered_tracks")
    suspend fun clearDiscoveryCache()

    @Query("SELECT COUNT(*) FROM discovered_tracks")
    fun getDiscoveredCount(): Flow<Int>
}

@Dao
interface TrackPlayStatDao {
    @Query("SELECT * FROM track_play_stats ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
    fun getMostPlayed(limit: Int = 100): Flow<List<TrackPlayStatEntity>>

    @Query("SELECT * FROM track_play_stats ORDER BY playCount ASC, lastPlayedAt DESC LIMIT :limit")
    fun getLeastPlayed(limit: Int = 100): Flow<List<TrackPlayStatEntity>>

    @Query("SELECT * FROM track_play_stats WHERE trackId = :trackId LIMIT 1")
    suspend fun getStat(trackId: String): TrackPlayStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: TrackPlayStatEntity)

    @Query("UPDATE track_play_stats SET playCount = playCount + 1, lastPlayedAt = :now WHERE trackId = :trackId")
    suspend fun incrementPlayCount(trackId: String, now: Long = System.currentTimeMillis()): Int

    @Transaction
    suspend fun recordPlay(
        trackId: String,
        title: String,
        artist: String,
        albumTitle: String = "",
        artworkUrl: String = "",
        youtubeVideoId: String = "",
        durationSeconds: Int = 0,
        genre: String = "Music"
    ) {
        val updated = incrementPlayCount(trackId)
        if (updated == 0) {
            insertOrUpdate(
                TrackPlayStatEntity(
                    trackId = trackId,
                    title = title,
                    artist = artist,
                    albumTitle = albumTitle,
                    artworkUrl = artworkUrl,
                    youtubeVideoId = youtubeVideoId,
                    durationSeconds = durationSeconds,
                    genre = genre,
                    playCount = 1,
                    lastPlayedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
