package com.example.groclistapp.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.groclistapp.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth

class ProfileViewModel : ViewModel() {

    private val repository = ProfileRepository()

    private val _displayName = MutableLiveData<String>()
    val displayName: LiveData<String> get() = _displayName

    fun updateProfile(fullName: String?, oldPassword: String?, newPassword: String?, imageUri: Uri?, callback: (Boolean, String) -> Unit) {
        repository.updateUserProfile(fullName, oldPassword, newPassword, imageUri, callback)
    }

    fun loadProfileImage(userId: String, callback: (String?) -> Unit) {
        repository.getProfileImageUrl(userId, callback)
    }

    fun loadDisplayName() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        _displayName.value = currentUser?.displayName ?: ""
    }
}


