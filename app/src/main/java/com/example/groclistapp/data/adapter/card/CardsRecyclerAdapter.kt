package com.example.groclistapp.data.adapter.card

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.model.ShoppingList

class CardsRecyclerAdapter(
    private var shoppingLists: MutableList<ShoppingList>
) : RecyclerView.Adapter<CardViewHolder>() {

    var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.cards_list_row, parent, false)
        return CardViewHolder(itemView, listener)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(shoppingLists[position])
    }

    override fun getItemCount(): Int = shoppingLists.size


    fun updateData(newLists: List<ShoppingList>) {
        val diffCallback = ShoppingListDiffCallback(shoppingLists, newLists)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        shoppingLists.clear()
        shoppingLists.addAll(newLists)
        diffResult.dispatchUpdatesTo(this)
    }
}


class ShoppingListDiffCallback(
    private val oldList: List<ShoppingList>,
    private val newList: List<ShoppingList>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
