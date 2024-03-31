package com.example.mybudget.drawersection.finance

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.mybudget.R
import com.example.mybudget.start_pages.CustomAdapter


class CurrencyDialogFragment : DialogFragment() {
    private var selection: Triple<String, String, String>? = Triple("","", "")

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currencyCodes = requireContext().resources.getStringArray(R.array.currency_values)
        val currencyList = ArrayList<Triple<String, String, String>>()
        for (currencyCode in currencyCodes){
            val currencySymbol =  requireContext().resources.getString(requireContext().resources.getIdentifier(currencyCode, "string", requireContext().packageName))
            val currencyName = requireContext().resources.getString(requireContext().resources.getIdentifier("${currencyCode}_name", "string", requireContext().packageName))
            currencyList.add(Triple(currencyCode,currencyName, currencySymbol))
        }
        val adapter = CustomAdapter(requireContext(), currencyList)
        val dialogView = View.inflate(context, R.layout.dialog_currency, null)
        val list = dialogView.findViewById<ListView>(R.id.recyclerNewBudgetCurrency)
        val search = dialogView.findViewById<SearchView>(R.id.searchNewCurrency)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            selection = adapter.getItem(position)
            list.setSelection(position)
            adapter.setSelectedPosition(position)
        }

        val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)


        search.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?) = true

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText) {
                    if (adapter.getPosition(selection)!=-1 && list.getItemAtPosition(adapter.getPosition(selection))==selection) {
                        adapter.setSelectedPosition(adapter.getPosition(selection))
                    } else adapter.setSelectedPosition(-1)
                }
                return true
            }
        })

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)

        builder.setPositiveButton("Выбрать"){dialog, _->
            if (selection?.third == null) {
                Toast.makeText(context, "Вы не выбрали валюту!", Toast.LENGTH_LONG).show()}
            else {
                sharedViewModel.dataToPass.value = selection
                dialog.dismiss()
            }
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
        dialog.window?.setGravity(Gravity.BOTTOM)
        return dialog
    }

}