package com.example.groclistapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppingListViewModel(application: Application, private val repository: ShoppingListRepository)
    : AndroidViewModel(application) {

    private val _shoppingLists = MediatorLiveData<List<ShoppingList>>()
    val localShoppingLists: LiveData<List<ShoppingList>> get() = _shoppingLists


    suspend fun addShoppingList(shoppingList: ShoppingList): Int {
        return withContext(Dispatchers.IO) {
            repository.insertAndGetId(shoppingList).toInt() // עכשיו מחזיר את ה-ID
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
        }
    }

    suspend fun getShoppingListById(listId: Int): ShoppingList? {
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
