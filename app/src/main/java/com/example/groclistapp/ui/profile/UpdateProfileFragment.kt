package com.example.groclistapp.ui.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.groclistapp.databinding.FragmentUpdateProfileBinding
import com.example.groclistapp.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.storage.FirebaseStorage
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.data.repository.ShoppingListRepository
import com.example.groclistapp.viewmodel.ShoppingListViewModel

class UpdateProfileFragment : Fragment() {

    private var _binding: FragmentUpdateProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var imageHandler: ImageHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?

    ): View {
        _binding = FragmentUpdateProfileBinding.inflate(inflater, container, false)
        binding.pbUpdateProfileSpinner.visibility = View.VISIBLE
        shoppingListViewModel = ViewModelProvider(
            this,
            ShoppingListViewModel.Factory(requireActivity().application, ShoppingListRepository(
                AppDatabase.getDatabase(requireContext()).shoppingListDao(),
                AppDatabase.getDatabase(requireContext()).shoppingItemDao()
            )
            )
        )[ShoppingListViewModel::class.java]

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        imageHandler = ImageHandler(binding.civUpdateProfileUserImage, this, binding.ibUpdateProfileUploadImageFromGallery, binding.ibUpdateProfileTakePhoto)

        setupListeners()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.loadProfileImage(userId) { imageUrl ->
                if (!imageUrl.isNullOrEmpty()) {
                    imageHandler.loadImage(imageUrl, R.drawable.user_placeholder)
                    binding.pbUpdateProfileSpinner.visibility = View.GONE
                }
            }
        } else {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            binding.pbUpdateProfileSpinner.visibility = View.GONE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadDisplayName()

        viewModel.displayName.observe(viewLifecycleOwner) { name ->
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
        val fullName = binding.tilUpdateProfileFullName.editText?.text.toString().trim()
        val oldPassword = binding.tilUpdateProfileOldPassword.editText?.text.toString().trim()
        val newPassword = binding.tilUpdateProfilePassword.editText?.text.toString().trim()
        val confirmPassword = binding.tilUpdateProfileConfirmPassword.editText?.text.toString().trim()

        if (fullName.isEmpty() && newPassword.isEmpty() && imageHandler.selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please enter at least one field to update", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isNotEmpty()) {
            if (oldPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Enter your current password to change password", Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return
            }

            if (oldPassword == newPassword) {
                Toast.makeText(requireContext(), "New password must be different from the old password", Toast.LENGTH_SHORT).show()
                return
            }
        }


        binding.pbUpdateProfileSpinner.visibility = View.VISIBLE
        binding.btnUpdateProfileUpdate.isEnabled = false
        binding.btnUpdateProfileUpdate.text = "Updating..."

        viewModel.updateProfile(
            fullName = fullName.ifEmpty { null },
            oldPassword = oldPassword.ifEmpty { null },
            newPassword = newPassword.ifEmpty { null },
            imageUri = if (imageHandler.selectedImageUri != null) imageHandler.selectedImageUri else null
        ) { success, message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            binding.pbUpdateProfileSpinner.visibility = View.GONE
            binding.btnUpdateProfileUpdate.isEnabled = true
            binding.btnUpdateProfileUpdate.text = "Update"
        }
    }


    private fun logout() {
        binding.pbUpdateProfileSpinner.visibility = View.VISIBLE
        viewModel.logout()

        viewModel.logoutStatus.observe(viewLifecycleOwner) { success ->
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