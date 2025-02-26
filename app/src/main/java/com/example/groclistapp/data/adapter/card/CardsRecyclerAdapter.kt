package com.example.groclistapp.data.adapter.card

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R

class CardsRecyclerAdapter(private val shoppingCards: MutableList<Any>?) : RecyclerView.Adapter<CardViewHolder>() {
    var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.cards_list_row, parent, false)
        return CardViewHolder(itemView, listener)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(shoppingCards?.get(position))
    }

    override fun getItemCount(): Int = shoppingCards?.size ?: 0
}