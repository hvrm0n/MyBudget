package com.example.mybudget.drawersection.finance

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.category.SwipeHelper
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var recyclerHistory: RecyclerView
    private lateinit var spinner: Spinner
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbAll: RadioButton
    private lateinit var rbCategory: RadioButton
    private lateinit var rbBudget: RadioButton
    private lateinit var rbPlans: RadioButton
    private lateinit var dateRange: TextView
    private lateinit var noTransactionImage: ImageView
    private lateinit var noTransaction: TextView

    private var startDate : Calendar? = null
    private var endDate : Calendar? = null

    private lateinit var planList:List<HistoryItem>
    private lateinit var historyList:List<HistoryItem>
    private lateinit var financeViewModel:FinanceViewModel
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerHistory = view.findViewById(R.id.historyList)
        spinner = view.findViewById(R.id.spinnerTypeOfHistory)
        radioGroup = view.findViewById(R.id.radioGroupHistory)
        rbAll = view.findViewById(R.id.rbAll)
        rbCategory = view.findViewById(R.id.rbCategoryHistory)
        rbBudget = view.findViewById(R.id.rbBudgetHistory)
        rbPlans = view.findViewById(R.id.rbPlansHistory)
        dateRange = view.findViewById(R.id.dateRangeHistory)
        noTransactionImage = view.findViewById(R.id.noTransactionImage)
        noTransaction = view.findViewById(R.id.noTransactionText)

    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference

        requireActivity().let {financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java]
            financeViewModel.historyLiveData.observe(viewLifecycleOwner){history->
                historyList = history
                updateData()
            }
            financeViewModel.planLiveData.observe(viewLifecycleOwner){plan->
                planList = plan
                updateData()
            }
        }

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerHistory.layoutManager = layoutManager
        historyAdapter = HistoryAdapter(requireContext(), emptyList() /*historyList.sortedByDescending { it.date }.toList()*/, table, auth, requireActivity())
        recyclerHistory.adapter = historyAdapter
        historyAdapter.sortByDate(null, null)

        dateRange.setOnClickListener {
            showDateRangePicker()
        }

        radioGroup.setOnCheckedChangeListener { _, _ ->
            updateData()
        }
    }

    private fun updateData(){
        if (rbAll.isChecked){
            spinner.visibility = View.GONE
            historyAdapter.sortByDate(startDate, endDate, historyList.sortedByDescending { it.date }.toList())
            isTransactionExists()
        } else if (rbCategory.isChecked && financeViewModel.categoryLiveData.value!=null){
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, historyList
                .filter {place-> place.placeId.isNotEmpty()}.map {placeItem-> financeViewModel.categoryBeginLiveData.value!!.filter { it.key == placeItem.placeId }[0].categoryBegin.name})
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            if(historyList.none { place -> place.placeId != "" }){
                historyAdapter.sortByDate(startDate, endDate, emptyList())
            }
            spinner.adapter = adapter
            spinner.visibility = View.VISIBLE
            historyAdapter.checkPlan(false)
            isTransactionExists()
            spinner.onItemSelectedListener = object:OnItemSelectedListener{
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    Log.e("checkWhatIsInHistory", historyList.toString())
                    historyAdapter.sortByDate(startDate, endDate, historyList.filter {placeItem-> placeItem.placeId.isNotEmpty()}.filter {placeItem-> financeViewModel.categoryBeginLiveData.value!!.filter { it.key == placeItem.placeId }[0].categoryBegin.name == spinner.getItemAtPosition(position).toString()}.sortedByDescending { it.date}.toList())
                    isTransactionExists()

                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } else if (rbBudget.isChecked && financeViewModel.budgetLiveData.value!=null){
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, financeViewModel.budgetLiveData.value!!.map { budget -> budget.budgetItem.name })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            spinner.visibility = View.VISIBLE
            historyAdapter.checkPlan(false)
            isTransactionExists()
            spinner.onItemSelectedListener = object:OnItemSelectedListener{
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    historyAdapter.sortByDate(startDate, endDate, historyList.filter {financeViewModel.budgetLiveData.value!!.filter {budget->budget.key == it.budgetId }[0].budgetItem.name == spinner.getItemAtPosition(position).toString()
                    }.sortedByDescending { it.date }.toList())
                    isTransactionExists()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } else if(rbPlans.isChecked){
            historyAdapter.checkPlan(true)
            spinner.visibility = View.GONE
            historyAdapter.sortByDate(startDate, endDate, planList.sortedByDescending { it.date }.toList())
            isTransactionExists()
        }
    }

    private fun showDateRangePicker(){
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Выберите промежуток")
            .build()
        dateRangePicker.show(requireActivity().supportFragmentManager,"date_range_picker")

        dateRangePicker.addOnPositiveButtonClickListener { datePicker ->
            dateRange.text = "${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(datePicker.first)}-${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(datePicker.second)}"
            startDate = Calendar.getInstance().apply { timeInMillis = datePicker.first }
            endDate = Calendar.getInstance().apply {  timeInMillis = datePicker.second}
            if(!rbPlans.isChecked) historyAdapter.sortByDate(startDate, endDate, historyList)
            else historyAdapter.sortByDate(startDate, endDate, planList)
            isTransactionExists()
        }
    }

    private fun isTransactionExists(){
        if (historyAdapter.itemCount == 0){
            noTransactionImage.visibility = View.VISIBLE
            noTransaction.visibility = View.VISIBLE
            if(rbCategory.isChecked){
                when(historyList.all { it.isCategory == false }){
                    true-> spinner.visibility = View.GONE
                    else-> spinner.visibility = View.VISIBLE
                }
            }
        } else {
            if(!rbAll.isChecked && !rbPlans.isChecked){
                spinner.visibility = View.VISIBLE
            }
            noTransactionImage.visibility = View.GONE
            noTransaction.visibility = View.GONE
        }
    }
}
