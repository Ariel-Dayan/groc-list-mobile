package com.example.groclistapp.ui.list

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.adapter.card.CardsRecyclerAdapter
import com.example.groclistapp.viewmodel.ShoppingListViewModel
import androidx.fragment.app.setFragmentResultListener
import com.example.groclistapp.data.adapter.card.OnItemClickListener
import com.example.groclistapp.data.network.jokes.JokesClient.setJoke
import com.example.groclistapp.utils.ListUtils
import com.example.groclistapp.utils.MessageUtils
import android.widget.ProgressBar
import androidx.lifecycle.viewModelScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch

class MyCardsListFragment : Fragment() {
    private lateinit var cardsRecyclerView: RecyclerView
    private lateinit var adapter: CardsRecyclerAdapter
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var jokeTextView: TextView
    private lateinit var noCardsTextView: TextView
    private lateinit var cardsProgressBar: ProgressBar
    private lateinit var jokeProgressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val listUtils = ListUtils.instance
    private val messageUtils = MessageUtils.instance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_my_cards_list, container, false)

        jokeTextView = view.findViewById(R.id.tvMyCardsListJoke)
        noCardsTextView = view.findViewById(R.id.tvMyCardsListNoCardsMessage)
        cardsProgressBar = view.findViewById(R.id.pbMyCardsListCardsSpinner)
        jokeProgressBar = view.findViewById(R.id.pbMyCardsListJokeSpinner)
        swipeRefreshLayout = view.findViewById(R.id.srlMyCardsListSwipeRefresh)

        cardsProgressBar.visibility = View.VISIBLE

        viewModel = ViewModelProvider(this)[ShoppingListViewModel::class.java]

        setupRecyclerView(view)
        observeShoppingLists()
        setupAddButton(view)
        setJoke(jokeTextView, jokeProgressBar)

        setFragmentResultListener("shoppingListUpdated") { _, bundle ->
            if (bundle.getBoolean("updated", false)) {
                viewModel.loadShoppingLists()
            }
        }

        swipeRefreshLayout.setOnRefreshListener {
            listUtils.refreshData(
                cardsRecyclerView,
                { fetchUserListsFromFirebase() },
                swipeRefreshLayout
            )
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchUserListsFromFirebase()
    }

    private fun setupRecyclerView(view: View) {
        cardsRecyclerView = view.findViewById(R.id.rvMyCardsList)
        adapter = CardsRecyclerAdapter(
            mutableListOf(),
            object : OnItemClickListener {
                override fun onItemClick(listId: String) {
                    val bundle = Bundle().apply { putString("listId", listId) }
                    findNavController().navigate(R.id.action_myCardsListFragment_to_updateCardFragment, bundle)
                }

                override fun onShareCodeClick(code: String, itemView: View) {
                    messageUtils.shareShoppingListCode(code, itemView)
                }
            }
        )

        cardsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        cardsRecyclerView.adapter = adapter
    }

    private fun observeShoppingLists() {
        viewModel.localShoppingLists.observe(viewLifecycleOwner) { shoppingLists ->
            listUtils.toggleNoCardListsMessage(noCardsTextView, shoppingLists)
            shoppingLists?.let {
                adapter.updateData(it)
                cardsRecyclerView.post {
                    cardsProgressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setupAddButton(view: View) {
        view.findViewById<View>(R.id.btnMyCardsListAddCard).setOnClickListener {
            findNavController().navigate(R.id.action_myCardsListFragment_to_addCardFragment)
        }
    }

    private fun fetchUserListsFromFirebase() {
        viewModel.viewModelScope.launch {
            try {
                viewModel.syncUserDataFromFirebase()
            } catch (e: Exception) {
                Log.e("MyCardsListFragment", "Error syncing user lists: ${e.message}")
            }
        }
    }
}
