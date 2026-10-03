package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val albumTitle: String,
    val albumId: String,
    val durationSeconds: Int,
    val artworkUrl: String,
    val youtubeVideoId: String,
    val genre: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recent_playback",
    indices = [Index(value = ["trackId"], unique = false)]
)
data class RecentPlaybackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val title: String,
    val artist: String,
    val albumTitle: String,
    val durationSeconds: Int,
    val artworkUrl: String,
    val youtubeVideoId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverUrl: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"]), Index(value = ["playlistId", "trackId"])]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val albumTitle: String,
    val durationSeconds: Int,
    val artworkUrl: String,
    val youtubeVideoId: String,
    val position: Int
)

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["query"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(
    tableName = "uploaded_tracks",
    indices = [Index(value = ["title"]), Index(value = ["artist"])]
)
data class UploadedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val artworkUrl: String,
    val youtubeVideoId: String,
    val localAudioUri: String,
    val genre: String,
    val frequencyHz: Int,
    val uploadedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "discovered_tracks",
    indices = [
        Index(value = ["trackId"], unique = true),
        Index(value = ["discoveredAt"]),
        Index(value = ["category"])
    ]
)
data class DiscoveredTrackEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val albumTitle: String = "",
    val albumId: String = "",
    val durationSeconds: Int = 180,
    val artworkUrl: String = "",
    val youtubeVideoId: String = "",
    val genre: String = "Frequency",
    val frequencyHz: Int = 432,
    val category: String = "Trending",
    val source: String = "youtube",
    val discoveredAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "track_play_stats",
    indices = [
        Index(value = ["playCount"]),
        Index(value = ["lastPlayedAt"])
    ]
)
data class TrackPlayStatEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val albumTitle: String = "",
    val artworkUrl: String = "",
    val youtubeVideoId: String = "",
    val durationSeconds: Int = 0,
    val genre: String = "Music",
    val frequencyHz: Int = 432,
    val playCount: Int = 1,
    val lastPlayedAt: Long = System.currentTimeMillis()
)
