package com.example.groclistapp.data.repository

import androidx.room.*
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingItem
import androidx.lifecycle.LiveData

@Dao
interface ShoppingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingList): Long

    @Update
    suspend fun updateShoppingList(shoppingList: ShoppingList): Int

    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingList): Int

    @Query("SELECT * FROM shopping_lists ORDER BY name ASC")
    fun getAllShoppingLists(): LiveData<List<ShoppingList>>

    @Query("SELECT * FROM shopping_lists WHERE id = :listId LIMIT 1")
    suspend fun getListById(listId: Int): ShoppingList?

    @Query("SELECT * FROM shopping_lists ORDER BY name ASC")
    suspend fun getAllShoppingListsNow(): List<ShoppingList>
}



