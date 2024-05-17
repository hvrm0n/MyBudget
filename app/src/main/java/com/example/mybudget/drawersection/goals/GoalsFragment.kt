package com.example.mybudget.drawersection.goals

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.category.SwipeHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar

class GoalsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var recyclerGoals:RecyclerView
    private lateinit var adapterGoals: GoalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_goals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerGoals = view.findViewById(R.id.goalsList)
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        activity?.let {financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java]}
        adapterGoals = GoalsAdapter(requireContext(), emptyList(), table, auth, financeViewModel, this)
        recyclerGoals.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerGoals.adapter = adapterGoals

        financeViewModel.goalsData.observe(viewLifecycleOwner){
            adapterGoals.updateData(it.filter { item -> !item.goalItem.isDeleted && if ( PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("showCompleted", true) && PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("showReachedGoals", true)) true else !item.goalItem.isReached}
                .sortedByDescending {giwk->
                    when (giwk.goalItem.date){
                        null->0
                        else->giwk.goalItem.date?.split('.')?.let {date->
                            ChronoUnit.DAYS.between(LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                LocalDate.of(date[2].toInt(), date[1].toInt(), date[0].toInt()))
                        }
                    }
                }
                .sortedBy { reach -> reach.goalItem.isReached }
            )

        }

        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(recyclerGoals, adapterGoals, "goal") {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                if(position == adapterGoals.itemCount-1){return emptyList() }
                else if (position >= adapterGoals.placeReach() && adapterGoals.placeReach()!=-1) {return listOf(deleteButton(position))}
                val deleteButton = deleteButton(position)
                val editButton = editButton(position)
                return listOf(deleteButton, editButton)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerGoals)
    }

    private fun deleteButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            requireContext(),
            R.drawable.trash,
            R.color.dark_orange,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    AlertDialog.Builder(context)
                        .setTitle(resources.getString(R.string.delete_goal))
                        .setMessage(resources.getString(R.string.delete_goal_sure))
                        .setPositiveButton(resources.getString(R.string.agree)) { dialog, _ ->
                            adapterGoals.deleteItemAtPosition(position)
                            dialog.dismiss()
                        }
                        .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }.show()
                }
            })
    }

    private fun editButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            requireContext(),
            R.drawable.pencil,
            R.color.dark_green,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    adapterGoals.editItemAtPosition(position)
                }
            })
    }
}