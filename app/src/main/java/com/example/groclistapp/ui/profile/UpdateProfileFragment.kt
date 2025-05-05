package com.example.groclistapp.ui.profile


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.groclistapp.databinding.FragmentUpdateProfileBinding
import com.example.groclistapp.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.viewmodel.AuthViewModel
import com.example.groclistapp.viewmodel.ShoppingListViewModel
import com.google.android.material.textfield.TextInputLayout

class UpdateProfileFragment : Fragment() {

    private var _binding: FragmentUpdateProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var imageHandler: ImageHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?

    ): View {
        _binding = FragmentUpdateProfileBinding.inflate(inflater, container, false)
        binding.pbUpdateProfileSpinner.visibility = View.VISIBLE
        shoppingListViewModel = ViewModelProvider(this)[ShoppingListViewModel::class.java]

        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        imageHandler = ImageHandler(binding.civUpdateProfileUserImage, this, binding.ibUpdateProfileUploadImageFromGallery, binding.ibUpdateProfileTakePhoto)

        setupListeners()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            profileViewModel.loadProfileImage(userId) { imageUrl ->
                if (!imageUrl.isNullOrEmpty()) {
                    imageHandler.loadImage(imageUrl, R.drawable.user_placeholder)
                }

                binding.pbUpdateProfileSpinner.visibility = View.GONE
            }
        } else {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            binding.pbUpdateProfileSpinner.visibility = View.GONE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileViewModel.loadDisplayName()

        profileViewModel.displayName.observe(viewLifecycleOwner) { name ->
            binding.tilUpdateProfileFullName.editText?.setText(name)
        }
    }

    private fun setupListeners() {
        binding.btnUpdateProfileUpdate.setOnClickListener {
            updateProfile()
        }

        binding.btnUpdateProfileLogout.setOnClickListener {
            logout()
        }
    }

    private fun updateProfile() {
        val fullName = getTrimmedText(binding.tilUpdateProfileFullName)
        val oldPassword = getTrimmedText(binding.tilUpdateProfileOldPassword)
        val newPassword = getTrimmedText(binding.tilUpdateProfilePassword)
        val confirmPassword = getTrimmedText(binding.tilUpdateProfileConfirmPassword)

        if (!validateFields(fullName, oldPassword, newPassword, confirmPassword)) return

        showUpdatingUI()

        profileViewModel.updateProfile(
            fullName = fullName.ifEmpty { null },
            oldPassword = oldPassword.ifEmpty { null },
            newPassword = newPassword.ifEmpty { null },
            imageUri = imageHandler.selectedImageUri
        ) { _, message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            hideUpdatingUI()
        }
    }


    private fun getTrimmedText(field: TextInputLayout): String {
        return field.editText?.text?.toString()?.trim().orEmpty()
    }

    private fun validateFields(
        fullName: String,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        if (fullName.isEmpty() && newPassword.isEmpty() && imageHandler.selectedImageUri == null) {
            showToast("Please enter at least one field to update")
            return false
        }

        if (newPassword.isNotEmpty()) {
            if (oldPassword.isEmpty()) {
                showToast("Enter your current password to change password")
                return false
            }
            if (newPassword != confirmPassword) {
                showToast("Passwords do not match")
                return false
            }
            if (newPassword.length < 6) {
                showToast("Password must be at least 6 characters long")
                return false
            }
            if (oldPassword == newPassword) {
                showToast("New password must be different from the old password")
                return false
            }
        }

        return true
    }

    private fun showUpdatingUI() {
        binding.pbUpdateProfileSpinner.visibility = View.VISIBLE
        binding.btnUpdateProfileUpdate.isEnabled = false
        binding.btnUpdateProfileUpdate.text = getString(R.string.updating_action)
    }

    private fun hideUpdatingUI() {
        binding.pbUpdateProfileSpinner.visibility = View.GONE
        binding.btnUpdateProfileUpdate.isEnabled = true
        binding.btnUpdateProfileUpdate.text =  getString(R.string.update)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun logout() {
        binding.pbUpdateProfileSpinner.visibility = View.VISIBLE
        authViewModel.logout()

        authViewModel.logoutStatus.observe(viewLifecycleOwner) { success ->
            binding.pbUpdateProfileSpinner.visibility = View.GONE
            if (success) {
                Toast.makeText(requireContext(), "Successfully logged out!", Toast.LENGTH_SHORT).show()

                val navController = findNavController()
                navController.navigate(
                    R.id.loginFragment,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(navController.graph.id, true)
                        .build()
                )
            } else {
                Toast.makeText(requireContext(), "Logout failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}