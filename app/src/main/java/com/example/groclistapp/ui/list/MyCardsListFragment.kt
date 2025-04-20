package com.example.groclistapp.ui.list

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.adapter.card.CardsRecyclerAdapter
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.data.repository.ShoppingListRepository
import com.example.groclistapp.viewmodel.ShoppingListViewModel
import kotlinx.coroutines.launch
import androidx.fragment.app.setFragmentResultListener
import com.example.groclistapp.data.repository.ShoppingListDao
import com.example.groclistapp.data.repository.ShoppingItemDao
import com.example.groclistapp.data.adapter.card.OnItemClickListener
import com.example.groclistapp.data.network.jokes.JokesClient.setJoke
import com.example.groclistapp.utils.ListUtils


class MyCardsListFragment : Fragment() {
    private lateinit var cardsRecyclerView: RecyclerView
    private lateinit var adapter: CardsRecyclerAdapter
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var jokeTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_my_cards_list, container, false)

        val shoppingListDao = AppDatabase.getDatabase(requireContext()).shoppingListDao()
        val shoppingItemDao = AppDatabase.getDatabase(requireContext()).shoppingItemDao()
        val repository = ShoppingListRepository(shoppingListDao, shoppingItemDao)

        jokeTextView = view.findViewById(R.id.tvMyCardsListJoke)
        viewModel = ViewModelProvider(
            this,
            ShoppingListViewModel.Factory(requireActivity().application, repository)
        )[ShoppingListViewModel::class.java]

        setupRecyclerView(view, shoppingListDao, shoppingItemDao)
        observeShoppingLists()
        setupAddButton(view)
        setJoke(jokeTextView)

        setFragmentResultListener("shoppingListUpdated") { _, bundle ->
            if (bundle.getBoolean("updated", false)) {
                Log.d("MyCardsListFragment", "📌 רשימה חדשה נוספה, טוען מחדש נתונים...")
                viewModel.loadShoppingLists() // טוען מחדש
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadShoppingLists()
    }

    private fun setupRecyclerView(view: View, shoppingListDao: ShoppingListDao, shoppingItemDao: ShoppingItemDao) {
        cardsRecyclerView = view.findViewById(R.id.rvMyCardsList)
        adapter = CardsRecyclerAdapter(
            mutableListOf(),
            shoppingListDao,
            shoppingItemDao,
            object : OnItemClickListener {
                override fun onItemClick(listId: String) {
                    val bundle = Bundle().apply { putString("listId", listId) }
                    findNavController().navigate(R.id.action_myCardsListFragment_to_updateCardFragment, bundle)
                }
            }
        )

        cardsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        cardsRecyclerView.adapter = adapter

    }

    private fun observeShoppingLists() {
        viewModel.localShoppingLists.observe(viewLifecycleOwner) { shoppingLists ->
            shoppingLists?.let { adapter.updateData(it) }
        }
    }

    private fun setupAddButton(view: View) {
        view.findViewById<View>(R.id.btnMyCardsListAddCard).setOnClickListener {
            findNavController().navigate(R.id.action_myCardsListFragment_to_addCardFragment)
        }
    }
}



