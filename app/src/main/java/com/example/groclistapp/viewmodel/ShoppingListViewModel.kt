package com.example.groclistapp.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.groclistapp.data.model.ShoppingListSummary
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.repository.ShoppingListRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppingListViewModel(
    application: Application,
    private val repository: ShoppingListRepository
) : AndroidViewModel(application) {

    private val _shoppingLists = MediatorLiveData<List<ShoppingListSummary>>()
    val localShoppingLists: LiveData<List<ShoppingListSummary>> get() = _shoppingLists


    suspend fun addShoppingList(shoppingList: ShoppingListSummary): Boolean {
        Log.d("ShoppingListViewModel", "addShoppingList called with list: ${shoppingList.name}")
        return withContext(Dispatchers.IO) {
            val isSaved = repository.insertAndGetId(shoppingList)
            Log.d("ShoppingListViewModel", "addShoppingList isSaved: $isSaved")
            isSaved
        }
    }

    suspend fun addItemSuspend(item: ShoppingItem) {
        withContext(Dispatchers.IO) {
            try {
                repository.insertItem(item)
                Log.d("ShoppingListViewModel", "addItemSuspend: Item '${item.name}' added successfully, listId: ${item.listId}")
            } catch (e: Exception) {
                Log.e("ShoppingListViewModel", "addItemSuspend: Error adding item '${item.name}': ${e.message}")
                throw e
            }
        }
    }


    fun updateShoppingList(shoppingList: ShoppingListSummary) {
        viewModelScope.launch {
            repository.update(shoppingList)
        }
    }

    fun deleteShoppingList(shoppingList: ShoppingListSummary) {
        viewModelScope.launch {
            repository.delete(shoppingList)
        }
    }

    suspend fun getShoppingListById(listId: String): ShoppingListSummary? {
        return repository.getShoppingListById(listId)
    }

    fun getItemsForList(listId: String): LiveData<List<ShoppingItem>> {
        return repository.getItemsForList(listId)
    }

    fun addItem(item: ShoppingItem) {
        Log.d("ShoppingListViewModel", "addItem called with item: ${item.name}, listId: ${item.listId}, amount: ${item.amount}")
        viewModelScope.launch {
            try {
                repository.insertItem(item)
                Log.d("ShoppingListViewModel", "Item '${item.name}' added successfully to repository.")
            } catch (e: Exception) {
                Log.e("ShoppingListViewModel", "Error adding item '${item.name}': ${e.message}")
            }
        }
    }


    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun deleteAllItemsForList(listId: String) {
        viewModelScope.launch {
            repository.deleteAllItemsForList(listId)
        }
    }

    init {
        _shoppingLists.addSource(repository.allShoppingLists) { lists ->
            lists?.let {
                val updatedLists = lists.map { list ->
                    ShoppingListSummary(
                        id = list.id,
                        name = list.name,
                        description = list.description ?: "",
                        imageUrl = list.imageUrl,
                        itemsCount = list.itemsCount,
                        creatorId = list.creatorId,
                        shareCode = list.shareCode
                    )
                }
                _shoppingLists.postValue(updatedLists)
                Log.d("ShoppingListViewModel", " רשימות נטענו עם תיאורים: ${updatedLists.map { it.description }}")
            }
        }
    }


    fun loadShoppingLists() {
        viewModelScope.launch {
            val lists = repository.allShoppingLists.value
            Log.d("ShoppingListViewModel", " מספר הרשימות שנמשכו: ${lists?.size ?: 0}")
            _shoppingLists.postValue(lists ?: emptyList())
        }
    }

    fun uploadImageAndUpdateList(imageUri: Uri, list: ShoppingListSummary, onComplete: (ShoppingListSummary) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileName = "shopping_list_images/${System.currentTimeMillis()}.jpg"
        val imageRef = storageRef.child(fileName)

        imageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    Log.e("Upload", "Upload failed: ${task.exception?.message}")
                    throw task.exception ?: Exception("Upload failed")
                }
                imageRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                Log.d("Upload", "Received new image URL: ${uri.toString()}")

                val updatedList = list.copy(imageUrl = uri.toString())
                Log.d("Upload", "Updated list object: $updatedList")

                updateShoppingList(updatedList)

                onComplete(updatedList)
            }
            .addOnFailureListener {
                Log.e("Upload", " שגיאה בהעלאת תמונה: ${it.message}")
            }
    }


    suspend fun deleteAllItemsForListNow(listId: String) {
        repository.deleteAllItemsForList(listId)
    }


    class Factory(
        private val application: Application,
        private val repository: ShoppingListRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ShoppingListViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

