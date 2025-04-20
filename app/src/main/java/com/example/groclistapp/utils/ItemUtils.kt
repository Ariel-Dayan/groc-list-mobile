package com.example.groclistapp.utils

import com.example.groclistapp.data.model.ShoppingItem
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ItemUtils private constructor() {
    val itemSeparator = ":"

    fun extractItemsFromChips(chipGroup: ChipGroup, listId: String): List<ShoppingItem> {
        val items = mutableListOf<ShoppingItem>()

        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            val text = chip.text.toString()

            val (name, amount) = parseItemChipText(text)
            if (!name.isNullOrEmpty()) {
                val item = ShoppingItem(id = 0, name = name, amount = amount, listId = listId)
                items.add(item)
            }
        }

        return items
    }

    fun createItemChipText(name: String, amount: String): String {
        return "$name$itemSeparator $amount"
    }

    fun parseItemChipText(text: String): Pair<String, Int> {
        val parts = text.split(itemSeparator)
        val name = parts.getOrNull(0)?.trim().orEmpty()
        val amount = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 1
        return name to amount
    }

    fun validateName(newName: String, existingNames: List<String>, currentName: String? = null): String? {
        return when {
            newName.isEmpty() -> "Name cannot be empty"
            newName != currentName && newName in existingNames -> "Name already exists"
            else -> null
        }
    }

    fun validateAmount(amountText: String): Pair<Int?, String?> {
        val value = amountText.toIntOrNull()
        return when {
            amountText.isEmpty() -> null to "Amount cannot be empty"
            value == null || value <= 0 -> null to "Amount must be a positive number"
            else -> value to null
        }
    }

    companion object {
        val instance = ItemUtils()
    }
}