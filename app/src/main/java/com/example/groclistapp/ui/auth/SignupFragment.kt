package com.example.groclistapp.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.viewmodel.AuthViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
import android.widget.ImageButton
import android.widget.ProgressBar

class SignupFragment : Fragment(R.layout.fragment_signup) {
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var emailInput: TextInputEditText
    private lateinit var fullNameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText
    private lateinit var registerButton: MaterialButton
    private lateinit var userImageView: CircleImageView
    private lateinit var takePhotoButton: ImageButton
    private lateinit var uploadGalleryButton: ImageButton
    private lateinit var imageHandler: ImageHandler
    private lateinit var progressBar: ProgressBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        observeSignup()
    }

    private fun initViews(view: View) {
        emailInput = (view.findViewById<TextInputLayout>(R.id.tilSignupEmail).editText as TextInputEditText)
        fullNameInput = (view.findViewById<TextInputLayout>(R.id.tilSignupFullName).editText as TextInputEditText)
        passwordInput = (view.findViewById<TextInputLayout>(R.id.tilSignupPassword).editText as TextInputEditText)
        confirmPasswordInput = (view.findViewById<TextInputLayout>(R.id.tilSignupConfirmPassword).editText as TextInputEditText)
        userImageView = view.findViewById(R.id.civSignupUserImage)
        takePhotoButton = view.findViewById(R.id.ibSignupTakePhoto)
        uploadGalleryButton = view.findViewById(R.id.ibSignupUploadImageFromGallery)
        imageHandler = ImageHandler(userImageView, this, uploadGalleryButton, takePhotoButton)
        registerButton = view.findViewById(R.id.btnSignupRegister)
        progressBar = view.findViewById(R.id.pbSignupSpinner)
    }

    private fun setupListeners() {
        registerButton.setOnClickListener { handleRegisterClick() }
    }

    private fun handleRegisterClick() {
        val email = emailInput.text?.toString()?.trim()
        val fullName = fullNameInput.text?.toString()?.trim()
        val password = passwordInput.text?.toString()?.trim()
        val confirmPassword = confirmPasswordInput.text?.toString()?.trim()
        if (!isInputValid(email, fullName, password, confirmPassword)) return
        registerButton.isEnabled = false
        registerButton.text = getString(R.string.signing_up)
        progressBar.visibility = View.VISIBLE
        authViewModel.signup(email!!, password!!, fullName!!, imageHandler.selectedImageUri)
    }

    private fun isInputValid(
        email: String?,
        fullName: String?,
        password: String?,
        confirmPassword: String?
    ): Boolean {
        if (email.isNullOrEmpty() || fullName.isNullOrEmpty() || password.isNullOrEmpty() || confirmPassword.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(requireContext(), getString(R.string.error_password_length), Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(requireContext(), getString(R.string.error_passwords_mismatch), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun observeSignup() {
        authViewModel.signupStatus.observe(viewLifecycleOwner) { success ->
            progressBar.visibility = View.GONE
            if (success) {
                Toast.makeText(requireContext(), getString(R.string.success_user_registered), Toast.LENGTH_SHORT).show()
                authViewModel.logout()
            } else {
                registerButton.isEnabled = true
                registerButton.text = getString(R.string.sign_up)
            }
        }
        authViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), getString(R.string.error_signup_failed, it), Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.logoutStatus.observe(viewLifecycleOwner) { success ->
            if (!success) {
                Log.w("SignupFragment", "Logout failed after signup")
            }
            findNavController().navigateUp()
        }
    }
}




