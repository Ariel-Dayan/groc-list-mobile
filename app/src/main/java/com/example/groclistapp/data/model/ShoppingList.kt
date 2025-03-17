package com.example.groclistapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.groclistapp.utils.Converters
import androidx.room.Ignore

@Entity(tableName = "shopping_lists")
@TypeConverters(Converters::class)
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var name: String = "",
    var items: List<String> = emptyList()
) {
    @Ignore
    constructor() : this(0, "", emptyList())
}


