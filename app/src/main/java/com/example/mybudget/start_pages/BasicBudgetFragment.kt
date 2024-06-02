package com.example.mybudget.start_pages

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.example.mybudget.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database


class BasicBudgetFragment : Fragment() {

    private lateinit var endButton: Button
    private lateinit var spinnerTypeOfBudget: Spinner
    private lateinit var nameBudget: EditText
    private lateinit var savings: EditText
    private lateinit var viewModel:BasicBudgetViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_basic_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        endButton = view.findViewById(R.id.buttonCurrencyNext)
        spinnerTypeOfBudget = view.findViewById(R.id.spinnerTypeOfBudget)
        nameBudget = view.findViewById(R.id.nameBasicBudget)
        savings = view.findViewById(R.id.savingsBasicBudget)

        val adapter = ArrayAdapter.createFromResource(requireContext(), R.array.budget_types, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTypeOfBudget.adapter = adapter
        viewModel = ViewModelProvider(this)[BasicBudgetViewModel::class.java]

        viewModel.name.value?.let {
            nameBudget.setText(it)
        }

        viewModel.savings.value?.let {
            savings.setText(it)
        }

        viewModel.type.value?.let {
            spinnerTypeOfBudget.setSelection(it)
        }

        savings.doAfterTextChanged {
            endButton.isEnabled = nameBudget.text.isNotEmpty()&&savings.text.isNotEmpty()&&(spinnerTypeOfBudget.selectedItemId!=-1L)
            viewModel.savings.value = it.toString()
        }

        nameBudget.doAfterTextChanged {
            endButton.isEnabled = nameBudget.text.isNotEmpty()&&savings.text.isNotEmpty()&&(spinnerTypeOfBudget.selectedItemId!=-1L)
            viewModel.name.value = it.toString()
        }

        spinnerTypeOfBudget.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                endButton.isEnabled = savings.text.isNotEmpty()&&nameBudget.text.isNotEmpty()
                viewModel.type.value = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                endButton.isEnabled = false
                viewModel.type.value = -1
            }
        }

        endButton.setOnClickListener {
            Navigation.findNavController(requireView()).navigate(R.id.action_basicBudgetFragment_to_homePageActivity)
        }
    }

    override fun onStart() {
        super.onStart()

        endButton.setOnClickListener {
           /* table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").child("name").setValue(nameBudget.text.toString())
            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").child("amount").setValue(savings.text.toString())
            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").child("type").setValue(spinnerTypeOfBudget.selectedItem.toString())
            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").child("count").setValue(0)*/
            viewModel.createBaseBudget(nameBudget = nameBudget.text.toString(), savings = savings.text.toString(), type = spinnerTypeOfBudget.selectedItem.toString())
            Navigation.findNavController(requireView()).navigate(R.id.action_basicBudgetFragment_to_homePageActivity)
        }
    }
}