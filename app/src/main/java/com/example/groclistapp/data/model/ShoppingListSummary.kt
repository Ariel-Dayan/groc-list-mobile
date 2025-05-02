package com.example.groclistapp.data.model

data class ShoppingListSummary(
    val id: String,
    val name: String,
    val creatorId: String,
    val shareCode: String,
    val description: String,
    val imageUrl: String?,
    val creatorName: String
)


