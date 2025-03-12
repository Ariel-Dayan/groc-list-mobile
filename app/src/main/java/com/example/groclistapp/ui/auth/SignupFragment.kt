package com.example.groclistapp.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.viewmodel.AuthViewModel
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton

class SignupFragment : Fragment(R.layout.fragment_signup) {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var emailInput: TextInputEditText
    private lateinit var fullNameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var registerButton: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val emailLayout = view.findViewById<TextInputLayout>(R.id.tilSignupEmail)
            val fullNameLayout = view.findViewById<TextInputLayout>(R.id.tilSignupFullName)
            val passwordLayout = view.findViewById<TextInputLayout>(R.id.tilSignupPassword)
            val confirmPasswordLayout = view.findViewById<TextInputLayout>(R.id.tilSignupConfirmPassword)

            emailInput = emailLayout.editText as? TextInputEditText ?: throw NullPointerException("TextInputEditText for email not found")
            fullNameInput = fullNameLayout.editText as? TextInputEditText ?: throw NullPointerException("TextInputEditText for full name not found")
            passwordInput = passwordLayout.editText as? TextInputEditText ?: throw NullPointerException("TextInputEditText for password not found")
            confirmPasswordInput = confirmPasswordLayout.editText as? TextInputEditText ?: throw NullPointerException("TextInputEditText for confirm password not found")

            registerButton = view.findViewById(R.id.btnSignupRegister)
        } catch (e: Exception) {
            Log.e("SignupFragment", "Error initializing views: ${e.localizedMessage}", e)
            Toast.makeText(requireContext(), "Error initializing UI. Please restart the app.", Toast.LENGTH_LONG).show()
            return
        }

        registerButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim()
            val fullName = fullNameInput.text?.toString()?.trim()
            val password = passwordInput.text?.toString()?.trim()
            val confirmPassword = confirmPasswordInput.text?.toString()?.trim()

            if (email.isNullOrEmpty() || fullName.isNullOrEmpty() || password.isNullOrEmpty() || confirmPassword.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerButton.isEnabled = false
            registerButton.text = getString(R.string.signing_up)
            authViewModel.signup(email, password, fullName)
        }

        authViewModel.signupStatus.observe(viewLifecycleOwner, Observer { isSuccess ->
            registerButton.isEnabled = true
            registerButton.text = getString(R.string.sign_up)
            if (isSuccess) {
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "Signup failed. Try a different email.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}



