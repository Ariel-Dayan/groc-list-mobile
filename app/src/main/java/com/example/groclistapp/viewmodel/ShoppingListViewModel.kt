package com.example.groclistapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.groclistapp.data.model.ShoppingListSummary
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppingListViewModel(application: Application, private val repository: ShoppingListRepository)
    : AndroidViewModel(application) {

    private val _shoppingLists = MediatorLiveData<List<ShoppingListSummary>>()
    val localShoppingLists: LiveData<List<ShoppingListSummary>> get() = _shoppingLists

    suspend fun addShoppingList(shoppingList: ShoppingListSummary): Int {
        return withContext(Dispatchers.IO) {
            repository.insertAndGetId(shoppingList).toInt()
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

    suspend fun getShoppingListById(listId: Int): ShoppingListSummary? {
        return repository.getShoppingListById(listId)
    }

    fun getItemsForList(listId: Int): LiveData<List<ShoppingItem>> {
        return repository.getItemsForList(listId)
    }

    fun addItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.insertItem(item)
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

    init {
        _shoppingLists.addSource(repository.allShoppingLists) { lists ->
            _shoppingLists.value = lists
        }
    }

    fun loadShoppingLists() {
        viewModelScope.launch {
            val lists = repository.allShoppingLists.value
            Log.d("ShoppingListViewModel", "📥 מספר הרשימות שנמשכו: ${lists?.size ?: 0}")
            _shoppingLists.postValue(lists ?: emptyList()) // עדכון ה-LiveData
        }
    }

    class Factory(private val application: Application, private val repository: ShoppingListRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ShoppingListViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

