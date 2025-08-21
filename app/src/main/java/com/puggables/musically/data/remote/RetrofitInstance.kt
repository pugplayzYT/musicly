package com.puggables.musically.data.remote

import com.puggables.musically.MusicallyApplication
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "https://cents-mongolia-difficulties-mortgage.trycloudflare.com/"

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

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
