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
                    updateData: () -> Unit,
                    swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout) {
        updateData()
        cardsRecyclerView?.post {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    companion object {
        val instance = ListUtils()
    }
}