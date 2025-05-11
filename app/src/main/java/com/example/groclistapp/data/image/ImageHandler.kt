package com.example.groclistapp.data.image

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.groclistapp.R


class ImageHandler(
    private val imageView: ImageView,
    activity: Fragment?,
    uploadPhotoFromGalleryButton: ImageButton?,
    takePhotoButton: ImageButton?
) {
    var selectedImageUri: Uri? = null

    private val cameraLauncher: ActivityResultLauncher<Void?>? =
        activity?.registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap == null) {
                activity?.let {
                    Toast.makeText(it.requireContext(), it.getString(R.string.error_take_photo), Toast.LENGTH_SHORT).show()
                }
            } else {
                imageView.setImageBitmap(bitmap)
                try {
                    val file = java.io.File(activity.requireContext().cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        activity.requireContext(),
                        "${activity.requireContext().packageName}.provider",
                        file
                    )
                    selectedImageUri = uri
                } catch (e: Exception) {
                    activity?.let {
                        Toast.makeText(it.requireContext(), it.getString(R.string.error_saving_image), Toast.LENGTH_SHORT).show()
                    }
                    e.printStackTrace()
                }

            }
        }

    private val galleryLauncher: ActivityResultLauncher<String>? =
        activity?.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) {
                activity?.let {
                    Toast.makeText(it.requireContext(), it.getString(R.string.error_saving_image), Toast.LENGTH_SHORT).show()
                }
            } else {
                selectedImageUri = uri
                imageView.setImageURI(uri)
            }
        }

    init {
        takePhotoButton?.setOnClickListener { cameraLauncher?.launch(null) }
        uploadPhotoFromGalleryButton?.setOnClickListener { galleryLauncher?.launch("image/*") }
    }

    fun loadImage(url: String, fallback: Int) {
        Glide.with(imageView.context)
            .load(url)
            .placeholder(fallback)
            .error(fallback)
            .into(imageView)
    }

    fun getBitmapPhoto(): Bitmap? {
        return (imageView.drawable as? BitmapDrawable)?.bitmap
    }
}
