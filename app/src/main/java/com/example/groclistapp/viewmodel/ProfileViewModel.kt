package com.example.groclistapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.groclistapp.data.repository.ProfileRepository

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()

    fun updateProfile(fullName: String?, oldPassword: String?, newPassword: String?, imageUri: Uri?, callback: (Boolean, String) -> Unit) {
        repository.updateUserProfile(fullName, oldPassword, newPassword, imageUri, callback)
    }

    fun loadProfileImage(userId: String, callback: (String?) -> Unit) {
        repository.getProfileImageUrl(userId, callback)
    }

}


