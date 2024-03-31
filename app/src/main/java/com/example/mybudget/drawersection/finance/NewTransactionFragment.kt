package com.example.mybudget.drawersection.finance

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.mybudget.R
import com.google.android.material.button.MaterialButton


class NewTransactionFragment : Fragment() {
    private lateinit var radioGroup:RadioGroup
    private lateinit var radioBudget:RadioButton
    private lateinit var radioCategory:RadioButton
    private lateinit var value:EditText
    private lateinit var textViewCategory:TextView
    private lateinit var spinnerCategory:Spinner
    private lateinit var spinnerBudget:Spinner
    private lateinit var calendar: CalendarView
    private lateinit var buttonAdd:MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_new_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        radioBudget = view.findViewById(R.id.income)
        radioCategory = view.findViewById(R.id.expence)
        value = view.findViewById(R.id.savingsValue)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        textViewCategory = view.findViewById(R.id.categoryTitle)
        spinnerBudget= view.findViewById(R.id.spinnerBudget)
        calendar = view.findViewById(R.id.calendarView)
        buttonAdd = view.findViewById(R.id.buttonAddIncome)
        radioGroup = view.findViewById(R.id.radioGroupNewExpence)

        radioGroup.setOnCheckedChangeListener { _, _ ->
            if (radioBudget.isChecked){
                textViewCategory.visibility = View.GONE
                spinnerCategory.visibility = View.GONE
            } else {
                textViewCategory.visibility = View.VISIBLE
                spinnerCategory.visibility = View.VISIBLE
            }
        }

    }
}