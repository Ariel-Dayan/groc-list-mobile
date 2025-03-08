package com.example.groclistapp.data.network.jokes

import com.example.groclistapp.data.model.JokeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface JokesApiService {
    @GET("random")
    fun getRandomJokeByCategory(
        @Query("category") category: String
    ): Call<JokeResponse>
}