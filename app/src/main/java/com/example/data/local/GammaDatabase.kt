package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteTrackEntity::class,
        RecentPlaybackEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        SearchHistoryEntity::class,
        UserPreferenceEntity::class,
        UploadedTrackEntity::class,
        DiscoveredTrackEntity::class,
        TrackPlayStatEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class GammaDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentPlaybackDao(): RecentPlaybackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun uploadedTrackDao(): UploadedTrackDao
    abstract fun discoveryCacheDao(): DiscoveryCacheDao
    abstract fun trackPlayStatDao(): TrackPlayStatDao

    companion object {
        @Volatile
        private var INSTANCE: GammaDatabase? = null

        fun getInstance(context: Context): GammaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GammaDatabase::class.java,
                    "gamma_music.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
