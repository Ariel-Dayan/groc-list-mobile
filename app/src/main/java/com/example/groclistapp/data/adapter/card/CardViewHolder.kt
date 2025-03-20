package com.example.groclistapp.data.adapter.card

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.model.ShoppingListSummary


class CardViewHolder(itemView: View, private val listener: OnItemClickListener?)
    : RecyclerView.ViewHolder(itemView) {

    private val titleTextView: TextView = itemView.findViewById(R.id.tvCardsListRowTitle)
    private val descriptionTextView: TextView = itemView.findViewById(R.id.tvCardsListRowDescription)
    val creatorTextView: TextView = itemView.findViewById(R.id.tvCardsListRowCreatedByHint)
    private val shareCodeTextView: TextView = itemView.findViewById(R.id.tvCardsListRowSharedCodeValue) // ✅ ודא שזה ה-ID הנכון מ-XML

    fun bind(shoppingList: ShoppingListSummary) {
        titleTextView.text = shoppingList.name
        descriptionTextView.text = if (!shoppingList.description.isNullOrEmpty()) {
            shoppingList.description
        } else {
            "No description available"
        }




        shareCodeTextView.text = if (!shoppingList.shareCode.isNullOrEmpty()) {
            shoppingList.shareCode
        } else {
            "No share code"
        }

        itemView.setOnClickListener {
            listener?.onItemClick(shoppingList)
        }
    }
}


