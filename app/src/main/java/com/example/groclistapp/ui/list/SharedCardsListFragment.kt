package com.example.groclistapp.ui.list

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.adapter.card.CardsRecyclerAdapter
import com.example.groclistapp.data.adapter.card.OnItemClickListener
import com.example.groclistapp.data.network.jokes.JokesClient.setJoke
import com.example.groclistapp.viewmodel.SharedCardsViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.groclistapp.utils.ListUtils
import com.example.groclistapp.utils.MessageUtils
import kotlinx.coroutines.launch

class SharedCardsListFragment : Fragment() {
    private var adapter: CardsRecyclerAdapter? = null
    private var cardsRecyclerView: RecyclerView? = null
    private lateinit var viewModel: SharedCardsViewModel
    private lateinit var jokeTextView: TextView
    private lateinit var noCardsMessageTextView: TextView
    private val listUtils = ListUtils.instance
    private val messageUtils = MessageUtils.instance
    private lateinit var cardsProgressBar: ProgressBar
    private lateinit var jokeProgressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shared_cards_list, container, false)
        viewModel = ViewModelProvider(requireActivity())[SharedCardsViewModel::class.java]

        setupView(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        fetchSharedListsFromFirebase()
    }

    private fun setupView(view: View) {
        cardsRecyclerView = view.findViewById(R.id.rvSharedCardsList)
        jokeTextView = view.findViewById(R.id.tvSharedCardsListJoke)
        cardsProgressBar = view.findViewById(R.id.pbSharedCardsListCardsSpinner)
        jokeProgressBar = view.findViewById(R.id.pbSharedCardsListJokeSpinner)
        swipeRefreshLayout = view.findViewById(R.id.srlSharedCardsListSwipeRefresh)

        cardsProgressBar.visibility = View.VISIBLE

        val inputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSharedCardsListSharedCode)
        val shareCodeInput = inputLayout.editText

        val addButton = view.findViewById<View>(R.id.btnSharedCardsListAdd)

        noCardsMessageTextView = view.findViewById(R.id.tvSharedCardsListNoCardsMessage)

        adapter = CardsRecyclerAdapter(
            mutableListOf(),
            object : OnItemClickListener {
                override fun onItemClick(listId: String) {
                    val bundle = Bundle().apply {
                        putString("listId", listId)
                    }
                    val navController = findNavController()
                    navController.navigate(R.id.action_sharedCardsListFragment_to_displayCardFragment, bundle)
                }

                override fun onShareCodeClick(code: String, itemView: View) {
                    messageUtils.shareShoppingListCode(code, itemView)
                }
            }

        )

        setJoke(jokeTextView, jokeProgressBar)

        cardsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = this@SharedCardsListFragment.adapter
        }

        swipeRefreshLayout.setOnRefreshListener {
            listUtils.refreshData(
                cardsRecyclerView,
                { fetchSharedListsFromFirebase() },
                swipeRefreshLayout
            )
        }

        viewModel.sharedLists.observe(viewLifecycleOwner) { list ->
            listUtils.toggleNoCardListsMessage(noCardsMessageTextView, list)
            adapter?.updateData(list)
            cardsRecyclerView?.post {
                cardsProgressBar.visibility = View.GONE
            }
        }

        viewModel.addSharedListStatus.observe(viewLifecycleOwner) { result ->
            result
                .onSuccess { list ->
                    cardsProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Shared list loaded: ${list.name}", Toast.LENGTH_SHORT).show()
                }
                .onFailure { e ->
                    cardsProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("SharedCardsListFragment", "Error adding shared list", e)
                }
        }

        addButton.setOnClickListener {
            val shareCode = shareCodeInput?.text?.toString()?.trim()

            if (!shareCode.isNullOrEmpty()) {
                cardsProgressBar.visibility = View.VISIBLE
                viewModel.addSharedListByCode(shareCode)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid share code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchSharedListsFromFirebase() {
        viewModel.viewModelScope.launch {
            try {
                viewModel.syncSharedListsFromFirebase()
            } catch (e: Exception) {
                Log.e("SharedCardsListFragment", "Error syncing shared lists: ${e.message}")
            }
        }
    }
}
