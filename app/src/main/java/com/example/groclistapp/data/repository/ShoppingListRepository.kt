package com.example.groclistapp.data.repository

import androidx.lifecycle.LiveData
import android.util.Log
import com.example.groclistapp.data.database.dao.ShoppingItemDao
import com.example.groclistapp.data.database.dao.ShoppingListDao
import com.example.groclistapp.data.database.schema.ShoppingList
import com.example.groclistapp.data.database.schema.ShoppingItem
import com.example.groclistapp.data.database.schema.ShoppingListWithItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FieldPath


class ShoppingListRepository(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao
) {
    private val db = FirebaseFirestore.getInstance()

    val allShoppingLists: LiveData<List<ShoppingList>> =
        shoppingListDao.getAllShoppingLists(FirebaseAuth.getInstance().uid)

    suspend fun insertList(shoppingList: ShoppingList): Boolean {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Log.e("ShoppingListRepository", "Cannot create list. User is not logged in!")
            return false
        }

        val newList = ShoppingList(
            id = shoppingList.id,
            name = shoppingList.name,
            description = shoppingList.description,
            imageUrl = shoppingList.imageUrl,
            creatorId = user.uid,
            shareCode = shoppingList.shareCode
        )

        shoppingListDao.insertShoppingList(newList)
        try {
            db.collection("shoppingLists")
                .document(newList.id)
                .set(newList, SetOptions.merge())
                .await()
            return true
        } catch (e: Exception) {
            Log.e("ShoppingListRepository", "Error saving list to Firestore: ${e.message}")
            return false
        }
    }

    suspend fun update(shoppingList: ShoppingList) {
        shoppingListDao.updateShoppingList(
            ShoppingList(
                id = shoppingList.id,
                name = shoppingList.name,
                description = shoppingList.description,
                imageUrl = shoppingList.imageUrl,
                creatorId = shoppingList.creatorId,
                shareCode = shoppingList.shareCode
            )
        )
        updateShoppingListInFirestore(shoppingList)
    }


    suspend fun delete(shoppingList: ShoppingList) {
        shoppingListDao.deleteShoppingList(
            ShoppingList(id = shoppingList.id, name = shoppingList.name)
        )
        deleteShoppingListFromFirestore(shoppingList.id)
    }

    suspend fun getShoppingListById(listId: String): ShoppingListWithItems? {
        val localList = shoppingListDao.getListById(listId)
        if (localList != null) return localList

        return try {
            val documentSnapshot = db.collection("shoppingLists")
                .document(listId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                val firebaseList = documentSnapshot.toObject(ShoppingList::class.java)
                if (firebaseList != null) {
                    val itemsSnapshot = db.collection("shoppingLists")
                        .document(listId)
                        .collection("items")
                        .get()
                        .await()

                    val items = itemsSnapshot.toObjects(ShoppingItem::class.java)
                    ShoppingListWithItems(
                        items = items,
                        shoppingList = firebaseList
                    )
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("Firestore", "Error retrieving document from Firestore: ${e.message}")
            null
        }
    }
    
//    fun getItemsForList(listId: String): LiveData<List<ShoppingItem>> {
//        return shoppingItemDao.getItemsForList(listId)
//    }

    fun getCreatorNames(
        creatorIds: List<String>,
        callback: (Map<String, String>) -> Unit
    ) {
        val uniqueIds = creatorIds.filter { it.isNotBlank() }.distinct()

        if (uniqueIds.isEmpty()) {
            callback(emptyMap())
            return
        }

        val batches = uniqueIds.chunked(10)
        val resultMap = mutableMapOf<String, String>()
        var completedBatches = 0

        for (batch in batches) {
            db.collection("users")
                .whereIn(FieldPath.documentId(), batch)
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val name = doc.getString("fullName") ?: "Unknown"
                        resultMap[id] = name
                    }

                    completedBatches++
                    if (completedBatches == batches.size) {
                        callback(resultMap)
                    }
                }
                .addOnFailureListener {
                    for (id in batch) {
                        resultMap[id] = "Unknown"
                    }
                    completedBatches++

                    if (completedBatches == batches.size) {
                        callback(resultMap)
                    }
                }
        }
    }

    suspend fun insertItems(items: List<ShoppingItem>) {
        try {
            shoppingItemDao.upsertItems(items)
            val listRef = db.collection("shoppingLists").document(items[0].listId)
            val itemRef = listRef.collection("items")
            val batch = db.batch()

            items.map { it ->
                val itemDocRef = itemRef.document(it.id)
                batch.set(itemDocRef, it)
            }

            batch.commit().await()

        } catch (e: Exception) {
            Log.e("insertItems", "Error adding items to Firestore: ${e.message}")
        }
    }

    fun updateShoppingListInFirestore(shoppingList: ShoppingList) {
        val data = mapOf(
            "name" to shoppingList.name,
            "description" to shoppingList.description,
            "imageUrl" to shoppingList.imageUrl,
            "creatorId" to shoppingList.creatorId,
            "items" to emptyList<Any>()
        )

        db.collection("shoppingLists")
            .document(shoppingList.id)
            .set(data, SetOptions.merge())
            .addOnFailureListener {
                Log.e("Firestore", "Error updating shopping list: ${it.message}")
            }
    }



    fun deleteShoppingListFromFirestore(listId: String) {
        val listRef = db.collection("shoppingLists").document(listId)
        val itemsRef = listRef.collection("items")

        itemsRef.get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents

                if (documents.isEmpty()) {
                    deleteListDocument(listRef)
                    return@addOnSuccessListener
                }

                var deletedCount = 0
                for (doc in documents) {
                    doc.reference.delete()
                        .addOnSuccessListener {
                            deletedCount++

                            if (deletedCount == documents.size) {
                                deleteListDocument(listRef)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error deleting item: ${e.message}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error retrieving items for deletion: ${e.message}", e)
            }
    }


    private fun deleteListDocument(listRef: DocumentReference) {
        listRef.delete()
            .addOnFailureListener {
                Log.e("Firestore", "Error deleting list: ${it.message}", it)
            }
    }

    suspend fun deleteAllItemsForList(listId: String) {
        shoppingItemDao.deleteItemsByListId(listId)

        try {
            val itemsRef = db.collection("shoppingLists")
                .document(listId)
                .collection("items")

            val snapshot = itemsRef.get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }

        } catch (e: Exception) {
            Log.e("ShoppingListRepository", "Failed to delete items from Firestore: ${e.message}")
        }
    }

    fun addSharedListByCode(
        shareCode: String,
        onSuccess: (ShoppingList) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onFailure(Exception("User is not authenticated"))
            return
        }

        fetchSharedList(shareCode,
            onFailure = onFailure,
            onListFetched = { document, sharedList ->
                if (sharedList.creatorId == currentUser.uid) {
                    onFailure(Exception("You cannot add your own list"))
                    return@fetchSharedList
                }

                checkIfUserAlreadyHasList(
                    userId = currentUser.uid,
                    listId = sharedList.id,
                    onAlreadyExists = {
                        onFailure(Exception("This list is already added to your account"))
                    },
                    onNotExists = {
                        saveListAndItemsLocallyAndLinkUser(
                            documentId = document.id,
                            sharedList = sharedList,
                            userId = currentUser.uid,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    },
                    onFailure = onFailure
                )
            }
        )
    }

    private fun fetchSharedList(
        shareCode: String,
        onFailure: (Exception) -> Unit,
        onListFetched: (DocumentSnapshot, ShoppingList) -> Unit
    ) {
        db.collection("shoppingLists")
            .whereEqualTo("shareCode", shareCode)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onFailure(Exception("No list found with this share code"))
                    return@addOnSuccessListener
                }

                val document = documents.documents[0]
                val sharedList = document.toObject(ShoppingList::class.java)
                sharedList?.creatorId = document.getString("creatorId") ?: ""

                if (sharedList == null) {
                    Log.e("SharedList", "Failed to convert document to ShoppingList object")
                    onFailure(Exception("Invalid shared list format"))
                    return@addOnSuccessListener
                }

                onListFetched(document, sharedList)
            }
            .addOnFailureListener { e ->
                onFailure(Exception("Failed to retrieve list: ${e.message}"))
            }
    }

    private fun checkIfUserAlreadyHasList(
        userId: String,
        listId: String,
        onAlreadyExists: () -> Unit,
        onNotExists: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userSnapshot ->
                val sharedListIds = userSnapshot.get("sharedListIds") as? List<*> ?: emptyList<Any>()
                if (sharedListIds.contains(listId)) {
                    onAlreadyExists()
                } else {
                    onNotExists()
                }
            }
            .addOnFailureListener { e ->
                onFailure(Exception("Failed to retrieve user data: ${e.message}"))
            }
    }

    private fun saveListAndItemsLocallyAndLinkUser(
        documentId: String,
        sharedList: ShoppingList,
        userId: String,
        onSuccess: (ShoppingList) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                shoppingListDao.insertShoppingList(sharedList)
                fetchAndInsertItems(
                    documentId = documentId,
                    listId = sharedList.id,
                    onSuccess = {
                        linkListToUser(userId, sharedList.id, onSuccess, onFailure, sharedList)
                    },
                    onFailure = onFailure
                )
            } catch (e: Exception) {
                onFailure(Exception("Error saving shared list locally: ${e.message}"))
            }
        }
    }

    private fun fetchAndInsertItems(
        documentId: String,
        listId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("shoppingLists").document(documentId)
            .collection("items")
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.toObjects(ShoppingItem::class.java).onEach {
                    it.listId = listId
                }

                CoroutineScope(Dispatchers.IO).launch {
                    shoppingItemDao.upsertItems(items)
                    onSuccess()
                }
            }
            .addOnFailureListener { e ->
                onFailure(Exception("Failed to load items: ${e.message}"))
            }
    }

    private fun linkListToUser(
        userId: String,
        listId: String,
        onSuccess: (ShoppingList) -> Unit,
        onFailure: (Exception) -> Unit,
        sharedList: ShoppingList
    ) {
        db.collection("users").document(userId)
            .update("sharedListIds", FieldValue.arrayUnion(listId))
            .addOnSuccessListener { onSuccess(sharedList) }
            .addOnFailureListener { e ->
                onFailure(Exception("Failed to update user with shared list: ${e.message}"))
            }
    }

    suspend fun loadAllUserDataFromFirebase() {
       val user = FirebaseAuth.getInstance().currentUser ?: return

        try {
            val snapshot = db.collection("shoppingLists")
                .whereEqualTo("creatorId", user.uid)
                .get()
                .await()

            for (doc in snapshot.documents) {
                val list = doc.toObject(ShoppingList::class.java) ?: continue
                shoppingListDao.insertShoppingList(list)

                val itemsSnapshot = doc.reference.collection("items").get().await()
                val items = itemsSnapshot.toObjects(ShoppingItem::class.java)
                items.forEach { item ->
                    item.listId = list.id
                }
                shoppingItemDao.upsertItems(items)
            }

        } catch (e: Exception) {
            Log.e("Sync", "Error loading user data from Firebase: ${e.message}")
        }
    }

    suspend fun loadAllSharedListsFromFirebase() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        try {
            val userDoc = db.collection("users").document(user.uid).get().await()
            val sharedListIds = userDoc.get("sharedListIds") as? List<String> ?: emptyList()

            if (sharedListIds.isEmpty()) {
               return
            }

            val chunks = sharedListIds.chunked(10)
            for (chunk in chunks) {
                val listsSnapshot = db.collection("shoppingLists")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                for (doc in listsSnapshot.documents) {
                    val list = doc.toObject(ShoppingList::class.java) ?: continue
                    shoppingListDao.insertShoppingList(list)

                    val itemsSnapshot = doc.reference.collection("items").get().await()
                    val items = itemsSnapshot.toObjects(ShoppingItem::class.java)
                    items.forEach { it.listId = list.id }
                    shoppingItemDao.upsertItems(items)
                }
            }

        } catch (e: Exception) {
            Log.e("SharedSync", "Failed loading shared lists: ${e.message}")
        }
    }

    fun removeListIdFromSharedListArray(listId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userDoc = db.collection("users").document(user.uid)

        userDoc.update("sharedListIds", FieldValue.arrayRemove(listId))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}





