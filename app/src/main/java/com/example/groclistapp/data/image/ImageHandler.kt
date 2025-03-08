package com.example.groclistapp.data.image

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso

class ImageHandler(
    activity: Fragment,
    private val imageView: ImageView,
    uploadPhotoFromGalleryButton: ImageButton,
    takePhotoButton: ImageButton
) {
    private val cameraLauncher: ActivityResultLauncher<Void?> =
        activity.registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap == null) {
                Toast.makeText(activity.context, "Failed to take photo", Toast.LENGTH_SHORT).show()
            } else {
                imageView.setImageBitmap(bitmap)
            }
        }

    private val galleryLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) {
                Toast.makeText(activity.context, "Failed to upload photo from gallery", Toast.LENGTH_SHORT).show()
            } else {
                imageView.setImageURI(uri)
            }
        }

    init {
        takePhotoButton.setOnClickListener { cameraLauncher.launch(null) }
        uploadPhotoFromGalleryButton.setOnClickListener { galleryLauncher.launch("image/*") }
    }

    fun loadImage(url: String) {
        Picasso.get().load(url).into(imageView)
    }

    fun getBitmapPhoto(): Bitmap? {
        return (imageView.drawable as? BitmapDrawable)?.bitmap
    }
}
