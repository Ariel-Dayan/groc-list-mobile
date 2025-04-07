package com.example.groclistapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.groclistapp.utils.Converters
import androidx.room.Ignore

@Entity(tableName = "shopping_lists")
@TypeConverters(Converters::class)
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String = "",
    var description: String = "",
    var items: List<ShoppingItem> = emptyList(),
    val imageUrl: String? = null,
    var creatorId: String = "",
    var shareCode: String = ""
) {
    @Ignore
    constructor() : this(0, "","", emptyList(), null, "", "")
}


