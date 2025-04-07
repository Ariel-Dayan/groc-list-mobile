package com.example.groclistapp.data.adapter.card

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.model.ShoppingListSummary


class CardViewHolder(itemView: View, private val listener: OnItemClickListener?)
    : RecyclerView.ViewHolder(itemView) {

    private val titleTextView: TextView = itemView.findViewById(R.id.tvCardsListRowTitle)
    private val descriptionTextView: TextView = itemView.findViewById(R.id.tvCardsListRowDescription)
    val creatorTextView: TextView = itemView.findViewById(R.id.tvCardsListRowCreatedByHint)
    private val shareCodeTextView: TextView = itemView.findViewById(R.id.tvCardsListRowSharedCodeValue) // ✅ ודא שזה ה-ID הנכון מ-XML
    val ivTop: ImageView = itemView.findViewById(R.id.ivCardsListRowTop)

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
            Log.d("CardViewHolder", "Item clicked: ${shoppingList.id}")
            listener?.onItemClick(shoppingList.id)
        }

        if (!shoppingList.imageUrl.isNullOrEmpty()) {
            Glide.with(itemView.context)
                .load(shoppingList.imageUrl)
                .placeholder(R.drawable.shopping_card_placeholder)
                .into(ivTop)
        } else {
            ivTop.setImageResource(R.drawable.shopping_card_placeholder)
        }

    }
}


