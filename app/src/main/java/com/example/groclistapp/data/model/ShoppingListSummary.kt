package com.example.groclistapp.data.model

import androidx.room.DatabaseView

@DatabaseView("""
    SELECT shopping_lists.id, shopping_lists.name, 
    (SELECT COUNT(*) FROM shopping_items WHERE shopping_items.listId = shopping_lists.id) AS itemsCount 
    FROM shopping_lists
""")
data class ShoppingListSummary(
    val id: Int,
    val name: String,
    val itemsCount: Int
)

