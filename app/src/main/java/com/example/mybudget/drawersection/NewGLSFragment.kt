package com.example.mybudget.drawersection

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.ExchangeRateResponse
import com.example.mybudget.BudgetNotificationManager
import com.example.mybudget.R
import com.example.mybudget.databinding.PageNewGlsBinding
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.history.HistoryItem
import com.example.mybudget.drawersection.finance.IconsChooserAlertDialog
import com.example.mybudget.drawersection.finance.SharedViewModel
import com.example.mybudget.drawersection.goals.GoalItem
import com.example.mybudget.drawersection.goals.GoalItemWithKey
import com.example.mybudget.drawersection.loans.LoanItem
import com.example.mybudget.drawersection.loans.LoanItemWithKey
import com.example.mybudget.drawersection.subs.SubItem
import com.example.mybudget.drawersection.subs.SubItemWithKey
import com.example.mybudget.start_pages.Constants
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale


class NewGLSFragment : Fragment() {
    private lateinit var binding: PageNewGlsBinding
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var glsViewModel: GLSViewModel
    private lateinit var periodList: Array<String>
    private lateinit var adapterPeriod: ArrayAdapter<String>
    private lateinit var budgetAdapter: ArrayAdapter<String>
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private var currencyConvertor: ExchangeRateResponse? = null
    private var selection: Triple<String, String, String> = Triple("","", "")
    private var oldCurrency: String? = null
    private var periodLS: String? = null

    private var key: String? = null
    private var type: String? = null
    private var dateOfEnd: Calendar = Calendar.getInstance().also { it.set(Calendar.DAY_OF_MONTH, it.get(Calendar.DAY_OF_MONTH)+1) }
    private var dateLS: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_new_gls, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = PageNewGlsBinding.bind(view)

