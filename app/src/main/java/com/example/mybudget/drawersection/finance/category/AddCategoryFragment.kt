package com.example.mybudget.drawersection.finance.category

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import java.util.Calendar


class AddCategoryFragment : Fragment() {

    private lateinit var recyclerCategoryChooser:RecyclerView
    private lateinit var spinnerPriority:Spinner
    private lateinit var button: MaterialButton
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var categoriesAlready: Array<String>
    private var selectedIcon = ""

    private lateinit var financeViewModel:FinanceViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.card_default_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerCategoryChooser = view.findViewById(R.id.chooseCategory)
        spinnerPriority = view.findViewById(R.id.spinnerPrority)
        button = view.findViewById(R.id.chooseNewCategory)
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        financeViewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
        val adapterPriority = ArrayAdapter.createFromResource(requireContext(), R.array.category_priority, android.R.layout.simple_spinner_item)
        adapterPriority.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapterPriority
        categoriesAlready = financeViewModel.categoryBeginLiveData.value
            ?.filter { it.key in financeViewModel.categoryLiveData.value!!.map { base -> base.key } }
            ?.map { it.key }!!.toTypedArray()


        val layoutManager = GridLayoutManager(context, 4)
        recyclerCategoryChooser.layoutManager = layoutManager

        val adapter = ChooseCategoryAdapter(requireContext(), emptyList(), this, table, auth, button)
        recyclerCategoryChooser.adapter = adapter

        financeViewModel.categoryBeginLiveData.observe(viewLifecycleOwner){
            adapter.updateData(it.filter {category-> categoriesAlready.all {categoriesAlready-> categoriesAlready !=  category.key}})
        }

        button.setOnClickListener {

            adapter.getChose()?.let{last->
                val name = last.findViewById<TextView>(R.id.nameCategory)?.text.toString()
                table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                    .child("${financeViewModel.financeDate.value!!.second}/${financeViewModel.financeDate.value!!.first}")
                .child("ExpenseCategories")
                .child( financeViewModel.categoryBeginLiveData.value!!.filter { it.categoryBegin.name == name }[0].key).setValue(_CategoryItem("0", "0.00", spinnerPriority.selectedItemPosition,
                    isPlanned = (Calendar.getInstance().get(Calendar.YEAR)<financeViewModel.financeDate.value!!.second||Calendar.getInstance().get(Calendar.MONTH)+1<financeViewModel.financeDate.value!!.first))).addOnSuccessListener {
                    view?.findNavController()?.popBackStack()
                }.addOnFailureListener { ex->
                    Log.e("SaveError", ex.message.toString())
                }
            }
        }
    }

    fun changeSelectedIcon(newIcon:String){
        selectedIcon = newIcon
    }
}