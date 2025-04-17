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

class UpdateProfileFragment : Fragment() {

    private var _binding: FragmentUpdateProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateProfileBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        setupListeners()
        return binding.root
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
        val currentUser = auth.currentUser

        if (fullName.isEmpty() || oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnUpdateProfileUpdate.isEnabled = false
        binding.btnUpdateProfileUpdate.text = "Updating..."

        if (currentUser != null) {
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, oldPassword)
            currentUser.reauthenticate(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    viewModel.updateProfile(fullName, oldPassword, newPassword, selectedImageUri) { success, message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        binding.btnUpdateProfileUpdate.isEnabled = true
                        binding.btnUpdateProfileUpdate.text = "Update"
                    }
                } else {
                    Toast.makeText(requireContext(), "Re-authentication failed. Please check your old password.", Toast.LENGTH_SHORT).show()
                    binding.btnUpdateProfileUpdate.isEnabled = true
                    binding.btnUpdateProfileUpdate.text = "Update"
                }
            }
        }
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(requireContext(), "Successfully logged out!", Toast.LENGTH_SHORT).show()
        findNavController().navigate(
            R.id.loginFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.updateProfileFragment, true)
                .build()
        )
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