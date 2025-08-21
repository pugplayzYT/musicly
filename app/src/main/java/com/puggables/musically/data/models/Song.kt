package com.puggables.musically.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val image: String,
    val filepath: String,
    @SerializedName("artist_id") val artistId: Int? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("stream_url") val streamUrl: String? = null
) : Parcelable
