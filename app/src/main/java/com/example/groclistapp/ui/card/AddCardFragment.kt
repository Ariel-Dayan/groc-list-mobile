package com.example.groclistapp.ui.card

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.data.repository.ShoppingListRepository
import com.example.groclistapp.viewmodel.ShoppingListViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.fragment.app.setFragmentResult
import androidx.core.os.bundleOf

class AddCardFragment : Fragment() {

    private var listId: Int = -1
    private lateinit var viewModel: ShoppingListViewModel
    private val pendingItems = mutableListOf<ShoppingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_add_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val shoppingListDao = AppDatabase.getDatabase(requireContext()).shoppingListDao()
        val shoppingItemDao = AppDatabase.getDatabase(requireContext()).shoppingItemDao()
        val repository = ShoppingListRepository(shoppingListDao, shoppingItemDao)
        viewModel = ViewModelProvider(
            this,
            ShoppingListViewModel.Factory(requireActivity().application, repository)
        )[ShoppingListViewModel::class.java]

        val tilListName = view.findViewById<TextInputLayout>(R.id.tilAddCardTitle)
        val tilItemName = view.findViewById<TextInputLayout>(R.id.tilAddCardItemName)
        val tilItemAmount = view.findViewById<TextInputLayout>(R.id.tilAddCardItemAmount)
        val chipGroup = view.findViewById<ChipGroup>(R.id.cgAddCardItemsContainer)
        val btnAddItem = view.findViewById<Button>(R.id.btnAddCardAddItem)
        val btnSave = view.findViewById<Button>(R.id.btnAddCardSave)
        val btnCancel = view.findViewById<Button>(R.id.btnAddCardCancel)

        btnAddItem.setOnClickListener {
            val name = tilItemName.editText?.text?.toString()?.trim().orEmpty()
            val amountStr = tilItemAmount.editText?.text?.toString()?.trim().orEmpty()
            val amount = amountStr.toIntOrNull() ?: 0

            if (name.isNotEmpty() && amount > 0) {
                val chip = createChip(name, amountStr, chipGroup)
                chipGroup.addView(chip)
                pendingItems.add(ShoppingItem(name = name, amount = amount, listId = -1))

                tilItemName.editText?.text?.clear()
                tilItemAmount.editText?.text?.clear()
            }
        }

        btnSave.setOnClickListener {
            val listName = tilListName.editText?.text.toString().trim()

            if (listName.isEmpty()) {
                tilListName.error = "The list name cannot be empty"
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val newList = ShoppingList(name = listName)
                val newListId = viewModel.addShoppingList(newList)

                pendingItems.forEach { it.listId = newListId }
                pendingItems.forEach {
                    viewModel.addItem(it)
                    Log.d("AddCardFragment", "🔹 פריט נוסף: ${it.name}, כמות: ${it.amount}, listId: ${it.listId}")
                }

                withContext(Dispatchers.Main) {
                    listId = newListId
                    Log.d("AddCardFragment", "✅ רשימה חדשה נוצרה עם ID: $listId")
                    setFragmentResult("shoppingListUpdated", bundleOf("updated" to true))
                    findNavController().navigateUp()
                }
            }
        }

        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun createChip(name: String, amount: String, chipGroup: ChipGroup): Chip {
        val chip = Chip(requireContext())
        chip.text = "$name: $amount"
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener { chipGroup.removeView(chip) }
        return chip
    }
}
