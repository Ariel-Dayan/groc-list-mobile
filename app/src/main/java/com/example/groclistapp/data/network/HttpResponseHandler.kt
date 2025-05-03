package com.example.groclistapp.data.network

interface HttpResponseHandler<T> {
    fun onComplete(data: T)
    fun onFailure(t: Throwable?, default: T?)
}