package com.example.groclistapp.data.database

import androidx.room.*
import com.example.groclistapp.data.database.dao.ShoppingItemDao
import com.example.groclistapp.data.database.dao.ShoppingListDao
import com.example.groclistapp.data.database.schema.ShoppingList
import com.example.groclistapp.data.database.schema.ShoppingItem

@Database(
    entities = [ShoppingList::class, ShoppingItem::class],
    views = [],
    version = 3,
    exportSchema = false
)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao
}

object AppLocalDB {
    fun getAppDB(): AppLocalDbRepository {
        return Room.databaseBuilder(
            GrocListApplication.getMyContext(),
            AppLocalDbRepository::class.java,
            "shopping_list_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}





