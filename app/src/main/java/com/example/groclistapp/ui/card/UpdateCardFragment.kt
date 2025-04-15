package com.example.groclistapp.ui.card

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import com.example.groclistapp.data.image.ImageHandler
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
import com.example.groclistapp.data.model.ShoppingListSummary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class UpdateCardFragment : Fragment() {

    private lateinit var viewModel: ShoppingListViewModel
    private var currentListSummary: ShoppingListSummary? = null
    private lateinit var imageHandler: ImageHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_update_card, container, false)
    }

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
        val ibGallery = view.findViewById<ImageButton>(R.id.ibUpdateCardUploadImageFromGallery)
        val ibCamera = view.findViewById<ImageButton>(R.id.ibUpdateCardTakePhoto)

        imageHandler = ImageHandler(this, ivTop, ibGallery, ibCamera)

        lifecycleScope.launch {
            currentListSummary = viewModel.getShoppingListById(listId)
            currentListSummary?.let {
                tilTitle.editText?.setText(it.name)
                tilDescription.editText?.setText(it.description)
                if (!it.imageUrl.isNullOrEmpty()) {
                    imageHandler.loadImage(it.imageUrl)
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

        val tilName = view.findViewById<TextInputLayout>(R.id.tilUpdateCardItemName)
        val tilAmount = view.findViewById<TextInputLayout>(R.id.tilUpdateCardItemAmount)
        val btnAddItem = view.findViewById<Button>(R.id.btnUpdateCardAddItem)

        val etName = tilName.editText
        val etAmount = tilAmount.editText

        btnAddItem.setOnClickListener {
            val name = etName?.text?.toString()?.trim().orEmpty()
            val amountText = etAmount?.text?.toString()?.trim().orEmpty()
            val amount = amountText.toIntOrNull() ?: 1

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "please enter an item name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val chip = createChip(name, amount.toString(), chipGroup)
            chipGroup.addView(chip)

            etName?.text?.clear()
            etAmount?.text?.clear()
        }


        val btnCancel = view.findViewById<Button>(R.id.btnUpdateCardCancel)
        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        val btnUpdate = view.findViewById<Button>(R.id.btnUpdateCardUpdate)
        btnUpdate.setOnClickListener {
            val updatedName = tilTitle.editText?.text?.toString()?.trim()
            val updatedDescription = tilDescription.editText?.text?.toString()?.trim()

            if (updatedName.isNullOrEmpty() || updatedDescription.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Name and description cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentListSummary?.let { oldList ->
                val selectedImageUri = imageHandler.selectedImageUri

                val updatedList = oldList.copy(
                    name = updatedName,
                    description = updatedDescription,
                    creatorId = oldList.creatorId
                )
                Log.d("UpdateFragment", "Before image upload, updatedList.imageUrl: ${updatedList.imageUrl}")
                lifecycleScope.launch {
                    if (selectedImageUri != null) {
                        viewModel.uploadImageAndUpdateList(selectedImageUri, updatedList) { newUpdatedList ->
                            lifecycleScope.launch {
                                updateItemsAndFinish(listId, newUpdatedList)
                            }
                        }

                    } else {
                        updateItemsAndFinish(listId, updatedList)
                    }
                }


            } ?: run {
                Log.e("UpdateTest", "currentListSummary היה null")
            }
        }

    val btnDelete = view.findViewById<Button>(R.id.btnUpdateCardDelete)
    btnDelete.setOnClickListener {
        currentListSummary?.let { list ->
            AlertDialog.Builder(requireContext())
                .setTitle("Delete List")
                .setMessage("Are you sure you want to delete this list?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.deleteShoppingList(list)
                    Toast.makeText(requireContext(), "List deleted successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
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
        val nameInputLayout = inputLayout.findViewById<TextInputLayout>(R.id.tilDialogItemName)
        val amountInputLayout = inputLayout.findViewById<TextInputLayout>(R.id.tilDialogItemAmount)

        val nameInput = nameInputLayout.editText
        val amountInput = amountInputLayout.editText

        nameInput?.setText(currentName)
        amountInput?.setText(currentAmount)

        AlertDialog.Builder(context)
            .setTitle("Edit Item")
            .setView(inputLayout)
            .setPositiveButton("Update") { _, _ ->
                val newName = nameInput?.text.toString()
                val newAmount = amountInput?.text.toString()
                if (newName.isNotEmpty() && newAmount.isNotEmpty()) {
                    onUpdate(newName, newAmount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun extractItemsFromChips(chipGroup: ChipGroup, listId: Int): List<ShoppingItem> {
        val items = mutableListOf<ShoppingItem>()

        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            val text = chip.text.toString()

            val parts = text.split(":")
            if (parts.size == 2) {
                val name = parts[0].trim()
                val amount = parts[1].trim().toIntOrNull() ?: 1
                val item = ShoppingItem(id = 0, name = name, amount = amount, listId = listId)
                items.add(item)
            }
        }

        return items
    }

    private suspend fun updateItemsAndFinish(listId: Int, updatedList: ShoppingListSummary) {
        viewModel.deleteAllItemsForListNow(listId)


        val newItems = extractItemsFromChips(
            requireView().findViewById(R.id.cgUpdateCardItemsContainer),
            listId
        )

        for (item in newItems) {
            viewModel.addItemSuspend(item)
        }

        viewModel.updateShoppingList(updatedList)

        Toast.makeText(requireContext(), "List updated successfully", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }



}
