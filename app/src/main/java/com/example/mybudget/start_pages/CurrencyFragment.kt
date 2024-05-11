package com.example.mybudget.start_pages

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Filter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.mybudget.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database

class CurrencyFragment : Fragment() {

    private lateinit var nextPageButton: Button
    private lateinit var listViewCurrency: ListView
    private lateinit var search: SearchView
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var currencyCodes: Array<String>
    private  var selection: Triple<String, String, String>? = Triple("","", "")
    private  var positionOfSelection = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_choose_currency, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nextPageButton = view.findViewById(R.id.buttonCurrencyNext)
        listViewCurrency = view.findViewById(R.id.listViewBasicCurrencyStart)
        search = view.findViewById(R.id.searchCurrencyBegin)

        val viewModel = ViewModelProvider(this)[CurrencyViewModel::class.java]

        currencyCodes = resources.getStringArray(R.array.currency_values)
        val currencyList = ArrayList<Triple<String, String, String>>()

        for (currencyCode in currencyCodes){
            val currencySymbol =  resources.getString(resources.getIdentifier(currencyCode, "string", requireActivity().packageName))
            val currencyName = resources.getString(resources.getIdentifier("${currencyCode}_name", "string", requireActivity().packageName))
            currencyList.add(Triple(currencyCode,currencyName, currencySymbol))
        }

        val adapter = CustomAdapter(requireContext(), currencyList)
        listViewCurrency.adapter = adapter

        viewModel.selection.value?.let {
            if (adapter.getPosition(it)!=-1 && listViewCurrency.getItemAtPosition(adapter.getPosition(it))==it) {
                adapter.setSelectedPosition(adapter.getPosition(it))
                nextPageButton.isEnabled = true
            } else adapter.setSelectedPosition(-1)

            positionOfSelection = adapter.getPosition(it)
            selection = it
        }

        listViewCurrency.setOnItemClickListener { _, _, position, _ ->
            nextPageButton.isEnabled = true
            selection = adapter.getItem(position)
            positionOfSelection = position
            listViewCurrency.setSelection(position)
            adapter.setSelectedPosition(position)
            viewModel.selection.value = selection
        }

        search.setOnQueryTextListener(object:SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?) = true

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText) {
                    if (adapter.getPosition(selection)!=-1 && listViewCurrency.getItemAtPosition(adapter.getPosition(selection))==selection) {
                        adapter.setSelectedPosition(adapter.getPosition(selection))
                    } else adapter.setSelectedPosition(-1)
                }
                return true
            }
        })


    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        nextPageButton.setOnClickListener {
            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").child("currency").setValue(selection?.first.toString())
            Navigation.findNavController(requireView()).navigate(R.id.action_currencyFragment_to_basicBudgetFragment) }
    }
}

class CustomAdapter(context: Context, private var items: List<Triple<String, String, String>>) : ArrayAdapter<Triple<String, String, String>>(context, R.layout.currency_item, items) {
    private var selectedPosition = -1
    var originalItems: List<Triple<String, String, String>> = items.toList()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.currency_item, parent, false)
        }

        view!!.findViewById<TextView>(R.id.shortCurrency).text = items[position].first
        view.findViewById<TextView>(R.id.nameCurrency).text = items[position].second
        view.findViewById<TextView>(R.id.symbolCurrency).text = items[position].third

        if (position == selectedPosition) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.light_green))
        } else {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.very_light_green))
        }

        return view
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    private val filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = mutableListOf<Triple<String, String, String>>()

            if (constraint.isNullOrEmpty()) {
                filteredList.addAll(originalItems)
            } else {
                val filterPattern = constraint.toString().lowercase().trim()

                for (item in originalItems) {
                    if (item.first.lowercase().contains(filterPattern) ||
                        item.second.lowercase().contains(filterPattern) ||
                        item.third.lowercase().contains(filterPattern)) {
                        filteredList.add(item)
                    }
                }
            }

            val results = FilterResults()
            results.values = filteredList.toList()
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            addAll(results?.values as? List<Triple<String, String, String>> ?: mutableListOf())
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter {
        return filter
    }
}