package com.example.groclistapp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.google.firebase.firestore.FirebaseFirestore

class ProfileRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    fun updateUserProfile(
        fullName: String?,
        oldPassword: String?,
        newPassword: String?,
        imageUri: Uri?,
        callback: (Boolean, String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            callback(false, "No user is logged in")
            return
        }

        if (!oldPassword.isNullOrEmpty() && !newPassword.isNullOrEmpty()) {
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
            user.reauthenticate(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    updateNameAndPasswordAndImage(user, fullName, newPassword, imageUri, callback)
                } else {
                    callback(false, "Re-authentication failed. Please check your old password")
                }
            }
        } else {
            updateNameAndPasswordAndImage(user, fullName, null, imageUri, callback)
        }
    }

    private fun updateNameAndPasswordAndImage(
        user: FirebaseUser,
        fullName: String?,
        newPassword: String?,
        imageUri: Uri?,
        callback: (Boolean, String) -> Unit
    ) {
        updateDisplayName(user, fullName) { displayOk, displayErr ->
            if (!displayOk) {
                callback(false, displayErr)
                return@updateDisplayName
            }
            updatePasswordIfNeeded(user, newPassword) { passwordOk, passwordErr ->
                if (!passwordOk) {
                    callback(false, passwordErr)
                    return@updatePasswordIfNeeded
                }
                handleImageUpload(user, imageUri, callback)
            }
        }
    }

    private fun updateDisplayName(
        user: FirebaseUser,
        fullName: String?,
        onComplete: (Boolean, String) -> Unit
    ) {
        val profileBuilder = UserProfileChangeRequest.Builder()
        if (!fullName.isNullOrEmpty()) {
            profileBuilder.setDisplayName(fullName)
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update("fullName", fullName)
        }
        val profileUpdates = profileBuilder.build()
        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, "")
                } else {
                    onComplete(false, "Failed to update profile information")
                }
            }
    }

    private fun updatePasswordIfNeeded(
        user: FirebaseUser,
        newPassword: String?,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (!newPassword.isNullOrEmpty()) {
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onComplete(true, "")
                    } else {
                        onComplete(false, "Failed to update password")
                    }
                }
        } else {
            onComplete(true, "")
        }
    }

    private fun handleImageUpload(
        user: FirebaseUser,
        imageUri: Uri?,
        callback: (Boolean, String) -> Unit
    ) {
        if (imageUri != null) {
            val storageRef = storage.reference
                .child("profile_images/${user.uid}_${UUID.randomUUID()}.jpg")
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(uri)
                            .build()
                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener { updateTask ->
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user.uid)
                                    .update("imageUrl", uri.toString())
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

    fun getProfileImageUrl(userId: String, callback: (String?) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                callback(document.getString("imageUrl"))
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}


