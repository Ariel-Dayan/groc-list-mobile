package com.example.groclistapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> get() = _loginStatus

    private val _signupStatus = MutableLiveData<Boolean>()
    val signupStatus: LiveData<Boolean> get() = _signupStatus

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
        _loginStatus.value = false
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loginStatus.value = task.isSuccessful
                if (task.isSuccessful) {
                    _currentUser.value = auth.currentUser
                } else {
                    Log.e("LoginError", task.exception?.message ?: "Unknown error")
                    _errorMessage.value = task.exception?.message ?: "Login failed"
                }
            }
    }

    fun signup(email: String, password: String, fullName: String) {
        _signupStatus.value = false
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
                    Log.e("SignupError", task.exception?.message ?: "Unknown error")
                    _errorMessage.value = task.exception?.message ?: "Signup failed"
                }
            }
    }

    fun updateProfile(fullName: String, oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user == null) {
            _errorMessage.value = "No user is logged in."
            _updateProfileStatus.value = false
            return
        }

        if (oldPassword.isEmpty()) {
            _errorMessage.value = "Old password is required for authentication."
            _updateProfileStatus.value = false
            return
        }

        val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    Log.d("AuthViewModel", "Re-authentication successful.")

                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Log.d("AuthViewModel", "Profile updated successfully.")

                                if (newPassword.isNotEmpty()) {
                                    user.updatePassword(newPassword)
                                        .addOnCompleteListener { passwordTask ->
                                            if (passwordTask.isSuccessful) {
                                                _updateProfileStatus.value = true
                                                Log.d("AuthViewModel", "Password updated successfully.")
                                            } else {
                                                _errorMessage.value = "Failed to update password."
                                                _updateProfileStatus.value = false
                                            }
                                        }
                                } else {
                                    _updateProfileStatus.value = true
                                }
                            } else {
                                _errorMessage.value = "Profile update failed."
                                _updateProfileStatus.value = false
                            }
                        }
                } else {
                    Log.e("AuthViewModel", "Re-authentication failed: ${authTask.exception?.message}")
                    _errorMessage.value = "Re-authentication failed. Please check your old password."
                    _updateProfileStatus.value = false
                }
            }
    }

    fun checkUserLoggedIn() {
        val user = auth.currentUser
        if (user != null) {
            user.reload().addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    _currentUser.value = auth.currentUser
                    Log.d("UserReload", "User session reloaded successfully.")
                } else {
                    Log.e("UserReloadError", "Failed to reload user session: ${reloadTask.exception?.message}")
                }
            }
        } else {
            _currentUser.value = null
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}

