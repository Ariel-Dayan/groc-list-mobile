package com.example.groclistapp.ui.card

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.google.android.material.textfield.TextInputLayout
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingListWithItems
import com.example.groclistapp.utils.DialogUtils
import com.example.groclistapp.utils.InputUtils
import com.example.groclistapp.utils.ItemUtils
import kotlinx.coroutines.launch

class UpdateCardFragment : Fragment() {
    private lateinit var viewModel: ShoppingListViewModel
    private var currentList: ShoppingListWithItems? = null
    private lateinit var imageHandler: ImageHandler
    private lateinit var chipGroup: ChipGroup
    private lateinit var progressBar: ProgressBar
    private val itemUtils = ItemUtils.instance
    private val inputUtils = InputUtils.instance
    private val dialogUtils = DialogUtils.instance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_update_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listId = arguments?.getString("listId") ?: return

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
        val tilName = view.findViewById<TextInputLayout>(R.id.tilUpdateCardItemName)
        val tilAmount = view.findViewById<TextInputLayout>(R.id.tilUpdateCardItemAmount)
        val btnAddItem = view.findViewById<Button>(R.id.btnUpdateCardAddItem)
        progressBar = view.findViewById(R.id.pbUpdateCardSpinner)

        inputUtils.addCleanErrorMessageOnInputListener(tilTitle)
        inputUtils.addCleanErrorMessageOnInputListener(tilDescription)

        imageHandler = ImageHandler(ivTop, this, ibGallery, ibCamera)

        progressBar.visibility = View.VISIBLE

        chipGroup = view.findViewById(R.id.cgUpdateCardItemsContainer)
        chipGroup.layoutDirection = View.LAYOUT_DIRECTION_LOCALE

        lifecycleScope.launch {
            currentList = viewModel.getShoppingListById(listId)
            currentList?.let {
                tilTitle.editText?.setText(it.shoppingList.name)
                tilDescription.editText?.setText(it.shoppingList.description)
                if (!it.shoppingList.imageUrl.isNullOrEmpty()) {
                    imageHandler.loadImage(it.shoppingList.imageUrl, R.drawable.shopping_card_default)
                }
                if (it.items.isNotEmpty()) {
                    chipGroup.removeAllViews()
                    it.items.forEach { item ->
                        val chip = createChip(item.name, item.amount.toString(), chipGroup, listId)
                        chipGroup.addView(chip)
                    }
                }

                progressBar.visibility = View.GONE
            }
        }

        val etName = tilName.editText
        val etAmount = tilAmount.editText

        btnAddItem.setOnClickListener {
            val name = etName?.text?.toString()?.trim().orEmpty()
            val amountText = etAmount?.text?.toString()?.trim().orEmpty()
            val previousItems = itemUtils.extractItemsFromChips(chipGroup, listId)

            val nameError = itemUtils.validateName(name, previousItems.map { it.name })

            if (nameError != null) {
                Toast.makeText(requireContext(), "Invalid item name: $nameError", Toast.LENGTH_SHORT).show()
                Log.d("UpdateCardFragment", "Invalid item input: name='$name', nameError='$nameError'")
                return@setOnClickListener
            }

            val (amount, amountError) = itemUtils.validateAmount(amountText)

            if (amountError != null) {
                Toast.makeText(requireContext(), "Invalid item amount: $amountError", Toast.LENGTH_SHORT).show()
                Log.d("UpdateCardFragment", "Invalid item input: name='$amount', amountError='$amountError'")
                return@setOnClickListener
            }

            val chip = createChip(name, (amount ?: 0).toString(), chipGroup, listId)
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
            var isValid = true

            if (updatedName.isNullOrEmpty()) {
                tilTitle.error = "Name cannot be empty"
                isValid = false
            }

            if (updatedDescription.isNullOrEmpty()) {
                tilDescription.error = "Description cannot be empty"
                isValid = false
            }

            if (!isValid) {
                return@setOnClickListener
            }

            currentList?.shoppingList?.let { oldList ->
                progressBar.visibility = View.VISIBLE

                val selectedImageUri = imageHandler.selectedImageUri

                val updatedList = oldList.copy(
                    name = updatedName ?: "",
                    description = updatedDescription?: "",
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
                Log.e("UpdateTest", "currentList היה null")
            }
        }

    val btnDelete = view.findViewById<Button>(R.id.btnUpdateCardDelete)
    btnDelete.setOnClickListener {
        currentList?.shoppingList?.let { list ->
            AlertDialog.Builder(requireContext())
                .setTitle("Delete List")
                .setMessage("Are you sure you want to delete this list?")
                .setPositiveButton("Yes") { _, _ ->
                    progressBar.visibility = View.VISIBLE
                    viewModel.deleteShoppingList(list)
                    viewModel.deleteAllItemsForList(list.id)
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "List deleted successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

}

    private fun createChip(name: String, amount: String, chipGroup: ChipGroup, listId: String): Chip {
        val chip = Chip(requireContext())
        chip.text = itemUtils.createItemChipText(name, amount)
        chip.isCloseIconVisible = true

        chip.setOnCloseIconClickListener {
            chipGroup.removeView(chip)
        }


        chip.setOnClickListener {
            val updatedItemInfo = itemUtils.parseItemChipText(chip.text.toString())

            dialogUtils.showEditItemDialog(
                context = requireContext(),
                parent = view as? ViewGroup,
                currentName = updatedItemInfo.first,
                currentAmount = updatedItemInfo.second.toString(),
                existingNames = itemUtils.extractItemsFromChips(chipGroup, listId).map { it.name },
            ) { newName, newAmount ->
                chip.text = itemUtils.createItemChipText(newName, newAmount.toString())
            }
        }

        return chip
    }

    private suspend fun updateItemsAndFinish(listId: String, updatedList: ShoppingList) {
        viewModel.deleteAllItemsForListNow(listId)

        val newItems = itemUtils.extractItemsFromChips(
            chipGroup,
            listId
        )

        viewModel.addItems(newItems)
        viewModel.updateShoppingList(updatedList)

        progressBar.visibility = View.GONE
        Toast.makeText(requireContext(), "List updated successfully", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }



}
