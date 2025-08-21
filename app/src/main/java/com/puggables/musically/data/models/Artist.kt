package com.puggables.musically.data.models

data class Artist(
    val id: Int,
    val username: String,
    val avatar: String?,
    val avatar_url: String?,
    val songs: List<Song>
)
