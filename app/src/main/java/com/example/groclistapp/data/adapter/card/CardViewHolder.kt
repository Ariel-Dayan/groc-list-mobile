package com.example.groclistapp.data.adapter.card

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.data.model.ShoppingListSummary


class CardViewHolder(itemView: View, private val listener: OnItemClickListener?)
    : RecyclerView.ViewHolder(itemView) {
    private val titleTextView: TextView = itemView.findViewById(R.id.tvCardsListRowTitle)
    private val descriptionTextView: TextView = itemView.findViewById(R.id.tvCardsListRowDescription)
    val creatorTextView: TextView = itemView.findViewById(R.id.tvCardsListRowCreatedByHint)
    private val shareCodeTextView: TextView = itemView.findViewById(R.id.tvCardsListRowSharedCodeValue)
    private val ivTop: ImageView = itemView.findViewById(R.id.ivCardsListRowTop)
    private val shareCodeIcon: ImageView = itemView.findViewById(R.id.ivShareIcon)
    private val imageHandler: ImageHandler = ImageHandler(ivTop, null, null, null)

    fun bind(shoppingList: ShoppingListSummary) {
        titleTextView.text = shoppingList.name
        descriptionTextView.text = shoppingList.description.ifEmpty {
            "No description available"
        }

        shareCodeTextView.text = shoppingList.shareCode.ifEmpty {
            "No share code"
        }

        itemView.setOnClickListener {
            Log.d("CardViewHolder", "Item clicked: ${shoppingList.id}")
            listener?.onItemClick(shoppingList.id)
        }

        shareCodeIcon.setOnClickListener {
            Log.d("CardViewHolder", "Share icon clicked for list: ${shoppingList.id}")
            listener?.onShareCodeClick(shoppingList.shareCode, itemView)
        }

        if (!shoppingList.imageUrl.isNullOrEmpty()) {
            imageHandler.loadImage(shoppingList.imageUrl, R.drawable.shopping_card_placeholder)
        } else {
            ivTop.setImageResource(R.drawable.shopping_card_placeholder)
        }

    }
}


