package com.example.groclistapp.utils

import kotlin.random.Random

class CardUtils private constructor() {
    fun generateShareCode(): String {
        val timestamp = System.currentTimeMillis() / 1000
        val salt = Random.nextInt(0, 100)
        val combined = timestamp * 100 + salt
        return combined.toString(36).uppercase()
    }

    companion object {
        val instance = CardUtils()
    }
}