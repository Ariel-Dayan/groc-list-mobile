package com.example.groclistapp.data.repository

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingListSummary

@Database(entities = [ShoppingList::class, ShoppingItem::class], views = [ShoppingListSummary::class], version = 8, exportSchema = false)
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
                )
                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }



        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP VIEW IF EXISTS ShoppingListSummary")
                database.execSQL(
                    "CREATE VIEW ShoppingListSummary AS " +
                            "SELECT shopping_lists.id, shopping_lists.name, shopping_lists.creatorId, " +
                            "shopping_lists.shareCode, " +
                            "(SELECT COUNT(*) FROM shopping_items WHERE shopping_items.listId = shopping_lists.id) AS itemsCount " +
                            "FROM shopping_lists"
                )
            }
        }
    }
}






