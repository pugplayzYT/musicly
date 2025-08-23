package com.puggables.musically.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: DownloadedSong)

    @Query("SELECT * FROM downloaded_songs ORDER BY title ASC")
    fun getAll(): Flow<List<DownloadedSong>>

    @Query("SELECT * FROM downloaded_songs WHERE id = :songId")
    suspend fun getById(songId: Int): DownloadedSong?

    @Delete
    suspend fun delete(song: DownloadedSong)
}