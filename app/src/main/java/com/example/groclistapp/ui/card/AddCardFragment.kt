package com.example.groclistapp.ui.card

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingListSummary
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class AddCardFragment : Fragment() {

    private var listId: Int = -1
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var repository: ShoppingListRepository
    private val pendingItems = mutableListOf<ShoppingItem>()
    private lateinit var ivImagePreview: ImageView
    private lateinit var imageHandler: ImageHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_add_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivImagePreview = view.findViewById(R.id.ivAddCardTop)
        val btnGallery = view.findViewById<ImageButton>(R.id.ibAddCardUploadImageFromGallery)
        val btnCamera = view.findViewById<ImageButton>(R.id.ibAddCardTakePhoto)

        imageHandler = ImageHandler(this, ivImagePreview, btnGallery, btnCamera)

        val shoppingListDao = AppDatabase.getDatabase(requireContext()).shoppingListDao()
        val shoppingItemDao = AppDatabase.getDatabase(requireContext()).shoppingItemDao()
        repository = ShoppingListRepository(shoppingListDao, shoppingItemDao)

        viewModel = ViewModelProvider(
            this,
            ShoppingListViewModel.Factory(requireActivity().application, repository)
        )[ShoppingListViewModel::class.java]

        val tilListName = view.findViewById<TextInputLayout>(R.id.tilAddCardTitle)
        val tilItemName = view.findViewById<TextInputLayout>(R.id.tilAddCardItemName)
        val tilItemAmount = view.findViewById<TextInputLayout>(R.id.tilAddCardItemAmount)
        val tilListDescription = view.findViewById<TextInputLayout>(R.id.tilAddCardDescription)
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
            val listDescription = tilListDescription.editText?.text.toString().trim()
            Log.d("AddCardFragment", "\uD83D\uDCC5 שמירת רשימה - שם: $listName | תיאור: $listDescription")

            if (listName.isEmpty()) {
                tilListName.error = "The list name cannot be empty"
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            val creatorId = user?.uid ?: ""
            val shareCode = repository.generateShareCode()

            val uri = imageHandler.selectedImageUri ?: imageHandler.getBitmapPhoto()?.let { saveBitmapToFile(it) }
            Log.d("AddCardFragment", "Final image URI to upload: $uri")

            if (uri != null) {
                uploadImageToFirebaseStorage(
                    uri,
                    onSuccess = { url ->
                        imageHandler.loadImage(url)
                        continueSaving(listName, listDescription, url, creatorId, shareCode)
                    },
                    onFailure = { e ->
                        Log.e("AddCardFragment", "\u274C שגיאה בהעלאת התמונה: ${e.message}")
                        continueSaving(listName, listDescription, null, creatorId, shareCode)
                    }
                )
            } else {
                continueSaving(listName, listDescription, null, creatorId, shareCode)
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

    private fun uploadImageToFirebaseStorage(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("shopping_list_images/${System.currentTimeMillis()}.jpg")

        Log.d("AddCardFragment", "Uploading image from URI: $uri")

        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            Log.d("AddCardFragment", "InputStream from URI is available: ${inputStream != null}")
            inputStream?.close()
        } catch (e: Exception) {
            Log.e("AddCardFragment", "Error opening InputStream from URI", e)
        }

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d("AddCardFragment", "Uploaded image URL: ${downloadUrl.toString()}")
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
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            Log.d("AddCardFragment", "Bitmap saved to file: ${file.absolutePath}, exists: ${file.exists()}, size: ${file.length()} bytes")
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            Log.d("AddCardFragment", "URI from FileProvider: $uri")
            uri
        } catch (e: Exception) {
            e.printStackTrace()
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
                id = 0,
                name = listName,
                description = listDescription,
                imageUrl = imageUrlString,
                itemsCount = 0,
                creatorId = creatorId,
                shareCode = shareCode
            )

            val newListIdLong = viewModel.addShoppingList(newList)
            val newListId = newListIdLong.toInt()

            pendingItems.forEach { it.listId = newListId }

            pendingItems.forEach {
                viewModel.addItem(it)
                Log.d("AddCardFragment", "\uD83D\uDD39 פריט נוסף: ${it.name}, כמות: ${it.amount}, listId: ${it.listId}")
            }

            withContext(Dispatchers.Main) {
                listId = newListId
                Log.d("AddCardFragment", "\u2705 רשימה חדשה נוצרה עם ID: $listId ושיתוף קוד: $shareCode")
                setFragmentResult("shoppingListUpdated", bundleOf("updated" to true))
                findNavController().navigateUp()
            }
        }
    }
}



