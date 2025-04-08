package com.example.groclistapp.ui.card

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.example.groclistapp.R
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.data.repository.ShoppingListRepository
import com.example.groclistapp.viewmodel.ShoppingListViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.groclistapp.data.model.ShoppingItem
import com.google.android.material.textfield.TextInputLayout
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch


class UpdateCardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_update_card, container, false)
    }
    private lateinit var viewModel: ShoppingListViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listId = arguments?.getInt("listId") ?: return

        val repository = ShoppingListRepository(
            AppDatabase.getDatabase(requireContext()).shoppingListDao(),
            AppDatabase.getDatabase(requireContext()).shoppingItemDao()
        )

        val factory = ShoppingListViewModel.Factory(requireActivity().application, repository)

        viewModel = ViewModelProvider(this, factory).get(ShoppingListViewModel::class.java)

        val tilTitle = view.findViewById<TextInputLayout>(R.id.tilUpdateCardTitle)
        val tilDescription = view.findViewById<TextInputLayout>(R.id.tilUpdateCardDescription)
        val ivTop = view.findViewById<ImageView>(R.id.ivUpdateCardTop)

        lifecycleScope.launch {
            val list = viewModel.getShoppingListById(listId)
            list?.let {
                tilTitle.editText?.setText(it.name)
                tilDescription.editText?.setText(it.description)
                if (!it.imageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(it.imageUrl)
                        .into(ivTop)
                }
            }
        }

        val chipGroup = view.findViewById<ChipGroup>(R.id.cgUpdateCardItemsContainer)
        chipGroup.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        viewModel.getItemsForList(listId).observe(viewLifecycleOwner) { items ->
            items.forEach { item ->
                val chip = createChip(item.name, item.amount.toString(), chipGroup)
                chipGroup.addView(chip)
            }
        }

        val btnAddItem = view.findViewById<Button>(R.id.btnUpdateCardAddItem)

        btnAddItem.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_item, null)

            val tilName = dialogView.findViewById<TextInputLayout>(R.id.tilDialogItemName)
            val tilAmount = dialogView.findViewById<TextInputLayout>(R.id.tilDialogItemAmount)

            val etName = tilName.editText
            val etAmount = tilAmount.editText

            AlertDialog.Builder(requireContext())
                .setTitle("add item")
                .setView(dialogView)
                .setPositiveButton("add") { _, _ ->
                    val name = etName?.text?.toString()?.trim() ?: ""
                    val amountText = etAmount?.text?.toString()?.trim() ?: ""

                    if (name.isNotEmpty() && amountText.isNotEmpty()) {
                        val amount = amountText.toIntOrNull() ?: 1

                        val chip = createChip(name, amount.toString(), chipGroup)
                        chipGroup.addView(chip)

                        val newItem = ShoppingItem(
                            id = 0,
                            name = name,
                            amount = amount,
                            listId = listId
                        )
                        viewModel.addItem(newItem)
                    }
                }
                .setNegativeButton("cancel", null)
                .show()
        }

        val btnCancel = view.findViewById<Button>(R.id.btnUpdateCardCancel)
        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

    }


    private fun createChip(name: String, amount: String, chipGroup: ChipGroup): Chip {
        val chip = Chip(requireContext())
        chip.text = "$name: $amount"
        chip.isCloseIconVisible = true

        chip.setOnCloseIconClickListener {
            chipGroup.removeView(chip)
        }

        chip.setOnClickListener {
            showEditItemDialog(
                context = requireContext(),
                currentName = name,
                currentAmount = amount
            ) { newName, newAmount ->
                chip.text = "$newName: $newAmount"
            }
        }

        return chip
    }

    fun showEditItemDialog(
        context: Context,
        currentName: String,
        currentAmount: String,
        onUpdate: (String, String) -> Unit
    ) {
        val inputLayout = LayoutInflater.from(context).inflate(R.layout.dialog_item, null)
        val nameInput = inputLayout.findViewById<EditText>(R.id.tilDialogItemName)
        val amountInput = inputLayout.findViewById<EditText>(R.id.tilDialogItemAmount)

        nameInput.setText(currentName)
        amountInput.setText(currentAmount)

        AlertDialog.Builder(context)
            .setTitle("Edit Item")
            .setView(inputLayout)
            .setPositiveButton("Update") { _, _ ->
                val newName = nameInput.text.toString()
                val newAmount = amountInput.text.toString()

                // Call the onUpdate callback with the new values
                if (newName.isNotEmpty() && newAmount.isNotEmpty()) {
                    onUpdate(newName, newAmount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}