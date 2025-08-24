// In pugplayzyt/musicly/musicly-b0a4421341799e1dd9ed032a85c388a3db9c0c20/app/src/main/java/com/puggables/musically/data/remote/ApiService.kt

package com.puggables.musically.data.remote

import com.puggables.musically.data.models.Album
import com.puggables.musically.data.models.Artist
import com.puggables.musically.data.models.AuthResponse
import com.puggables.musically.data.models.Song
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ... (All your existing routes like login, register, etc.)
    @POST("register")
    suspend fun register(@Body params: Map<String, String>): Response<Unit>
    @POST("login")
    suspend fun login(@Body params: Map<String, String>): Response<AuthResponse>
    @GET("songs")
    suspend fun getAllSongs(): Response<List<Song>>
    @GET("songs/search")
    suspend fun searchSongs(@Query("q") query: String): Response<List<Song>>
    @Multipart
    @POST("upload")
    suspend fun uploadSong(@Part("title") title: RequestBody, @Part("album_id") albumId: RequestBody?, @Part audio: MultipartBody.Part, @Part image: MultipartBody.Part?): Response<Unit>
    @DELETE("songs/{id}")
    suspend fun deleteSong(@Path("id") id: Int): Response<Unit>
    @GET("artists/{id}")
    suspend fun getArtist(@Path("id") id: Int): Response<Artist>
    @Multipart
    @POST("artists/me/avatar")
    suspend fun updateMyAvatar(@Part avatar: MultipartBody.Part): Response<Map<String, Any>>
    @GET("artists/me/albums")
    suspend fun getMyAlbums(): Response<List<Album>>
    @Multipart
    @POST("albums")
    suspend fun createAlbum(@Part("title") title: RequestBody, @Part coverImage: MultipartBody.Part?): Response<Album>
    @DELETE("albums/{id}")
    suspend fun deleteAlbum(@Path("id") id: Int): Response<Unit>
    @POST("api/update-url")
    suspend fun updateServerUrl(@Body params: Map<String, String>): Response<Unit>

    // --- PRO ROUTES ---
    @POST("pro/generate-qr-token")
    suspend fun generateProQrToken(): Response<Map<String, String>>

    @POST("pro/activate")
    suspend fun activatePro(@Body params: Map<String, String>): Response<Map<String, String>>

    // --- THIS IS THE MISSING FUNCTION ---
    @GET("version")
    suspend fun getVersionInfo(): Response<Map<String, Any>>
}