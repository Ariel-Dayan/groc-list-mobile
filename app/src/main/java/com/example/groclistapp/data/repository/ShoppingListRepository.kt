package com.example.groclistapp.data.repository

import androidx.lifecycle.LiveData
import android.util.Log
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingListSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class ShoppingListRepository(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao
) {
    private val db = FirebaseFirestore.getInstance()

    val allShoppingLists: LiveData<List<ShoppingListSummary>> = shoppingListDao.getAllShoppingLists().also {
        Log.d("ShoppingListRepository", "📥 משיכת רשימות מהמסד: ${it.value?.size ?: 0}")
    }


    fun generateShareCode(): String {
        val timestamp = System.currentTimeMillis() / 1000
        val salt = Random.nextInt(0, 100)
        val combined = timestamp * 100 + salt
        return combined.toString(36).uppercase()
    }

    suspend fun insertAndGetId(shoppingList: ShoppingListSummary): Long {
        val user = FirebaseAuth.getInstance().currentUser

        Log.d("ShoppingListRepository", "insertAndGetId() called. user=$user, uid=${user?.uid}")

        if (user == null) {
            Log.e("ShoppingListRepository", "❌ Cannot create list. User is not logged in!")
            return -1
        }

        Log.d("ShoppingListRepository", "ShoppingListSummary => id=${shoppingList.id}, name=${shoppingList.name}, itemsCount=${shoppingList.itemsCount}, creatorId=${shoppingList.creatorId}")

        val newList = ShoppingList(
            id = shoppingList.id,
            name = shoppingList.name,
            description = shoppingList.description,
            creatorId = user.uid,
            shareCode = generateShareCode()
        )

        Log.d("ShoppingListRepository", "📝 Creating newList => id=${newList.id}, name=${newList.name}, creatorId=${newList.creatorId}, shareCode=${newList.shareCode}")

        val listId = shoppingListDao.insertShoppingList(newList)
        val listWithUpdatedId = newList.copy(id = listId.toInt())

        Log.d("ShoppingListRepository", "Local DB saved. listId=$listId => Now saving to Firestore...")

        db.collection("shoppingLists")
            .document(listId.toString())
            .set(listWithUpdatedId)
            .addOnSuccessListener {
                Log.d("ShoppingListRepository", "✅ רשימה נשמרה בפיירבייס עם ID: $listId, creatorId=${user.uid}, shareCode=${listWithUpdatedId.shareCode}")
            }
            .addOnFailureListener { e ->
                Log.e("ShoppingListRepository", "❌ שגיאה בשמירה בפיירבייס: ${e.message}")
            }

        return listId
    }

    suspend fun update(shoppingList: ShoppingListSummary) {
        shoppingListDao.updateShoppingList(
            ShoppingList(id = shoppingList.id, name = shoppingList.name,
                description = shoppingList.description,  creatorId = shoppingList.creatorId,
                shareCode = shoppingList.shareCode)
        )
        updateShoppingListInFirestore(shoppingList)
    }

    suspend fun delete(shoppingList: ShoppingListSummary) {
        shoppingListDao.deleteShoppingList(
            ShoppingList(id = shoppingList.id, name = shoppingList.name)
        )
        deleteShoppingListFromFirestore(shoppingList.id)
    }

    suspend fun getShoppingListById(listId: Int): ShoppingListSummary? {
        return shoppingListDao.getListById(listId)
    }

    fun getItemsForList(listId: Int): LiveData<List<ShoppingItem>> {
        return shoppingItemDao.getItemsForList(listId)
    }

    fun getCreatorName(creatorId: String, callback: (String) -> Unit) {
        if (creatorId.isBlank()) {
            callback("Unknown")
            return
        }
        val userRef = db.collection("users").document(creatorId)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("fullName") ?: "Unknown"
                    callback(name)
                } else {
                    callback("Unknown")
                }
            }
            .addOnFailureListener {
                callback("Unknown")
            }
    }

    suspend fun insertItem(item: ShoppingItem) {
        shoppingItemDao.insertItem(item)
        saveItemToFirestore(item)
        val listRef = db.collection("shoppingLists").document(item.listId.toString())

        listRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentItems = document.get("items") as? MutableList<Map<String, Any>> ?: mutableListOf()
                val newItem = mapOf("name" to item.name, "amount" to item.amount)

                currentItems.add(newItem)
                listRef.update("items", currentItems)
                    .addOnSuccessListener {
                        Log.d("Firestore", "✅ פריט נוסף בהצלחה לשדה `items` במסמך הראשי של הרשימה")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "❌ שגיאה בשמירת הפריט בתוך `items`: ${e.message}")
                    }
            }
        }.addOnFailureListener {
            Log.e("Firestore", "❌ שגיאה בשליפת הרשימה: ${it.message}")
        }
    }


    suspend fun updateItem(item: ShoppingItem) {
        shoppingItemDao.updateItem(item)
        updateItemInFirestore(item)
    }

    suspend fun deleteItem(item: ShoppingItem) {
        shoppingItemDao.deleteItem(item)
        deleteItemFromFirestore(item.id)
    }

    private fun updateShoppingListInFirestore(shoppingList: ShoppingListSummary) {
        db.collection("shoppingLists")
            .document(shoppingList.id.toString())
            .update("name", shoppingList.name)
            .addOnSuccessListener {
                Log.d("Firestore", "✅ רשימה עודכנה בהצלחה: ${shoppingList.id}")
            }
            .addOnFailureListener {
                Log.e("Firestore", "❌ שגיאה בעדכון הרשימה: ${it.message}")
            }
    }

    private fun deleteShoppingListFromFirestore(listId: Int) {
        db.collection("shoppingLists")
            .document(listId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "🗑️ רשימה נמחקה בהצלחה: $listId")
            }
            .addOnFailureListener {
                Log.e("Firestore", "❌ שגיאה במחיקת הרשימה: ${it.message}")
            }
    }

    private fun saveItemToFirestore(item: ShoppingItem) {
        val listRef = db.collection("shoppingLists").document(item.listId.toString())

        listRef.update("items", FieldValue.arrayUnion(mapOf("name" to item.name, "amount" to item.amount)))
            .addOnSuccessListener {
                Log.d("Firestore", "✅ פריט נוסף בהצלחה לרשימה `items` בפיירבייס")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ שגיאה בהוספת הפריט: ${e.message}")
            }
    }






    private fun updateItemInFirestore(item: ShoppingItem) {
        db.collection("shoppingLists")
            .document(item.listId.toString())
            .collection("items")
            .document(item.id.toString())
            .update("name", item.name, "amount", item.amount)
            .addOnSuccessListener {
                Log.d("Firestore", "✅ פריט עודכן בהצלחה: ${item.id}")
            }
            .addOnFailureListener {
                Log.e("Firestore", "❌ שגיאה בעדכון הפריט: ${it.message}")
            }
    }

    private fun deleteItemFromFirestore(itemId: Int) {
        db.collection("shoppingLists")
            .document(itemId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", "🗑️ פריט נמחק בהצלחה: $itemId")
            }
            .addOnFailureListener {
                Log.e("Firestore", "❌ שגיאה במחיקת הפריט: ${it.message}")
            }
    }
}





