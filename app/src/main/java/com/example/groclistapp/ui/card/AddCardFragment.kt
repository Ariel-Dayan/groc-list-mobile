package com.example.groclistapp.ui.card

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingListSummary
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.data.repository.ShoppingListRepository
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
import java.util.*

class AddCardFragment : Fragment() {
    private var listId: String = "-1"
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var repository: ShoppingListRepository
    private lateinit var imageHandler: ImageHandler
    private lateinit var progressBar: ProgressBar
    private val pendingItems = mutableListOf<ShoppingItem>()
    private val itemUtils = ItemUtils.instance
    private val dialogUtils = DialogUtils.instance
    private val inputUtils = InputUtils.instance

    private lateinit var tilListName: TextInputLayout
    private lateinit var tilListDescription: TextInputLayout
    private lateinit var tilItemName: TextInputLayout
    private lateinit var tilItemAmount: TextInputLayout
    private lateinit var chipGroup: ChipGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_add_card, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()

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
                        Toast.makeText(requireContext(), "Failed to save shopping list", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("AddCardFragment", "Error showing Toast: ${e.message}")
                    }
                }

                viewModel.resetAddListStatus()
            }
        }

    }

    private fun initViews(view: View) {
        tilListName = view.findViewById(R.id.tilAddCardTitle)
        tilListDescription = view.findViewById(R.id.tilAddCardDescription)
        tilItemName = view.findViewById(R.id.tilAddCardItemName)
        tilItemAmount = view.findViewById(R.id.tilAddCardItemAmount)
        chipGroup = view.findViewById(R.id.cgAddCardItemsContainer)
        progressBar = view.findViewById(R.id.pbAddCardSpinner)

        val ivImagePreview = view.findViewById<ImageView>(R.id.ivAddCardTop)
        val btnGallery = view.findViewById<ImageButton>(R.id.ibAddCardUploadImageFromGallery)
        val btnCamera = view.findViewById<ImageButton>(R.id.ibAddCardTakePhoto)
        imageHandler = ImageHandler(ivImagePreview, this, btnGallery, btnCamera)

        val shoppingListDao = AppDatabase.getDatabase(requireContext()).shoppingListDao()
        val shoppingItemDao = AppDatabase.getDatabase(requireContext()).shoppingItemDao()
        repository = ShoppingListRepository(shoppingListDao, shoppingItemDao)

        viewModel = ViewModelProvider(
            this,
            ShoppingListViewModel.Factory(requireActivity().application, repository)
        )[ShoppingListViewModel::class.java]

        inputUtils.addCleanErrorMessageOnInputListener(tilListName)
        inputUtils.addCleanErrorMessageOnInputListener(tilListDescription)
    }

    private fun setupListeners() {
        view?.findViewById<Button>(R.id.btnAddCardAddItem)?.setOnClickListener { addItem() }
        view?.findViewById<Button>(R.id.btnAddCardSave)?.setOnClickListener { saveList() }
        view?.findViewById<Button>(R.id.btnAddCardCancel)?.setOnClickListener { findNavController().navigateUp() }
    }

    private fun addItem() {
        val name = tilItemName.editText?.text?.toString()?.trim().orEmpty()
        val amountStr = tilItemAmount.editText?.text?.toString()?.trim().orEmpty()

        itemUtils.validateName(name, pendingItems.map { it.name })?.let {
            Toast.makeText(requireContext(), "Invalid item name: $it", Toast.LENGTH_SHORT).show()
            return
        }

        val (amount, amountError) = itemUtils.validateAmount(amountStr)
        amountError?.let {
            Toast.makeText(requireContext(), "Invalid item amount: $it", Toast.LENGTH_SHORT).show()
            return
        }

        val chip = createChip(name, amountStr)
        chipGroup.addView(chip)
        pendingItems.add(ShoppingItem(id = UUID.randomUUID().toString(), name = name, amount = amount ?: 0, listId = "-1"))

        tilItemName.editText?.text?.clear()
        tilItemAmount.editText?.text?.clear()
    }

    private fun saveList() {
        val listName = tilListName.editText?.text.toString().trim()
        val listDescription = tilListDescription.editText?.text.toString().trim()
        var isValid = true

        if (listName.isEmpty()) {
            tilListName.error = "The list name cannot be empty"
            isValid = false
        }
        if (listDescription.isEmpty()) {
            tilListDescription.error = "Description cannot be empty"
            isValid = false
        }
        if (!isValid) return

        progressBar.visibility = View.VISIBLE

        val user = FirebaseAuth.getInstance().currentUser
        val creatorId = user?.uid.orEmpty()
        val shareCode = repository.generateShareCode()

        val uri = imageHandler.selectedImageUri ?: imageHandler.getBitmapPhoto()?.let { saveBitmapToFile(it) }

        if (uri != null) {
            uploadImageToFirebaseStorage(
                uri,
                onSuccess = { url ->
                    continueSaving(listName, listDescription, url, creatorId, shareCode)
                },
                onFailure = { e ->
                    Log.e("AddCardFragment", "\u274C Error uploading image: ${e.message}")
                    continueSaving(listName, listDescription, null, creatorId, shareCode)
                }
            )
        } else {
            continueSaving(listName, listDescription, null, creatorId, shareCode)
        }
    }

    private fun createChip(name: String, amount: String): Chip {
        return Chip(requireContext()).apply {
            text = itemUtils.createItemChipText(name, amount)
            isCloseIconVisible = true
            setOnCloseIconClickListener { chipGroup.removeView(this) }
            setOnClickListener {
                val (currentName, currentAmount) = itemUtils.parseItemChipText(text.toString())
                dialogUtils.showEditItemDialog(
                    context = requireContext(),
                    parent = view as? ViewGroup,
                    currentName = currentName,
                    currentAmount = currentAmount.toString(),
                    existingNames = itemUtils.extractItemsFromChips(chipGroup, listId).map { it.name },
                ) { newName, newAmount ->
                    text = itemUtils.createItemChipText(newName, newAmount.toString())
                }
            }
        }
    }

    private fun uploadImageToFirebaseStorage(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("shopping_list_images/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    onSuccess(downloadUrl.toString())
                }.addOnFailureListener { e ->
                    Log.e("AddCardFragment", "Error getting download URL", e)
                    onFailure(e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("AddCardFragment", "Error uploading image", e)
                onFailure(e)
            }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): Uri? {
        return try {
            val file = File(requireContext().cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        } catch (e: Exception) {
            Log.e("AddCardFragment", "Error saving bitmap", e)
            null
        }
    }

    private fun continueSaving(
        listName: String,
        listDescription: String,
        imageUrlString: String?,
        creatorId: String,
        shareCode: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newList = ShoppingListSummary(
                id = UUID.randomUUID().toString(),
                name = listName,
                description = listDescription,
                imageUrl = imageUrlString,
                itemsCount = 0,
                creatorId = creatorId,
                shareCode = shareCode
            )
            viewModel.addShoppingListWithItems(newList, pendingItems)
        }
    }
}
