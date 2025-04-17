package com.example.groclistapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.groclistapp.data.model.ShoppingListSummary
import com.example.groclistapp.data.repository.AppDatabase
import com.google.firebase.auth.FirebaseAuth

class SharedCardsViewModel(application: Application) : AndroidViewModel(application) {

    private val shoppingListDao = AppDatabase.getDatabase(application).shoppingListDao()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    val sharedLists: LiveData<List<ShoppingListSummary>> =
        shoppingListDao.getAllShoppingListsFiltered(currentUserId)
}
