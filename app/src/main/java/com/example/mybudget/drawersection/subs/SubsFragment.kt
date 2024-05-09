package com.example.mybudget.drawersection.subs

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
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


class SubsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var recyclerSubs: RecyclerView
    private lateinit var adapterSubs: SubsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_subs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerSubs = view.findViewById(R.id.subsList)
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        activity?.let {financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java]}
        adapterSubs = SubsAdapter(requireContext(), emptyList(), table, auth, financeViewModel, this)
        recyclerSubs.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerSubs.adapter = adapterSubs

        financeViewModel.subLiveData.observe(viewLifecycleOwner){
            adapterSubs.updateData(it.filter { item-> !item.subItem.isDeleted })
        }

        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(recyclerSubs, adapterSubs, "sub") {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                if(position == adapterSubs.itemCount-1){return emptyList() }
                val deleteButton = deleteButton(position)
                val editButton = editButton(position)
                return listOf(deleteButton, editButton)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerSubs)
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
                        .setMessage("Вы уверены, что хотите удалить подписку?")
                        .setPositiveButton("Подтвердить") { dialog, _ ->
                            adapterSubs.deleteItemAtPosition(position)
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
                    adapterSubs.editItemAtPosition(position)
                }
            })
    }

}