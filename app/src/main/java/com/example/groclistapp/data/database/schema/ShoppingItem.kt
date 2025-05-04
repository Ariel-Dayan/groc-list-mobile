package com.example.groclistapp.data.database.schema

import androidx.room.*
import java.util.UUID

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
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "amount")
    var amount: Int = 0,

    @ColumnInfo(name = "listId")
    var listId: String = "0"
) {
    constructor() : this(UUID.randomUUID().toString(), "", 0, "0")
}
