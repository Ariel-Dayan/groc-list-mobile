package com.example.groclistapp.data.database

import android.app.Application
import android.content.Context

class GrocListApplication: Application() {
    companion object {
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