package com.example.mybudget.drawersection.finance.budget

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.SelectedBudgetViewModel
import com.example.mybudget.drawersection.finance.SharedViewModel

class BudgetEditDialogFragment:DialogFragment() {

    private lateinit var currency: TextView
    private lateinit var financeViewModel:FinanceViewModel
    private lateinit var budgetEditViewModel: BudgetEditViewModel
    private lateinit var selectedBudgetViewModel: SelectedBudgetViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.card_add_budget, null)
        val etName: EditText = dialogView.findViewById(R.id.nameBudgetNew)
        val tvName: TextView = dialogView.findViewById(R.id.titleAddBudget)
        val etAmount: EditText = dialogView.findViewById(R.id.amountNew)
        val tvAmount: TextView = dialogView.findViewById(R.id.amountAddBudget)
        val spinnerType: Spinner = dialogView.findViewById(R.id.typeNewBudget)

        val contextForRestore = requireContext()
        currency = dialogView.findViewById(R.id.currencyNewBudget)

        val checkBox: CheckBox = dialogView.findViewById(R.id.checkBoxBasic)
        builder.setView(dialogView)

        val adapterType = ArrayAdapter.createFromResource(requireContext(), R.array.budget_types, android.R.layout.simple_spinner_item)
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapterType
        val sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        financeViewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
        budgetEditViewModel = ViewModelProvider(requireActivity())[BudgetEditViewModel::class.java]
        budgetEditViewModel.updateSelection(Triple(requireArguments().getString("currency")?:"", "", ""))
        selectedBudgetViewModel = ViewModelProvider(requireActivity())[SelectedBudgetViewModel::class.java]
        sharedViewModel.dataToPass.value = null
        sharedViewModel.dataToPass.observe(this) { data ->
            if (data!=null && data.first.isNotEmpty()){
                currency.text = data.third
                if (requireArguments().getString("basicCurrency")==null){
                    etAmount.setText(
                        budgetEditViewModel.changeCurrencyAmount(
                        oldCurrency = budgetEditViewModel.oldCurrency?:requireArguments().getString("currency")!!,
                        newCurrency = data.first,
                        newAmount = etAmount.text.toString(),
                        context = requireContext()))
                }
                budgetEditViewModel.updateSelection(data)
                sharedViewModel.dataToPass.value = Triple("","","")
            }
        }

        if(arguments?.getString("basicCurrency") == null) {
            val name = requireArguments().getString("name")
            val amount = requireArguments().getString("amount")
            val type = requireArguments().getString("type")
            val transaction = requireArguments().getString("transaction")
            val base = requireArguments().getBoolean("base")
            val currencySymbol = requireArguments().getString("symbol")
            val currencyShort = requireArguments().getString("currency")
            val key = requireArguments().getString("key")

            val context = requireContext()

            tvName.text = resources.getString(R.string.basic_budget_name)
            tvAmount.text = resources.getString(R.string.basic_budget_savings)
            currency.text = currencySymbol
            checkBox.isChecked = base
            etName.setText(name)
            etAmount.setText(amount)

            currency.setOnClickListener { findNavController().navigate(R.id.action_budgetEditDialogFragment_to_currencyDialogFragment) }

            spinnerType.setSelection(adapterType.getPosition(type))

            builder.setPositiveButton(resources.getString(R.string.save)) { dialog, _ ->
                if (etName.text.isNotEmpty() && etAmount.text.isNotEmpty()){
                    if (!base){
                            if(financeViewModel.budgetLiveData.value?.filter { key!=it.key }?.all { it.budgetItem.name != etName.text.toString()} == false) Toast.makeText(context, resources.getString(R.string.error_budget_exists), Toast.LENGTH_LONG).show()
                            else {
                                if (!checkBox.isChecked) {
                                        budgetEditViewModel.updateNotBaseBudget(
                                            key = key!!,
                                            name = etName.text.toString(),
                                            amount = etAmount.text.toString(),
                                            type = spinnerType.selectedItem.toString(),
                                            transaction = transaction?.toInt() ?: 0,
                                            currencyShort = currencyShort!!,
                                            context = context,
                                            financeViewModel = financeViewModel)
                                } else {
                                    budgetEditViewModel.updateBaseBudget(
                                        key = key!!,
                                        name =  etName.text.toString(),
                                        amount = etAmount.text.toString(),
                                        type = spinnerType.selectedItem.toString(),
                                        transaction = transaction?.toInt() ?: 0,
                                        currencyShort = currencyShort!!,
                                        context = context,
                                        financeViewModel = financeViewModel,
                                        selectedBudgetViewModel = selectedBudgetViewModel
                                    )
                                }
                                dialog.dismiss()
                            }
                    }
                    else{
                        if(financeViewModel.budgetLiveData.value?.filter { key!=it.key }?.all { it.budgetItem.name != etName.text.toString()} == false) Toast.makeText(context, "Счет с таким названием уже существует!", Toast.LENGTH_LONG).show()
                        else {
                            budgetEditViewModel.updateOldBaseBudget(
                                name =  etName.text.toString(),
                                amount =  etAmount.text.toString(),
                                type = spinnerType.selectedItem.toString(),
                                transaction =  transaction?.toInt() ?: 0,
                                currencyShort = currencyShort!!,
                                context = context,
                                financeViewModel = financeViewModel
                            )
                            dialog.dismiss()
                        }
                    }
                } else Toast.makeText(context, resources.getString(R.string.error_not_all_data), Toast.LENGTH_LONG).show()
            }

            builder.setNegativeButton(resources.getString(R.string.delete)) { dialog, _ ->
                if (base) Toast.makeText(context, resources.getString(R.string.error_budget_base_delete), Toast.LENGTH_LONG).show()
                else if(financeViewModel.planLiveData.value!!.any { it.budgetId == key})Toast.makeText(context, resources.getString(R.string.error_budget_have_plan), Toast.LENGTH_LONG).show()
                else if(financeViewModel.subLiveData.value!!.any { it.subItem.budgetId == key})Toast.makeText(context, resources.getString(R.string.error_budget_have_sub), Toast.LENGTH_LONG).show()
                else {
                    AlertDialog.Builder(context)
                        .setTitle(resources.getString(R.string.delete_budget))
                        .setMessage(resources.getString(R.string.delete_budget_sure))
                        .setPositiveButton(resources.getString(R.string.agree)) { dialog2, _ ->
                            if (name == etName.text.toString() && etName.text.isNotEmpty() && etAmount.text.isNotEmpty()) {
                                budgetEditViewModel.deleteBudget(key!!, context, financeViewModel)
                                dialog.dismiss()
                            } else when {
                                name != etName.text.toString() -> Toast.makeText(
                                    context,
                                    resources.getString(R.string.error_budget_edit_delete),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            dialog2.dismiss()
                            dialog.dismiss()
                        }
                        .setNegativeButton(resources.getString(R.string.cancel)) { dialog2, _ ->
                            dialog2.dismiss()
                        }.show()
                }
            }

            builder.setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
        }
        else {
            val currencyShort = requireArguments().getString("currency")
            val basicCurrency = requireArguments().getString("basicCurrency")
            val financeViewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
            currency.text = basicCurrency

            currency.setOnClickListener {
                findNavController().navigate(R.id.action_budgetEditDialogFragment_to_currencyDialogFragment)
            }
            builder.setPositiveButton(resources.getString(R.string.add)) { dialog, _ ->
                if (etName.text.isNotEmpty() && etAmount.text.isNotEmpty() && financeViewModel.budgetLiveData.value!!.none {it.budgetItem.name == etName.text.toString()}){
                    if (!checkBox.isChecked){
                        budgetEditViewModel.newOtherBudget(
                            name = etName.text.toString(),
                            amount = etAmount.text.toString(),
                            type = spinnerType.selectedItem.toString(),
                            currencyShort = currencyShort!!
                        )
                        dialog.dismiss()
                    }
                    else{
                        budgetEditViewModel.newBaseBudget(
                            name = etName.text.toString(),
                            amount = etAmount.text.toString(),
                            type = spinnerType.selectedItem.toString(),
                            currencyShort = currencyShort!!,
                            context = contextForRestore,
                            financeViewModel = financeViewModel,
                            selectedBudgetViewModel = selectedBudgetViewModel
                        )
                        dialog.dismiss()
                    }
                } else when{

                    !financeViewModel.budgetLiveData.value!!.none {it.budgetItem.name == etName.text.toString()}-> {
                        val deletedBudget = financeViewModel.budgetLiveData.value!!.find { it.budgetItem.name == etName.text.toString()}!!
                        when(deletedBudget.budgetItem.isDeleted){
                            true->{
                                AlertDialog.Builder(context)
                                    .setTitle(resources.getString(R.string.repair_budget))
                                    .setMessage(resources.getString(R.string.repair_budget_ask))
                                    .setPositiveButton(resources.getString(R.string.yes)) { dialog2, _ ->
                                        budgetEditViewModel.restoreBudget(deletedBudget, contextForRestore, financeViewModel)
                                        dialog2.dismiss()
                                    }
                                    .setNegativeButton(resources.getString(R.string.no)) { dialog2, _ ->
                                        dialog2.dismiss()
                                    }.show()


                            }
                            else-> Toast.makeText(
                                context,
                                resources.getString(R.string.error_budget_exists),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    etName.text.isEmpty() || etAmount.text.isEmpty() -> Toast.makeText(context, resources.getString(R.string.error_not_all_data), Toast.LENGTH_LONG).show()}
            }
            builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog

    }
}