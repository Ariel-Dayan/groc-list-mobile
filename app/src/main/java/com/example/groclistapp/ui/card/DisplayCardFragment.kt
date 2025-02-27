package com.example.groclistapp.ui.card

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.groclistapp.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class DisplayCardFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_display_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chipGroup = view.findViewById<ChipGroup>(R.id.cgDisplayCardItemsContainer)
        chipGroup.layoutDirection = View.LAYOUT_DIRECTION_LOCALE

        chipGroup.addView(createChip("Apples", "5", chipGroup))
        chipGroup.addView(createChip("Bananas", "10", chipGroup))
        chipGroup.addView(createChip("Oranges", "3", chipGroup))
        chipGroup.addView(createChip("Apples", "5", chipGroup))
        chipGroup.addView(createChip("Bananas", "10", chipGroup))
        chipGroup.addView(createChip("Oranges", "3", chipGroup))

        chipGroup.addView(createChip("Apples", "5", chipGroup))
        chipGroup.addView(createChip("Bananas", "10", chipGroup))
        chipGroup.addView(createChip("Oranges", "3", chipGroup))

        chipGroup.addView(createChip("Apples", "5", chipGroup))
        chipGroup.addView(createChip("Bananas", "10", chipGroup))
        chipGroup.addView(createChip("Oranges", "3", chipGroup))
        chipGroup.addView(createChip("Apples", "5", chipGroup))
        chipGroup.addView(createChip("Bananas", "10", chipGroup))
        chipGroup.addView(createChip("Oranges", "3", chipGroup))
        chipGroup.addView(createChip("Apples", "5", chipGroup))
        chipGroup.addView(createChip("Bananas", "10", chipGroup))
        chipGroup.addView(createChip("Oranges", "3", chipGroup))
        chipGroup.addView(createChip("Apples", "5", chipGroup))
        chipGroup.addView(createChip("Bananas", "10", chipGroup))
        chipGroup.addView(createChip("Oranges", "3", chipGroup))
        chipGroup.addView(createChip("Apples", "5", chipGroup))
        chipGroup.addView(createChip("Bananas", "10", chipGroup))
        chipGroup.addView(createChip("Oranges", "3", chipGroup))


    }
    private fun createChip(name: String, amount: String, chipGroup: ChipGroup): Chip {
        val chip = Chip(requireContext())

        chip.text = "$name: $amount"
        chip.isCloseIconVisible = false

        return chip
    }
}