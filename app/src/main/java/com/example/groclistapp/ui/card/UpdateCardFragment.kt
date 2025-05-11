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
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.database.schema.ShoppingListWithItems
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.utils.DialogUtils
import com.example.groclistapp.utils.InputUtils
import com.example.groclistapp.utils.ItemUtils
import com.example.groclistapp.viewmodel.ShoppingListViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class UpdateCardFragment : Fragment() {
    private lateinit var viewModel: ShoppingListViewModel
    private var currentList: ShoppingListWithItems? = null
    private lateinit var imageHandler: ImageHandler
    private lateinit var chipGroup: ChipGroup
    private lateinit var progressBar: ProgressBar
    private lateinit var tilTitle: TextInputLayout
    private lateinit var tilDescription: TextInputLayout
    private lateinit var tilName: TextInputLayout
    private lateinit var tilAmount: TextInputLayout
    private lateinit var ivTop: ImageView
    private lateinit var ibGallery: ImageButton
    private lateinit var ibCamera: ImageButton
    private lateinit var btnAddItem: Button
    private lateinit var btnCancel: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button

    private val itemUtils = ItemUtils.instance
    private val inputUtils = InputUtils.instance
    private val dialogUtils = DialogUtils.instance
    private lateinit var listId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_update_card, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInitialState(view)
        setupObservers()
        setupClickListeners()
        loadShoppingList()
    }

    private fun setupInitialState(view: View) {
        viewModel = ViewModelProvider(this)[ShoppingListViewModel::class.java]
        listId = UpdateCardFragmentArgs.fromBundle(requireArguments()).listId
        initializeViews(view)
        setupInputValidation()
        progressBar.visibility = View.VISIBLE
        imageHandler = ImageHandler(ivTop, this, ibGallery, ibCamera)
    }

    private fun initializeViews(view: View) {
        with(view) {
            tilTitle = findViewById(R.id.tilUpdateCardTitle)
            tilDescription = findViewById(R.id.tilUpdateCardDescription)
            ivTop = findViewById(R.id.ivUpdateCardTop)
            ibGallery = findViewById(R.id.ibUpdateCardUploadImageFromGallery)
            ibCamera = findViewById(R.id.ibUpdateCardTakePhoto)
            tilName = findViewById(R.id.tilUpdateCardItemName)
            tilAmount = findViewById(R.id.tilUpdateCardItemAmount)
            btnAddItem = findViewById(R.id.btnUpdateCardAddItem)
            btnCancel = findViewById(R.id.btnUpdateCardCancel)
            btnUpdate = findViewById(R.id.btnUpdateCardUpdate)
            btnDelete = findViewById(R.id.btnUpdateCardDelete)
            progressBar = findViewById(R.id.pbUpdateCardSpinner)
            chipGroup = findViewById(R.id.cgUpdateCardItemsContainer)
        }
        chipGroup.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
    }

    private fun setupInputValidation() {
        inputUtils.apply {
            addCleanErrorMessageOnInputListener(tilTitle)
            addCleanErrorMessageOnInputListener(tilDescription)
        }
    }

    private fun setupObservers() {
        viewModel.currentList.observe(viewLifecycleOwner) { list ->
            updateUIWithList(list)
        }

        viewModel.deleteStatus.observe(viewLifecycleOwner) { isDeleted ->
            handleDeleteStatus(isDeleted)
        }
    }

    private fun updateUIWithList(list: ShoppingListWithItems?) {
        list?.let {
            with(it.shoppingList) {
                tilTitle.editText?.setText(name)
                tilDescription.editText?.setText(description)
                imageUrl?.let { url ->
                    imageHandler.loadImage(url, R.drawable.shopping_card_default)
                }
            }
            updateChipGroup(it.items)
        }
        progressBar.visibility = View.GONE
    }

    private fun updateChipGroup(items: List<com.example.groclistapp.data.database.schema.ShoppingItem>) {
        chipGroup.removeAllViews()
        items.forEach { item ->
            val chip = createChip(item.name, item.amount.toString(), chipGroup)
            chipGroup.addView(chip)
        }
    }

    private fun setupClickListeners() {
        setupAddItemButton()
        setupUpdateButton()
        setupCancelButton()
        setupDeleteButton()
    }

    private fun setupAddItemButton() {
        btnAddItem.setOnClickListener {
            handleAddItem()
        }
    }

    private fun handleAddItem() {
        val name = tilName.editText?.text?.toString()?.trim().orEmpty()
        val amountText = tilAmount.editText?.text?.toString()?.trim().orEmpty()
        val previousItems = itemUtils.extractItemsFromChips(chipGroup, listId)

        val nameError = itemUtils.validateName(name, previousItems.map { it.name })
        if (nameError != null) {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_item_name_with_detail, nameError), Toast.LENGTH_SHORT).show()
            return
        }

        val (amount, amountError) = itemUtils.validateAmount(amountText)
        if (amountError != null) {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_item_amount_with_detail, amountError), Toast.LENGTH_SHORT).show()
            return
        }

        addChipToGroup(name, amount?.toString() ?: "0")
        clearInputFields()
    }

    private fun addChipToGroup(name: String, amount: String) {
        val chip = createChip(name, amount, chipGroup)
        chipGroup.addView(chip)
    }

    private fun clearInputFields() {
        tilName.editText?.text?.clear()
        tilAmount.editText?.text?.clear()
    }

    private fun createChip(name: String, amount: String, chipGroup: ChipGroup): Chip {
        return Chip(requireContext()).apply {
            text = itemUtils.createItemChipText(name, amount)
            isCloseIconVisible = true
            setOnCloseIconClickListener { chipGroup.removeView(this) }
            setOnClickListener { showEditDialog(this) }
        }
    }

    private fun showEditDialog(chip: Chip) {
        val itemInfo = itemUtils.parseItemChipText(chip.text.toString())
        dialogUtils.showEditItemDialog(
            context = requireContext(),
            parent = view as? ViewGroup,
            currentName = itemInfo.first,
            currentAmount = itemInfo.second.toString(),
            existingNames = itemUtils.extractItemsFromChips(chipGroup, listId).map { it.name }
        ) { newName, newAmount ->
            chip.text = itemUtils.createItemChipText(newName, newAmount.toString())
        }
    }

    private fun setupUpdateButton() {
        btnUpdate.setOnClickListener {
            if (validateInputs()) {
                updateShoppingList()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = tilTitle.editText?.text?.toString()?.trim()
        val description = tilDescription.editText?.text?.toString()?.trim()

        var isValid = true
        if (name.isNullOrEmpty()) {
            tilTitle.error = getString(R.string.error_title_empty)
            isValid = false
        }
        if (description.isNullOrEmpty()) {
            tilDescription.error = getString(R.string.error_description_empty)
            isValid = false
        }
        return isValid
    }

    private fun updateShoppingList() {
        currentList?.shoppingList?.let { oldList ->
            val updatedList = oldList.copy(
                name = tilTitle.editText?.text?.toString()?.trim() ?: "",
                description = tilDescription.editText?.text?.toString()?.trim() ?: "",
                creatorId = oldList.creatorId
            )
            val newItems = itemUtils.extractItemsFromChips(chipGroup, listId)
            progressBar.visibility = View.VISIBLE

            if (imageHandler.selectedImageUri != null) {
                updateWithNewImage(updatedList, newItems)
            } else {
                updateWithoutImage(updatedList, newItems)
            }
        } ?: Log.e("UpdateTest", "currentList was null")
    }

    private fun updateWithNewImage(updatedList: com.example.groclistapp.data.database.schema.ShoppingList, newItems: List<com.example.groclistapp.data.database.schema.ShoppingItem>) {
        viewModel.uploadImageAndUpdateList(imageHandler.selectedImageUri!!, updatedList) { newUpdatedList ->
            viewModel.updateListWithItems(listId, newUpdatedList, newItems) {
                handleUpdateSuccess()
            }
        }
    }

    private fun updateWithoutImage(updatedList: com.example.groclistapp.data.database.schema.ShoppingList, newItems: List<com.example.groclistapp.data.database.schema.ShoppingItem>) {
        viewModel.updateListWithItems(listId, updatedList, newItems) {
            handleUpdateSuccess()
        }
    }

    private fun handleUpdateSuccess() {
        progressBar.visibility = View.GONE
        Toast.makeText(requireContext(), getString(R.string.success_list_updated), Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun setupCancelButton() {
        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupDeleteButton() {
        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        currentList?.let { list ->
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_delete_list_title))
                .setMessage(getString(R.string.dialog_delete_list_message))
                .setPositiveButton(getString(R.string.dialog_button_yes)) { _, _ ->
                    viewModel.deleteShoppingList(list.shoppingList)
                    progressBar.visibility = View.VISIBLE
                }
                .setNegativeButton(getString(R.string.dialog_button_cancel), null)
                .show()
        }
    }

    private fun handleDeleteStatus(isDeleted: Boolean?) {
        if (isDeleted == true) {
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), getString(R.string.success_list_deleted), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun loadShoppingList() {
        lifecycleScope.launch {
            currentList = viewModel.getShoppingListById(listId)
            currentList?.let { updateUIWithList(it) }
        }
    }
}