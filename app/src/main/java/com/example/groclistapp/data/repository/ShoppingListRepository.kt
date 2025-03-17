package com.example.groclistapp.data.repository

import androidx.lifecycle.LiveData
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingListSummary
import android.util.Log

class ShoppingListRepository(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao
) {
    val allShoppingLists: LiveData<List<ShoppingListSummary>> = shoppingListDao.getAllShoppingLists().also {
        Log.d("ShoppingListRepository", "📥 משיכת רשימות מהמסד: ${it.value?.size ?: 0}")
    }

    suspend fun insertAndGetId(shoppingList: ShoppingListSummary): Long {
        return shoppingListDao.insertShoppingList(
            ShoppingList(id = shoppingList.id, name = shoppingList.name)
        )
    }

    suspend fun update(shoppingList: ShoppingListSummary) {
        shoppingListDao.updateShoppingList(
            ShoppingList(id = shoppingList.id, name = shoppingList.name)
        )
    }

    suspend fun delete(shoppingList: ShoppingListSummary) {
        shoppingListDao.deleteShoppingList(
            ShoppingList(id = shoppingList.id, name = shoppingList.name)
        )
    }

    suspend fun getShoppingListById(listId: Int): ShoppingListSummary? {
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





