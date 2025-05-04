package com.example.groclistapp.data.database.dao

import androidx.room.*
import com.example.groclistapp.data.database.schema.ShoppingList
import androidx.lifecycle.LiveData
import com.example.groclistapp.data.database.schema.ShoppingListWithItems

@Dao
interface ShoppingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingList): Long

    @Update
    suspend fun updateShoppingList(shoppingList: ShoppingList): Int

    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingList): Int

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAllShoppingLists()

    @Query("""
    SELECT shopping_lists.id, shopping_lists.name, shopping_lists.description, 
           shopping_lists.creatorId, shopping_lists.shareCode, shopping_lists.imageUrl
    FROM shopping_lists WHERE creatorId = :userId ORDER BY name ASC
    """)
    fun getAllShoppingLists(userId: String?): LiveData<List<ShoppingList>>

    @Transaction
    @Query("""
    SELECT shopping_lists.id, shopping_lists.name, shopping_lists.description, 
           shopping_lists.creatorId, shopping_lists.shareCode, shopping_lists.imageUrl
    FROM shopping_lists WHERE shopping_lists.id = :listId LIMIT 1
""")
    suspend fun getListById(listId: String): ShoppingListWithItems?

    @Transaction
    @Query("""
    SELECT sl.id, sl.name, sl.description, sl.creatorId, sl.shareCode, sl.imageUrl
    FROM shopping_lists sl
    WHERE sl.id IN (:ids)
    ORDER BY name ASC
""")
    fun getAllShoppingListsFiltered(ids: List<String>): LiveData<List<ShoppingList>>
}




