package com.example.groclistapp.data.adapter.card

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.model.ShoppingListSummary
import com.example.groclistapp.data.repository.ShoppingItemDao
import com.example.groclistapp.data.repository.ShoppingListDao
import com.example.groclistapp.data.repository.ShoppingListRepository

class CardsRecyclerAdapter(
    private var shoppingLists: MutableList<ShoppingListSummary>,
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao,
    var listener: OnItemClickListener
) : RecyclerView.Adapter<CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.cards_list_row, parent, false)
        return CardViewHolder(itemView, listener)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val shoppingList = shoppingLists[position]

        val repository = ShoppingListRepository(shoppingListDao, shoppingItemDao)
        Log.d("AdapterDebug", "Binding list: ${shoppingList.name}, creatorId=${shoppingList.creatorId}")

        repository.getCreatorName(shoppingList.creatorId) { creatorName ->
            holder.creatorTextView.text = "Created by: $creatorName"
        }

        holder.bind(shoppingList)
    }

    override fun getItemCount(): Int = shoppingLists.size

    fun updateData(newLists: List<ShoppingListSummary>) {
        shoppingLists = newLists.toMutableList()
        notifyDataSetChanged()
    }

    fun setData(newLists: List<ShoppingListSummary>) {
        shoppingLists.clear()
        shoppingLists.addAll(newLists)
        notifyDataSetChanged()
    }


}

class ShoppingListDiffCallback(
    private val oldList: List<ShoppingListSummary>,
    private val newList: List<ShoppingListSummary>
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


