package com.example.groclistapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun registerUserWithProfileImage(
        fullName: String,
        email: String,
        imageUri: Uri?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false, "User ID not found")
            return
        }

        val userRef = db.collection("users").document(userId)

        fun saveUserData(imageUrl: String?) {
            val userData = hashMapOf(
                "fullName" to fullName,
                "email" to email
            )
            imageUrl?.let { userData["imageUrl"] = it }

            userRef.set(userData)
                .addOnSuccessListener { onComplete(true, null) }
                .addOnFailureListener { e -> onComplete(false, e.message) }
        }

        if (imageUri != null) {
            val imageRef = storage.reference.child("profile_images/$userId.jpg")
            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveUserData(uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, "Image upload failed: ${e.message}")
                }
        } else {
            saveUserData(null)
        }
    }
}
