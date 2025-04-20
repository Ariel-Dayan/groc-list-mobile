package com.example.groclistapp.data.model

import androidx.room.DatabaseView

@DatabaseView(
    "SELECT shopping_lists.id, shopping_lists.name, shopping_lists.creatorId, " +
            "shopping_lists.shareCode, shopping_lists.description, shopping_lists.imageUrl," +
            "(SELECT COUNT(*) FROM shopping_items WHERE shopping_items.listId = shopping_lists.id) AS itemsCount " +
            "FROM shopping_lists"
)
data class ShoppingListSummary(
    val id: String,
    val name: String,
    val creatorId: String,
    val shareCode: String,
    val description: String,
    val imageUrl: String?,
    val itemsCount: Int
)


