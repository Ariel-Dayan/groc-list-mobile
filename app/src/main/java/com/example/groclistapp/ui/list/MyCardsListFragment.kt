package com.example.groclistapp.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.adapter.card.CardsRecyclerAdapter
import com.example.groclistapp.model.ShoppingList
import com.example.groclistapp.repository.AppDatabase
import com.example.groclistapp.repository.ShoppingListRepository
import com.example.groclistapp.viewmodel.ShoppingListViewModel

class MyCardsListFragment : Fragment() {

    private lateinit var cardsRecyclerView: RecyclerView
    private lateinit var adapter: CardsRecyclerAdapter
    private lateinit var viewModel: ShoppingListViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_my_cards_list, container, false)


        val shoppingListDao = AppDatabase.getDatabase(requireContext()).shoppingListDao()
        val repository = ShoppingListRepository(shoppingListDao)

        viewModel = ViewModelProvider(
            this,
            ShoppingListViewModel.Factory(requireActivity().application, repository)
        ).get(ShoppingListViewModel::class.java)

        setupRecyclerView(view)
        observeShoppingLists()

        return view
    }

    private fun setupRecyclerView(view: View) {
        cardsRecyclerView = view.findViewById(R.id.rvMyCardsList)
        adapter = CardsRecyclerAdapter(mutableListOf())
        cardsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        cardsRecyclerView.adapter = adapter
    }

    private fun observeShoppingLists() {
        viewModel.remoteShoppingLists.observe(viewLifecycleOwner) { shoppingLists: List<ShoppingList>? ->
            shoppingLists?.let { list ->
                adapter.updateData(list)
            }
        }
    }
}

