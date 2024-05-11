package com.example.mybudget.drawersection.loans

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


class LoansFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var recyclerLoans: RecyclerView
    private lateinit var adapterLoans: LoansAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_loans, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerLoans = view.findViewById(R.id.loansList)
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        activity?.let {financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java]}
        adapterLoans = LoansAdapter(requireContext(), emptyList(), table, auth, financeViewModel, this)
        recyclerLoans.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerLoans.adapter = adapterLoans

        financeViewModel.loansLiveData.observe(viewLifecycleOwner){
            adapterLoans.updateData(it.filter { item-> !item.loanItem.isDeleted && if(PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("showCompleted", true) && PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("showFinishedLoans", true)) true else !item.loanItem.isFinished})
        }

        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(recyclerLoans, adapterLoans, "loan") {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                if(position == adapterLoans.itemCount-1){return emptyList() }
                val deleteButton = deleteButton(position)
                val editButton = editButton(position)
                return listOf(deleteButton, editButton)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerLoans)
    }

    private fun deleteButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            requireContext(),
            R.drawable.trash,
            R.color.dark_orange,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    AlertDialog.Builder(context)
                        .setTitle("Удаление подписки")
                        .setMessage("Вы уверены, что хотите удалить выплату?")
                        .setPositiveButton("Подтвердить") { dialog, _ ->
                            adapterLoans.deleteItemAtPosition(position)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Отмена") { dialog, _ ->
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
                    adapterLoans.editItemAtPosition(position)
                }
            })
    }

}