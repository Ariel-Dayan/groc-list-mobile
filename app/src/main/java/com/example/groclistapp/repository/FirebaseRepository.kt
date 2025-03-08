package com.example.groclistapp.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.example.groclistapp.model.ShoppingList

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()

    //  הוספת רשימת קניות עם מזהה אוטומטי
    fun addShoppingList(shoppingList: ShoppingList, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("shoppingLists").add(shoppingList)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "List added with ID: ${documentReference.id}")
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
                onFailure(e)
            }
    }

    //  יצירת רשימת קניות עם מזהה קבוע
    fun setShoppingList(listId: String, shoppingList: ShoppingList, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("shoppingLists").document(listId).set(shoppingList)
            .addOnSuccessListener {
                Log.d("Firestore", "List set successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error setting document", e)
                onFailure(e)
            }
    }

    //  שליפת כל הרשימות מהמסד
    fun getShoppingLists(onSuccess: (List<ShoppingList>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("shoppingLists").get()
            .addOnSuccessListener { result ->
                val lists = result.documents.mapNotNull { it.toObject(ShoppingList::class.java) }
                onSuccess(lists)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error getting documents", e)
                onFailure(e)
            }
    }

    //  עדכון רשימת קניות (שדות ספציפיים)
    fun updateShoppingList(listId: String, updatedData: Map<String, Any>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("shoppingLists").document(listId).update(updatedData)
            .addOnSuccessListener {
                Log.d("Firestore", "List updated successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error updating document", e)
                onFailure(e)
            }
    }

    //  מחיקת רשימה לפי ה-ID שלה
    fun deleteShoppingList(listId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("shoppingLists").document(listId).delete()
            .addOnSuccessListener {
                Log.d("Firestore", "List deleted successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error deleting document", e)
                onFailure(e)
            }
    }
}
