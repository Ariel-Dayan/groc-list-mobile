package com.example.groclistapp.ui.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.groclistapp.R
import com.example.groclistapp.utils.InputUtils
import com.example.groclistapp.utils.ItemUtils
import com.google.android.material.textfield.TextInputLayout

class DialogUtils private constructor() {
    private val inputUtils = InputUtils.instance
    private val itemUtils = ItemUtils.instance

    private fun inflateDialogItemLayout(context: Context): Triple<View, TextInputLayout, TextInputLayout> {
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_item, null)
        val nameLayout = layout.findViewById<TextInputLayout>(R.id.tilDialogItemName)
        val amountLayout = layout.findViewById<TextInputLayout>(R.id.tilDialogItemAmount)
        return Triple(layout, nameLayout, amountLayout)
    }

    fun showEditItemDialog(
        context: Context,
        currentName: String,
        currentAmount: String,
        existingNames: List<String>,
        onUpdate: (String, Int) -> Unit
    ) {
        val (layout, nameLayout, amountLayout) = inflateDialogItemLayout(context)

        inputUtils.addCleanErrorMessageOnInputListener(nameLayout)
        inputUtils.addCleanErrorMessageOnInputListener(amountLayout)

        val nameInput = nameLayout.editText
        val amountInput = amountLayout.editText

        nameInput?.setText(currentName)
        amountInput?.setText(currentAmount)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Edit Item")
            .setView(layout)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val name = nameInput?.text.toString().trim()
                val amountText = amountInput?.text.toString().trim()

                val nameError = itemUtils.validateName(name, existingNames, currentName)
                val (amountValue, amountError) = itemUtils.validateAmount(amountText)

                nameLayout.error = nameError
                amountLayout.error = amountError

                if (nameError == null && amountError == null && amountValue != null) {
                    dialog.dismiss()
                    onUpdate(name, amountValue)
                }
            }
        }

        dialog.show()
    }

    companion object {
        val instance = DialogUtils()
    }
}
