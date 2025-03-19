package com.example.groclistapp.data.adapter.card

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
    private val shoppingItemDao: ShoppingItemDao
) : RecyclerView.Adapter<CardViewHolder>() {

    var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.cards_list_row, parent, false)
        return CardViewHolder(itemView, listener)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val shoppingList = shoppingLists[position]

        // קבלת שם היוצר ועדכון ה-TextView בתוך ה-Adapter
        val repository = ShoppingListRepository(shoppingListDao, shoppingItemDao)
        repository.getCreatorName(shoppingList.creatorId) { creatorName ->
            holder.creatorTextView.text = "Created by: $creatorName"
        }

        // קריאה ל-bind() בלי repository
        holder.bind(shoppingList)
    }



    override fun getItemCount(): Int = shoppingLists.size

    fun updateData(newLists: List<ShoppingListSummary>) {
        val diffCallback = ShoppingListDiffCallback(shoppingLists, newLists)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        shoppingLists.clear()
        shoppingLists.addAll(newLists)
        diffResult.dispatchUpdatesTo(this)
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

