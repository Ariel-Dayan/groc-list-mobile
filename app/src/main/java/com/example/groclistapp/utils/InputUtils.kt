package com.example.groclistapp.utils

import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout

class InputUtils private constructor() {
    fun addCleanErrorMessageOnInputListener(textInputLayout: TextInputLayout) {
        textInputLayout.editText?.addTextChangedListener {
            if (textInputLayout.error != null) {
                textInputLayout.error = null
            }
        }
    }

    companion object {
        val instance = InputUtils()
    }
}