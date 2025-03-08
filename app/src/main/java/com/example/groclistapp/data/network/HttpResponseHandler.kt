package com.example.groclistapp.data.network

import retrofit2.Call

interface HttpResponseHandler<T> {
    fun onComplete(data: T)
    fun onFailure(t: Throwable?, default: T?)
}