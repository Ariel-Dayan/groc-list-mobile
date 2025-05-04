package com.example.groclistapp.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.groclistapp.data.model.ShoppingListSummary
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingListWithItems
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.data.repository.ShoppingListRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ShoppingListViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val shoppingListDao = AppDatabase.getDatabase(application).shoppingListDao()
    private val shoppingItemDao = AppDatabase.getDatabase(application).shoppingItemDao()
    private val repository = ShoppingListRepository(
        shoppingListDao,
        shoppingItemDao
    )

    private val _shoppingLists = MediatorLiveData<List<ShoppingListSummary>>()
    val localShoppingLists: LiveData<List<ShoppingListSummary>> get() = _shoppingLists

    private val _addListStatus = MutableLiveData<Boolean?>()
    val addListStatus: LiveData<Boolean?> get() = _addListStatus

    private val _currentList = MutableLiveData<ShoppingListWithItems?>()
    val currentList: LiveData<ShoppingListWithItems?> get() = _currentList

    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus

    fun deleteSharedListById(listId: String) {
        viewModelScope.launch {
            val list = repository.getShoppingListById(listId)

            if (list != null) {
                repository.deleteAllItemsForList(listId)
                repository.delete(list.shoppingList)
                removeSharedListReference(listId,
                    onSuccess = {
                        _deleteStatus.postValue(true)
                    },
                    onFailure = { exception ->
                        _deleteStatus.postValue(false)
                    }
                )
            } else {
                _deleteStatus.postValue(false)
            }
        }
    }

    fun loadShoppingListById(listId: String) {
        viewModelScope.launch {
            _currentList.value = repository.getShoppingListById(listId)
        }
    }


    fun resetAddListStatus() {
        _addListStatus.value = null
    }

     suspend fun addItems(items: List<ShoppingItem>) {
        withContext(Dispatchers.IO) {
            try {
                repository.insertItems(items)
            } catch (e: Exception) {
                Log.e("ShoppingListViewModel", "addItems: Error adding items: ${e.message}")
                throw e
            }
        }
    }


    fun updateShoppingList(shoppingList: ShoppingList) {
        viewModelScope.launch {
            repository.update(shoppingList)
        }
    }
    
    fun deleteShoppingList(shoppingList: ShoppingList) {
        viewModelScope.launch {
            repository.delete(shoppingList)
            _deleteStatus.postValue(true)
        }
    }

    suspend fun getShoppingListById(listId: String): ShoppingListWithItems? {
        return repository.getShoppingListById(listId)
    }


    init {
        _shoppingLists.addSource(repository.allShoppingLists) { lists ->
            lists?.let {
                postFullShoppingLists(lists)
            }
        }
    }

    private fun postFullShoppingLists(lists: List<ShoppingList>) {
        viewModelScope.launch {
            repository.getCreatorNames(lists.map { it.creatorId }) { creatorNames ->
                val updatedLists = lists.map { list ->
                    ShoppingListSummary(
                        id = list.id,
                        name = list.name,
                        creatorId = list.creatorId,
                        shareCode = list.shareCode,
                        description = list.description,
                        imageUrl = list.imageUrl,
                        creatorName = creatorNames[list.creatorId] ?: "Unknown"
                    )
                }

                _shoppingLists.postValue(updatedLists)
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
                postFullShoppingLists(lists)
            }
        }
    }

    fun uploadImageAndUpdateList(imageUri: Uri, list: ShoppingList, onComplete: (ShoppingList) -> Unit) {
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
                val updatedList = list.copy(imageUrl = uri.toString())
                updateShoppingList(updatedList)
                onComplete(updatedList)
            }
            .addOnFailureListener {
                Log.e("Upload", "Error uploading image: ${it.message}")
            }
    }


    suspend fun deleteAllItemsForListNow(listId: String) {
        repository.deleteAllItemsForList(listId)
    }

    suspend fun syncUserDataFromFirebase() {
        repository.loadAllUserDataFromFirebase()
    }

    fun removeSharedListReference(listId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        repository.removeListIdFromSharedListArray(listId, onSuccess, onFailure)
    }

    fun addShoppingListWithItems(
        list: ShoppingList,
        items: List<ShoppingItem>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val isSaved = repository.insertList(list)
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
        updatedList: ShoppingList,
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
}

