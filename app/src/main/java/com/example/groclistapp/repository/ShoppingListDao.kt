package com.example.groclistapp.repository

import androidx.room.*
import com.example.groclistapp.model.ShoppingList
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
}


