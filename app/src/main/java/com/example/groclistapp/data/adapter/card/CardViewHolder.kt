package com.example.groclistapp.data.adapter.card

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.model.ShoppingList

class CardViewHolder(itemView: View, private val listener: OnItemClickListener?)
    : RecyclerView.ViewHolder(itemView) {

    private val titleTextView: TextView = itemView.findViewById(R.id.tvCardsListRowTitle)
    private val descriptionTextView: TextView = itemView.findViewById(R.id.tvCardsListRowDescription)

    fun bind(shoppingList: ShoppingList) {
        titleTextView.text = shoppingList.name
        descriptionTextView.text = "Items: ${shoppingList.items.size}"


        itemView.setOnClickListener {
            listener?.onItemClick(shoppingList)
        }
    }
}


