package com.example.groclistapp.ui.list

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.groclistapp.R
import com.example.groclistapp.data.adapter.card.CardsRecyclerAdapter
import com.example.groclistapp.data.adapter.card.OnItemClickListener
import com.example.groclistapp.data.model.ShoppingList
import com.example.groclistapp.data.model.ShoppingListSummary
import com.example.groclistapp.data.repository.AppDatabase

class SharedCardsListFragment : Fragment() {
    private var adapter: CardsRecyclerAdapter? = null
    private var cardsRecyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shared_cards_list, container, false)

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
//        noStudentsTextView = view.findViewById(R.id.tvStudentsListNoStudentsMessage)
//        addStudentButton = view.findViewById(R.id.btnStudentsListAddStudent)
        cardsRecyclerView = view.findViewById(R.id.rvSharedCardsList)
//
        setStudentsRecyclerView(view)
//        addStudentButton?.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_studentsListFragment_to_addStudentFragment))
    }

    private fun setStudentsRecyclerView(view: View) {
        val shoppingListDao = AppDatabase.getDatabase(requireContext()).shoppingListDao()
        val shoppingItemDao = AppDatabase.getDatabase(requireContext()).shoppingItemDao()
        var cards: MutableList<Any> = mutableListOf(
            "Apples: 5",
            "Bananas: 10",
            "Oranges: 3"
        )


        cardsRecyclerView?.setHasFixedSize(true)
//
        adapter = CardsRecyclerAdapter(
            mutableListOf(),
            shoppingListDao,
            shoppingItemDao,
            object : OnItemClickListener {
                override fun onItemClick(listId: Int) {

                }
            }
        )


//        adapter?.listener = object : OnItemClickListener {
//            override fun onItemClick(student: Student?) {
//                student?.let {
//                    val action = StudentsListFragmentDirections.actionStudentsListFragmentToStudentDetailsFragment(it.id)
//
//                    Model.instance.currStudentId = it.id
//                    Navigation.findNavController(view).navigate(action)
//                }
//            }
//        }
//
        cardsRecyclerView?.layoutManager = LinearLayoutManager(context)
        cardsRecyclerView?.adapter = adapter
    }
}