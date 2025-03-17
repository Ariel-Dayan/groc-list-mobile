package com.example.groclistapp.data.repository

import androidx.lifecycle.LiveData
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingItem
import android.util.Log

class ShoppingListRepository(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao
) {
    val allShoppingLists: LiveData<List<ShoppingList>> = shoppingListDao.getAllShoppingLists().also {
        Log.d("ShoppingListRepository", "📥 משיכת רשימות מהמסד: ${it.value?.size ?: 0}")
    }


    suspend fun insert(shoppingList: ShoppingList) {
        shoppingListDao.insertShoppingList(shoppingList)
    }

    suspend fun insertAndGetId(shoppingList: ShoppingList): Long {
        Log.d("ShoppingListRepository", "📝 ניסיון להוספת רשימה: $shoppingList")
        val id = shoppingListDao.insertShoppingList(shoppingList)
        Log.d("ShoppingListRepository", "✅ רשימה נוספה עם ID: $id")
        return id
    }


    suspend fun update(shoppingList: ShoppingList) {
        shoppingListDao.updateShoppingList(shoppingList)
    }

    suspend fun delete(shoppingList: ShoppingList) {
        shoppingListDao.deleteShoppingList(shoppingList)
    }

    suspend fun getShoppingListById(listId: Int): ShoppingList? {
        return shoppingListDao.getListById(listId)
    }

    fun getItemsForList(listId: Int): LiveData<List<ShoppingItem>> {
        return shoppingItemDao.getItemsForList(listId)
    }

    suspend fun insertItem(item: ShoppingItem) {
        shoppingItemDao.insertItem(item)
    }

    suspend fun updateItem(item: ShoppingItem) {
        shoppingItemDao.updateItem(item)
    }

    suspend fun deleteItem(item: ShoppingItem) {
        shoppingItemDao.deleteItem(item)
    }
}


