package com.example.groclistapp.ui.card

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.groclistapp.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class AddCardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chipGroup = view.findViewById<ChipGroup>(R.id.cgAddCardContainer)
        chipGroup.layoutDirection = View.LAYOUT_DIRECTION_LOCALE

        val btnAddItem = view.findViewById<Button>(R.id.btnAddCardAddItem)

        // Add chips to the chip group
        chipGroup.addView(createChip("Apples", "5", chipGroup))
        chipGroup.addView(createChip("Bananas", "10", chipGroup))
        chipGroup.addView(createChip("Oranges", "3", chipGroup))
        chipGroup.addView(createChip("Apples1", "5", chipGroup))
        chipGroup.addView(createChip("Bananas1", "10", chipGroup))
        chipGroup.addView(createChip("Oranges1", "3", chipGroup))
        chipGroup.addView(createChip("Bananas2", "10", chipGroup))
        chipGroup.addView(createChip("Oranges2", "3", chipGroup))

        btnAddItem.setOnClickListener {
            val name = "aaaa"
            val amount = "3"

            if (name.isNotEmpty() && amount.isNotEmpty()) {
                val chip = createChip(name, amount, chipGroup)
                chipGroup.addView(chip)

                // Clear the text fields after adding
//                nameField.text.clear()
//                amountField.text.clear()
            }
        }
    }

    private fun createChip(name: String, amount: String, chipGroup: ChipGroup): Chip {
        val chip = Chip(requireContext())
        chip.text = "$name: $amount"
        chip.isCloseIconVisible = true

        chip.setOnCloseIconClickListener {
            chipGroup.removeView(chip)
        }

        chip.setOnClickListener {
            showEditItemDialog(
                context = requireContext(),
                currentName = name,
                currentAmount = amount
            ) { newName, newAmount ->
                chip.text = "$newName: $newAmount"
            }
        }

        return chip
    }

    fun showEditItemDialog(
        context: Context,
        currentName: String,
        currentAmount: String,
        onUpdate: (String, String) -> Unit
    ) {
        val inputLayout = LayoutInflater.from(context).inflate(R.layout.dialog_item, null)
        val nameInput = inputLayout.findViewById<EditText>(R.id.tilDialogItemName)
        val amountInput = inputLayout.findViewById<EditText>(R.id.tilDialogItemAmount)

        nameInput.setText(currentName)
        amountInput.setText(currentAmount)

        AlertDialog.Builder(context)
            .setTitle("Edit Item")
            .setView(inputLayout)
            .setPositiveButton("Update") { _, _ ->
                val newName = nameInput.text.toString()
                val newAmount = amountInput.text.toString()

                if (newName.isNotEmpty() && newAmount.isNotEmpty()) {
                    onUpdate(newName, newAmount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}