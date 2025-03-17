package com.example.groclistapp.data.repository

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.groclistapp.data.model.ShoppingItem

@Dao
interface ShoppingItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem): Long

    @Update
    suspend fun updateItem(item: ShoppingItem): Int

    @Delete
    suspend fun deleteItem(item: ShoppingItem): Int

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY name ASC")
    fun getItemsForList(listId: Int): LiveData<List<ShoppingItem>>
}