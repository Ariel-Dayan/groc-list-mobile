package com.example.groclistapp.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.groclistapp.model.ShoppingList
import com.example.groclistapp.repository.FirebaseRepository
import com.example.groclistapp.repository.ShoppingListRepository
import com.example.groclistapp.repository.AppDatabase
import kotlinx.coroutines.launch

class ShoppingListViewModel(application: Application, private val repository: ShoppingListRepository) : AndroidViewModel(application) {

    private val firebaseRepository = FirebaseRepository()

    private val _localShoppingLists = repository.allShoppingLists.asLiveData()
    val localShoppingLists: LiveData<List<ShoppingList>> = _localShoppingLists

    private val _remoteShoppingLists = MutableLiveData<List<ShoppingList>>()
    val remoteShoppingLists: LiveData<List<ShoppingList>> = _remoteShoppingLists

    init {
        syncShoppingLists()
    }

    private fun syncShoppingLists() {
        firebaseRepository.getShoppingLists(
            onSuccess = { lists ->
                _remoteShoppingLists.postValue(lists)
                viewModelScope.launch {
                    lists.forEach { shoppingList ->
                        repository.insert(shoppingList)
                    }
                }
            },
            onFailure = {
                viewModelScope.launch {
                    repository.allShoppingLists.asLiveData().observeForever { lists ->
                        _remoteShoppingLists.postValue(lists)
                    }
                }
            }
        )
    }

    fun addShoppingList(shoppingList: ShoppingList) {
        viewModelScope.launch {
            repository.insert(shoppingList)
        }
        firebaseRepository.addShoppingList(shoppingList, {}, {})
    }

    fun updateShoppingList(listId: String, updatedData: Map<String, Any>, shoppingList: ShoppingList) {
        viewModelScope.launch {
            repository.update(shoppingList)
        }
        firebaseRepository.updateShoppingList(listId, updatedData, {}, {})
    }

    fun deleteShoppingList(listId: String, shoppingList: ShoppingList) {
        viewModelScope.launch {
            repository.delete(shoppingList)
        }
        firebaseRepository.deleteShoppingList(listId, {}, {})
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


