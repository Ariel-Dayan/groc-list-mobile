package com.example.groclistapp.ui.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.groclistapp.R
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.data.repository.ShoppingItemDao
import com.example.groclistapp.data.repository.ShoppingListDao
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.data.repository.ShoppingListRepository
import com.example.groclistapp.utils.ItemUtils
import com.example.groclistapp.viewmodel.ShoppingListViewModel


class DisplayCardFragment : Fragment() {
    private var listId: String = "-1"
    private val itemUtils = ItemUtils.instance
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var progressBar: ProgressBar
    private lateinit var imageHandler: ImageHandler
    private lateinit var chipGroup: ChipGroup
    private lateinit var cardTitle: TextView
    private lateinit var cardDescription: TextView
    private lateinit var imageView: ImageView
    private lateinit var deleteButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            ShoppingListViewModel.Factory(
                requireActivity().application,
            )
        )[ShoppingListViewModel::class.java]

        listId = arguments?.getString("listId") ?: "-1"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_display_card, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipGroup = view.findViewById(R.id.cgDisplayCardItemsContainer)
        cardTitle = view.findViewById(R.id.tvDisplayCardTitle)
        cardDescription = view.findViewById(R.id.tvDisplayCardDescription)
        imageView = view.findViewById(R.id.ivDisplayCardTop)
        progressBar = view.findViewById(R.id.pbDisplayCardSpinner)
        deleteButton = view.findViewById(R.id.btnDisplayCardRemove)

        if (listId == "-1") {
            Toast.makeText(requireContext(), "Invalid list ID", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.loadShoppingListById(listId)

        chipGroup.layoutDirection = View.LAYOUT_DIRECTION_LOCALE

        imageHandler = ImageHandler(
            imageView,
            this,
            null,
            null
        )

        progressBar.visibility = View.VISIBLE

        viewModel.currentListSummary.observe(viewLifecycleOwner) { list ->
            list?.let {
                cardTitle.text = it.name
                cardDescription.text = it.description
                it.imageUrl?.let { imageUrl ->
                    imageHandler.loadImage(imageUrl, R.drawable.shopping_card_placeholder)
                }

                viewModel.getItemsForList(listId)
            }
        }

        viewModel.getItemsForList(listId).observe(viewLifecycleOwner) { items ->
            chipGroup.removeAllViews()
            for (item in items) {
                chipGroup.addView(createChip(item.name, item.amount.toString()))
            }
            progressBar.visibility = View.GONE
        }

        deleteButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            viewModel.deleteSharedListById(listId)
        }

        viewModel.deleteStatus.observe(viewLifecycleOwner) { isDeleted ->
            if (isDeleted == true) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "List deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

    }

    private fun createChip(name: String, amount: String): Chip {
        val chip = Chip(requireContext())
        chip.text = itemUtils.createItemChipText(name, amount)
        chip.isCloseIconVisible = false
        return chip
    }
}