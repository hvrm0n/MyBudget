package com.example.mybudget.drawersection.finance.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mybudget.R
import com.example.mybudget.databinding.PageHistoryBinding
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryFragment : Fragment() {
    private lateinit var binding: PageHistoryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private var startDate : Calendar? = null
    private var endDate : Calendar? = null

    private lateinit var planList:List<HistoryItem>
    private lateinit var historyList:List<HistoryItem>
    private lateinit var financeViewModel: FinanceViewModel
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
        binding = PageHistoryBinding.bind(view)

        val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    updateData(it.position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        }

        binding.tabs.addOnTabSelectedListener(tabSelectedListener)
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.historyList.layoutManager = layoutManager

        requireActivity().let {financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java]
            financeViewModel.historyLiveData.observe(viewLifecycleOwner){history->
                historyList = history
                updateData(binding.tabs.selectedTabPosition)
            }
            financeViewModel.planLiveData.observe(viewLifecycleOwner){plan->
                planList = plan
                updateData(binding.tabs.selectedTabPosition)
            }
        }

        binding.dateRangeHistory.setOnClickListener {
            showDateRangePicker()
        }

        historyAdapter = HistoryAdapter(requireContext(), emptyList(), table, auth, requireActivity())
        binding.historyList.adapter = historyAdapter
        binding.spinnerTypeOfHistory.visibility = View.GONE

    }

    override fun onResume() {
        super.onResume()
        historyAdapter.sortByDate(startDate, endDate, historyList)
        isTransactionExists()
    }

    private fun showDateRangePicker(){
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(resources.getString(R.string.choose_range))
            .build()
        dateRangePicker.show(requireActivity().supportFragmentManager,"date_range_picker")

        dateRangePicker.addOnPositiveButtonClickListener { datePicker ->
            binding.dateRangeHistory.text = "${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(datePicker.first)}-${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(datePicker.second)}"
            startDate = Calendar.getInstance().apply { timeInMillis = datePicker.first }
            endDate = Calendar.getInstance().apply {  timeInMillis = datePicker.second}
            updateData(binding.tabs.selectedTabPosition)
            isTransactionExists()
        }
    }

    private fun isTransactionExists(){
        if (historyAdapter.itemCount == 0){
            binding.noTransactionImage.visibility = View.VISIBLE
            binding.noTransactionText.visibility = View.VISIBLE
            if(binding.spinnerTypeOfHistory.count == 0) {
                binding.spinnerTypeOfHistory.visibility = View.GONE
            }
        } else {
            if(binding.tabs.selectedTabPosition!=0 && binding.tabs.selectedTabPosition!=3){
                binding.spinnerTypeOfHistory.visibility = View.VISIBLE
            }
            binding.noTransactionImage.visibility = View.GONE
            binding.noTransactionText.visibility = View.GONE
        }
    }

    private fun updateData(position: Int){
        when (position) {
            //tabAll
            0 -> {
                historyAdapter.checkPlan(false)
                binding.spinnerTypeOfHistory.visibility = View.GONE
                historyAdapter.sortByDate(startDate, endDate, historyList)
                isTransactionExists()
            }
            //tabBudget
            1 -> {
                historyAdapter.checkPlan(false)
                when {
                    financeViewModel.budgetLiveData.value==null-> {
                        historyList = emptyList()
                        historyAdapter.notifyDataSetChanged()
                        isTransactionExists()
                    }
                    else -> {
                        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, financeViewModel.budgetLiveData.value!!.map { budget -> budget.budgetItem.name }.toSet().toList())
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.spinnerTypeOfHistory.adapter = adapter
                        binding.spinnerTypeOfHistory.onItemSelectedListener = object:OnItemSelectedListener{
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                historyAdapter.sortByDate(startDate, endDate,
                                    historyList
                                            .filter {financeViewModel.budgetLiveData.value!!
                                            .firstOrNull {budget->budget.key == it.budgetId}
                                            ?.budgetItem?.name == binding.spinnerTypeOfHistory
                                                .getItemAtPosition(position).toString() }.toList()

                                            +

                                            historyList
                                                .filter {financeViewModel.budgetLiveData.value!!
                                                .firstOrNull { budget->budget.key == it.placeId}
                                                ?.budgetItem?.name == binding.spinnerTypeOfHistory
                                                    .getItemAtPosition(position).toString() }.toList()

                                )
                                isTransactionExists()
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                        binding.spinnerTypeOfHistory.setSelection(0, true)
                    }
                }
            }
            //tabCategory
            2->{
                historyAdapter.checkPlan(false)
                when{
                    financeViewModel.categoryLiveData.value == null ->{
                        historyList = emptyList()
                        historyAdapter.notifyDataSetChanged()
                        isTransactionExists()
                    }
                    else->{
                        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, historyList
                            .filter {place-> place.placeId.isNotEmpty() && place.isCategory==true}.map {placeItem-> financeViewModel.categoryBeginLiveData.value!!.filter { it.key == placeItem.placeId }[0].categoryBegin.name}.toSet().toList())
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        if(historyList.all { place -> place.isCategory == false }){
                            historyAdapter.sortByDate(startDate, endDate, emptyList())
                        }
                        binding.spinnerTypeOfHistory.adapter = adapter
                        binding.spinnerTypeOfHistory.onItemSelectedListener = object:OnItemSelectedListener{
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                historyAdapter.sortByDate(startDate, endDate, historyList.filter {placeItem-> placeItem.placeId.isNotEmpty() && placeItem.isCategory==true}.filter {placeItem-> financeViewModel.categoryBeginLiveData.value!!.filter { it.key == placeItem.placeId }[0].categoryBegin.name == binding.spinnerTypeOfHistory.getItemAtPosition(position).toString()})
                                isTransactionExists()
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                        binding.spinnerTypeOfHistory.setSelection(0, true)
                    }
                }
            }

            //tabPlan
            3 -> {
                historyAdapter.checkPlan(true)
                binding.spinnerTypeOfHistory.visibility = View.GONE
                historyAdapter.sortByDate(startDate, endDate, planList)
                isTransactionExists()
            }

            //tabGoal
            4 -> {
                historyAdapter.checkPlan(false)
                when{
                    financeViewModel.goalsData.value == null ->{
                        historyList = emptyList()
                        historyAdapter.notifyDataSetChanged()
                        isTransactionExists()
                    }
                    else->{
                        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, historyList
                            .filter {place-> place.placeId.isNotEmpty() && place.isGoal==true}.map {placeItem-> financeViewModel.goalsData.value!!.filter { it.key == placeItem.placeId }[0].goalItem.name}.toSet().toList())
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        if(historyList.all { place -> place.isGoal == false }){
                            historyAdapter.sortByDate(startDate, endDate, emptyList())
                        }
                        binding.spinnerTypeOfHistory.adapter = adapter
                        binding.spinnerTypeOfHistory.onItemSelectedListener = object:OnItemSelectedListener{
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                historyAdapter.sortByDate(startDate, endDate, historyList.filter {placeItem-> placeItem.placeId.isNotEmpty() && placeItem.isGoal==true}.filter {placeItem-> financeViewModel.goalsData.value!!.filter { it.key == placeItem.placeId }[0].goalItem.name == binding.spinnerTypeOfHistory.getItemAtPosition(position).toString()})
                                isTransactionExists()
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                        binding.spinnerTypeOfHistory.setSelection(0, true)
                    }
                }
            }

            //tabLoan
            5 ->{
                historyAdapter.checkPlan(false)
                when{
                    financeViewModel.loansLiveData.value == null ->{
                        historyList = emptyList()
                        historyAdapter.notifyDataSetChanged()
                        isTransactionExists()
                    }
                    else->{
                        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, historyList
                            .filter {place-> place.placeId.isNotEmpty() && place.isLoan==true}.map {placeItem-> financeViewModel.loansLiveData.value!!.filter { it.key == placeItem.placeId }[0].loanItem.name}.toSet().toList())
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        if(historyList.all { place -> place.isSub == false }){
                            historyAdapter.sortByDate(startDate, endDate, emptyList())
                        }
                        binding.spinnerTypeOfHistory.adapter = adapter
                        binding.spinnerTypeOfHistory.onItemSelectedListener = object:OnItemSelectedListener{
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                historyAdapter.sortByDate(startDate, endDate, historyList.filter {placeItem-> placeItem.placeId.isNotEmpty() && placeItem.isLoan==true}.filter {placeItem-> financeViewModel.loansLiveData.value!!.filter { it.key == placeItem.placeId }[0].loanItem.name == binding.spinnerTypeOfHistory.getItemAtPosition(position).toString()})
                                isTransactionExists()
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                        binding.spinnerTypeOfHistory.setSelection(0, true)
                    }
                }
            }

            //tabSub
            6->{
                historyAdapter.checkPlan(false)
                when{
                    financeViewModel.subLiveData.value == null ->{
                        historyList = emptyList()
                        historyAdapter.notifyDataSetChanged()
                        isTransactionExists()
                    }
                    else->{
                        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, historyList
                            .filter {place-> place.placeId.isNotEmpty() && place.isSub==true}.map {placeItem-> financeViewModel.subLiveData.value!!.filter { it.key == placeItem.placeId }[0].subItem.name}.toSet().toList())
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        if(historyList.all { place -> place.isSub == false }){
                            historyAdapter.sortByDate(startDate, endDate, emptyList())
                        }
                        binding.spinnerTypeOfHistory.adapter = adapter
                        binding.spinnerTypeOfHistory.onItemSelectedListener = object:OnItemSelectedListener{
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                historyAdapter.sortByDate(startDate, endDate, historyList.filter {placeItem-> placeItem.placeId.isNotEmpty() && placeItem.isSub==true}.filter {placeItem-> financeViewModel.subLiveData.value!!.filter { it.key == placeItem.placeId }[0].subItem.name == binding.spinnerTypeOfHistory.getItemAtPosition(position).toString()})
                                isTransactionExists()
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                        binding.spinnerTypeOfHistory.setSelection(0, true)
                    }
                }
            }
        }
    }
}
