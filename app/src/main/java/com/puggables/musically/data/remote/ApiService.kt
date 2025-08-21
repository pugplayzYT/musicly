package com.puggables.musically.data.remote

import com.puggables.musically.data.models.Artist
import com.puggables.musically.data.models.AuthResponse
import com.puggables.musically.data.models.Song
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("register")
    suspend fun register(@Body params: Map<String, String>): Response<Unit>

    @POST("login")
    suspend fun login(@Body params: Map<String, String>): Response<AuthResponse>

    @GET("songs")
    suspend fun getAllSongs(): Response<List<Song>>

    // upload (server trusts token for artist_id, no genres anymore)
    @Multipart
    @POST("upload")
    suspend fun uploadSong(
        @Part("title") title: RequestBody,
        @Part("album") album: RequestBody,
        @Part audio: MultipartBody.Part,
        @Part image: MultipartBody.Part?
    ): Response<Unit>

    // artist profile & avatar
    @GET("artists/{id}")
    suspend fun getArtist(@Path("id") id: Int): Response<Artist>

    @Multipart
    @POST("artists/me/avatar")
    suspend fun updateMyAvatar(@Part avatar: MultipartBody.Part): Response<Map<String, Any>>

    // edit / delete song
    @Multipart
    @PATCH("songs/{id}")
    suspend fun editSong(
        @Path("id") id: Int,
        @Part("title") title: RequestBody?,
        @Part("album") album: RequestBody?,
        @Part image: MultipartBody.Part?,
        @Part audio: MultipartBody.Part?
    ): Response<Unit>

    @DELETE("songs/{id}")
    suspend fun deleteSong(@Path("id") id: Int): Response<Unit>
}
