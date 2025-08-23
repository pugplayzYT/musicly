package com.puggables.musically.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_songs")
data class DownloadedSong(
    @PrimaryKey val id: Int, // The original song ID from the server
    val title: String,
    val artist: String,
    val album: String,
    val duration: Float,
    val remoteImageUrl: String,
    val localAudioPath: String, // Path to the downloaded MP3
    val localImagePath: String? // Path to the downloaded image
)