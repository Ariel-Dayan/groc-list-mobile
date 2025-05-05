package com.example.groclistapp.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.viewmodel.AuthViewModel
import com.example.groclistapp.viewmodel.ShoppingListViewModel
import com.example.groclistapp.viewmodel.SharedCardsViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import android.widget.ProgressBar
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var sharedCardsViewModel: SharedCardsViewModel
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signupNavigationButton: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModels()
        initViews(view)
        setupListeners()
        observeLoginStatus()
    }

    private fun initViewModels() {
        sharedCardsViewModel = ViewModelProvider(this)[SharedCardsViewModel::class.java]
        shoppingListViewModel = ViewModelProvider(this)[ShoppingListViewModel::class.java]
    }

    private fun initViews(view: View) {
        emailInput = (view.findViewById<TextInputLayout>(R.id.tilLoginEmail).editText as TextInputEditText)
        passwordInput = (view.findViewById<TextInputLayout>(R.id.tilLoginPassword).editText as TextInputEditText)
        loginButton = view.findViewById(R.id.btnLoginEnter)
        signupNavigationButton = view.findViewById(R.id.btnLoginSignupNavigation)
        progressBar = view.findViewById(R.id.pbLoginSpinner)
    }

    private fun setupListeners() {
        loginButton.setOnClickListener { onLoginClicked() }
        signupNavigationButton.setOnClickListener { findNavController().navigate(R.id.action_loginFragment_to_signupFragment) }
    }

    private fun onLoginClicked() {
        val email = emailInput.text?.toString()?.trim()
        val password = passwordInput.text?.toString()?.trim()
        if (!isInputValid(email, password)) return
        startLogin()
    }

    private fun isInputValid(email: String?, password: String?): Boolean {
        if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun startLogin() {
        loginButton.isEnabled = false
        loginButton.text = getString(R.string.logging_in)
        progressBar.visibility = View.VISIBLE
        val email = emailInput.text!!.toString().trim()
        val password = passwordInput.text!!.toString().trim()
        authViewModel.login(email, password)
    }

    private fun observeLoginStatus() {
        authViewModel.loginStatus.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) handleLoginSuccess() else handleLoginFailure()
        }
    }

    private fun handleLoginSuccess() {
        lifecycleScope.launch {
            shoppingListViewModel.syncUserDataFromFirebase()
            sharedCardsViewModel.syncSharedListsFromFirebase()
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build()
            progressBar.visibility = View.GONE
            findNavController().navigate(R.id.myCardsListFragment, null, navOptions)
        }
    }

    private fun handleLoginFailure() {
        progressBar.visibility = View.GONE
        loginButton.isEnabled = true
        loginButton.text = getString(R.string.login)
        Toast.makeText(requireContext(), "Login failed. Please check your details.", Toast.LENGTH_SHORT).show()
    }
}





