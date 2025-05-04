package com.example.groclistapp.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.viewmodel.AuthViewModel
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
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

        val emailLayout = view.findViewById<TextInputLayout>(R.id.tilSignupEmail)
        val fullNameLayout = view.findViewById<TextInputLayout>(R.id.tilSignupFullName)
        val passwordLayout = view.findViewById<TextInputLayout>(R.id.tilSignupPassword)
        val confirmPasswordLayout = view.findViewById<TextInputLayout>(R.id.tilSignupConfirmPassword)

        emailInput = emailLayout.editText as TextInputEditText
        fullNameInput = fullNameLayout.editText as TextInputEditText
        passwordInput = passwordLayout.editText as TextInputEditText
        confirmPasswordInput = confirmPasswordLayout.editText as TextInputEditText
        userImageView = view.findViewById(R.id.civSignupUserImage)
        takePhotoButton = view.findViewById(R.id.ibSignupTakePhoto)
        uploadGalleryButton = view.findViewById(R.id.ibSignupUploadImageFromGallery)

        imageHandler = ImageHandler(userImageView, this, uploadGalleryButton, takePhotoButton)

        registerButton = view.findViewById(R.id.btnSignupRegister)
        progressBar = view.findViewById(R.id.pbSignupSpinner)

        registerButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim()
            val fullName = fullNameInput.text?.toString()?.trim()
            val password = passwordInput.text?.toString()?.trim()
            val confirmPassword = confirmPasswordInput.text?.toString()?.trim()

            if (email.isNullOrEmpty() || fullName.isNullOrEmpty() ||
                password.isNullOrEmpty() || confirmPassword.isNullOrEmpty()
            ) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
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

            progressBar.visibility = View.VISIBLE
            authViewModel.signup(
                email = email,
                password = password,
                fullName = fullName,
                imageUri = imageHandler.selectedImageUri
            )
        }

        authViewModel.signupStatus.observe(viewLifecycleOwner) { success ->
            progressBar.visibility = View.GONE
            if (success) {
                Toast.makeText(requireContext(), "User registered successfully!", Toast.LENGTH_SHORT).show()
                authViewModel.logout()
            } else {
                registerButton.isEnabled = true
                registerButton.text = getString(R.string.sign_up)
            }
        }

        authViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), "Signup failed: $it", Toast.LENGTH_SHORT).show()
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




