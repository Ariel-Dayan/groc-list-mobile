package com.example.groclistapp.data.network.jokes

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object JokesRetrofitClient {
    private const val BASE_URL = "https://api.chucknorris.io/jokes/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Converts JSON
            .build()
    }
}