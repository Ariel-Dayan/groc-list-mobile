package com.example.groclistapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val items: List<String> = emptyList()
) {

    constructor() : this("", "", emptyList())
}
