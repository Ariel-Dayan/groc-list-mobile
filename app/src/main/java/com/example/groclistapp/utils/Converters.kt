package com.example.groclistapp.utils

import androidx.room.TypeConverter
import com.example.groclistapp.data.model.ShoppingItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return if (value == null) "[]" else gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromShoppingItemList(items: List<ShoppingItem>?): String {
        return gson.toJson(items ?: emptyList<ShoppingItem>())

    }

    @TypeConverter
    fun toShoppingItemList(data: String?): List<ShoppingItem> {
        if (data.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<ShoppingItem>>() {}.type
        return gson.fromJson(data, listType)
    }
}

