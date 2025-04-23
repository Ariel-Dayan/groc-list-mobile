package com.example.groclistapp.utils

import android.content.Intent
import android.view.View
import com.example.groclistapp.R

class MessageUtils private constructor() {
    fun shareShoppingListCode(code: String, view: View) {
        val shareText = "Check out the shopping list on ${view.context.getString(R.string.app_name)}!\nCode: $code"

        sendMessage(shareText, view)
    }

    private fun sendMessage(message: String, view: View) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        view.context.startActivity(shareIntent)
    }

    companion object {
        val instance = MessageUtils()
    }
}