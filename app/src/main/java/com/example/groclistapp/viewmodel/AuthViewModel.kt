package com.example.groclistapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> get() = _loginStatus

    private val _signupStatus = MutableLiveData<Boolean>()
    val signupStatus: LiveData<Boolean> get() = _signupStatus

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> get() = _currentUser

    init {
        checkUserLoggedIn()
    }

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loginStatus.value = task.isSuccessful
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                }
            }
    }

    fun signup(email: String, password: String, fullName: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        _signupStatus.value = true
                    }
                } else {
                    _signupStatus.value = false
                }
            }
    }

    fun checkUserLoggedIn() {
        _currentUser.value = auth.currentUser
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}