        activity?.let {
            financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java]
            glsViewModel = ViewModelProvider(it)[GLSViewModel::class.java]
        }
        key = requireArguments().getString("key")
        type =  requireArguments().getString("type")

        binding.nameGLSEdit.doAfterTextChanged {
            when (type) {
                "goal"->checkAllFieldsGoal()
                "loan"->checkAllFieldsLoan()
                "sub"->checkAllFieldsSub()
            }
        }

        binding.glsValue.doAfterTextChanged {
            when (type) {
                "goal"->checkAllFieldsGoal()
                "loan"->checkAllFieldsLoan()
                "sub"->checkAllFieldsSub()
            }
        }

        binding.imageOfGLM.setOnClickListener {
            IconsChooserAlertDialog(requireContext()){ path->
                binding.imageOfGLM.setImageDrawable(ContextCompat.getDrawable(requireContext(), requireContext().resources.getIdentifier(path, "drawable", requireContext().packageName)))
                binding.imageOfGLM.tag = path
                when (type) {
                    "goal"->checkAllFieldsGoal()
                    "loan"->checkAllFieldsLoan()
                    "sub"->checkAllFieldsSub()
                }
            }
        }

        binding.timeOfNotificationsGLS.setOnClickListener {
            it as TextView
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                it.text = formattedTime
            }, hour, minute, true)
            timePickerDialog.show()
        }

        when(key){
            null->{
                binding.currencyGLS.text = requireContext().resources.getString(requireContext().resources.getIdentifier(financeViewModel.budgetLiveData.value?.find { it.key == "Base budget" }!!.budgetItem.currency, "string", requireContext().packageName))
                binding.buttonAddGLS.text = requireContext().resources.getString(R.string.add)
                when (type) {
                    "goal"->{
                        initPeriod()
                        initCalendar()
                        initGoal()
                        setOnSelectionChanged()
                        binding.withouthDate.isChecked = true
                        binding.buttonAddGLS.setOnClickListener { saveGoal() }
                    }
                    "loan"->{
                        initLoan()
                        initPeriodLS()
                        initCalendarSimple()
                        binding.calendarViewGLS.minDate = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH,  Calendar.getInstance().get(Calendar.DAY_OF_MONTH)+1) }.timeInMillis
                        binding.buttonAddGLS.setOnClickListener { saveLoan() }
                    }
                    "sub"->{
                        initSub()
                        initPeriodLS()
                        initCalendarSimple()
                        binding.buttonAddGLS.setOnClickListener { saveSub() }
                        financeViewModel.budgetLiveData.observe(viewLifecycleOwner){
                            budgetAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
                                it.filter { budget->!budget.budgetItem.isDeleted && budget.budgetItem.type != resources.getStringArray(R.array.budget_types)[0] }.map { budget -> budget.budgetItem.name })
                                .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)}
                            binding.spinnerBudgetGLS.adapter = budgetAdapter
                        }
                    }
                }
            }
            else->{
                binding.buttonAddGLS.apply {
                    text = requireContext().resources.getString(R.string.save)
                    isEnabled = true
                }
                when (type) {
                    "goal"->{
                        initPeriod()
                        initCalendar()
                        initGoal()
                        setOnSelectionChanged()
                        fillAllGoal(financeViewModel.goalsData.value!!.find { it.key == key }!!)
                        val sharedPreferences = requireContext().getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
                        val periodBegin = sharedPreferences.getString(key, "|")?.split("|")?.get(0)?:periodList[0]
                        val timeBegin = sharedPreferences.getString(key, "|")?.split("|")?.get(1)?:"12:00"
                        val day = dateOfEnd
                        financeViewModel.goalsData.value!!.find { it.key == key }!!.goalItem.date?.split(".")?.let {
                            day.apply {
                                    val dateEnd = LocalDate.of(it[2].toInt(), it[1].toInt()+1, it[0].toInt())
                                    val dateNow = LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                                    val daysBetween = ChronoUnit.DAYS.between(dateNow, dateEnd)
                                    val resultList = mutableListOf<String>()
                                    if(dateOfEnd>Calendar.getInstance()){
                                        when {
                                            daysBetween>=365 -> resultList.addAll(periodList)
                                            daysBetween>=30 -> {
                                                for (i in 0 until 7) {
                                                    resultList.add(periodList[i])
                                                }
                                            }
                                            daysBetween<30 ->{
                                                if (daysBetween>7){
                                                    for (i in 0 until 6){
                                                        resultList.add(periodList[i])
                                                    }
                                                } else if (daysBetween.toInt() ==7){
                                                    for (i in 0 until 5){
                                                        resultList.add(periodList[i])
                                                    }

                                                }else if (daysBetween>=3){
                                                    for (i in 0 until 3){
                                                        resultList.add(periodList[i])
                                                    }
                                                    resultList.add(periodList[4])
                                                }
                                                else if (daysBetween>=1) {
                                                    resultList.add(periodList[0])
                                                    resultList.add(periodList[1])
                                                    resultList.add(periodList[4])
                                                }
                                                else{
                                                    resultList.add(periodList[0])
                                                }
                                            }
                                            else ->  resultList.add(periodList[0])
                                        }
                                    }

                                    if (periodBegin.isNotEmpty()){
                                        if (resultList.indexOf(periodBegin) == -1) resultList.add(periodBegin)
                                        when(periodList.indexOf(periodBegin)){
                                            0 -> {
                                                binding.timeTitleGLS.visibility = View.GONE
                                                binding.timeOfNotificationsGLS.visibility = View.GONE
                                            }
                                            else -> {
                                                binding.timeTitleGLS.visibility = View.VISIBLE
                                                binding.timeOfNotificationsGLS.visibility = View.VISIBLE
                                            }
                                        }
                                        binding.timeOfNotificationsGLS.text = timeBegin.ifEmpty { "12:00" }
                                    }

                                    adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resultList)
                                    adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    binding.periodOfNotificationGLS.adapter = adapterPeriod
                                    binding.periodOfNotificationGLS.setSelection(resultList.indexOf(periodBegin.ifEmpty { periodList[0] }))

                                set(Calendar.YEAR, it[2].toInt())
                                set(Calendar.MONTH, it[1].toInt()-1)
                                set(Calendar.DAY_OF_MONTH, it[0].toInt())
                            }

                            binding.periodOfNotificationGLS.onItemSelectedListener = object : OnItemSelectedListener {
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    if(id!=0L){
                                        binding.timeOfNotificationsGLS.visibility = View.VISIBLE
                                        binding.timeTitleGLS.visibility = View.VISIBLE
                                    } else {
                                        binding.timeOfNotificationsGLS.visibility = View.GONE
                                        binding.timeTitleGLS.visibility = View.GONE
                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    binding.timeOfNotificationsGLS.visibility = View.GONE
                                    binding.timeTitleGLS.visibility = View.GONE
                                }
                            }
                        }

                        if(financeViewModel.goalsData.value!!.find { it.key == key }!!.goalItem.date == null){
                            binding.calendarViewGLS.visibility = View.GONE
                            val resultList = listOf(periodList[0], periodList[4], periodList[5], periodList[6], periodList[7])
                            adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resultList)
                            adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            binding.periodOfNotificationGLS.adapter = adapterPeriod
                            if (periodBegin.isNotEmpty()){
                                when(resultList.indexOf(periodBegin)){
                                    0 -> {
                                        binding.timeTitleGLS.visibility = View.GONE
                                        binding.timeOfNotificationsGLS.visibility = View.GONE
                                    }
                                    else -> {
                                        binding.timeTitleGLS.visibility = View.VISIBLE
                                        binding.timeOfNotificationsGLS.visibility = View.VISIBLE
                                    }
                                }
                                binding.periodOfNotificationGLS.setSelection(resultList.indexOf(periodBegin))
                                binding.timeOfNotificationsGLS.text = timeBegin.ifEmpty { "12:00" }
                            }
                        }

                        binding.calendarViewGLS.date = day.timeInMillis
                        dateOfEnd = day
                        binding.buttonAddGLS.setOnClickListener { updateGoal(requireContext()) }
                    }
                    "loan"->{
                        initLoan()
                        initPeriodLS()
                        binding.calendarViewGLS.minDate = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH,  Calendar.getInstance().get(Calendar.DAY_OF_MONTH)+1) }.timeInMillis
                        initCalendarSimple()
                        financeViewModel.loansLiveData.value?.find { it.key == key }?.let { loanItemWithKey ->
                            val sharedPreferences = requireContext().getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
                            val periodBegin = sharedPreferences.getString(key, "|")?.split("|")?.get(0)?:periodList[0]
                            val timeBegin = sharedPreferences.getString(key, "|")?.split("|")?.get(1)?:"12:00"

                            periodLS = loanItemWithKey.loanItem.period

                            periodLS?.let {
                                billingResult(
                                   it.split(" ")[0].toInt(), when( it.split(" ")[1]){
                                        "d"->0
                                        "w"->1
                                        "y"->3
                                        else->2}
                                )
                            }

                            dateLS = Calendar.getInstance().apply {
                                set(Calendar.YEAR, (loanItemWithKey.loanItem.dateNext?: loanItemWithKey.loanItem.dateOfEnd).split(".")[2].toInt())
                                set(Calendar.MONTH, (loanItemWithKey.loanItem.dateNext?: loanItemWithKey.loanItem.dateOfEnd).split(".")[1].toInt()-1)
                                set(Calendar.DAY_OF_MONTH, (loanItemWithKey.loanItem.dateNext?: loanItemWithKey.loanItem.dateOfEnd).split(".")[0].toInt())
                            }

                            oldCurrency = loanItemWithKey.loanItem.currency
                            binding.calendarViewGLS.date = dateLS.timeInMillis
                            fillAllLoan(loanItemWithKey)
                            binding.periodOfNotificationGLS.setSelection(periodList.indexOf(periodBegin.ifEmpty { periodList[0] }))
                            binding.timeOfNotificationsGLS.text = timeBegin.ifEmpty { "12:00" }
                            binding.buttonAddGLS.setOnClickListener { updateLoan(requireContext(), loanItemWithKey.loanItem) }
                        }
                    }
                    "sub"->{
                        initSub()
                        initPeriodLS()
                        initCalendarSimple()
                        var set = false
                        val subItem = financeViewModel.subLiveData.value?.find { it.key == key }
                        if(subItem!=null){
                            val sharedPreferences = requireContext().getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
                            val periodBegin = sharedPreferences.getString(key, "|")?.split("|")?.get(0)?:periodList[0]
                            val timeBegin = sharedPreferences.getString(key, "|")?.split("|")?.get(1)?:"12:00"
                            periodLS = subItem.subItem.period
                            billingResult(
                                subItem.subItem.period.split(" ")[0].toInt(), when(subItem.subItem.period.split(" ")[1]){
                                "d"->0
                                "w"->1
                                "y"->3
                                 else->2}
                            )
                            dateLS = Calendar.getInstance().apply {
                                set(Calendar.YEAR, subItem.subItem.date.split(".")[2].toInt())
                                set(Calendar.MONTH, subItem.subItem.date.split(".")[1].toInt()-1)
                                set(Calendar.DAY_OF_MONTH, subItem.subItem.date.split(".")[0].toInt())
                            }
                            oldCurrency = financeViewModel.budgetLiveData.value?.find { it.key == subItem.subItem.budgetId }?.budgetItem?.currency

                            binding.calendarViewGLS.date = dateLS.timeInMillis
                            financeViewModel.budgetLiveData.observe(viewLifecycleOwner){
                                budgetAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
                                    it.filter { budget->!budget.budgetItem.isDeleted && budget.budgetItem.type != resources.getStringArray(R.array.budget_types)[0] }.map { budget -> budget.budgetItem.name })
                                    .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)}
                                binding.spinnerBudgetGLS.adapter = budgetAdapter
                                if (!set){
                                    binding.spinnerBudgetGLS.setSelection(budgetAdapter.getPosition(
                                        financeViewModel.budgetLiveData.value?.find { budget -> budget.key == subItem.subItem.budgetId }?.budgetItem?.name
                                    ))
                                    set = true
                                }
                            }
                            fillAllSub(subItem)
                            binding.periodOfNotificationGLS.setSelection(periodList.indexOf(periodBegin.ifEmpty { periodList[0] }))
                            binding.timeOfNotificationsGLS.text = timeBegin.ifEmpty { "12:00" }
                            binding.buttonAddGLS.setOnClickListener { updateSub(requireContext(), subItem) }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth = Firebase.auth
        table = Firebase.database.reference
        currencyConvertor = ExchangeRateManager.getExchangeRateResponse(requireContext())
        val sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        when (type){
            "goal", "loan"->{
                oldCurrency =
                    if (type == "goal") financeViewModel.goalsData.value?.find { it.key == key }?.goalItem?.currency
                    else financeViewModel.loansLiveData.value?.find { it.key == key }?.loanItem?.currency

                sharedViewModel.dataToPass.value = null
                sharedViewModel.dataToPass.observe(this) { data ->
                    if (data!=null && data.first.isNotEmpty()){
                        binding.currencyGLS.text = data.third
                        selection = data
                        if (key!=null){
                            binding.glsValue.setText(changeCurrencyAmount(
                                oldCurrency = oldCurrency!!,
                                newCurrency = data.first,
                                newAmount = binding.glsValue.text.toString(),
                                context = requireContext())
                            )
                        }
                        sharedViewModel.dataToPass.value = Triple("","","")
                        oldCurrency = selection.first
                    }
                }

                binding.currencyGLS.setOnClickListener {
                    findNavController().navigate(R.id.action_newGLSFragment_to_currencyDialogFragment)
                }
            }
        }
    }

    //Интерфейс
    private fun initGoal(){
        binding.calendarViewGLS.minDate = Calendar.getInstance().also { it.set(Calendar.DAY_OF_MONTH, it.get(Calendar.DAY_OF_MONTH)+1) }.timeInMillis
        binding.radioGroupGLS.visibility = View.VISIBLE
        binding.calendarViewGLS.visibility = View.GONE
        binding.budgetGLS.visibility = View.GONE
        binding.spinnerBudgetGLS.visibility = View.GONE
        binding.glsValue.hint = requireContext().resources.getString(R.string.titleGoals)
        binding.timeTitleGLS.visibility = View.GONE
        binding.timeOfNotificationsGLS.visibility = View.GONE
        binding.billingPeriodGLS.visibility = View.GONE
        binding.billingPeriodTitleGLS.visibility = View.GONE
        binding.periodOfLoan.visibility = View.GONE
    }

    private fun initSub(){

        if(financeViewModel.budgetLiveData.value?.filter { it.budgetItem.type == resources.getStringArray(R.array.budget_types)[1] }.isNullOrEmpty()){
            Toast.makeText(context, resources.getString(R.string.error_sub_budget_notexist), Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }

        binding.radioGroupGLS.visibility = View.GONE
        binding.calendarViewGLS.visibility = View.VISIBLE
        binding.budgetGLS.visibility = View.VISIBLE
        binding.spinnerBudgetGLS.visibility = View.VISIBLE
        binding.glsValue.hint = requireContext().resources.getString(R.string.titleSubs)
        binding.timeTitleGLS.visibility = View.GONE
        binding.timeOfNotificationsGLS.visibility = View.GONE
        binding.nameGLS.text = requireContext().resources.getString(R.string.nameSubs)
        binding.glsDate.text = requireContext().resources.getString(R.string.dateSubs)
        binding.periodOfLoan.visibility = View.GONE

        binding.spinnerBudgetGLS.onItemSelectedListener = object : OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val budgetItem = financeViewModel.budgetLiveData.value?.find { it.budgetItem.name == binding.spinnerBudgetGLS.selectedItem.toString() }!!.budgetItem
                binding.currencyGLS.text = requireContext().resources.getString(requireContext().resources.getIdentifier(budgetItem.currency, "string", requireContext().packageName))
                if (oldCurrency!=budgetItem.currency && binding.glsValue.text.isNotEmpty()){
                    binding.glsValue.setText(changeCurrencyAmount(oldCurrency?:budgetItem.currency, budgetItem.currency, binding.glsValue.text.toString(), requireContext()))
                    oldCurrency = budgetItem.currency
                }
                checkAllFieldsSub()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        binding.billingPeriodGLS.setOnClickListener {
            openBillingDialog()
            checkAllFieldsSub()
        }
    }

    private fun initLoan(){
        binding.radioGroupGLS.visibility = View.VISIBLE
        binding.calendarViewGLS.visibility = View.VISIBLE
        binding.periodOfLoan.visibility = View.VISIBLE
        binding.budgetGLS.visibility = View.GONE
        binding.periodOfLoan.visibility = View.GONE
        binding.spinnerBudgetGLS.visibility = View.GONE
        binding.timeTitleGLS.visibility = View.GONE
        binding.timeOfNotificationsGLS.visibility = View.GONE
        binding.billingPeriodTitleGLS.visibility = View.GONE
        binding.billingPeriodGLS.visibility = View.GONE
        binding.nameGLS.text = requireContext().resources.getString(R.string.nameLoans)
        binding.glsDate.text = requireContext().resources.getString(R.string.dateLoans)
        binding.withouthDate.text = requireContext().resources.getString(R.string.onceLoans)
        binding.withDate.text = requireContext().resources.getString(R.string.regularLoans)

        binding.billingPeriodGLS.setOnClickListener {
            openBillingDialog()
            checkAllFieldsLoan()
        }

        binding.radioGroupGLS.setOnCheckedChangeListener { _, _ ->
            when{
                binding.withDate.isChecked-> {
                    binding.billingPeriodGLS.visibility = View.VISIBLE
                    binding.billingPeriodTitleGLS.visibility = View.VISIBLE
                    binding.calendarViewGLS.visibility = View.GONE
                    binding.periodOfLoan.visibility = View.VISIBLE}
                else -> {
                    binding.billingPeriodGLS.visibility = View.GONE
                    binding.calendarViewGLS.visibility = View.VISIBLE
                    binding.billingPeriodTitleGLS.visibility = View.GONE
                    binding.periodOfLoan.visibility = View.GONE}
            }
            checkAllFieldsLoan()
        }

        binding.periodOfLoan.setOnClickListener { showDateRangePicker() }
    }

    private fun initPeriod(){
        periodList = requireContext().resources.getStringArray(R.array.periodicity)
        adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf(periodList[0], periodList[4], periodList[5], periodList[6], periodList[7]))
        adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.periodOfNotificationGLS.adapter = adapterPeriod
        binding.periodOfNotificationGLS.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if(position == 0){
                    binding.timeTitleGLS.visibility = View.INVISIBLE
                    binding.timeOfNotificationsGLS.visibility = View.INVISIBLE
                } else {
                    binding.timeTitleGLS.visibility = View.VISIBLE
                    binding.timeOfNotificationsGLS.visibility = View.VISIBLE
                }
                checkAllFieldsGoal()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                binding.timeTitleGLS.visibility = View.INVISIBLE
                binding.timeOfNotificationsGLS.visibility = View.INVISIBLE
            }
        }
    }

    private fun initPeriodLS(){
        periodList = requireContext().resources.getStringArray(R.array.periodicity)
        adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf(periodList[0], periodList[1], periodList[2], periodList[3]))
        adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.periodOfNotificationGLS.adapter = adapterPeriod
        binding.periodOfNotificationGLS.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if(position == 0){
                    binding.timeTitleGLS.visibility = View.INVISIBLE
                    binding.timeOfNotificationsGLS.visibility = View.INVISIBLE
                } else {
                    binding.timeTitleGLS.visibility = View.VISIBLE
                    binding.timeOfNotificationsGLS.visibility = View.VISIBLE
                }
                when(type){
                    "sub"-> checkAllFieldsSub()
                    "loan" -> checkAllFieldsLoan()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                binding.timeTitleGLS.visibility = View.INVISIBLE
                binding.timeOfNotificationsGLS.visibility = View.INVISIBLE
            }
        }
    }

    private fun fillAllGoal(goalItemWithKey: GoalItemWithKey){
        binding.nameGLSEdit.setText(goalItemWithKey.goalItem.name)
        binding.currencyGLS.text = requireContext().resources.getString(requireContext().resources.getIdentifier(goalItemWithKey.goalItem.currency, "string", requireContext().packageName))
        binding.glsValue.setText(goalItemWithKey.goalItem.target)
        binding.imageOfGLM.setImageDrawable(ContextCompat.getDrawable(requireContext(), requireContext().resources.getIdentifier(goalItemWithKey.goalItem.path, "drawable", requireContext().packageName)))
        binding.imageOfGLM.tag = goalItemWithKey.goalItem.path
        when (goalItemWithKey.goalItem.date){
            null-> binding.withouthDate.isChecked = true
            else -> {
                binding.withDate.isChecked = true
            }
        }
    }

    private fun fillAllSub(subItemWithKey: SubItemWithKey){
        binding.nameGLSEdit.setText(subItemWithKey.subItem.name)
        binding.glsValue.setText(subItemWithKey.subItem.amount)
        binding.imageOfGLM.setImageDrawable(ContextCompat.getDrawable(requireContext(), requireContext().resources.getIdentifier(subItemWithKey.subItem.path, "drawable", requireContext().packageName)))
        binding.imageOfGLM.tag = subItemWithKey.subItem.path
    }

    private fun fillAllLoan(loanItemWithKey: LoanItemWithKey){
        binding.nameGLSEdit.setText(loanItemWithKey.loanItem.name)
        binding.glsValue.setText(loanItemWithKey.loanItem.amount)
        binding.currencyGLS.text = requireContext().resources.getString(requireContext().resources.getIdentifier(loanItemWithKey.loanItem.currency, "string", requireContext().packageName))
        binding.imageOfGLM.setImageDrawable(ContextCompat.getDrawable(requireContext(), requireContext().resources.getIdentifier(loanItemWithKey.loanItem.path, "drawable", requireContext().packageName)))
        binding.imageOfGLM.tag = loanItemWithKey.loanItem.path
        when (loanItemWithKey.loanItem.period){
            null-> {
                binding.withouthDate.isChecked = true
            }
            else-> {
                binding.withDate.isChecked = true
                binding.periodOfLoan.text = "${loanItemWithKey.loanItem.dateNext}-${loanItemWithKey.loanItem.dateOfEnd}"
            }
        }
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

    //Обработчики
    private fun initCalendar(){
        binding.calendarViewGLS.setOnDateChangeListener { _, year, month, dayOfMonth ->
            dateOfEnd = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            val dateEnd = LocalDate.of(year, month+1, dayOfMonth)
            val dateNow = LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            val daysBetween = ChronoUnit.DAYS.between(dateNow, dateEnd)
            if(dateOfEnd>Calendar.getInstance()){
                val resultList = mutableListOf<String>()
                when {
                    daysBetween>=365 -> resultList.addAll(periodList)
                    daysBetween>=30 -> {
                        for (i in 0 until 7) {
                            resultList.add(periodList[i])
                        }
                    }
                    daysBetween<30 ->{
                        if (daysBetween>7){
                            for (i in 0 until 6){
                                resultList.add(periodList[i])
                            }
                        } else if (daysBetween.toInt() ==7){
                            for (i in 0 until 5){
                                resultList.add(periodList[i])
                            }

                        }else if (daysBetween>=3){
                            for (i in 0 until 3){
                                resultList.add(periodList[i])
                            }
                            resultList.add(periodList[4])
                        }
                        else if (daysBetween>=1) {
                            resultList.add(periodList[0])
                            resultList.add(periodList[1])
                            resultList.add(periodList[4])
                        }
                        else{
                            resultList.add(periodList[0])
                        }
                    }
                    else ->  resultList.add(periodList[0])
                }

                adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resultList)
                adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.periodOfNotificationGLS.adapter = adapterPeriod
                checkAllFieldsGoal()
            }
        }
    }

    private fun initCalendarSimple(){
        binding.calendarViewGLS.setOnDateChangeListener { _, year, month, dayOfMonth ->
            dateLS = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
                when (type){
                    "sub"->checkAllFieldsSub()
                    "loan"->checkAllFieldsLoan()
                }
            }
        }
    }

    private fun setOnSelectionChanged(){
        binding.radioGroupGLS.setOnCheckedChangeListener { _, _ ->
            if (binding.withouthDate.isChecked){
                binding.calendarViewGLS.visibility = View.GONE
                adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf(periodList[0], periodList[4], periodList[5], periodList[6], periodList[7]))
                adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.periodOfNotificationGLS.adapter = adapterPeriod
            } else if (binding.withDate.isChecked){
                binding.calendarViewGLS.visibility = View.VISIBLE
                val resultList = mutableListOf<String>()
                resultList.add(periodList[0])
                resultList.add(periodList[1])
                resultList.add(periodList[4])
                adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resultList)
                adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.periodOfNotificationGLS.adapter = adapterPeriod
            }
            when(type){
                "goal"->checkAllFieldsGoal()
            }
        }
    }

    private fun openBillingDialog(){
        val dialogView = View.inflate(requireContext(), R.layout.card_billing_period, null)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)

        val count = dialogView.findViewById<NumberPicker>(R.id.numberOf)
        val period = dialogView.findViewById<NumberPicker>(R.id.period)
        val periodBegin = requireContext().resources.getStringArray(R.array.periodicityBegin)

        count.minValue = 1
        count.maxValue = Int.MAX_VALUE
        count.wrapSelectorWheel = false

        period.minValue = 0
        period.maxValue = periodBegin.lastIndex
        period.displayedValues = periodBegin
        period.wrapSelectorWheel = false

        builder.setPositiveButton("Выбрать") {dialog, _ ->
            billingResult(count.value, period.value)
            periodLS = "${count.value} " + when (period.value){
                0-> "d"
                1-> "w"
                2-> "m"
                else->"y"
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
        dialog.show()
    }

    private fun billingResult(count:Int, period:Int){
        binding.billingPeriodGLS.text =
            when(count%10){
                1->{
                    if (period == 1){
                        requireContext().resources.getStringArray(R.array.periodicityBillingEvery)[2] + " " +
                                requireContext().resources.getString(R.string.periodicityBillingweek)
                    } else {
                        requireContext().resources.getStringArray(R.array.periodicityBillingEvery)[0] + " " +
                                requireContext().resources.getStringArray(R.array.periodicityBegin)[period].lowercase()
                    }
                }
                2,3,4->requireContext().resources.getStringArray(R.array.periodicityBillingEvery)[1] + " $count " +
                       requireContext().resources.getStringArray(R.array.periodicityBilling234)[period].lowercase()

                else ->requireContext().resources.getStringArray(R.array.periodicityBillingEvery)[1] + " $count " +
                        requireContext().resources.getStringArray(R.array.periodicityBillingOther)[period].lowercase()
            }
    }

    //FB
    private fun saveGoal(){

        if (financeViewModel.goalsData.value?.filter { !it.goalItem.isDeleted }?.all{it.goalItem.name !=  binding.nameGLSEdit.text.toString()}==false)
            Snackbar.make(binding.buttonAddGLS, resources.getString(R.string.error_goal_exists), Snackbar.LENGTH_LONG).show()
        else if (financeViewModel.goalsData.value?.filter { it.goalItem.isDeleted }?.all{it.goalItem.name !=  binding.nameGLSEdit.text.toString()}==false){
            AlertDialog.Builder(context)
                .setTitle(resources.getString(R.string.repair))
                .setMessage(resources.getString(R.string.error_goal_repair))
                .setPositiveButton(resources.getString(R.string.repair_agree)) { dialog, _ ->
                    glsViewModel.saveGoal(financeViewModel.goalsData.value!!.find { it.goalItem.name ==  binding.nameGLSEdit.text.toString()}!!.key)
                    findNavController().popBackStack()
                    dialog.dismiss()
                }
                .setNegativeButton(resources.getString(R.string.make_new)) { dialog, _ ->
                    makeNewGoal()
                    dialog.dismiss()
                }
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
        else makeNewGoal()
    }

    private fun updateGoal(context: Context){
        if (financeViewModel.goalsData.value?.filter { !it.goalItem.isDeleted  && it.key != key}?.all{it.goalItem.name !=  binding.nameGLSEdit.text.toString()}==false)
            Snackbar.make(binding.buttonAddGLS, resources.getString(R.string.error_goal_exists), Snackbar.LENGTH_LONG).show()
        else{
            val beginItem = financeViewModel.goalsData.value?.find { it.key == key }!!.goalItem
            glsViewModel.updateGoal(
                key = key!!,
                goalItem = GoalItem(
                    name = binding.nameGLSEdit.text.toString(),
                    target = "%.2f".format(binding.glsValue.text.toString().toDouble()).replace(",", "."),
                    current = changeCurrencyAmount(beginItem.currency,
                        selection.first.ifEmpty { beginItem.currency },
                        beginItem.current,
                        context),
                    currency = selection.first.ifEmpty { financeViewModel.goalsData.value?.find { it.key == key }!!.goalItem.currency },
                    date =  if (binding.withouthDate.isChecked) null else SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfEnd.time),
                    path = binding.imageOfGLM.tag.toString(),
                    isDeleted = false),
                context = context,
                periodOfNotificationPosition = binding.periodOfNotificationGLS.selectedItemPosition,
                time = binding.timeOfNotificationsGLS.text.toString(),
                dateOfEnd = dateOfEnd,
                periodOfNotification = binding.periodOfNotificationGLS.selectedItem.toString()
            )


            table.child("Users")
                .child(auth.currentUser!!.uid)
                .child("History")
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (years in snapshot.children){
                            for (months in years.children){
                                for (historyItem in months.children){
                                    historyItem.getValue(HistoryItem::class.java)?.let {
                                        if (it.placeId == key){
                                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                .child(years.key.toString())
                                                .child(months.key.toString())
                                                .child(it.key)
                                                .child("baseAmount")
                                                .setValue(
                                                    when{
                                                        financeViewModel.budgetLiveData.value?.find{finding->it.budgetId == finding.key }!!.budgetItem.currency==selection.first.ifEmpty { beginItem.currency}->{
                                                            it.amount
                                                        }
                                                        else -> changeCurrencyAmount(beginItem.currency, selection.first.ifEmpty { beginItem.currency}, it.baseAmount, context)
                                                    }
                                                )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })
            findNavController().popBackStack()
        }
    }

    private fun makeNewGoal(){
        glsViewModel.makeNewGoal(
            goalItem = GoalItem(
                name = binding.nameGLSEdit.text.toString(),
                target = "%.2f".format(binding.glsValue.text.toString().toDouble()).replace(",", "."),
                currency = selection.first.ifEmpty { financeViewModel.budgetLiveData.value?.find { it.key == "Base budget" }!!.budgetItem.currency },
                date = if (binding.withouthDate.isChecked) null else SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfEnd.time),
                path = binding.imageOfGLM.tag.toString(),
                isDeleted = false),
            periodOfNotificationPosition = binding.periodOfNotificationGLS.selectedItemPosition,
            context = requireContext(),
            time = binding.timeOfNotificationsGLS.text.toString(),
            dateOfEnd = dateOfEnd,
            periodOfNotification = binding.periodOfNotificationGLS.selectedItem.toString()
        )
        findNavController().popBackStack()
    }

    private fun saveSub(){
        if (financeViewModel.subLiveData.value?.filter { !it.subItem.isDeleted }?.all{it.subItem.name !=  binding.nameGLSEdit.text.toString()}==false)
            Snackbar.make(binding.buttonAddGLS, resources.getString(R.string.error_sub_exists), Snackbar.LENGTH_LONG).show()
        else if (financeViewModel.subLiveData.value?.filter { it.subItem.isDeleted }?.all{it.subItem.name !=  binding.nameGLSEdit.text.toString()}==false){
            AlertDialog.Builder(context)
                .setTitle(resources.getString(R.string.repair))
                .setMessage(resources.getString(R.string.repair_sub))
                .setPositiveButton(resources.getString(R.string.repair_agree)) { dialog, _ ->
                    glsViewModel.restoreSub(financeViewModel.subLiveData.value!!.find { it.subItem.name ==  binding.nameGLSEdit.text.toString()}!!.key)
                    findNavController().popBackStack()
                    dialog.dismiss()
                }
                .setNegativeButton(resources.getString(R.string.make_new)) { dialog, _ ->
                    makeNewSub()
                    dialog.dismiss()
                }
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
        else if (financeViewModel.subLiveData.value?.filter { it.subItem.isCancelled }?.all{it.subItem.name !=  binding.nameGLSEdit.text.toString()}==false){
            AlertDialog.Builder(context)
                .setTitle(resources.getString(R.string.repair))
                .setMessage(resources.getString(R.string.resub_sub))
                .setPositiveButton(resources.getString(R.string.renew)) { dialog, _ ->
                    glsViewModel.cancelledSub(financeViewModel.subLiveData.value!!.find { it.subItem.name ==  binding.nameGLSEdit.text.toString()}!!.key)
                    findNavController().popBackStack()
                    dialog.dismiss()
                }
                .setNegativeButton(resources.getString(R.string.make_new)) { dialog, _ ->
                    makeNewSub()
                    dialog.dismiss()
                }
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
        else makeNewSub()
    }

    private fun updateSub(context: Context, subItemWithKey: SubItemWithKey){
        if (financeViewModel.subLiveData.value?.filter { !it.subItem.isDeleted  && it.key != key}?.all{it.subItem.name !=  binding.nameGLSEdit.text.toString()}==false)
            Snackbar.make(binding.buttonAddGLS, resources.getString(R.string.error_sub_exists), Snackbar.LENGTH_LONG).show()
        else{

            while(dateLS.timeInMillis<Calendar.getInstance().timeInMillis){
                dateLS.add(
                    when ((periodLS ?: "1 m").split(" ")[1]) {
                        "d" -> Calendar.DAY_OF_MONTH
                        "w" -> Calendar.WEEK_OF_MONTH
                        "m" -> Calendar.MONTH
                        else -> Calendar.YEAR
                    }, (periodLS ?: "1 m").split(" ")[0].toInt()
                )
            }

            glsViewModel.updateSub(
                key = key!!,
                subItem = SubItem(
                    name = binding.nameGLSEdit.text.toString(),
                    amount = "%.2f".format(binding.glsValue.text.toString().toDouble()).replace(",", "."),
                    date =  SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateLS.time),
                    path = binding.imageOfGLM.tag.toString(),
                    isCancelled = subItemWithKey.subItem.isCancelled,
                    budgetId = financeViewModel.budgetLiveData.value?.find { it.budgetItem.name == binding.spinnerBudgetGLS.selectedItem.toString() }!!.key,
                    isDeleted = false,
                    period = periodLS ?: "1 m"),
                subItemWithKey = subItemWithKey,
                context = context,
                time = if (binding.timeOfNotificationsGLS.visibility == View.VISIBLE) binding.timeOfNotificationsGLS.text.toString() else "",
                dateLS = dateLS,
                periodOfNotification = binding.periodOfNotificationGLS.selectedItem.toString()
            )
            findNavController().popBackStack()
        }
    }

    private fun makeNewSub(){
        while(dateLS.timeInMillis<Calendar.getInstance().timeInMillis){
            dateLS.add(
                when ((periodLS ?: "1 m").split(" ")[1]) {
                    "d" -> Calendar.DAY_OF_MONTH
                    "w" -> Calendar.WEEK_OF_MONTH
                    "m" -> Calendar.MONTH
                    else -> Calendar.YEAR
                }, (periodLS ?: "1 m").split(" ")[0].toInt()
            )
        }
        glsViewModel.makeNewSub(
            subItem = SubItem(
                name = binding.nameGLSEdit.text.toString(),
                amount = "%.2f".format(binding.glsValue.text.toString().toDouble()).replace(",", "."),
                date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateLS.time),
                path = binding.imageOfGLM.tag.toString(),
                budgetId = financeViewModel.budgetLiveData.value?.find { it.budgetItem.name == binding.spinnerBudgetGLS.selectedItem.toString() }!!.key,
                isDeleted = false,
                isCancelled = false,
                period = periodLS ?: "1 m"),
            context = requireContext(),
            time = if (binding.timeOfNotificationsGLS.visibility == View.VISIBLE) binding.timeOfNotificationsGLS.text.toString() else "",
            dateLS = dateLS,
            periodOfNotification = binding.periodOfNotificationGLS.selectedItem.toString()
        )
        findNavController().popBackStack()

    }

    private fun saveLoan(){
        if (financeViewModel.loansLiveData.value?.filter { !it.loanItem.isDeleted }?.all{it.loanItem.name !=  binding.nameGLSEdit.text.toString()}==false)
            Snackbar.make(binding.buttonAddGLS, resources.getString(R.string.error_loan_exists), Snackbar.LENGTH_LONG).show()
        else if (financeViewModel.loansLiveData.value?.filter { it.loanItem.isDeleted }?.all{it.loanItem.name !=  binding.nameGLSEdit.text.toString()}==false){
            AlertDialog.Builder(context)
                .setTitle(resources.getString(R.string.repair))
                .setMessage(resources.getString(R.string.error_loan_renew))
                .setPositiveButton(resources.getString(R.string.repair_agree)) { dialog, _ ->
                    glsViewModel.restoreLoan(financeViewModel.loansLiveData.value!!.find { it.loanItem.name ==  binding.nameGLSEdit.text.toString()}!!.key)
                    Toast.makeText(context, requireContext().resources.getString(R.string.history_renotify), Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    dialog.dismiss()
                }
                .setNegativeButton(resources.getString(R.string.make_new_2)) { dialog, _ ->
                    makeNewLoan()
                    dialog.dismiss()
                }
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
        else makeNewLoan()
    }

    private fun makeNewLoan(){
        val beginDate:List<String>
        val beginCalendar = Calendar.getInstance()
        val endDate:List<String>

        if(binding.withDate.isChecked) {
            beginDate = binding.periodOfLoan.text.toString().split("-")[0].split(".")
            beginCalendar.set(beginDate[2].toInt(), beginDate[1].toInt()-1, beginDate[0].toInt())
            endDate = binding.periodOfLoan.text.toString().split("-")[1].split(".")

            while(beginCalendar.timeInMillis<Calendar.getInstance().timeInMillis){
                beginCalendar.add(
                    when ((periodLS ?: "1 m").split(" ")[1]) {
                        "d" -> Calendar.DAY_OF_MONTH
                        "w" -> Calendar.WEEK_OF_MONTH
                        "m" -> Calendar.MONTH
                        else -> Calendar.YEAR
                    }, (periodLS ?: "1 m").split(" ")[0].toInt()
                )
                if(beginCalendar.timeInMillis>=Calendar.getInstance().apply {
                        set(endDate[2].toInt(), endDate[1].toInt()-1, endDate[0].toInt())
                    }.timeInMillis){
                    Toast.makeText(requireContext(), getString(R.string.error_date), Toast.LENGTH_SHORT).show()
                    binding.buttonAddGLS.isEnabled = false
                    return
                }
            }
        }
        glsViewModel.makeLoan(
            loanItem = LoanItem(
                name = binding.nameGLSEdit.text.toString(),
                amount = "%.2f".format(binding.glsValue.text.toString().toDouble()).replace(",", "."),
                dateOfEnd = if (binding.withDate.isChecked) binding.periodOfLoan.text.toString().split("-")[1] else {SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateLS.time)},
                dateNext = if (binding.withDate.isChecked){SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(beginCalendar.time)} else null,
                path = binding.imageOfGLM.tag.toString(),
                currency = selection.first.ifEmpty { financeViewModel.budgetLiveData.value?.find { it.key == "Base budget" }!!.budgetItem.currency },
                isDeleted = false,
                isFinished = false,
                period = if(binding.withDate.isChecked) periodLS ?: "1 m" else null
            ),
            context = requireContext(),
            time = if (binding.timeOfNotificationsGLS.visibility == View.VISIBLE) binding.timeOfNotificationsGLS.text.toString() else "",
            beginCalendar = if (binding.withDate.isChecked) beginCalendar else dateLS,
            periodOfNotification = binding.periodOfNotificationGLS.selectedItem.toString()
        )
        findNavController().popBackStack()
    }

    private fun updateLoan(context: Context, beginItem: LoanItem){
        if (financeViewModel.loansLiveData.value?.filter { !it.loanItem.isDeleted  && it.key != key}?.all{it.loanItem.name !=  binding.nameGLSEdit.text.toString()}==false)
            Snackbar.make(binding.buttonAddGLS, resources.getString(R.string.error_loan_exists), Snackbar.LENGTH_LONG).show()
        else{
            val beginDate:List<String>
            val beginCalendar = Calendar.getInstance()
            val endDate:List<String>

            if(binding.withDate.isChecked) {
                beginDate = binding.periodOfLoan.text.toString().split("-")[0].split(".")
                beginCalendar.set(beginDate[2].toInt(), beginDate[1].toInt()-1, beginDate[0].toInt())
                endDate = binding.periodOfLoan.text.toString().split("-")[1].split(".")

                while(beginCalendar.timeInMillis<Calendar.getInstance().timeInMillis){
                    beginCalendar.add(
                        when ((periodLS ?: "1 m").split(" ")[1]) {
                            "d" -> Calendar.DAY_OF_MONTH
                            "w" -> Calendar.WEEK_OF_MONTH
                            "m" -> Calendar.MONTH
                            else -> Calendar.YEAR
                        }, (periodLS ?: "1 m").split(" ")[0].toInt()
                    )
                    if(beginCalendar.timeInMillis>=Calendar.getInstance().apply {
                            set(endDate[2].toInt(), endDate[1].toInt()-1, endDate[0].toInt())
                        }.timeInMillis){
                        Toast.makeText(requireContext(), context.resources.getString(R.string.error_date), Toast.LENGTH_SHORT).show()
                        binding.buttonAddGLS.isEnabled = false
                        return
                    }
                }
            }

            glsViewModel.updateLoan(key!!, LoanItem(
                name = binding.nameGLSEdit.text.toString(),
                amount = "%.2f".format(binding.glsValue.text.toString().toDouble()).replace(",", "."),
                dateOfEnd =  if (binding.withDate.isChecked) binding.periodOfLoan.text.toString().split("-")[1] else {SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateLS.time)},
                dateNext = if (binding.withDate.isChecked){SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(beginCalendar.time)} else null,
                path = binding.imageOfGLM.tag.toString(),
                currency =  selection.first.ifEmpty {  beginItem.currency },
                isDeleted = beginItem.isDeleted,
                isFinished = beginItem.isFinished,
                period = if(binding.withDate.isChecked) periodLS ?: "1 m" else null
            ))
            BudgetNotificationManager.cancelAlarmManager(context, key!!)
            if (binding.periodOfNotificationGLS.selectedItemPosition!=-1){
                BudgetNotificationManager.notification(
                    context = requireContext(),
                    channelID = Constants.CHANNEL_ID_LOAN,
                    placeId = null,
                    id = key!!,
                    time = if (binding.timeOfNotificationsGLS.visibility == View.VISIBLE) binding.timeOfNotificationsGLS.text.toString() else "",
                    dateOfExpence = if (binding.withDate.isChecked) beginCalendar else dateLS,
                    periodOfNotification = binding.periodOfNotificationGLS.selectedItem.toString()
                )
            }


            table.child("Users")
                .child(auth.currentUser!!.uid)
                .child("History")
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (years in snapshot.children){
                            for (months in years.children){
                                for (historyItem in months.children){
                                    historyItem.getValue(HistoryItem::class.java)?.let {
                                        if (it.placeId == key){
                                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                .child(years.key.toString())
                                                .child(months.key.toString())
                                                .child(it.key)
                                                .child("baseAmount")
                                                .setValue(
                                                    when{
                                                        financeViewModel.budgetLiveData.value?.find{finding->it.budgetId == finding.key }!!.budgetItem.currency==selection.first.ifEmpty { beginItem.currency}->{
                                                            it.amount
                                                        }
                                                        else -> changeCurrencyAmount(beginItem.currency, selection.first.ifEmpty { beginItem.currency}, it.baseAmount, context)
                                                    }
                                                )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}

                })
            findNavController().popBackStack()
        }
    }

    private fun showDateRangePicker(){
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(resources.getString(R.string.choose_range))
            .build()
        dateRangePicker.show(requireActivity().supportFragmentManager,"date_range_picker")

        dateRangePicker.addOnPositiveButtonClickListener { datePicker ->
            binding.periodOfLoan.text = "${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(datePicker.first)}-${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(datePicker.second)}"
            checkAllFieldsLoan()
        }
    }

    private fun checkAllFieldsGoal(){
        binding.buttonAddGLS.isEnabled = binding.glsValue.text.isNotEmpty() && binding.nameGLS.text.isNotEmpty() && binding.imageOfGLM.tag!=null
    }

    private fun checkAllFieldsLoan(){
        binding.buttonAddGLS.isEnabled = binding.glsValue.text.isNotEmpty() && binding.nameGLS.text.isNotEmpty() && binding.imageOfGLM.tag!=null && if (binding.withDate.isChecked) binding.periodOfLoan.text.toString()!=requireContext().resources.getString(R.string.periodLoans) else true
    }

    private fun checkAllFieldsSub(){
        binding.buttonAddGLS.isEnabled =  binding.glsValue.text.isNotEmpty() && binding.nameGLS.text.isNotEmpty() && binding.imageOfGLM.tag!=null
    }

}
