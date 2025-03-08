package com.example.groclistapp.repository

import androidx.lifecycle.LiveData
import com.example.groclistapp.model.ShoppingList

class ShoppingListRepository(private val shoppingListDao: ShoppingListDao) {

    val allShoppingLists: LiveData<List<ShoppingList>> = shoppingListDao.getAllShoppingLists()

    suspend fun insert(shoppingList: ShoppingList) {
        shoppingListDao.insertShoppingList(shoppingList)
    }

    suspend fun update(shoppingList: ShoppingList) {
        shoppingListDao.updateShoppingList(shoppingList)
    }

    suspend fun delete(shoppingList: ShoppingList) {
        shoppingListDao.deleteShoppingList(shoppingList)
    }
}

