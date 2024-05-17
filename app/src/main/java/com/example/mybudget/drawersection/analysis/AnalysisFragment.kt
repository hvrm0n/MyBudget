package com.example.mybudget.drawersection.analysis

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.ExchangeRateResponse
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
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
import kotlin.math.abs

class AnalysisFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var pieChart: PieChart
    private lateinit var tabs: TabLayout
    private lateinit var typeSpinnerForBudgets: Spinner
    private lateinit var noTransactionImage: ImageView
    private lateinit var noTransactionTextView: TextView

    private lateinit var dateRangeAnalysis: TextView
    private var startDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    private var endDate = Calendar.getInstance()
    private val historyDate = Calendar.getInstance()

    private var currencyConvertor: ExchangeRateResponse? = null
    private val entries = mutableListOf<PieEntry>()
    private var goalsResult = 0.0
    private var loansResult = 0.0
    private var subsResult = 0.0
    private var categoryResult = 0.0
    private var budgetResult = 0.0
    private lateinit var baseCurrency:String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pieChart = view.findViewById(R.id.pieChart)
        tabs = view.findViewById(R.id.tabsAnalys)
        typeSpinnerForBudgets = view.findViewById(R.id.spinnerAnalysBudget)
        dateRangeAnalysis = view.findViewById(R.id.dateRangeAnalysis)
        noTransactionTextView = view.findViewById(R.id.noTransactionTextAnalysis)
        noTransactionImage = view.findViewById(R.id.noTransactionImageAnalysis)

        typeSpinnerForBudgets.onItemSelectedListener = object :OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if(tabs.selectedTabPosition == 1){
                    when (position){
                        0->showBudgetDataIncomes()
                        1->showBudgetDataExpences()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        dateRangeAnalysis.setOnClickListener { showDateRangePicker() }
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        activity?.let {
            financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java]
        }
        baseCurrency = financeViewModel.budgetLiveData.value?.find { it.key == "Base budget" }!!.budgetItem.currency


        val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    updateData(it.position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        }
        tabs.addOnTabSelectedListener(tabSelectedListener)
        updateData(0)
    }

    private fun updateData(position:Int){
        if(position!=1) typeSpinnerForBudgets.visibility = View.GONE
        else typeSpinnerForBudgets.visibility = View.VISIBLE
        when(position){
            0-> showAllData()
            1-> {
                typeSpinnerForBudgets.apply {
                    visibility = View.VISIBLE
                    setSelection(0)
                }
                showBudgetDataIncomes()
            }
            2-> showCategoryData()
            3-> showGoalsData()
            4-> showLoansData()
            5-> showSubsData()
        }
    }

    private fun setHistoryDate(year:Int, month:Int, day: Int) {
        historyDate.apply {
            set(year, month, day, 0,0 ,0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun showAllData(){
        clearFields()
        financeViewModel.historyLiveData.value?.let {historyList->
            historyList.forEach { historyItem->
                if (historyItem.date.isNotEmpty()) {
                    setHistoryDate(
                        year = historyItem.date.split(".")[2].toInt(),
                        month = historyItem.date.split(".")[1].toInt() - 1,
                        day = historyItem.date.split(".")[0].toInt()
                    )

                    if (historyDate.timeInMillis >= startDate.timeInMillis && historyDate.timeInMillis <= endDate.timeInMillis) {
                        when {
                            historyItem.isGoal == true -> {
                                financeViewModel.goalsData.value?.find { it.key == historyItem.placeId }?.goalItem?.let {
                                    //Без abs потому что там могут быть списания обратно на счет
                                    goalsResult += changeCurrencyAmount(
                                        oldCurrency = it.currency,
                                        newCurrency = baseCurrency,
                                        newAmount = historyItem.baseAmount,
                                        context = requireContext()
                                    ).toDouble()
                                }
                            }

                            historyItem.isSub == true -> {
                                financeViewModel.budgetLiveData.value?.find { it.key == historyItem.budgetId }?.budgetItem?.let {
                                    subsResult += abs(
                                        changeCurrencyAmount(
                                            oldCurrency = it.currency,
                                            newCurrency = baseCurrency,
                                            newAmount = historyItem.baseAmount,
                                            context = requireContext()
                                        ).toDouble()
                                    )
                                }
                            }

                            historyItem.isCategory == true -> {
                                categoryResult += abs(
                                    changeCurrencyAmount(
                                        oldCurrency = baseCurrency,
                                        newCurrency = baseCurrency,
                                        newAmount = historyItem.baseAmount,
                                        context = requireContext()
                                    ).toDouble()
                                )
                            }

                            historyItem.isLoan == true -> {
                                financeViewModel.loansLiveData.value?.find { it.key == historyItem.placeId }?.loanItem?.let {
                                    loansResult += abs(
                                        changeCurrencyAmount(
                                            oldCurrency = it.currency,
                                            newCurrency = baseCurrency,
                                            newAmount = historyItem.baseAmount,
                                            context = requireContext()
                                        ).toDouble()
                                    )
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }

            if (categoryResult > 0.0) entries.add(PieEntry(categoryResult.toFloat(), resources.getString(R.string.category)))
            if (loansResult > 0.0) entries.add(PieEntry(loansResult.toFloat(), resources.getString(R.string.menu_loans)))
            if (goalsResult > 0.0) entries.add(PieEntry(goalsResult.toFloat(), resources.getString(R.string.menu_goals)))
            if (subsResult > 0.0) entries.add(PieEntry(subsResult.toFloat(), resources.getString(R.string.menu_subs)))

            if (entries.isNotEmpty()) {
                val dataSet = PieDataSet(entries, "")
                dataSet.colors = resources.getIntArray(R.array.color_chart).toMutableList()
                dataSet.valueTextColor = Color.WHITE
                dataSet.setAutomaticallyDisableSliceSpacing(true)
                dataSet.valueTextSize = 16f
                dataSet.sliceSpace = 2f
                pieCharSetting(
                    dataSet,
                    "%.2f".format(categoryResult + loansResult + subsResult + goalsResult)
                        .replace(",", ".")
                )
            } else showNoTransaction()
        }
    }

    private fun showCategoryData(){
        clearFields()
        val categoryMap = HashMap<String, Double>()
        financeViewModel.historyLiveData.value?.let {historyList->
            historyList.forEach { historyItem->
                if (historyItem.date.isNotEmpty()) {
                    setHistoryDate(
                        year = historyItem.date.split(".")[2].toInt(),
                        month = historyItem.date.split(".")[1].toInt()-1,
                        day = historyItem.date.split(".")[0].toInt())

                    if(historyItem.isCategory == true  &&
                        historyDate.timeInMillis>=startDate.timeInMillis && historyDate.timeInMillis<=endDate.timeInMillis){
                        categoryResult = abs(changeCurrencyAmount(
                            oldCurrency = baseCurrency,
                            newCurrency = baseCurrency,
                            newAmount = historyItem.baseAmount,
                            context = requireContext()
                        ).toDouble())

                        if (categoryMap.containsKey(historyItem.placeId)) {
                            categoryMap[historyItem.placeId] =
                                (categoryMap[historyItem.placeId] ?: 0.0) + categoryResult
                        } else {
                            categoryMap[historyItem.placeId] = categoryResult
                        }
                    }
                }
            }

            categoryMap.forEach{
                financeViewModel.categoryBeginLiveData.value?.find {category-> category.key ==  it.key}?.categoryBegin?.name?.let {name->
                    entries.add(PieEntry(it.value.toFloat(),name))
                }
            }
        }

        if (entries.isNotEmpty()){
            val dataSet = PieDataSet(entries, "")
            dataSet.colors =  resources.getIntArray(R.array.color_chart).toMutableList()
            dataSet.valueTextColor  = Color.WHITE
            dataSet.setAutomaticallyDisableSliceSpacing(true)
            dataSet.valueTextSize = 16f
            dataSet.sliceSpace = 2f
            pieCharSetting(dataSet, "%.2f".format(categoryMap.values.sum()).replace(",", "."))
        } else showNoTransaction()
    }

    private fun showGoalsData(){
        clearFields()
        val goalMap = HashMap<String, Double>()
        financeViewModel.historyLiveData.value?.let {historyList->
            historyList.forEach { historyItem->
                if(historyItem.date.isNotEmpty()){
                    setHistoryDate(
                        year = historyItem.date.split(".")[2].toInt(),
                        month = historyItem.date.split(".")[1].toInt()-1,
                        day = historyItem.date.split(".")[0].toInt())
                    if(historyItem.isGoal == true && historyDate.timeInMillis>=startDate.timeInMillis && historyDate.timeInMillis<=endDate.timeInMillis){
                        financeViewModel.goalsData.value?.find { it.key == historyItem.placeId }?.goalItem?.currency?.let {
                            goalsResult = changeCurrencyAmount(
                                oldCurrency = it,
                                newCurrency = baseCurrency,
                                newAmount = historyItem.baseAmount,
                                context = requireContext()
                            ).toDouble()

                            if (goalMap.containsKey(historyItem.placeId)) {
                                goalMap[historyItem.placeId] =
                                    (goalMap[historyItem.placeId] ?: 0.0) + goalsResult
                            } else {
                                goalMap[historyItem.placeId] = goalsResult
                            }
                        }
                    }
                }
            }
        }

        goalMap.forEach{
            financeViewModel.goalsData.value?.find {goal-> goal.key ==  it.key}?.goalItem?.name?.let {name->
                entries.add(PieEntry(it.value.toFloat(),name))
            }
        }
        if(entries.isNotEmpty() && goalMap.values.sum()>0.0) {
            val dataSet = PieDataSet(entries, "")
            dataSet.colors =  resources.getIntArray(R.array.color_chart).toMutableList()
            dataSet.valueTextColor = Color.WHITE
            dataSet.setAutomaticallyDisableSliceSpacing(true)
            dataSet.valueTextSize = 16f
            dataSet.sliceSpace = 2f
            pieCharSetting(dataSet, "%.2f".format(goalMap.values.sum()).replace(",", "."))
        } else showNoTransaction()
    }

    private fun showLoansData(){
        clearFields()
        val loanMap = HashMap<String, Double>()
        financeViewModel.historyLiveData.value?.let {historyList->
            historyList.forEach { historyItem->
                if (historyItem.date.isNotEmpty()) {
                    setHistoryDate(
                        year = historyItem.date.split(".")[2].toInt(),
                        month = historyItem.date.split(".")[1].toInt() - 1,
                        day = historyItem.date.split(".")[0].toInt()
                    )
                    if (historyItem.isLoan == true && historyDate.timeInMillis >= startDate.timeInMillis && historyDate.timeInMillis <= endDate.timeInMillis) {
                        financeViewModel.loansLiveData.value?.find { it.key == historyItem.placeId }?.loanItem?.currency?.let {
                            goalsResult = abs(
                                changeCurrencyAmount(
                                    oldCurrency = it,
                                    newCurrency = baseCurrency,
                                    newAmount = historyItem.baseAmount,
                                    context = requireContext()
                                ).toDouble()
                            )

                            if (loanMap.containsKey(historyItem.placeId)) {
                                loanMap[historyItem.placeId] =
                                    (loanMap[historyItem.placeId] ?: 0.0) + goalsResult
                            } else {
                                loanMap[historyItem.placeId] = goalsResult
                            }
                        }
                    }
                }
            }
        }
        loanMap.forEach{
            financeViewModel.loansLiveData.value?.find {loan-> loan.key ==  it.key}?.loanItem?.name?.let {name->
                entries.add(PieEntry(it.value.toFloat(),name))
            }
        }
        if(entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, "")
            dataSet.colors =  resources.getIntArray(R.array.color_chart).toMutableList()
            dataSet.valueTextColor = Color.WHITE
            dataSet.setAutomaticallyDisableSliceSpacing(true)
            dataSet.valueTextSize = 16f
            dataSet.sliceSpace = 2f
            pieCharSetting(dataSet, "%.2f".format(loanMap.values.sum()).replace(",", "."))
        } else showNoTransaction()
    }

    private fun showSubsData(){
        clearFields()
        val subMap = HashMap<String, Double>()
        financeViewModel.historyLiveData.value?.let {historyList->
            historyList.forEach { historyItem->
                if (historyItem.date.isNotEmpty()){
                    setHistoryDate(
                        year = historyItem.date.split(".")[2].toInt(),
                        month = historyItem.date.split(".")[1].toInt()-1,
                        day = historyItem.date.split(".")[0].toInt())
                    if(historyItem.isSub == true && historyDate.timeInMillis>=startDate.timeInMillis && historyDate.timeInMillis<=endDate.timeInMillis){
                        financeViewModel.budgetLiveData.value?.find { it.key == historyItem.budgetId }?.budgetItem?.currency?.let {
                            subsResult = abs(changeCurrencyAmount(
                                oldCurrency = it,
                                newCurrency = baseCurrency,
                                newAmount = historyItem.baseAmount,
                                context = requireContext()
                            ).toDouble())

                            if (subMap.containsKey(historyItem.placeId)) {
                                subMap[historyItem.placeId] =
                                    (subMap[historyItem.placeId] ?: 0.0) + subsResult
                            } else {
                                subMap[historyItem.placeId] = subsResult
                            }
                        }
                    }
                }
            }
        }

        subMap.forEach{
            financeViewModel.subLiveData.value?.find {sub-> sub.key ==  it.key}?.subItem?.name?.let {name->
                entries.add(PieEntry(it.value.toFloat(),name))
            }
        }
        if(entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, "")
            dataSet.colors =  resources.getIntArray(R.array.color_chart).toMutableList()
            dataSet.valueTextColor = Color.WHITE
            dataSet.setAutomaticallyDisableSliceSpacing(true)
            dataSet.valueTextSize = 16f
            dataSet.sliceSpace = 2f
            pieCharSetting(dataSet, "%.2f".format(subMap.values.sum()).replace(",", "."))
        } else showNoTransaction()
    }

    private fun showBudgetDataIncomes(){
        clearFields()
        var budgetId:String
        val budgetMap = HashMap<String, Double>()
        financeViewModel.historyLiveData.value?.let {historyList->
            historyList.forEach { historyItem->
                if (historyItem.date.isNotEmpty()){
                    setHistoryDate(
                        year = historyItem.date.split(".")[2].toInt(),
                        month = historyItem.date.split(".")[1].toInt()-1,
                        day = historyItem.date.split(".")[0].toInt())

                    if((historyItem.placeId == "" || historyItem.isTransfer == true || historyItem.isGoal == true && historyItem.amount.toDouble()<0.0)
                        && historyDate.timeInMillis>=startDate.timeInMillis && historyDate.timeInMillis<=endDate.timeInMillis &&
                        historyDate.timeInMillis<=Calendar.getInstance().timeInMillis){
                        budgetId = if (historyItem.isGoal==true) historyItem.budgetId else historyItem.placeId.ifEmpty { historyItem.budgetId }
                        financeViewModel.budgetLiveData.value?.find { it.key == budgetId}?.budgetItem?.currency?.let {
                            budgetResult = abs(changeCurrencyAmount(
                                oldCurrency = it,
                                newCurrency = baseCurrency,
                                newAmount = historyItem.amount,
                                context = requireContext()
                            ).toDouble())

                            if (budgetMap.containsKey(budgetId)) {
                                budgetMap[budgetId] =
                                    (budgetMap[budgetId] ?: 0.0) + budgetResult
                            } else {
                                budgetMap[budgetId] = budgetResult
                            }
                        }
                    }
                }
            }
        }

        budgetMap.forEach{
            financeViewModel.budgetLiveData.value?.find {category-> category.key ==  it.key}?.budgetItem?.name?.let {name->
                entries.add(PieEntry(it.value.toFloat(),name))
            }
        }
        if(entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, "")
            dataSet.colors =  resources.getIntArray(R.array.color_chart).toMutableList()
            dataSet.valueTextColor = Color.WHITE
            dataSet.setAutomaticallyDisableSliceSpacing(true)
            dataSet.sliceSpace = 2f
            dataSet.valueTextSize = 16f
            pieCharSetting(dataSet, "%.2f".format(budgetMap.values.sum()).replace(",", "."))
        } else showNoTransaction()
    }

    private fun showBudgetDataExpences(){
        clearFields()
        val budgetMap = HashMap<String, Double>()
        financeViewModel.historyLiveData.value?.let {historyList->
            historyList.forEach { historyItem->
                if (historyItem.date.isNotEmpty()){
                    setHistoryDate(
                        year = historyItem.date.split(".")[2].toInt(),
                        month = historyItem.date.split(".")[1].toInt()-1,
                        day = historyItem.date.split(".")[0].toInt())
                    if(historyItem.placeId != "" && !(historyItem.isGoal == true && historyItem.amount.toDouble()<0.0)
                        && historyDate.timeInMillis>=startDate.timeInMillis && historyDate.timeInMillis<=endDate.timeInMillis &&
                        historyDate.timeInMillis<=Calendar.getInstance().timeInMillis){
                        financeViewModel.budgetLiveData.value?.find { it.key == historyItem.budgetId}?.budgetItem?.currency?.let {
                            budgetResult = abs(changeCurrencyAmount(
                                oldCurrency = it,
                                newCurrency = baseCurrency,
                                newAmount = historyItem.amount,
                                context = requireContext()
                            ).toDouble())

                            if (budgetMap.containsKey(historyItem.budgetId )) {
                                budgetMap[historyItem.budgetId] =
                                    (budgetMap[historyItem.budgetId] ?: 0.0) + budgetResult
                            } else {
                                budgetMap[historyItem.budgetId] = budgetResult
                            }
                        }
                    }
                }
            }
        }

        budgetMap.forEach{
            financeViewModel.budgetLiveData.value?.find {category-> category.key ==  it.key}?.budgetItem?.name?.let {name->
                entries.add(PieEntry(it.value.toFloat(),name))
            }
        }
        if(entries.isNotEmpty()) {
            val dataSet = PieDataSet(entries, "")
            dataSet.colors =  resources.getIntArray(R.array.color_chart).toMutableList()
            dataSet.valueTextColor = Color.WHITE
            dataSet.setAutomaticallyDisableSliceSpacing(true)
            dataSet.valueTextSize = 16f
            dataSet.sliceSpace = 2f
            pieCharSetting(dataSet, "%.2f".format(budgetMap.values.sum()).replace(",", "."))
        } else showNoTransaction()
    }

    private fun pieCharSetting(dataSet: PieDataSet, centerText:String){
        showPieChart()
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "%.2f".format(value).replace(",", ".")
            }
        }
        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.centerText = "$centerText ${requireContext().resources.getString(
            requireContext().resources.getIdentifier(
                baseCurrency,
                "string",
                requireContext().packageName
            )
        )}"
        pieChart.setCenterTextSize(16f)
        pieChart.minAngleForSlices = 50f
        pieChart.holeRadius = 40f
        pieChart.setHoleColor(resources.getColor(R.color.light, requireActivity().theme))

        pieChart.legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.VERTICAL
            setDrawInside(false)
            xEntrySpace = 4f
            yEntrySpace = 0f
            isWordWrapEnabled = true
            textSize = 14f
        }
        pieChart.animate()
    }

    private fun clearFields(){
        entries.clear()
        budgetResult = 0.0
        goalsResult = 0.0
        loansResult = 0.0
        subsResult = 0.0
        categoryResult = 0.0
        pieChart.clear()
    }


    private fun changeCurrencyAmount(oldCurrency: String, newCurrency: String,newAmount:String, context: Context):String{
        currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
        if(currencyConvertor!=null && oldCurrency!=newCurrency){
            return when (oldCurrency){
                currencyConvertor!!.baseCode->{
                    "%.2f".format(newAmount.toDouble()* currencyConvertor!!.conversionRates[newCurrency]!!).replace(',','.')
                }
                else->{
                    "%.2f".format( newAmount.toDouble()* currencyConvertor!!.conversionRates[newCurrency]!!/ currencyConvertor!!.conversionRates[oldCurrency]!!).replace(',','.')
                }
            }
        }
        return newAmount
    }

    private fun showDateRangePicker(){
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(resources.getString(R.string.choose_range))
            .build()
        dateRangePicker.show(requireActivity().supportFragmentManager,"date_range_picker")

        dateRangePicker.addOnPositiveButtonClickListener { datePicker ->
           dateRangeAnalysis.text = "${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(datePicker.first)}-${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(datePicker.second)}"
            startDate = Calendar.getInstance().apply { timeInMillis = datePicker.first }
            endDate = Calendar.getInstance().apply {  timeInMillis = datePicker.second}
            updateData(tabs.selectedTabPosition)
        }
    }

    private fun showNoTransaction(){
        noTransactionImage.visibility = View.VISIBLE
        noTransactionTextView.visibility = View.VISIBLE
        pieChart.visibility = View.GONE
    }

    private fun showPieChart(){
        noTransactionImage.visibility = View.GONE
        noTransactionTextView.visibility = View.GONE
        pieChart.visibility = View.VISIBLE
    }
}