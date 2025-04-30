package com.example.groclistapp.utils

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.data.model.ShoppingListSummary

class ListUtils private constructor() {
    fun toggleNoCardListsMessage(textView: TextView, shoppingList: List<ShoppingListSummary>?) {
        if (shoppingList.isNullOrEmpty()) {
            textView.visibility = TextView.VISIBLE
        } else {
            textView.visibility = TextView.GONE
        }
    }

     fun refreshData(cardsRecyclerView: RecyclerView?,
                    updateData: (List<ShoppingListSummary>) -> Unit,
                    newData: List<ShoppingListSummary>?,
                    swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout) {
        if (newData != null) {
            updateData(newData)
            cardsRecyclerView?.post {
                swipeRefreshLayout.isRefreshing = false
            }
        } else {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    companion object {
        val instance = ListUtils()
    }
}