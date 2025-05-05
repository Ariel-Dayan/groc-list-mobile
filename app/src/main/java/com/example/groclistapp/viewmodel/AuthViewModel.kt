package com.example.groclistapp.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.groclistapp.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val repository = AuthRepository()

    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> get() = _loginStatus

    private val _signupStatus = MutableLiveData<Boolean>()
    val signupStatus: LiveData<Boolean> get() = _signupStatus

    private val _logoutStatus = MutableLiveData<Boolean>()
    val logoutStatus: LiveData<Boolean> get() = _logoutStatus

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: MutableLiveData<String?> get() = _errorMessage


    init {
        checkUserLoggedIn()
    }

    fun login(email: String, password: String) {
        repository.login(email, password) { success, _, error ->
            _loginStatus.postValue(success)

            if (!success) {
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
                _errorMessage.postValue(error)
            }
        }
    }

    private fun checkUserLoggedIn() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { reloadTask ->
            Log.d("AuthViewModel", "User reload task: ${reloadTask.isSuccessful}")
        }
    }

    fun logout() {
        repository.logout { success ->
            _logoutStatus.postValue(success)
        }
    }
}

