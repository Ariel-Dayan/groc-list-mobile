package com.example.groclistapp.data.model

import com.google.gson.annotations.SerializedName

data class JokeResponse(
    val id: String,
    val value: String,
    val categories: List<String>,

    @SerializedName("icon_url")
    val iconUrl: String,

    @SerializedName("url")
    val jokeUrl: String,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String
)