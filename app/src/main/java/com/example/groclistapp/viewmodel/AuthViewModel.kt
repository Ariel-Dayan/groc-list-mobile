package com.example.groclistapp.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import com.example.groclistapp.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val repository = AuthRepository()

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

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        checkUserLoggedIn()
    }

    fun login(email: String, password: String) {
        _isLoading.value = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _loginStatus.value = true
                    _currentUser.value = auth.currentUser
                    Log.d("AuthViewModel", "Login success. UID=${auth.currentUser?.uid}")
                } else {
                    _loginStatus.value = false
                    Log.e("LoginError", task.exception?.message ?: "Unknown error")
                    _errorMessage.value = task.exception?.message ?: "Login failed"
                }
            }
    }

    fun signup(email: String, password: String, fullName: String, imageUri: Uri?) {
        _signupStatus.value = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            repository.registerUserWithProfileImage(
                                fullName = fullName,
                                email = email,
                                imageUri = imageUri
                            ) { success, error ->
                                _signupStatus.postValue(success)
                                _errorMessage.postValue(error ?: "Unknown error")

                            }
                        } else {
                            _signupStatus.value = false
                            _errorMessage.value = "Failed to update profile"
                        }
                    }
                } else {
                    _signupStatus.value = false
                    _errorMessage.value = task.exception?.message ?: "Signup failed"
                }
            }
    }

    private fun saveUserToFirestore(userId: String, fullName: String, email: String) {
        val userInfo = hashMapOf(
            "name" to fullName,
            "email" to email
        )

        db.collection("users").document(userId).set(userInfo)
            .addOnSuccessListener {
                Log.d("Firestore", "✅ User added to Firestore!")
                _signupStatus.value = true
            }
            .addOnFailureListener {
                Log.e("Firestore", "❌ Failed to add user: ${it.message}")
                _signupStatus.value = false
            }
    }

    fun updateProfile(fullName: String, oldPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user == null) {
            _errorMessage.value = "No user is logged in."
            _updateProfileStatus.value = false
            return
        }

        // ... (אין שינוי משמעותי פה)
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

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }


    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }
}

