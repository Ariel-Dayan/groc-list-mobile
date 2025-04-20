package com.example.groclistapp.utils

import android.widget.TextView
import com.example.groclistapp.data.model.ShoppingListSummary

class ListUtils private constructor() {
    fun toggleNoCardListsMessage(textView: TextView, shoppingList: List<ShoppingListSummary>?) {
        if (shoppingList.isNullOrEmpty()) {
            textView.visibility = TextView.VISIBLE
        } else {
            textView.visibility = TextView.GONE
        }
    }

    companion object {
        val instance = ListUtils()
    }
}