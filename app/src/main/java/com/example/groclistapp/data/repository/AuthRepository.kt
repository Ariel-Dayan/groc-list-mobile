package com.example.groclistapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun registerUser(
        fullName: String,
        email: String,
        password: String,
        imageUri: Uri?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            completeUserRegistration(
                                fullName = fullName,
                                email = email,
                                imageUri = imageUri,
                                onComplete = onComplete
                            )
                        } else {
                            onComplete(false, "Failed to update profile")
                        }
                    }
                } else {
                    onComplete(false, task.exception?.message ?: "Signup failed")
                }
            }
    }

    fun login(email: String, password: String, onComplete: (Boolean, FirebaseUser?, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    onComplete(true, user, null)
                } else {
                    onComplete(false, null, task.exception?.message ?: "Login failed")
                }
            }
    }

    fun logout(callback: (Boolean) -> Unit) {
        try {
            FirebaseAuth.getInstance().signOut()
            callback(true)
        } catch (e: Exception) {
            callback(false)
        }
    }
    
    private fun completeUserRegistration(
        fullName: String,
        email: String,
        imageUri: Uri?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(false, "User ID not found")
            return
        }

        if (imageUri != null) {
            uploadProfileImageAndSaveData(
                storage = storage,
                db = db,
                userId = userId,
                imageUri = imageUri,
                fullName = fullName,
                email = email,
                onComplete = onComplete
            )
        } else {
            saveUserData(db, userId, fullName, email, null, onComplete)
        }
    }

    private fun saveUserData(
        db: FirebaseFirestore,
        userId: String,
        fullName: String,
        email: String,
        imageUrl: String?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val userRef = db.collection("users").document(userId)
        val userData = hashMapOf(
            "fullName" to fullName,
            "email" to email
        )
        imageUrl?.let { userData["imageUrl"] = it }

        userRef.set(userData)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }

    private fun uploadProfileImageAndSaveData(
        storage: FirebaseStorage,
        db: FirebaseFirestore,
        userId: String,
        imageUri: Uri,
        fullName: String,
        email: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val imageRef = storage.reference.child("profile_images/$userId.jpg")
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveUserData(db, userId, fullName, email, uri.toString(), onComplete)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, "Image upload failed: ${e.message}")
            }
    }
}
