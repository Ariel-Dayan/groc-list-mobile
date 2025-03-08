package com.example.groclistapp.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.viewmodel.AuthViewModel
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

        emailInput = view.findViewById(R.id.tilSignupEmail)
        fullNameInput = view.findViewById(R.id.tilSignupFullName)
        passwordInput = view.findViewById(R.id.tilSignupPassword)
        confirmPasswordInput = view.findViewById(R.id.tilSignupConfirmPassword)
        registerButton = view.findViewById(R.id.btnSignupRegister)

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val fullName = fullNameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.signup(email, password, fullName)
        }

        authViewModel.signupStatus.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                findNavController().popBackStack() // מחזיר את המשתמש ל-LoginFragment
            } else {
                Toast.makeText(requireContext(), "Signup failed", Toast.LENGTH_SHORT).show()
            }
        })
    }
}



