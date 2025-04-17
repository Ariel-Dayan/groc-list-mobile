package com.example.groclistapp.ui.list

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.adapter.card.CardsRecyclerAdapter
import com.example.groclistapp.data.adapter.card.OnItemClickListener
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingListSummary
import com.example.groclistapp.data.network.jokes.JokesClient.setJoke
import com.example.groclistapp.data.repository.AppDatabase
import com.example.groclistapp.viewmodel.SharedCardsViewModel
import androidx.navigation.fragment.findNavController


class SharedCardsListFragment : Fragment() {
    private var adapter: CardsRecyclerAdapter? = null
    private var cardsRecyclerView: RecyclerView? = null
    private lateinit var viewModel: SharedCardsViewModel
    private  lateinit var jokeTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shared_cards_list, container, false)
        viewModel = androidx.lifecycle.ViewModelProvider(this).get(SharedCardsViewModel::class.java)

        setupView(view)
//        toggleNoStudentsMessage()

        return view
    }

    //    private fun toggleNoStudentsMessage() {
//        if (students == null || students?.isEmpty() == true) {
//            noStudentsTextView?.visibility = TextView.VISIBLE
//        } else {
//            noStudentsTextView?.visibility = TextView.GONE
//        }
//    }

    private fun setupView(view: View) {
        cardsRecyclerView = view.findViewById(R.id.rvSharedCardsList)
        jokeTextView = view.findViewById(R.id.tvSharedCardsListJoke)

        val inputLayout = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSharedCardsListSharedCode)
        val shareCodeInput = inputLayout.editText

        val addButton = view.findViewById<View>(R.id.btnSharedCardsListAdd)

        val shoppingListDao = AppDatabase.getDatabase(requireContext()).shoppingListDao()
        val shoppingItemDao = AppDatabase.getDatabase(requireContext()).shoppingItemDao()

        adapter = CardsRecyclerAdapter(
            mutableListOf(),  // יתעדכן בהמשך
            shoppingListDao,
            shoppingItemDao,
            object : OnItemClickListener {
                override fun onItemClick(listId: Int) {
                    val bundle = Bundle().apply {
                        putInt("listId", listId)
                    }
                    val navController = findNavController()
                    navController.navigate(R.id.action_sharedCardsListFragment_to_displayCardFragment, bundle)
                }
            }

        )

        setJoke(jokeTextView)

        cardsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = this@SharedCardsListFragment.adapter
        }

        // תצפית על הרשימות המשותפות מתוך ViewModel
        viewModel.sharedLists.observe(viewLifecycleOwner) { list ->
            adapter?.setData(list)
        }

        addButton.setOnClickListener {
            val shareCode = shareCodeInput?.text?.toString()?.trim()

            if (!shareCode.isNullOrEmpty()) {
                val repository = com.example.groclistapp.data.repository.ShoppingListRepository(
                    shoppingListDao,
                    shoppingItemDao
                )

                repository.loadSharedListByCode(
                    shareCode = shareCode,
                    onSuccess = { list ->
                        requireActivity().runOnUiThread {
                            android.widget.Toast.makeText(requireContext(), "Shared list loaded: ${list.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { e ->
                        requireActivity().runOnUiThread {
                            android.widget.Toast.makeText(requireContext(), "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } else {
                android.widget.Toast.makeText(requireContext(), "Please enter a valid share code", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }



}