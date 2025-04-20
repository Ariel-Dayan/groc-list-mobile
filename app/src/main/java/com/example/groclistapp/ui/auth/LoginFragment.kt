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

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signupNavigationButton: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailLayout = view.findViewById<TextInputLayout>(R.id.tilLoginEmail)
        val passwordLayout = view.findViewById<TextInputLayout>(R.id.tilLoginPassword)

        emailInput = emailLayout.editText as TextInputEditText
        passwordInput = passwordLayout.editText as TextInputEditText

        loginButton = view.findViewById(R.id.btnLoginEnter)
        signupNavigationButton = view.findViewById(R.id.btnSignupNavigation)

        loginButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim()
            val password = passwordInput.text?.toString()?.trim()

            if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            loginButton.text = getString(R.string.logging_in)

            authViewModel.login(email, password)
        }

        authViewModel.loginStatus.observe(viewLifecycleOwner, Observer { isSuccess ->
            loginButton.isEnabled = true
            loginButton.text = getString(R.string.login)

            if (isSuccess) {
                Log.d("LoginFragment", "Login successful. Navigating to MyCardsListFragment...")

                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build()

                findNavController().navigate(
                    R.id.myCardsListFragment,
                    null,
                    navOptions
                )
            } else {
                Toast.makeText(requireContext(), "Login failed. Please check your details.", Toast.LENGTH_SHORT).show()
            }
        })

        signupNavigationButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }
}





