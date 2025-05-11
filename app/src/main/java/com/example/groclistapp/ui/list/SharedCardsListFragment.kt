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
        viewModel = androidx.lifecycle.ViewModelProvider(this)[SharedCardsViewModel::class.java]

        setupView(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        fetchSharedListsFromFirebase()
    }

    private fun setupView(view: View) {
        initViews(view)
        setupRecyclerView()
        setupJoke()
        setupSwipeToRefresh()
        observeSharedLists()
        observeAddSharedListStatus()
        setupAddButton(view)
    }

    private fun initViews(view: View) {
        cardsRecyclerView = view.findViewById(R.id.rvSharedCardsList)
        jokeTextView = view.findViewById(R.id.tvSharedCardsListJoke)
        cardsProgressBar = view.findViewById(R.id.pbSharedCardsListCardsSpinner)
        jokeProgressBar = view.findViewById(R.id.pbSharedCardsListJokeSpinner)
        swipeRefreshLayout = view.findViewById(R.id.srlSharedCardsListSwipeRefresh)
        noCardsMessageTextView = view.findViewById(R.id.tvSharedCardsListNoCardsMessage)

        cardsProgressBar.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = CardsRecyclerAdapter(
            mutableListOf(),
            object : OnItemClickListener {
                override fun onItemClick(listId: String) {
                    val bundle = Bundle().apply {
                        putString("listId", listId)
                    }

                    findNavController().navigate(
                        R.id.action_sharedCardsListFragment_to_displayCardFragment,
                        bundle
                    )
                }

                override fun onShareCodeClick(code: String, itemView: View) {
                    messageUtils.shareShoppingListCode(code, itemView)
                }
            }
        )

        cardsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = this@SharedCardsListFragment.adapter
        }
    }

    private fun setupJoke() {
        setJoke(jokeTextView, jokeProgressBar)
    }

    private fun setupSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            listUtils.refreshData(
                cardsRecyclerView,
                { fetchSharedListsFromFirebase() },
                swipeRefreshLayout
            )
        }
    }

    private fun observeSharedLists() {
        viewModel.sharedLists.observe(viewLifecycleOwner) { list ->
            listUtils.toggleNoCardListsMessage(noCardsMessageTextView, list)
            adapter?.updateData(list)
            cardsRecyclerView?.post {
                cardsProgressBar.visibility = View.GONE
            }
        }
    }

    private fun observeAddSharedListStatus() {
        viewModel.addSharedListStatus.observe(viewLifecycleOwner) { result ->
            result
                .onSuccess { list ->
                    cardsProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), getString(R.string.info_handling_shared_list, list.name), Toast.LENGTH_LONG).show()
                }
                .onFailure { e ->
                    cardsProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), getString(R.string.error_generic_with_message, e.message), Toast.LENGTH_LONG).show()
                    Log.e("SharedCardsListFragment", "Error handling shared list: ${e.message}")
                }
        }
    }

    private fun setupAddButton(view: View) {
        val inputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(
            R.id.tilSharedCardsListSharedCode
        )
        val shareCodeInput = inputLayout?.editText
        val addButton = view.findViewById<View>(R.id.btnSharedCardsListAdd)

        addButton?.setOnClickListener {
            val shareCode = shareCodeInput?.text?.toString()?.trim()
            if (!shareCode.isNullOrEmpty()) {
                cardsProgressBar.visibility = View.VISIBLE
                viewModel.addSharedListByCode(shareCode)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_invalid_share_code),
                    Toast.LENGTH_SHORT
                ).show()
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
