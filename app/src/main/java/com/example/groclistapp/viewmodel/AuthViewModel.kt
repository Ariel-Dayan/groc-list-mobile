package com.example.groclistapp.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.groclistapp.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val repository = AuthRepository()

    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> get() = _loginStatus

    private val _signupStatus = MutableLiveData<Boolean>()
    val signupStatus: LiveData<Boolean> get() = _signupStatus

    private val _logoutStatus = MutableLiveData<Boolean>()
    val logoutStatus: LiveData<Boolean> get() = _logoutStatus

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> get() = _currentUser

    private val _updateProfileStatus = MutableLiveData<Boolean>()
    val updateProfileStatus: LiveData<Boolean> get() = _updateProfileStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage


    init {
        checkUserLoggedIn()
    }

    fun login(email: String, password: String) {
        repository.login(email, password) { success, user, error ->
            _loginStatus.postValue(success)

            if (success) {
                _currentUser.postValue(user)
            } else {
                _errorMessage.postValue(error ?: "Login failed")
            }
        }
    }

    fun signup(email: String, password: String, fullName: String, imageUri: Uri?) {
        repository.registerUser(
            fullName = fullName,
            email = email,
            password = password,
            imageUri = imageUri
        ) { success, error ->
            _signupStatus.postValue(success)
            if (!success && error != null) {
                _errorMessage.postValue(error ?: "Unknown error")
            }
        }
    }

    fun checkUserLoggedIn() {
        val user = auth.currentUser
        if (user != null) {
            user.reload().addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    _currentUser.value = auth.currentUser
                }
            }
        } else {
            _currentUser.value = null
        }
    }

    fun logout() {
        repository.logout { success ->
            _logoutStatus.postValue(success)
        }
    }
}

