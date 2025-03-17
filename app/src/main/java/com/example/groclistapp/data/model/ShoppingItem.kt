package com.example.groclistapp.data.model

import androidx.room.*

@Entity(
    tableName = "shopping_items",
    foreignKeys = [ForeignKey(
        entity = ShoppingList::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["listId"])]
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "amount")
    val amount: Int = 0,

    @ColumnInfo(name = "listId")
    var listId: Int
)

