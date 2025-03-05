package com.example.groclistapp.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var signupNavigationButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        emailInput = view.findViewById(R.id.tilLoginEmail)
        passwordInput = view.findViewById(R.id.tilLoginPassword)
        loginButton = view.findViewById(R.id.btnLoginEnter)
        signupNavigationButton = view.findViewById(R.id.btnSignupNavigation)

        // בדיקה אם המשתמש כבר מחובר
        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_myCardsListFragment)
        }

        loginButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim()
            val password = passwordInput.text?.toString()?.trim()

            if (!email.isNullOrEmpty() && !password.isNullOrEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        signupNavigationButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_loginFragment_to_myCardsListFragment)
                } else {
                    handleAuthError(task.exception)
                }
            }
    }

    private fun handleAuthError(exception: Exception?) {
        Toast.makeText(requireContext(), "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
    }
}


