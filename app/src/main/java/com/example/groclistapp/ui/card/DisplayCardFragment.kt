package com.example.groclistapp.ui.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.groclistapp.R
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.data.repository.ShoppingItemDao
import com.example.groclistapp.data.repository.ShoppingListDao
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController


class DisplayCardFragment : Fragment() {
    private var listId: Int = -1
    private lateinit var shoppingListDao: ShoppingListDao
    private lateinit var shoppingItemDao: ShoppingItemDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listId = arguments?.getInt("listId") ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_display_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shoppingListDao = AppDatabase.getDatabase(requireContext()).shoppingListDao()
        shoppingItemDao = AppDatabase.getDatabase(requireContext()).shoppingItemDao()

        if (listId == -1) {
            Toast.makeText(requireContext(), "Invalid list ID", Toast.LENGTH_SHORT).show()
            return
        }

        val chipGroup = view.findViewById<ChipGroup>(R.id.cgDisplayCardItemsContainer)
        chipGroup.layoutDirection = View.LAYOUT_DIRECTION_LOCALE

        lifecycleScope.launch {
            val list = shoppingListDao.getListById(listId)
            val items = shoppingItemDao.getItemsForListNow(listId)



            list?.let {
                view.findViewById<TextView>(R.id.tvDisplayCardTitle).text = it.name
                view.findViewById<TextView>(R.id.tvDisplayCardDescription).text = it.description

                val imageView = view.findViewById<ImageView>(R.id.ivDisplayCardTop)
                Glide.with(requireContext())
                    .load(it.imageUrl)
                    .placeholder(R.drawable.shopping_card_placeholder)
                    .into(imageView)
            }

            chipGroup.removeAllViews()
            for (item in items) {
                chipGroup.addView(createChip(item.name, item.amount.toString(), chipGroup))
            }
        }

        val deleteButton = view.findViewById<View>(R.id.btnDisplayCardRemove)

        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                shoppingItemDao.deleteItemsByListId(listId)

                // מחזירים את הרשימה המקורית מה-DB למחיקה
                val list = shoppingListDao.getListById(listId)
                if (list != null) {
                    // המרה ל- ShoppingList מתוך ShoppingListSummary
                    val shoppingList = ShoppingList(
                        id = list.id,
                        name = list.name,
                        description = list.description,
                        creatorId = list.creatorId,
                        shareCode = list.shareCode,
                        imageUrl = list.imageUrl
                    )
                    shoppingListDao.deleteShoppingList(shoppingList)
                }

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "List deleted", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }

    }

    private fun createChip(name: String, amount: String, chipGroup: ChipGroup): Chip {
        val chip = Chip(requireContext())
        chip.text = "$name: $amount"
        chip.isCloseIconVisible = false
        return chip
    }
}
