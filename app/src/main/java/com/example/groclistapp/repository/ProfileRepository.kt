package com.example.groclistapp.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ProfileRepository {

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun updateUserProfile(
        fullName: String,
        oldPassword: String,
        newPassword: String,
        imageUri: Uri?,
        callback: (Boolean, String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            callback(false, "No user is logged in")
            return
        }

        // שלב 1: אימות המשתמש עם הסיסמה הישנה
        val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
        user.reauthenticate(credential).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                // שלב 2: עדכון השם
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()

                user.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
                    if (profileTask.isSuccessful) {
                        // שלב 3: עדכון סיסמה (אם נבחרה חדשה)
                        if (newPassword.isNotEmpty()) {
                            user.updatePassword(newPassword)
                                .addOnCompleteListener { passwordTask ->
                                    if (passwordTask.isSuccessful) {
                                        handleImageUpload(user, imageUri, callback)
                                    } else {
                                        callback(false, "Failed to update password")
                                    }
                                }
                        } else {
                            handleImageUpload(user, imageUri, callback)
                        }
                    } else {
                        callback(false, "Failed to update profile information")
                    }
                }
            } else {
                callback(false, "Re-authentication failed. Please check your old password")
            }
        }
    }

    private fun handleImageUpload(user: FirebaseUser, imageUri: Uri?, callback: (Boolean, String) -> Unit) {
        if (imageUri != null) {
            val storageRef = storage.reference.child("profile_images/${user.uid}_${UUID.randomUUID()}.jpg")
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(uri)
                            .build()

                        user.updateProfile(profileUpdates).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                callback(true, "Profile updated successfully!")
                            } else {
                                callback(false, "Profile updated, but image update failed")
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    callback(false, "Image upload failed")
                }
        } else {
            callback(true, "Profile updated successfully!")
        }
    }
}


