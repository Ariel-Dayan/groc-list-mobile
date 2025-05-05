package com.example.groclistapp.data.database.schema

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import java.util.UUID

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var description: String = "",
    val imageUrl: String? = null,
    var creatorId: String = "",
    var shareCode: String = ""
) {
    @Ignore
    constructor() : this(UUID.randomUUID().toString(), "","", null, "", "")
}


