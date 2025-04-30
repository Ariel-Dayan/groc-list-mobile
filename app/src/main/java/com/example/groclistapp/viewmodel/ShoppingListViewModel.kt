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
import kotlinx.coroutines.delay


class ShoppingListViewModel(
    application: Application,
    private val repository: ShoppingListRepository
) : AndroidViewModel(application) {

    private val _shoppingLists = MediatorLiveData<List<ShoppingListSummary>>()
    val localShoppingLists: LiveData<List<ShoppingListSummary>> get() = _shoppingLists

    private val _addListStatus = MutableLiveData<Boolean?>()
    val addListStatus: LiveData<Boolean?> get() = _addListStatus

    private val _currentListSummary = MutableLiveData<ShoppingListSummary?>()
    val currentListSummary: LiveData<ShoppingListSummary?> get() = _currentListSummary

    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus

    fun deleteSharedListById(listId: String) {
        viewModelScope.launch {
            val list = repository.getShoppingListById(listId)

            if (list != null) {
                repository.deleteAllItemsForList(listId)
                repository.delete(list)
                removeSharedListReference(listId)
                _deleteStatus.postValue(true)
            } else {
                _deleteStatus.postValue(false)
            }
        }
    }

    fun deleteShoppingListAsync(shoppingList: ShoppingListSummary) {
        viewModelScope.launch {
            repository.delete(shoppingList)
            _deleteStatus.postValue(true)
        }
    }

    fun loadShoppingListById(listId: String) {
        viewModelScope.launch {
            _currentListSummary.value = repository.getShoppingListById(listId)
        }
    }


    fun resetAddListStatus() {
        _addListStatus.value = null
    }

    suspend fun addShoppingList(shoppingList: ShoppingListSummary): Boolean {
        Log.d("ShoppingListViewModel", "addShoppingList called with list: ${shoppingList.name}")
        return withContext(Dispatchers.IO) {
            val isSaved = repository.insertAndGetId(shoppingList)
            Log.d("ShoppingListViewModel", "addShoppingList isSaved: $isSaved")
            isSaved
        }
    }

    suspend fun addItems(items: List<ShoppingItem>) {
        withContext(Dispatchers.IO) {
            try {
                repository.insertItems(items)
                Log.d("ShoppingListViewModel", "addItems: ${items.size} items added successfully")
            } catch (e: Exception) {
                Log.e("ShoppingListViewModel", "addItems: Error adding items: ${e.message}")
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
            if (lists == null) {
                Log.d("ShoppingListViewModel", "No lists found in local database")
            } else {
                Log.d("ShoppingListViewModel", "Found ${lists.size} lists in local database.")
                _shoppingLists.postValue(lists ?: emptyList())
            }
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

    suspend fun syncUserDataFromFirebase() {
        repository.loadAllUserDataFromFirebase()
    }

    suspend fun syncSharedListsFromFirebase() {
        repository.loadAllSharedListsFromFirebase()
    }

    fun removeSharedListReference(listId: String) {
        repository.removeListIdFromSharedListArray(listId)
    }

    fun addShoppingListWithItems(
        list: ShoppingListSummary,
        items: List<ShoppingItem>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val isSaved = repository.insertAndGetId(list)
            if (!isSaved) {
                _addListStatus.postValue(false)
                return@launch
            }

            items.forEach { it.listId = list.id }

            try {
                repository.insertItems(items)
                _addListStatus.postValue(true)
            } catch (e: Exception) {
                Log.e("ViewModel", "Error inserting items: ${e.message}")
                _addListStatus.postValue(false)
            }
        }
    }

    fun updateListWithItems(
        listId: String,
        updatedList: ShoppingListSummary,
        items: List<ShoppingItem>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            deleteAllItemsForListNow(listId)
            addItems(items)
            updateShoppingList(updatedList)
            onComplete()
        }
    }


    class Factory(
        private val application: Application,
        private val repository: ShoppingListRepository
    ) : ViewModelProvider.Factory {

        constructor(application: Application) : this(
            application,
            com.example.groclistapp.data.repository.ShoppingListRepository(
                com.example.groclistapp.data.repository.AppDatabase
                    .getDatabase(application.applicationContext)
                    .shoppingListDao(),
                com.example.groclistapp.data.repository.AppDatabase
                    .getDatabase(application.applicationContext)
                    .shoppingItemDao()
            )
        )

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ShoppingListViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

