package com.example.groclistapp.data.adapter.card

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.model.ShoppingListSummary

class CardsRecyclerAdapter(
    private var shoppingLists: MutableList<ShoppingListSummary>,
    private var listener: OnItemClickListener
) : RecyclerView.Adapter<CardViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.cards_list_row, parent, false)
        return CardViewHolder(itemView, listener)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val shoppingList = shoppingLists[position]

        holder.bind(shoppingList)
    }

    override fun getItemCount(): Int = shoppingLists.size

    fun updateData(newLists: List<ShoppingListSummary>) {
        shoppingLists = newLists.toMutableList()
        notifyDataSetChanged()
    }
}




