package com.example.mybudget.drawersection.finance.category

import android.os.Bundle
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
import com.google.firebase.auth.auth
import com.google.firebase.database.database


class AddCategoryFragment : Fragment() {

    private lateinit var recyclerCategoryChooser:RecyclerView
    private lateinit var spinnerPriority:Spinner
    private lateinit var button: MaterialButton
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
        financeViewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
        val adapterPriority = ArrayAdapter.createFromResource(requireContext(), R.array.category_priority, android.R.layout.simple_spinner_item)
        adapterPriority.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapterPriority
        categoriesAlready = financeViewModel.categoryBeginLiveData.value
            ?.filter { it.key in financeViewModel.categoryLiveData.value!!.map { base -> base.key } }
            ?.map { it.key }!!.toTypedArray()


        val layoutManager = GridLayoutManager(context, 4)
        recyclerCategoryChooser.layoutManager = layoutManager

        val adapter = ChooseCategoryAdapter(requireContext(), emptyList(), this, Firebase.database.reference,  Firebase.auth, button)
        recyclerCategoryChooser.adapter = adapter

        financeViewModel.categoryBeginLiveData.observe(viewLifecycleOwner){
            adapter.updateData(it.filter {category-> categoriesAlready.all {categoriesAlready-> categoriesAlready !=  category.key}})
        }

        button.setOnClickListener {

            adapter.getChose()?.let{last->
                financeViewModel.addCategory(name = last.findViewById<TextView>(R.id.nameCategory)?.text.toString(),
                                             spinnerPriority =  spinnerPriority.selectedItemPosition)
                view?.findNavController()?.popBackStack()
            }
        }
    }

    fun changeSelectedIcon(newIcon:String){
        selectedIcon = newIcon
    }
}