package com.example.groclistapp.data.adapter.card

import android.view.View

interface OnItemClickListener {
    fun onItemClick(listId: String)
    fun onShareCodeClick(code: String, itemView: View)
}