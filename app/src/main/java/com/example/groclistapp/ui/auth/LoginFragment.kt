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

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signupNavigationButton: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailInput = view.findViewById(R.id.tilLoginEmail)
        passwordInput = view.findViewById(R.id.tilLoginPassword)
        loginButton = view.findViewById(R.id.btnLoginEnter)
        signupNavigationButton = view.findViewById(R.id.btnSignupNavigation)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            authViewModel.login(email, password)
        }

        authViewModel.loginStatus.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                findNavController().popBackStack() // מחזיר את המשתמש אחורה למסך הבית
            } else {
                Toast.makeText(requireContext(), "Login failed", Toast.LENGTH_SHORT).show()
            }
        })

        signupNavigationButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }
}




