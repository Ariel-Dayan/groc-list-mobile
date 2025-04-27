package com.example.groclistapp.data.repository

import androidx.lifecycle.LiveData
import android.util.Log
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingItem
import com.example.groclistapp.data.model.ShoppingListSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FieldPath


class ShoppingListRepository(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingItemDao: ShoppingItemDao
) {
    private val db = FirebaseFirestore.getInstance()

    val allShoppingLists: LiveData<List<ShoppingListSummary>> = shoppingListDao.getAllShoppingLists().also {
        Log.d("ShoppingListRepository", " משיכת רשימות מהמסד: ${it.value?.size ?: 0}")
    }


    fun generateShareCode(): String {
        val timestamp = System.currentTimeMillis() / 1000
        val salt = Random.nextInt(0, 100)
        val combined = timestamp * 100 + salt
        return combined.toString(36).uppercase()
    }

    suspend fun insertAndGetId(shoppingList: ShoppingListSummary): Boolean {
        val user = FirebaseAuth.getInstance().currentUser

        Log.d("ShoppingListRepository", "insertAndGetId() called. user=$user, uid=${user?.uid}")

        if (user == null) {
            Log.e("ShoppingListRepository", "Cannot create list. User is not logged in!")
            return false
        }

        Log.d("ShoppingListRepository", "ShoppingListSummary received: id=${shoppingList.id}, name=${shoppingList.name}, itemsCount=${shoppingList.itemsCount}, creatorId=${shoppingList.creatorId}")

        val newList = ShoppingList(
            id = shoppingList.id,
            name = shoppingList.name,
            description = shoppingList.description,
            items = emptyList(),
            imageUrl = shoppingList.imageUrl,
            creatorId = user.uid,
            shareCode = generateShareCode()
        )

        Log.d("ShoppingListRepository", "Creating newList: id=${newList.id}, name=${newList.name}, creatorId=${newList.creatorId}, shareCode=${newList.shareCode}")

        val listId = shoppingListDao.insertShoppingList(newList)

        Log.d("ShoppingListRepository", "Local DB saved. listId=$listId. Proceeding to save to Firestore...")

        try {
            db.collection("shoppingLists")
                .document(newList.id)
                .set(newList, SetOptions.merge())
                .await()
            Log.d("ShoppingListRepository", "List successfully saved to Firestore with ID: $listId, creatorId=${user.uid}, shareCode=${newList.shareCode}")
            return true
        } catch (e: Exception) {
            Log.e("ShoppingListRepository", "Error saving list to Firestore: ${e.message}")
            return false
        }
    }

    suspend fun update(shoppingList: ShoppingListSummary) {
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


    suspend fun delete(shoppingList: ShoppingListSummary) {
        Log.d("RepoDelete", "מוחק את הרשימה ממסד מקומי ו-Firestore: id=${shoppingList.id}")
        shoppingListDao.deleteShoppingList(
            ShoppingList(id = shoppingList.id, name = shoppingList.name)
        )
        deleteShoppingListFromFirestore(shoppingList.id)
    }

    suspend fun getShoppingListById(listId: String): ShoppingListSummary? {

        val localList = shoppingListDao.getListById(listId)
        if (localList != null) return localList

        return try {
            val documentSnapshot = db.collection("shoppingLists")
                .document(listId.toString())
                .get()
                .await()

            if (documentSnapshot.exists()) {
                val firebaseList = documentSnapshot.toObject(ShoppingList::class.java)
                if (firebaseList != null) {

                    shoppingListDao.insertShoppingList(firebaseList)
                    shoppingListDao.getListById(listId)
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("Firestore", " שגיאה בשליפה מ־Firestore: ${e.message}")
            null
        }
    }


    fun getItemsForList(listId: String): LiveData<List<ShoppingItem>> {
        return shoppingItemDao.getItemsForList(listId)
    }

    fun getCreatorName(creatorId: String, callback: (String) -> Unit) {
        Log.d("CreatorLookup", "Looking up user by ID: $creatorId")

        if (creatorId.isBlank()) {
            callback("Unknown")
            return
        }
        val userRef = db.collection("users").document(creatorId)
        userRef.get()
            .addOnSuccessListener { document ->
                Log.d("CreatorLookup", "User exists=${document.exists()}, fullName=${document.getString("fullName")}")

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
        Log.d("InsertItemDebug", "insertItem called: name=${item.name}, amount=${item.amount}, listId=${item.listId}")

        val itemId = shoppingItemDao.insertItem(item)
        val itemWithId = item.copy(id = itemId.toInt())

        Log.d("InsertItemDebug", "Local DB inserted item '${item.name}' with ID: ${itemWithId.id}")

        val listRef = db.collection("shoppingLists").document(itemWithId.listId.toString())
        val itemRef = listRef.collection("items").document(itemWithId.id.toString())

        val newItemData = mapOf(
            "id" to itemWithId.id,
            "name" to itemWithId.name,
            "amount" to itemWithId.amount,
            "listId" to itemWithId.listId
        )

        try {
            Log.d("InsertItemDebug", "Attempting to add item '${item.name}' to Firestore under listId: ${itemWithId.listId}")
            itemRef.set(newItemData).await()
            Log.d("InsertItemDebug", "Successfully added item '${item.name}' in Firestore at items/${itemWithId.id}")
        } catch (e: Exception) {
            Log.e("InsertItemDebug", "Error adding item '${item.name}' to Firestore: ${e.message}")
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

    fun updateShoppingListInFirestore(shoppingList: ShoppingListSummary) {
        Log.d("UpdateTest", " נכנס לפונקציית updateShoppingListInFirestore")
        Log.d("UpdateRepo", "Preparing to update local DB with imageUrl: ${shoppingList.imageUrl}")
        val data = mapOf(
            "name" to shoppingList.name,
            "description" to shoppingList.description,
            "imageUrl" to shoppingList.imageUrl,
            "creatorId" to shoppingList.creatorId,
            "items" to emptyList<Any>()
        )

        db.collection("shoppingLists")
            .document(shoppingList.id.toString())
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", " רשימה עודכנה/נוצרה בהצלחה: ${shoppingList.id}")
            }
            .addOnFailureListener {
                Log.e("Firestore", " שגיאה בעדכון הרשימה: ${it.message}")
            }
    }



    fun deleteShoppingListFromFirestore(listId: String) {
        Log.d("Firestore", ">>> התחיל deleteShoppingListFromFirestore עבור ID=$listId")
        val listRef = db.collection("shoppingLists").document(listId.toString())
        val itemsRef = listRef.collection("items")

        itemsRef.get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents

                if (documents.isEmpty()) {
                    Log.d("Firestore", "אין פריטים למחוק, מוחק את הרשימה עצמה מיד")
                    deleteListDocument(listRef, listId)
                    return@addOnSuccessListener
                }

                var deletedCount = 0
                for (doc in documents) {
                    doc.reference.delete()
                        .addOnSuccessListener {
                            deletedCount++
                            Log.d("Firestore", "פריט נמחק (${deletedCount}/${documents.size})")

                            if (deletedCount == documents.size) {
                                Log.d("Firestore", "כל הפריטים נמחקו. מוחק את הרשימה.")
                                deleteListDocument(listRef, listId)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", " שגיאה במחיקת פריט: ${e.message}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", " שגיאה באחזור פריטים למחיקה: ${e.message}", e)
            }
    }


    private fun deleteListDocument(listRef: DocumentReference, listId: String) {
        listRef.delete()
            .addOnSuccessListener {
                Log.d("Firestore", " רשימה נמחקה בהצלחה: $listId")
            }
            .addOnFailureListener {
                Log.e("Firestore", " שגיאה במחיקת הרשימה: ${it.message}", it)
            }
    }


    private fun saveItemToFirestore(item: ShoppingItem) {
        val listRef = db.collection("shoppingLists").document(item.listId.toString())

        listRef.update("items", FieldValue.arrayUnion(mapOf("name" to item.name, "amount" to item.amount)))
            .addOnSuccessListener {
                Log.d("Firestore", " פריט נוסף בהצלחה לרשימה `items` בפיירבייס")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", " שגיאה בהוספת הפריט: ${e.message}")
            }
    }

    private fun updateItemInFirestore(item: ShoppingItem) {
        db.collection("shoppingLists")
            .document(item.listId.toString())
            .collection("items")
            .document(item.id.toString())
            .update("name", item.name, "amount", item.amount)
            .addOnSuccessListener {
                Log.d("Firestore", " פריט עודכן בהצלחה: ${item.id}")
            }
            .addOnFailureListener {
                Log.e("Firestore", " שגיאה בעדכון הפריט: ${it.message}")
            }
    }

    private fun deleteItemFromFirestore(itemId: Int) {
        db.collection("shoppingLists")
            .document(itemId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d("Firestore", " פריט נמחק בהצלחה: $itemId")
            }
            .addOnFailureListener {
                Log.e("Firestore", " שגיאה במחיקת הפריט: ${it.message}")
            }
    }

    fun addItemToFirestore(item: ShoppingItem) {
        val itemMap = hashMapOf(
            "name" to item.name,
            "amount" to item.amount
        )

        val itemRef = db.collection("shoppingLists")
            .document(item.listId.toString())
            .collection("items")
            .document(item.id.toString())

        itemRef.set(itemMap)
            .addOnSuccessListener {
                Log.d("Firestore", " פריט נשמר כתת-collection עם ID: ${item.id}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", " שגיאה בהוספת פריט ל־items: ${e.message}")
            }
    }

//    suspend fun fetchItemsFromFirestore(listId: Int) {
//        try {
//            val snapshot = db.collection("shoppingLists")
//                .document(listId.toString())
//                .collection("items")
//                .get()
//                .await()
//
//            val items = snapshot.documents.mapNotNull { doc ->
//                val name = doc.getString("name") ?: return@mapNotNull null
//                val amount = doc.getLong("amount")?.toInt() ?: 1
//                val id = doc.id.toIntOrNull() ?: return@mapNotNull null
//                ShoppingItem(id = id, name = name, amount = amount, listId = listId)
//            }
//
//            shoppingItemDao.insertAll(items)
//            Log.d("Firestore", " נטענו ${items.size} פריטים מ־Firestore")
//
//        } catch (e: Exception) {
//            Log.e("Firestore", " שגיאה בטעינת פריטים מ־Firestore: ${e.message}")
//        }
//    }

    suspend fun deleteAllItemsForList(listId: String) {

        shoppingItemDao.deleteItemsByListId(listId)

        try {
            val itemsRef = db.collection("shoppingLists")
                .document(listId.toString())
                .collection("items")

            val snapshot = itemsRef.get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Log.d("ShoppingListRepository", "All items deleted from Firestore for listId $listId")
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
                    items.forEach { shoppingItemDao.insertItem(it) }
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

    suspend fun clearAllLocalData() {
        shoppingItemDao.deleteAllShoppingItems()
        shoppingListDao.deleteAllShoppingLists()
    }

    suspend fun loadAllUserDataFromFirebase() {
       val user = FirebaseAuth.getInstance().currentUser ?: return
        Log.d("Sync", "Fetching lists for UID: ${user.uid}")
        try {
            val snapshot = db.collection("shoppingLists")
                .whereEqualTo("creatorId", user.uid)
                .get()
                .await()
            Log.d("Sync", "Fetched ${snapshot.documents.size} lists from Firebase")
            for (doc in snapshot.documents) {
                val list = doc.toObject(ShoppingList::class.java) ?: continue
                shoppingListDao.insertShoppingList(list)

                val itemsSnapshot = doc.reference.collection("items").get().await()
                val items = itemsSnapshot.toObjects(ShoppingItem::class.java)
                items.forEach { item ->
                    item.listId = list.id
                    shoppingItemDao.insertItem(item)
                }
            }

            Log.d("Sync", "User data successfully loaded from Firebase")

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
                Log.d("SharedSync", "No sharedListIds found for user ${user.uid}")
                return
            }

            Log.d("SharedSync", "Found ${sharedListIds.size} sharedListIds for user ${user.uid}")

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
                    items.forEach { shoppingItemDao.insertItem(it) }

                    Log.d("SharedSync", "Inserted shared list ${list.id} with ${items.size} items")
                }
            }

        } catch (e: Exception) {
            Log.e("SharedSync", "Failed loading shared lists: ${e.message}")
        }
    }

    fun removeListIdFromSharedListArray(listId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val userDoc = db.collection("users").document(user.uid)
        userDoc.update("sharedListIds", FieldValue.arrayRemove(listId))
            .addOnSuccessListener {
                Log.d("SharedList", "Removed listId=$listId from sharedListIds of user ${user.uid}")
            }
            .addOnFailureListener { e ->
                Log.e("SharedList", "Failed to remove listId from sharedListIds: ${e.message}")
            }
    }


}





