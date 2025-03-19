package com.example.groclistapp.data.repository

import android.content.Context
import androidx.room.*
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingListSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Database(entities = [ShoppingList::class, ShoppingItem::class], views = [ShoppingListSummary::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopping_list_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                CoroutineScope(Dispatchers.IO).launch {
                    instance.prepopulateDatabase()
                }

                instance
            }
        }
    }

    private suspend fun prepopulateDatabase() {
        val shoppingListDao = shoppingListDao()
        val existingLists = withContext(Dispatchers.IO) { shoppingListDao.getAllShoppingListsNow() }
        if (existingLists.isEmpty()) {
            val defaultList = ShoppingList(name = "My First List")
            shoppingListDao.insertShoppingList(defaultList)
        }
    }
}


