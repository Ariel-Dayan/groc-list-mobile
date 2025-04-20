package com.example.groclistapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.groclistapp.utils.Converters
import androidx.room.Ignore
import java.util.UUID

@Entity(tableName = "shopping_lists")
@TypeConverters(Converters::class)
data class ShoppingList(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var description: String = "",
    var items: List<ShoppingItem> = emptyList(),
    val imageUrl: String? = null,
    var creatorId: String = "",
    var shareCode: String = ""
) {
    @Ignore
    constructor() : this(UUID.randomUUID().toString(), "","", emptyList(), null, "", "")
}


