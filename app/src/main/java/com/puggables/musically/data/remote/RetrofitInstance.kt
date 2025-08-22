package com.puggables.musically.data.remote

import com.puggables.musically.MusicallyApplication
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val DEFAULT_BASE_URL = "https://cents-mongolia-difficulties-mortgage.trycloudflare.com/"

    // Public property to get the current base URL
    val currentBaseUrl: String
        get() = MusicallyApplication.sessionManager.getBaseUrl() ?: DEFAULT_BASE_URL

    private val logging by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val tok = MusicallyApplication.sessionManager.getToken()
                val req = if (tok != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $tok")
                        .build()
                } else chain.request()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
    }

    // This will now be a mutable property
    var api: ApiService = createApi()
        private set

    private fun createApi(): ApiService {
        return Retrofit.Builder()
            .baseUrl(currentBaseUrl) // Use the public property here
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Call this function to re-initialize the API with the new URL
    fun reinitializeApi() {
        api = createApi()
    }
}
