package com.example.groclistapp.ui.card

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.database.schema.ShoppingItem
import com.example.groclistapp.data.database.schema.ShoppingList
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.utils.CardUtils
import com.example.groclistapp.utils.DialogUtils
import com.example.groclistapp.utils.InputUtils
import com.example.groclistapp.utils.ItemUtils
import com.example.groclistapp.viewmodel.ShoppingListViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class AddCardFragment : Fragment(R.layout.fragment_add_card) {
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var imageHandler: ImageHandler
    private lateinit var progressBar: ProgressBar
    private lateinit var tilListName: TextInputLayout
    private lateinit var tilListDescription: TextInputLayout
    private lateinit var tilItemName: TextInputLayout
    private lateinit var tilItemAmount: TextInputLayout
    private lateinit var chipGroup: ChipGroup
    private val pendingItems = mutableListOf<ShoppingItem>()
    private val itemUtils = ItemUtils.instance
    private val dialogUtils = DialogUtils.instance
    private val cardUtils = CardUtils.instance
    private val inputUtils = InputUtils.instance

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_add_card, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        observeAddListStatus()
    }

    private fun initViews(view: View) {
        tilListName = view.findViewById(R.id.tilAddCardTitle)
        tilListDescription = view.findViewById(R.id.tilAddCardDescription)
        tilItemName = view.findViewById(R.id.tilAddCardItemName)
        tilItemAmount = view.findViewById(R.id.tilAddCardItemAmount)
        chipGroup = view.findViewById(R.id.cgAddCardItemsContainer)
        progressBar = view.findViewById(R.id.pbAddCardSpinner)
        val ivImage = view.findViewById<android.widget.ImageView>(R.id.ivAddCardTop)
        val btnGallery = view.findViewById<android.widget.ImageButton>(R.id.ibAddCardUploadImageFromGallery)
        val btnCamera = view.findViewById<android.widget.ImageButton>(R.id.ibAddCardTakePhoto)
        imageHandler = ImageHandler(ivImage, this, btnGallery, btnCamera)
        viewModel = ViewModelProvider(this)[ShoppingListViewModel::class.java]
        inputUtils.addCleanErrorMessageOnInputListener(tilListName)
        inputUtils.addCleanErrorMessageOnInputListener(tilListDescription)
    }

    private fun setupListeners() {
        view?.findViewById<Button>(R.id.btnAddCardAddItem)?.setOnClickListener { addItem() }
        view?.findViewById<Button>(R.id.btnAddCardSave)?.setOnClickListener { prepareSaveList() }
        view?.findViewById<Button>(R.id.btnAddCardCancel)?.setOnClickListener { findNavController().navigateUp() }
    }

    private fun addItem() {
        val name = tilItemName.editText?.text.toString().trim()
        val amountStr = tilItemAmount.editText?.text.toString().trim()
        itemUtils.validateName(name, pendingItems.map { it.name })?.let {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_item_name, it), Toast.LENGTH_SHORT).show()
            return
        }
        val (amount, amountError) = itemUtils.validateAmount(amountStr)
        amountError?.let {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_item_amount, it), Toast.LENGTH_SHORT).show()
            return
        }
        chipGroup.addView(createChip(name, amountStr))
        pendingItems.add(ShoppingItem(UUID.randomUUID().toString(), name, amount ?: 0, "-1"))
        tilItemName.editText?.text?.clear()
        tilItemAmount.editText?.text?.clear()
    }

    private fun prepareSaveList() {
        val name = tilListName.editText?.text.toString().trim()
        val description = tilListDescription.editText?.text.toString().trim()
        if (!validateListInputs(name, description)) return
        showProgress()
        val creatorId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val shareCode = cardUtils.generateShareCode()
        val uri = imageHandler.selectedImageUri ?: saveBitmapToFile()
        handleImageOrSave(name, description, creatorId, shareCode, uri)
    }

    private fun validateListInputs(name: String, description: String): Boolean {
        var valid = true
        if (name.isEmpty()) {
            tilListName.error = getString(R.string.error_list_name_empty)
            valid = false
        }
        if (description.isEmpty()) {
            tilListDescription.error = getString(R.string.error_description_empty)
            valid = false
        }
        return valid
    }

    private fun showProgress() {
        progressBar.visibility = View.VISIBLE
    }

    private fun saveBitmapToFile(): Uri? {
        return try {
            val file = File(requireContext().cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
            (imageHandler.getBitmapPhoto() ?: return null).compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, file.outputStream())
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        } catch (_: Exception) {
            null
        }
    }

    private fun handleImageOrSave(
        name: String,
        description: String,
        creatorId: String,
        shareCode: String,
        uri: Uri?
    ) {
        if (uri != null) uploadImage(name, description, creatorId, shareCode, uri)
        else continueSaving(name, description, null, creatorId, shareCode)
    }

    private fun uploadImage(
        name: String,
        description: String,
        creatorId: String,
        shareCode: String,
        uri: Uri
    ) {
        val imageRef = FirebaseStorage.getInstance().reference
            .child("shopping_list_images/${System.currentTimeMillis()}.jpg")
        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { url ->
                    continueSaving(name, description, url.toString(), creatorId, shareCode)
                }.addOnFailureListener {
                    continueSaving(name, description, null, creatorId, shareCode)
                }
            }
            .addOnFailureListener {
                continueSaving(name, description, null, creatorId, shareCode)
            }
    }

    private fun observeAddListStatus() {
        viewModel.addListStatus.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess != null && isAdded) {
                progressBar.visibility = View.GONE
                if (isSuccess) {
                    try {
                        setFragmentResult("shoppingListUpdated", bundleOf("updated" to true))
                        findNavController().navigateUp()
                    } catch (e: Exception) {
                        Log.e("AddCardFragment", "Error navigating up: ${e.message}")
                    }
                } else {
                    try {
                        Toast.makeText(requireContext(), getString(R.string.error_save_list), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("AddCardFragment", "Error showing Toast: ${e.message}")
                    }
                }
                viewModel.resetAddListStatus()
            }
        }
    }

    private fun createChip(name: String, amount: String): Chip {
        return Chip(requireContext()).apply {
            text = itemUtils.createItemChipText(name, amount)
            isCloseIconVisible = true
            setOnCloseIconClickListener { chipGroup.removeView(this) }
            setOnClickListener {
                val (currName, currAmount) = itemUtils.parseItemChipText(text.toString())
                dialogUtils.showEditItemDialog(
                    requireContext(),
                    view as ViewGroup,
                    currName,
                    currAmount.toString(),
                    itemUtils.extractItemsFromChips(chipGroup, "-1").map { it.name }
                ) { newName, newAmt ->
                    text = itemUtils.createItemChipText(newName, newAmt.toString())
                }
            }
        }
    }

    private fun continueSaving(
        name: String,
        description: String,
        imageUrl: String?,
        creatorId: String,
        shareCode: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newList = ShoppingList(
                UUID.randomUUID().toString(),
                name,
                description,
                imageUrl,
                creatorId,
                shareCode
            )
            viewModel.addShoppingListWithItems(newList, pendingItems)
        }
    }
}
