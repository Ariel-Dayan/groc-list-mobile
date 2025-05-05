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
import androidx.navigation.fragment.findNavController
import com.example.groclistapp.R
import com.example.groclistapp.data.image.ImageHandler
import com.example.groclistapp.utils.ItemUtils
import com.example.groclistapp.viewmodel.ShoppingListViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

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
    private lateinit var removeButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ShoppingListViewModel::class.java)
        listId = DisplayCardFragmentArgs.fromBundle(requireArguments()).listId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_display_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)

        if (!validateListId()) return

        loadShoppingList()
        setupImageHandler()
        observeCurrentList()
        setupRemoveButton()
        observeDeleteStatus()
    }

    private fun initViews(view: View) {
        chipGroup = view.findViewById(R.id.cgDisplayCardItemsContainer)
        cardTitle = view.findViewById(R.id.tvDisplayCardTitle)
        cardDescription = view.findViewById(R.id.tvDisplayCardDescription)
        imageView = view.findViewById(R.id.ivDisplayCardTop)
        progressBar = view.findViewById(R.id.pbDisplayCardSpinner)
        removeButton = view.findViewById(R.id.btnDisplayCardRemove)
    }

    private fun validateListId(): Boolean {
        if (listId == "-1") {
            Toast.makeText(requireContext(), "Invalid list ID", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun loadShoppingList() {
        viewModel.loadShoppingListById(listId)
    }

    private fun setupImageHandler() {
        chipGroup.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        imageHandler = ImageHandler(
            imageView,
            this,
            null,
            null
        )
        progressBar.visibility = View.VISIBLE
    }

    private fun observeCurrentList() {
        viewModel.currentList.observe(viewLifecycleOwner) { list ->
            list?.let {
                cardTitle.text = it.shoppingList.name
                cardDescription.text = it.shoppingList.description
                it.shoppingList.imageUrl?.let { imageUrl ->
                    imageHandler.loadImage(imageUrl, R.drawable.shopping_card_placeholder)
                }
                chipGroup.removeAllViews()
                for (item in it.items) {
                    chipGroup.addView(createChip(item.name, item.amount.toString()))
                }
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupRemoveButton() {
        removeButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            viewModel.deleteSharedListById(listId)
        }
    }

    private fun observeDeleteStatus() {
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
