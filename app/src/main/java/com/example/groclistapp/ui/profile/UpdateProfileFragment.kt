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
import com.bumptech.glide.Glide
import com.example.groclistapp.R
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
    private lateinit var progressBar: ProgressBar
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?

    ): View {
        shoppingListViewModel = ViewModelProvider(
            this,
            ShoppingListViewModel.Factory(requireActivity().application, ShoppingListRepository(
                AppDatabase.getDatabase(requireContext()).shoppingListDao(),
                AppDatabase.getDatabase(requireContext()).shoppingItemDao()
            )
            )
        )[ShoppingListViewModel::class.java]

        _binding = FragmentUpdateProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        setupListeners()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.loadProfileImage(userId) { imageUrl ->
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.user_placeholder)
                        .into(binding.civUpdateProfileUserImage)
                }
            }
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

        binding.ibUpdateProfileUploadImageFromGallery.setOnClickListener {
            openGallery()
        }

        binding.ibUpdateProfileTakePhoto.setOnClickListener {
            takePhoto()
        }
    }

    private fun updateProfile() {
        val fullName = binding.tilUpdateProfileFullName.editText?.text.toString().trim()
        val oldPassword = binding.tilUpdateProfileOldPassword.editText?.text.toString().trim()
        val newPassword = binding.tilUpdateProfilePassword.editText?.text.toString().trim()
        val confirmPassword = binding.tilUpdateProfileConfirmPassword.editText?.text.toString().trim()

        if (fullName.isEmpty() && newPassword.isEmpty() && selectedImageUri == null) {
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
        }
        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpdateProfileUpdate.isEnabled = false
        binding.btnUpdateProfileUpdate.text = "Updating..."

        viewModel.updateProfile(
            fullName = if (fullName.isNotEmpty()) fullName else null,
            oldPassword = if (oldPassword.isNotEmpty()) oldPassword else null,
            newPassword = if (newPassword.isNotEmpty()) newPassword else null,
            imageUri = selectedImageUri
        ) { success, message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.btnUpdateProfileUpdate.isEnabled = true
            binding.btnUpdateProfileUpdate.text = "Update"
        }
    }


    private fun logout() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.logout()

        viewModel.logoutStatus.observe(viewLifecycleOwner) { success ->
            binding.progressBar.visibility = View.GONE
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



    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    selectedImageUri = data?.data
                    binding.civUpdateProfileUserImage.setImageURI(selectedImageUri)
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val photo = data?.extras?.get("data") as? Bitmap
                    photo?.let {
                        binding.civUpdateProfileUserImage.setImageBitmap(it)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
        private const val REQUEST_IMAGE_CAPTURE = 1002
    }
}