package com.example.groclistapp.data.model

import androidx.room.*

@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["listId"])]
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "amount")
    var amount: Int = 0,

    @ColumnInfo(name = "listId")
    var listId: Int = 0
) {
    constructor() : this(0, "", 0, 0)
}
