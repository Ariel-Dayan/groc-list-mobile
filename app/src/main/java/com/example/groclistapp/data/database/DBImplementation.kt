package com.example.groclistapp.data.database

import com.example.groclistapp.data.database.dao.ShoppingItemDao
import com.example.groclistapp.data.database.dao.ShoppingListDao

class DBImplementation {
    private val localDB: AppLocalDbRepository = AppLocalDB.getAppDB()

    companion object {
        @Volatile
        private var instance: DBImplementation? = null

        fun getInstance(): DBImplementation {
            return instance ?: synchronized(this) {
                instance ?: DBImplementation().also { instance = it }
            }
        }
    }

    fun getShoppingListDao(): ShoppingListDao {
        return localDB.shoppingListDao()
    }

    fun getShoppingItemDao(): ShoppingItemDao {
        return localDB.shoppingItemDao()
    }
}