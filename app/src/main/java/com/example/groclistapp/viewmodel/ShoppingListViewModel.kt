package com.example.groclistapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.groclistapp.model.ShoppingList
import com.example.groclistapp.repository.FirebaseRepository

class ShoppingListViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _shoppingLists = MutableLiveData<List<ShoppingList>>()
    val shoppingLists: LiveData<List<ShoppingList>> = _shoppingLists

    //  שליפת כל רשימות הקניות
    fun fetchShoppingLists() {
        repository.getShoppingLists(
            onSuccess = { lists -> _shoppingLists.postValue(lists) },
            onFailure = { _shoppingLists.postValue(emptyList()) }
        )
    }

    //  הוספת רשימת קניות חדשה
    fun addShoppingList(shoppingList: ShoppingList) {
        repository.addShoppingList(shoppingList,
            onSuccess = { fetchShoppingLists() },
            onFailure = { /* , אם יבוא לנו ניתן להוסיף טיפול בשגיאות */ }
        )
    }

    //  עדכון רשימת קניות (לדוגמה, שינוי שם)
    fun updateShoppingList(listId: String, updatedData: Map<String, Any>) {
        repository.updateShoppingList(listId, updatedData,
            onSuccess = { fetchShoppingLists() },
            onFailure = { /* ניתן להוסיף טיפול בשגיאות */ }
        )
    }

    //  מחיקת רשימה מהמסד
    fun deleteShoppingList(listId: String) {
        repository.deleteShoppingList(listId,
            onSuccess = { fetchShoppingLists() },
            onFailure = { /* ניתן להוסיף טיפול בשגיאות */ }
        )
    }
}

