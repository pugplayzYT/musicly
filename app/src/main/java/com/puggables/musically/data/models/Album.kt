package com.puggables.musically.data.models

import com.google.gson.annotations.SerializedName

data class Album(
    val id: Int,
    val title: String,
    @SerializedName("artist_id") val artistId: Int,
    @SerializedName("cover_image_url") val coverImageUrl: String?,
    val songs: List<Song>? // Albums from the artist profile will include their songs
) {
    override fun toString(): String {
        return title
    }
}