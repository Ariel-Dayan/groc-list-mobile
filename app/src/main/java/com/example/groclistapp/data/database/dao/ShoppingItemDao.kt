package com.example.groclistapp.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.groclistapp.data.database.schema.ShoppingItem

@Dao
interface ShoppingItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ShoppingItem>)

    @Update
    suspend fun updateItem(item: ShoppingItem): Int

    @Delete
    suspend fun deleteItem(item: ShoppingItem): Int

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllShoppingItems()

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteItemsByListId(listId: String)
}