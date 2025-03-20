package com.example.groclistapp.data.repository

import androidx.room.*
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingListSummary
import androidx.lifecycle.LiveData

@Dao
interface ShoppingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingList): Long

    @Update
    suspend fun updateShoppingList(shoppingList: ShoppingList): Int

    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingList): Int

    @Query("""
        SELECT shopping_lists.id, shopping_lists.name, shopping_lists.description, shopping_lists.creatorId, shopping_lists.shareCode, 
        (SELECT COUNT(*) FROM shopping_items WHERE shopping_items.listId = shopping_lists.id) AS itemsCount 
        FROM shopping_lists ORDER BY name ASC
    """)
    fun getAllShoppingLists(): LiveData<List<ShoppingListSummary>>

    @Query("""
        SELECT shopping_lists.id, shopping_lists.name, shopping_lists.description, shopping_lists.creatorId, shopping_lists.shareCode, 
        (SELECT COUNT(*) FROM shopping_items WHERE shopping_items.listId = shopping_lists.id) AS itemsCount 
        FROM shopping_lists WHERE shopping_lists.id = :listId LIMIT 1
    """)
    suspend fun getListById(listId: Int): ShoppingListSummary?

    @Query("""
        SELECT shopping_lists.id, shopping_lists.name, shopping_lists.description, shopping_lists.creatorId, shopping_lists.shareCode, 
        (SELECT COUNT(*) FROM shopping_items WHERE shopping_items.listId = shopping_lists.id) AS itemsCount 
        FROM shopping_lists ORDER BY name ASC
    """)
    suspend fun getAllShoppingListsNow(): List<ShoppingListSummary>

    @Query("""
        SELECT shopping_lists.id, shopping_lists.name, shopping_lists.description, shopping_lists.creatorId, shopping_lists.shareCode, 
        (SELECT COUNT(*) FROM shopping_items WHERE shopping_items.listId = shopping_lists.id) AS itemsCount 
        FROM shopping_lists WHERE shopping_lists.shareCode = :shareCode LIMIT 1
    """)
    suspend fun getListByShareCode(shareCode: String): ShoppingListSummary?
}




