package com.example.groclistapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.groclistapp.data.model.ShoppingListSummary
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.data.repository.ShoppingListRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SharedCardsViewModel(application: Application) : AndroidViewModel(application) {
    private val shoppingListDao = AppDatabase.getDatabase(application).shoppingListDao()
    private val shoppingItemDao = AppDatabase.getDatabase(application).shoppingItemDao()
    private val repository = ShoppingListRepository(
        shoppingListDao,
        shoppingItemDao
    )

    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val _sharedListIds = MutableLiveData<List<String>>()

    val sharedLists = MediatorLiveData<List<ShoppingListSummary>>()

    init {
        userId?.let { fetchSharedListIds(it) }

        sharedLists.addSource(_sharedListIds) { ids ->
            val filteredLists = shoppingListDao.getAllShoppingListsFiltered(ids ?: emptyList())
            sharedLists.addSource(filteredLists) { lists ->
                viewModelScope.launch {
                    repository.getCreatorNames(lists.map { it.creatorId }) { creatorNames ->
                        val updatedLists = lists.map { list ->
                            ShoppingListSummary(
                                id = list.id,
                                name = list.name,
                                creatorId = list.creatorId,
                                creatorName = creatorNames[list.creatorId] ?: "Unknown",
                                shareCode = list.shareCode,
                                description = list.description,
                                imageUrl = list.imageUrl,
                            )
                        }

                        sharedLists.postValue(updatedLists)
                    }
                }
//                sharedLists.value = lists
            }
        }
    }

    private fun fetchSharedListIds(userId: String) {
        viewModelScope.launch {
            _sharedListIds.value = getSharedListIds(userId)
        }
    }

    private suspend fun getSharedListIds(userId: String): List<String> {
        try {
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            val sharedListIds = (userDoc.get("sharedListIds") as? List<*>)
                ?.mapNotNull { it as? String }
                ?: emptyList()

            return sharedListIds
        } catch (e: Exception) {
            Log.w("SharedCardsViewModel", "Error getting shared list IDs", e)
            return emptyList()
        }
    }

    fun addSharedListId(listId: String) {
        val updatedListIds = (_sharedListIds.value ?: emptyList()) + listId
        _sharedListIds.postValue(updatedListIds)
    }

    suspend fun syncSharedListsFromFirebase() {
        repository.loadAllSharedListsFromFirebase()
    }
}