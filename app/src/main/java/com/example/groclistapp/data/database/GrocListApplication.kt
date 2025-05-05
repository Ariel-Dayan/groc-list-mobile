package com.example.groclistapp.data.database

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

class GrocListApplication: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null

        fun getMyContext(): Context {
            return context ?: throw IllegalStateException("Application context is not initialized.")
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}