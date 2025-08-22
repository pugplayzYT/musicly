package com.puggables.musically.data.models

// This model is now much more structured
data class Artist(
    val id: Int,
    val username: String,
    val avatar: String?,
    val avatar_url: String?,
    val albums: List<Album>,
    val singles: List<Song>
)