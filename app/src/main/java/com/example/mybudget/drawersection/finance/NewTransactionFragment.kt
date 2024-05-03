package com.example.mybudget.drawersection.finance

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.NotificationManager
import com.example.mybudget.R
import com.example.mybudget.databinding.PageNewExpenseBinding
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.example.mybudget.drawersection.finance.budget._BudgetItem
import com.example.mybudget.drawersection.finance.category.CategoryItemWithKey
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.example.mybudget.start_pages.Constants
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

class NewTransactionFragment : Fragment() {
    private lateinit var binding: PageNewExpenseBinding
    private var textWatcher: TextWatcher? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var adapterBudget: ArrayAdapter<String>
    private lateinit var adapterCategory: ArrayAdapter<String>
    private lateinit var adapterPeriod: ArrayAdapter<String>
    private lateinit var financeViewModel:FinanceViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private var planned = false

    private lateinit var periodList: Array<String>

    private lateinit var categoryList: List<CategoryItemWithKey>
    private lateinit var budgetList: List<BudgetItemWithKey>

    private var dateOfExpence: Calendar = Calendar.getInstance()
    private var dateOfIncome: Calendar = Calendar.getInstance()

    private var newCurrency = ""
    private var beginCurrency = ""
    private var baseCurrency = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_new_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = PageNewExpenseBinding.bind(view)

        binding.calendarViewCategory.visibility = View.GONE

        Calendar.getInstance().apply {
            binding.calendarViewBudget.maxDate = this.timeInMillis
        }

        binding.buttonAddIncome.setOnClickListener {
            addOrPlan()
        }

        binding.timeOfNotifications.setOnClickListener {
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
    }

    override fun onStart() {
        super.onStart()

        auth = Firebase.auth
        table = Firebase.database.reference
        activity?.let { financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java]}

        Calendar.getInstance().apply{
            set(Calendar.YEAR, financeViewModel.financeDate.value!!.second)
            set(Calendar.MONTH, financeViewModel.financeDate.value!!.first-1)
            set(Calendar.DAY_OF_MONTH, 1)
            binding.calendarViewCategory.minDate = this.timeInMillis
        }

        val day =  when{
            (financeViewModel.financeDate.value!!.second == Calendar.getInstance().get(Calendar.YEAR)
                    && financeViewModel.financeDate.value!!.first == Calendar.getInstance().get(Calendar.MONTH)+1)-> Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            else->1
        }
        dateOfExpence.set(financeViewModel.financeDate.value!!.second, financeViewModel.financeDate.value!!.first-1, day)
        periodList = requireContext().resources.getStringArray(R.array.periodicity)
        whatIsChecked()


        binding.calendarViewBudget.setOnDateChangeListener { _, year, month, dayOfMonth ->
            dateOfIncome = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            binding.timeOfNotificationsTitle.visibility = View.INVISIBLE
            binding.timeOfNotifications.visibility = View.INVISIBLE
            binding.periodOfNotificationTitle.visibility = View.GONE
            binding.periodOfNotification.visibility = View.GONE
        }

        binding.periodOfNotification.onItemSelectedListener = object :OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if(position == 0){
                    binding.timeOfNotificationsTitle.visibility = View.INVISIBLE
                    binding.timeOfNotifications.visibility = View.INVISIBLE
                } else {
                    binding.timeOfNotificationsTitle.visibility = View.VISIBLE
                    binding.timeOfNotifications.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                binding.timeOfNotificationsTitle.visibility = View.INVISIBLE
                binding.timeOfNotifications.visibility = View.INVISIBLE
            }
        }

        binding.calendarViewCategory.setOnDateChangeListener { _, year, month, dayOfMonth ->
            dateOfExpence = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            if(year>Calendar.getInstance().get(Calendar.YEAR)
                || month > Calendar.getInstance().get(Calendar.MONTH) && year >= Calendar.getInstance().get(Calendar.YEAR)
                || dayOfMonth > Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && month >= Calendar.getInstance().get(Calendar.MONTH)){

                binding.buttonAddIncome.text = resources.getString(R.string.plan)
                binding.periodOfNotificationTitle.visibility = View.VISIBLE
                binding.periodOfNotification.visibility = View.VISIBLE
                val resultList = mutableListOf<String>()
                val dateEnd = LocalDate.of(year, month, dayOfMonth)
                val dateNow = LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                val daysBetween = ChronoUnit.DAYS.between(dateNow, dateEnd)
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
                binding.periodOfNotification.adapter = adapterPeriod
                planned = true
            } else {
                planned = false
                binding.timeOfNotificationsTitle.visibility = View.INVISIBLE
                binding.timeOfNotifications.visibility = View.INVISIBLE
                binding.periodOfNotificationTitle.visibility = View.GONE
                binding.periodOfNotification.visibility = View.GONE
                binding.buttonAddIncome.text = resources.getString(R.string.add)
            }
        }

        financeViewModel.budgetLiveData.observe(this)  {
            if (it.size > 1) binding.transfer.visibility = View.VISIBLE
            else {
                binding.transfer.visibility = View.GONE
                if (binding.transfer.isChecked) binding.income.isChecked = true
            }
            when{
                binding.transfer.isChecked->{
                    adapterBudget = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, it.filter { budgetExist-> !budgetExist.budgetItem.isDeleted }.map { budget -> budget.budgetItem.name } )
                    adapterBudget.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerBudget.adapter = adapterBudget

                    binding.spinnerBudgetTo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, it.filter { budgetExist-> !budgetExist.budgetItem.isDeleted && budgetExist.budgetItem.name != binding.spinnerBudget.selectedItem.toString() }.map { budget -> budget.budgetItem.name }.ifEmpty {
                        binding.income.isChecked = true
                        it.filter { budgetExist-> !budgetExist.budgetItem.isDeleted }.map { budget -> budget.budgetItem.name }
                    }).apply {  setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)}
                }
                else->{
                    adapterBudget = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, it.filter { budgetExist-> !budgetExist.budgetItem.isDeleted }.map { budget -> budget.budgetItem.name } )
                    adapterBudget.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerBudget.adapter = adapterBudget
                }
            }
            budgetList = it
        }

        financeViewModel.categoryLiveData.observe(this){
            adapterCategory = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, it.map { category -> financeViewModel.categoryBeginLiveData.value!!.filter { begin-> begin.key == category.key }[0].categoryBegin.name})
            adapterCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapterCategory
            categoryList = it
        }

        baseCurrency = financeViewModel.budgetLiveData.value?.get(0)!!.budgetItem.currency

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        sharedViewModel.dataToPass.value = null
        sharedViewModel.dataToPass.observe(this){
            if(it!=null){
                newCurrency = it.first
                binding.currencyNew.text = it.third

                if(newCurrency!=beginCurrency){
                    binding.currencyExpence.setOnClickListener(null)
                    binding.currencyNew.setOnClickListener {
                        findNavController().navigate(R.id.action_newTransactionFragment_to_currencyDialogFragment)
                    }
                } else {
                    binding.currencyNew.setOnClickListener(null)
                    binding.currencyExpence.setOnClickListener {
                        findNavController().navigate(R.id.action_newTransactionFragment_to_currencyDialogFragment)
                    }
                }
                binding.currencyNew.visibility = View.VISIBLE
                binding.translateValueNew.visibility = View.VISIBLE
                binding.equalSymbolNewExpence.visibility = View.VISIBLE
                textWatcher?.let { watcher->
                    binding.translateValueNew.removeTextChangedListener(watcher)
                    binding.savingsValue.removeTextChangedListener(watcher)
                    binding.translateValueNew.text.clear()
                    binding.savingsValue.text.clear()
                }
                convertValue()
                binding.savingsValue.clearFocus()
                binding.translateValueNew.clearFocus()
                }
        }
        radioGroupFunction()

        binding.savingsValue.doAfterTextChanged {
            when(binding.income.isChecked){
                true -> checkAllFilledIncome()
                else -> checkAllFilledExpence()
            }
        }

        var islBudgetTo: OnItemSelectedListener? = null
        var islBudget: OnItemSelectedListener? = null
        var updatingBudget = false
        var updatingBudgetTo = false

        islBudget = object : OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when{
                    binding.income.isChecked -> {
                        updateCurrency(false)
                        checkAllFilledIncome()
                    }
                    binding.transfer.isChecked->{
                        binding.spinnerBudgetTo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, budgetList.filter { budgetExist-> !budgetExist.budgetItem.isDeleted && budgetExist.budgetItem.name != binding.spinnerBudget.selectedItem.toString() }.map { budget -> budget.budgetItem.name } )
                            .apply {  setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)}
                        Log.e("checkSpinner", binding.spinnerBudget.selectedItem.toString())

                        beginCurrency = budgetList.find { it.budgetItem.name == binding.spinnerBudgetTo.selectedItem.toString() }!!.budgetItem.currency
                        newCurrency = budgetList.find { it.budgetItem.name == binding.spinnerBudget.selectedItem.toString() }!!.budgetItem.currency

                        binding.currencyNew.text = requireContext().resources.getString(
                                requireContext().resources.getIdentifier(
                                    newCurrency,
                                    "string",
                                    requireContext().packageName
                                ))
                        binding.currencyExpence.text = requireContext().resources.getString(
                                requireContext().resources.getIdentifier(
                                    beginCurrency,
                                    "string",
                                    requireContext().packageName
                                ))
                        binding.currencyNew.visibility = View.VISIBLE
                        binding.translateValueNew.visibility = View.VISIBLE
                        binding.equalSymbolNewExpence.visibility = View.VISIBLE
                        updateCurrency(true)
                    }

                    else -> {
                        binding.currencyNew.visibility = View.VISIBLE
                        binding.translateValueNew.visibility = View.VISIBLE
                        binding.equalSymbolNewExpence.visibility = View.VISIBLE
                        newCurrency = financeViewModel.budgetLiveData.value!!.filter { it.budgetItem.name == binding.spinnerBudget.selectedItem.toString()}[0].budgetItem.currency
                        binding.currencyNew.text = requireContext().resources.getString(requireContext().resources.getIdentifier(newCurrency, "string", requireContext().packageName))
                        binding.currencyExpence.setOnClickListener(null)
                        binding.currencyNew.setOnClickListener(null)
                        beginCurrency = baseCurrency
                        updateCurrency(true)
                        checkAllFilledExpence()
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        islBudgetTo = object : OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                beginCurrency = budgetList.find { it.budgetItem.name == binding.spinnerBudgetTo.selectedItem.toString() }!!.budgetItem.currency
                newCurrency = budgetList.find { it.budgetItem.name == binding.spinnerBudget.selectedItem.toString() }!!.budgetItem.currency

                binding.currencyNew.text = requireContext().resources.getString(
                    requireContext().resources.getIdentifier(
                        newCurrency,
                        "string",
                        requireContext().packageName
                    )
                )
                binding.currencyExpence.text = requireContext().resources.getString(
                    requireContext().resources.getIdentifier(
                        beginCurrency,
                        "string",
                        requireContext().packageName
                    )
                )
                binding.currencyNew.visibility = View.VISIBLE
                binding.translateValueNew.visibility = View.VISIBLE
                binding.equalSymbolNewExpence.visibility = View.VISIBLE
                updateCurrency(true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerBudget.onItemSelectedListener = islBudget
        binding.spinnerBudgetTo.onItemSelectedListener = islBudgetTo

        binding.spinnerCategory.setSelection(0)
    }

    private fun addOrPlan(){
        val valueDouble = binding.savingsValue.text.toString().toDouble()
        val valueDoubleOthers = when(binding.translateValueNew.visibility){
            View.VISIBLE->binding.translateValueNew.text.toString().toDoubleOrNull()?:0.0
            else ->0.0
        }
        val nameBudget = binding.spinnerBudget.selectedItem.toString()

        val budget = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!
        if(binding.income.isChecked){
            when(binding.spinnerBudget.selectedItemPosition){
                0 ->{
                    val reference = table.child("Users").child(auth.currentUser!!.uid)
                        .child("Budgets").child("Base budget")
                    reference.addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentBudgetItem = snapshot.getValue(_BudgetItem::class.java)
                            currentBudgetItem?.amount = String.format("%.2f",currentBudgetItem?.amount!!.toDouble()+valueDouble).replace(',','.')
                            currentBudgetItem.count += 1
                            reference.setValue(currentBudgetItem).addOnCompleteListener {

                                val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                                    .child("${dateOfIncome.get(Calendar.YEAR)}/${dateOfIncome.get(Calendar.MONTH)+1}")
                                    .push()

                                newHistory.setValue(HistoryItem(budgetId = budget.key, amount = String.format("%.2f",valueDouble).replace(',','.')/*${
                                    requireContext().resources.getString( requireContext().resources.getIdentifier(budget.budgetItem.currency, "string",  requireContext().packageName))}"*/,
                                    baseAmount = String.format("%.2f",valueDouble).replace(',','.'),
                                    date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfIncome.time), key = newHistory.key.toString()))
                               findNavController().popBackStack()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                    }
                -1 -> Toast.makeText(requireContext(), "Вы не выбрали бюджет", Toast.LENGTH_LONG).show()
                else ->{
                    val reference = table.child("Users").child(auth.currentUser!!.uid)
                        .child("Budgets").child("Other budget")
                        .child(financeViewModel.budgetLiveData.value!!.filter { it.budgetItem.name == binding.spinnerBudget.selectedItem.toString() }[0].key)
                    reference.addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentBudgetItem = snapshot.getValue(_BudgetItem::class.java)
                            currentBudgetItem?.amount = String.format("%.2f",currentBudgetItem?.amount!!.toDouble()+valueDouble).replace(',','.')
                            currentBudgetItem.count += 1
                            reference.setValue(currentBudgetItem).addOnCompleteListener {
                                val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                                    .child("${dateOfIncome.get(Calendar.YEAR)}/${dateOfIncome.get(Calendar.MONTH)+1}")
                                    .push()
                                newHistory.setValue(HistoryItem(budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key, amount = String.format("%.2f",if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.'),
                                    baseAmount = String.format("%.2f",if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.'),
                                    date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfIncome.time), key = newHistory.key.toString()))
                                findNavController().popBackStack()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
        }

        else if(binding.transfer.isChecked){
            when{
                binding.spinnerBudget.selectedItemPosition == -1 || binding.spinnerBudgetTo.selectedItemPosition == -1 -> Toast.makeText(requireContext(), "Вы не выбрали бюджет", Toast.LENGTH_LONG).show()
                else ->{
                    val budgetFrom = budgetList.find { it.budgetItem.name ==  binding.spinnerBudget.selectedItem.toString()}!!
                    val budgetTo = budgetList.find { it.budgetItem.name ==  binding.spinnerBudgetTo.selectedItem.toString()}!!
                    if (budgetFrom.budgetItem.amount.toDouble() - valueDoubleOthers < 0.0){
                        AlertDialog.Builder(context)
                            .setTitle("Перерасход")
                            .setMessage("После совершения данной операции вы уйдете в минус!\nПродолжить?")
                            .setPositiveButton("Да") { dialog2, _ ->
                                addTransfer(
                                    budgetFrom = budgetFrom,
                                    budgetTo = budgetTo,
                                    valueDoubleOthers = if (valueDoubleOthers == 0.0 ) valueDouble else valueDoubleOthers,
                                    valueDouble = valueDouble
                                )
                                dialog2.dismiss()
                            }
                            .setNegativeButton("Нет") { dialog2, _ ->
                                dialog2.dismiss()
                            }.show()
                    } else {
                        addTransfer(
                            budgetFrom = budgetFrom,
                            budgetTo = budgetTo,
                            valueDoubleOthers = if (valueDoubleOthers == 0.0 ) valueDouble else valueDoubleOthers,
                            valueDouble = valueDouble
                        )
                    }
                }
            }
        }
        else{
            val categoryPath = financeViewModel.categoryBeginLiveData.value!!.filter {  it.key == categoryList[binding.spinnerCategory.selectedItemPosition].key}[0].categoryBegin.path
            val nameCategory = binding.spinnerCategory.selectedItem.toString()
            Log.e("CheckId", financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key)

            when(binding.spinnerBudget.selectedItemPosition){
                0 ->{
                    val reference = table.child("Users").child(auth.currentUser!!.uid)
                        .child("Budgets").child("Base budget")
                    reference.addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentBudgetItem = snapshot.getValue(_BudgetItem::class.java)
                            if(dateOfExpence.get(Calendar.YEAR)<=Calendar.getInstance().get(Calendar.YEAR)&&
                                    dateOfExpence.get(Calendar.MONTH)<=Calendar.getInstance().get(Calendar.MONTH)
                                    && dateOfExpence.get(Calendar.DAY_OF_MONTH)<=Calendar.getInstance().get(Calendar.DAY_OF_MONTH)){

                                if(currentBudgetItem?.amount!!.toDouble() - valueDouble<0.0){
                                    AlertDialog.Builder(context)
                                        .setTitle("Перерасход")
                                        .setMessage("После совершения данной операции вы уйдете в минус!\nПродолжить?")
                                        .setPositiveButton("Да") { dialog2, _ ->
                                            addNewTransactionBase(currentBudgetItem, valueDoubleOthers, valueDouble, reference, nameCategory, nameBudget, budget)
                                            dialog2.dismiss()
                                        }
                                        .setNegativeButton("Нет") { dialog2, _ ->
                                            dialog2.dismiss()
                                        }.show()
                                } else{
                                    addNewTransactionBase(currentBudgetItem, valueDoubleOthers, valueDouble, reference, nameCategory, nameBudget, budget)
                                }
                            } else if(currentBudgetItem!=null){
                                addNewTransactionBase(currentBudgetItem, valueDoubleOthers, valueDouble, reference, nameCategory, nameBudget, budget)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
                -1 -> Toast.makeText(requireContext(), "Вы не выбрали бюджет", Toast.LENGTH_LONG).show()
                else ->{
                    val reference = table.child("Users").child(auth.currentUser!!.uid)
                        .child("Budgets").child("Other budget").child(financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key)
                    reference.addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentBudgetItem = snapshot.getValue(_BudgetItem::class.java)
                            if(dateOfExpence.get(Calendar.YEAR)<=Calendar.getInstance().get(Calendar.YEAR)&&
                                dateOfExpence.get(Calendar.MONTH)<=Calendar.getInstance().get(Calendar.MONTH)
                                && dateOfExpence.get(Calendar.DAY_OF_MONTH)<=Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {

                                if((currentBudgetItem?.amount!!.toDouble() - if(valueDoubleOthers==0.0) valueDouble else valueDoubleOthers)<0.0){
                                    AlertDialog.Builder(context)
                                        .setTitle("Перерасход")
                                        .setMessage("После совершения данной операции вы уйдете в минус!\nПродолжить?")
                                        .setPositiveButton("Да") { dialog2, _ ->
                                            addNewTransaction(currentBudgetItem, valueDoubleOthers, valueDouble, reference, nameCategory, nameBudget, budget)
                                            dialog2.dismiss()
                                        }
                                        .setNegativeButton("Нет") { dialog2, _ ->
                                            dialog2.dismiss()
                                        }.show()
                                } else{
                                    addNewTransaction(currentBudgetItem, valueDoubleOthers, valueDouble, reference, nameCategory, nameBudget, budget)
                                }
                            }else if(currentBudgetItem!=null){
                                addNewTransaction(currentBudgetItem, valueDoubleOthers, valueDouble, reference, nameCategory, nameBudget, budget)
                        }

                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
        }
    }

    private fun addTransfer(budgetFrom:BudgetItemWithKey, budgetTo:BudgetItemWithKey, valueDoubleOthers:Double, valueDouble:Double){
        budgetFrom.budgetItem.count++
        budgetTo.budgetItem.count++
        budgetFrom.budgetItem.amount = String.format("%.2f",budgetFrom.budgetItem.amount.toDouble() - valueDoubleOthers).replace(",", ".")
        budgetTo.budgetItem.amount = String.format("%.2f",budgetTo.budgetItem.amount.toDouble() + valueDouble).replace(",", ".")
        val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
            .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
            .push()

        val historyItem = HistoryItem(
            budgetId = budgetFrom.key,
            placeId = budgetTo.key,
            isTransfer = true,
            amount = valueDouble.toString(),
            date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),
            baseAmount = valueDoubleOthers.toString(),
            key = newHistory.key.toString()
        )

        when (budgetFrom.key){
            "Base budget" -> {
                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("Budgets")
                    .child("Base budget")
                    .setValue(budgetFrom.budgetItem)
            } else ->{
            table.child("Users")
                .child(auth.currentUser!!.uid)
                .child("Budgets")
                .child("Other budget")
                .child(budgetFrom.key)
                .setValue(budgetFrom.budgetItem)
            }
        }

        when (budgetTo.key){
            "Base budget" -> {
                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("Budgets")
                    .child("Base budget")
                    .setValue(budgetTo.budgetItem)
            } else ->{
            table.child("Users")
                .child(auth.currentUser!!.uid)
                .child("Budgets")
                .child("Other budget")
                .child(budgetTo.key)
                .setValue(budgetTo.budgetItem)
            }
        }

        newHistory.setValue(historyItem)
        findNavController().popBackStack()
    }

    private fun addNewTransactionBase(currentBudgetItem:_BudgetItem, valueDoubleOthers:Double, valueDouble:Double, reference: DatabaseReference, nameCategory:String, nameBudget:String, budget:BudgetItemWithKey){
        if(dateOfExpence.get(Calendar.YEAR)<=Calendar.getInstance().get(Calendar.YEAR)&&
            dateOfExpence.get(Calendar.MONTH)<=Calendar.getInstance().get(Calendar.MONTH)
            && dateOfExpence.get(Calendar.DAY_OF_MONTH)<=Calendar.getInstance().get(Calendar.DAY_OF_MONTH)){
            currentBudgetItem.amount = String.format("%.2f",currentBudgetItem.amount.toDouble()-valueDouble).replace(',','.')
            currentBudgetItem.count += 1
    }

        val reference2 = table.child("Users").child(auth.currentUser!!.uid)
            .child("Categories").child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
            .child("ExpenseCategories").child(financeViewModel.categoryBeginLiveData.value!!.filter { it.categoryBegin.name == nameCategory }[0].key)

        reference2.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var currentCategoryExpence = snapshot.getValue(_CategoryItem::class.java)
                if(currentCategoryExpence!=null && dateOfExpence.get(Calendar.YEAR)<=Calendar.getInstance().get(Calendar.YEAR)&&
                    dateOfExpence.get(Calendar.MONTH)<=Calendar.getInstance().get(Calendar.MONTH)
                    && dateOfExpence.get(Calendar.DAY_OF_MONTH)<=Calendar.getInstance().get(Calendar.DAY_OF_MONTH)){
                    val beginRemainder = currentCategoryExpence.remainder
                    if(currentCategoryExpence.remainder =="0"){
                        currentCategoryExpence.total = String.format("%.2f",currentCategoryExpence.total.toDouble()+if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')

                    } else {
                        currentCategoryExpence.remainder = String.format(
                            "%.2f",
                            currentCategoryExpence.remainder.toDouble() - if (valueDoubleOthers != 0.0) valueDoubleOthers else valueDouble
                        ).replace(',', '.')
                    }
                    if(beginRemainder.toDouble()>=0.0 && currentCategoryExpence.remainder.toDouble()<0.0) {
                            AlertDialog.Builder(context)
                                .setTitle("Перерасход")
                                .setMessage("После совершения данной операции Вы превысите распределенный бюджет на категорию!\nПродолжить?")
                                .setPositiveButton("Да") { dialog2, _ ->
                                    reference.setValue(currentBudgetItem)
                                    reference2.setValue(currentCategoryExpence).addOnCompleteListener {
                                        val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                                            .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                                            .push()
                                        newHistory.setValue(HistoryItem(budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key, financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key,
                                            isCategory = true, amount = "-${String.format("%.2f",if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}"/*${
                                                requireContext().resources.getString( requireContext().resources.getIdentifier(budget.budgetItem.currency, "string",  requireContext().packageName))
                                            }"*/, baseAmount = "-${String.format("%.2f",valueDouble).replace(',','.')}",
                                            date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),
                                            key = newHistory.key.toString()))
                                        findNavController().popBackStack()
                                        }
                                    dialog2.dismiss()
                                }
                                .setNegativeButton("Нет") { dialog2, _ ->

                                    dialog2.dismiss()
                                }.show()
                        } else{
                            reference.setValue(currentBudgetItem)
                            reference2.setValue(currentCategoryExpence).addOnCompleteListener {
                                val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                                    .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                                    .push()
                                newHistory.setValue(HistoryItem(budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key, financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key,
                                    isCategory = true, amount = "-${String.format("%.2f",if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}"
                                    , baseAmount = "-${String.format("%.2f",valueDouble).replace(',','.')}",
                                    date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),
                                    key = newHistory.key.toString()))
                                findNavController().popBackStack()
                        }
                    }
                } else{

                    if( dateOfExpence.get(Calendar.MONTH)>Calendar.getInstance().get(Calendar.MONTH)
                        || dateOfExpence.get(Calendar.YEAR)>Calendar.getInstance().get(Calendar.YEAR)){
                        when(currentCategoryExpence){
                            null->{
                                currentCategoryExpence = _CategoryItem(total = "%.2f".format(valueDouble).replace(",", "."),
                                    priority = financeViewModel.categoryLiveData.value?.find { category-> category.key == financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key }?.categoryItem?.priority?:0,
                                    isPlanned = true)
                            }
                            else ->{
                                currentCategoryExpence.total = "%.2f".format(currentCategoryExpence.total.toDouble()+valueDouble).replace(",", ".")
                            }
                        }

                        reference2.setValue(currentCategoryExpence).addOnCompleteListener {
                            val planReferense =
                                table.child("Users").child(auth.currentUser!!.uid)
                                    .child("Plan")
                                    .child(
                                        "${dateOfExpence.get(Calendar.YEAR)}/${
                                            dateOfExpence.get(
                                                Calendar.MONTH
                                            ) + 1
                                        }"
                                    )
                                    .push()
                            planReferense.setValue(
                                HistoryItem(placeId = financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }
                                    ?.get(0)!!.key,
                                    budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget }
                                        ?.get(0)!!.key,
                                    amount = "-${
                                        String.format(
                                            "%.2f",
                                            if (valueDoubleOthers != 0.0) valueDoubleOthers else valueDouble
                                        ).replace(',', '.')
                                    }",
                                    baseAmount = "-${
                                        String.format("%.2f", valueDouble)
                                            .replace(',', '.')
                                    }",
                                    date = SimpleDateFormat(
                                        "dd.MM.yyyy",
                                        Locale.getDefault()
                                    ).format(dateOfExpence.time),
                                    isCategory = true,
                                    key = planReferense.key.toString()
                                )
                            )
                            if(binding.periodOfNotification.selectedItemId!=0L && binding.periodOfNotification.selectedItemId!=-1L) {
                                NotificationManager.notification(
                                    requireContext(),
                                    Constants.CHANNEL_ID_PLAN,
                                    planReferense.key.toString(),
                                    financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory } ?.get(0)!!.key,
                                   /* binding.spinnerCategory.selectedItem.toString(),*/
                                    binding.timeOfNotifications.text.toString(),
                                    dateOfExpence,
                                    binding.periodOfNotification.selectedItem.toString())
                            }
                            NotificationManager.setAutoTransaction(
                                requireContext(),
                                planReferense.key.toString(),
                                financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key,
                                financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget }?.get(0)!!.key,
                                dateOfExpence.get(Calendar.YEAR),
                                dateOfExpence.get(Calendar.MONTH)+1,
                                dateOfExpence,
                                amount = "-${
                                    String.format(
                                        "%.2f",
                                        if (valueDoubleOthers != 0.0) valueDoubleOthers else valueDouble
                                    ).replace(',', '.')
                                }",
                                baseAmount = "-${
                                    String.format("%.2f", valueDouble)
                                        .replace(',', '.')
                                }")
                            findNavController().popBackStack()
                        }
                    } else {
                        val planReferense =  table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                            .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                            .push()
                        planReferense.setValue(HistoryItem(placeId = financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key, budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key,
                            amount = "-${String.format("%.2f",if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}"/*${
                                requireContext().resources.getString( requireContext().resources.getIdentifier(budget.budgetItem.currency, "string",  requireContext().packageName))
                            }"*/,baseAmount = "-${String.format("%.2f",valueDouble).replace(',','.')}",
                            date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),  isCategory = true,
                            key = planReferense.key.toString()))
                        if(binding.periodOfNotification.selectedItemId!=0L && binding.periodOfNotification.selectedItemId!=-1L) {
                            NotificationManager.notification(
                                requireContext(),
                                Constants.CHANNEL_ID_PLAN,
                                planReferense.key.toString(),
                                financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory } ?.get(0)!!.key,
                                binding.timeOfNotifications.text.toString(),
                                dateOfExpence,
                                binding.periodOfNotification.selectedItem.toString())
                        }
                        NotificationManager.setAutoTransaction(
                            requireContext(),
                            planReferense.key.toString(),
                            financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key,
                            financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget }?.get(0)!!.key,
                            dateOfExpence.get(Calendar.YEAR),
                            dateOfExpence.get(Calendar.MONTH)+1,
                            dateOfExpence,
                            amount = "-${
                                String.format(
                                    "%.2f",
                                    if (valueDoubleOthers != 0.0) valueDoubleOthers else valueDouble
                                ).replace(',', '.')
                            }",
                            baseAmount = "-${
                                String.format("%.2f", valueDouble)
                                    .replace(',', '.')
                            }")
                        findNavController().popBackStack()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}

        })
    }
    private fun addNewTransaction(currentBudgetItem:_BudgetItem, valueDoubleOthers:Double, valueDouble:Double, reference: DatabaseReference, nameCategory:String, nameBudget:String, budget:BudgetItemWithKey){
        if(dateOfExpence.get(Calendar.YEAR)<=Calendar.getInstance().get(Calendar.YEAR)&&
            dateOfExpence.get(Calendar.MONTH)<=Calendar.getInstance().get(Calendar.MONTH)
            && dateOfExpence.get(Calendar.DAY_OF_MONTH)<=Calendar.getInstance().get(Calendar.DAY_OF_MONTH)){

            currentBudgetItem.amount = String.format(
            "%.2f",
            currentBudgetItem.amount.toDouble() - (if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble)
        ).replace(',', '.')
        currentBudgetItem.count += 1
        }

       /* reference.setValue(currentBudgetItem).addOnCompleteListener {*/
            val reference2 = table.child("Users").child(auth.currentUser!!.uid)
                .child("Categories").child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                .child("ExpenseCategories").child(financeViewModel.categoryBeginLiveData.value!!.filter { it.categoryBegin.name == nameCategory }[0].key)

            reference2.addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    var currentCategoryExpence2 = snapshot.getValue(_CategoryItem::class.java)
                    if(currentCategoryExpence2!=null && dateOfExpence.get(Calendar.YEAR)<=Calendar.getInstance().get(Calendar.YEAR)&&
                        dateOfExpence.get(Calendar.MONTH)<=Calendar.getInstance().get(Calendar.MONTH)
                        && dateOfExpence.get(Calendar.DAY_OF_MONTH)<=Calendar.getInstance().get(Calendar.DAY_OF_MONTH)){
                        val beginReminder = currentCategoryExpence2.remainder
                        if(currentCategoryExpence2.remainder =="0"){
                            currentCategoryExpence2.total = String.format("%.2f",currentCategoryExpence2.total.toDouble()+valueDouble).replace(',','.')
                        } else {
                            currentCategoryExpence2.remainder = String.format("%.2f",currentCategoryExpence2.remainder.toDouble()-valueDouble).replace(',','.')
                        }
                        if(beginReminder.toDouble()>0.0 && currentCategoryExpence2.remainder.toDouble()<0.0){
                            AlertDialog.Builder(context)
                                .setTitle("Перерасход")
                                .setMessage("После совершения данной операции Вы превысите распределенный бюджет на категорию!\nПродолжить?")
                                .setPositiveButton("Да") { dialog2, _ ->
                                    reference.setValue(currentBudgetItem)
                                    reference2.setValue(currentCategoryExpence2).addOnCompleteListener {
                                        val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                                            .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                                            .push()
                                        newHistory.setValue(HistoryItem(budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key,placeId = financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key,
                                            isCategory = true, amount = "-${String.format("%.2f",if( valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}"/*${
                                                requireContext().resources.getString( requireContext().resources.getIdentifier(budget.budgetItem.currency, "string",  requireContext().packageName))
                                            }"*/,baseAmount = "-${String.format("%.2f",valueDouble).replace(',','.')}",
                                            date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),
                                            key = newHistory.key.toString()))
                                        findNavController().popBackStack()
                                    }
                                    dialog2.dismiss()
                                }
                                .setNegativeButton("Нет") { dialog2, _ ->

                                    dialog2.dismiss()
                                }.show()
                        } else {
                            reference.setValue(currentBudgetItem)
                            reference2.setValue(currentCategoryExpence2).addOnCompleteListener {
                                val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                                    .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                                    .push()
                                newHistory.setValue(HistoryItem(budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key,placeId = financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key,
                                    isCategory = true, amount = "-${String.format("%.2f",if( valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}"/*${
                                        requireContext().resources.getString( requireContext().resources.getIdentifier(budget.budgetItem.currency, "string",  requireContext().packageName))
                                    }"*/,baseAmount = "-${String.format("%.2f",valueDouble).replace(',','.')}",
                                    date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),
                                    key = newHistory.key.toString()))
                                findNavController().popBackStack()
                            }
                        }
                    } else{
                        if( dateOfExpence.get(Calendar.MONTH)>Calendar.getInstance().get(Calendar.MONTH)
                            || dateOfExpence.get(Calendar.YEAR)>Calendar.getInstance().get(Calendar.YEAR)){
                            when(currentCategoryExpence2){
                                null->{
                                    currentCategoryExpence2 = _CategoryItem(total = "%.2f".format(valueDouble).replace(",", "."),
                                        priority = financeViewModel.categoryLiveData.value?.find { category->  category.key == financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key }?.categoryItem?.priority?:0,
                                        isPlanned = true)
                                }
                                else ->{
                                    currentCategoryExpence2.total = "%.2f".format(currentCategoryExpence2.total.toDouble()+valueDouble).replace(",", ".")
                                }
                            }

                            reference2.setValue(currentCategoryExpence2).addOnCompleteListener {
                                val planReferense =
                                    table.child("Users").child(auth.currentUser!!.uid)
                                        .child("Plan")
                                        .child(
                                            "${dateOfExpence.get(Calendar.YEAR)}/${
                                                dateOfExpence.get(
                                                    Calendar.MONTH
                                                ) + 1
                                            }"
                                        )
                                        .push()
                                planReferense.setValue(
                                    HistoryItem(placeId = financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }
                                        ?.get(0)!!.key,
                                        budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget }
                                            ?.get(0)!!.key,
                                        amount = "-${
                                            String.format(
                                                "%.2f",
                                                if (valueDoubleOthers != 0.0) valueDoubleOthers else valueDouble
                                            ).replace(',', '.')
                                        }",
                                        baseAmount = "-${
                                            String.format("%.2f", valueDouble)
                                                .replace(',', '.')
                                        }",
                                        date = SimpleDateFormat(
                                            "dd.MM.yyyy",
                                            Locale.getDefault()
                                        ).format(dateOfExpence.time),
                                        isCategory = true,
                                        key = planReferense.key.toString()
                                    )
                                )
                                if(binding.periodOfNotification.selectedItemId!=0L && binding.periodOfNotification.selectedItemId!=-1L) {
                                    NotificationManager.notification(
                                        requireContext(),
                                        Constants.CHANNEL_ID_PLAN,
                                        planReferense.key.toString(),
                                        financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory } ?.get(0)!!.key,
                                        binding.timeOfNotifications.text.toString(),
                                        dateOfExpence,
                                        binding.periodOfNotification.selectedItem.toString())
                                }
                                NotificationManager.setAutoTransaction(
                                    requireContext(),
                                    planReferense.key.toString(),
                                    financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key,
                                    financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget }?.get(0)!!.key,
                                    dateOfExpence.get(Calendar.YEAR),
                                    dateOfExpence.get(Calendar.MONTH)+1,
                                    dateOfExpence,
                                    amount = "-${
                                        String.format(
                                            "%.2f",
                                            if (valueDoubleOthers != 0.0) valueDoubleOthers else valueDouble
                                        ).replace(',', '.')
                                    }",
                                    baseAmount = "-${
                                        String.format("%.2f", valueDouble)
                                            .replace(',', '.')
                                    }")
                                findNavController().popBackStack()
                            }
                        }

                        else {
                            val planReferense =  table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                                .push()
                            planReferense.setValue(HistoryItem(placeId = financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key, budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key,
                                amount = "-${String.format("%.2f",if( valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}",baseAmount = "-${String.format("%.2f",valueDouble).replace(',','.')}",
                                date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),  isCategory = true,
                                key = planReferense.key.toString()))
                            if(binding.periodOfNotification.selectedItemId!=0L && binding.periodOfNotification.selectedItemId!=-1L) {
                                NotificationManager.notification(
                                    requireContext(),
                                    Constants.CHANNEL_ID_PLAN,
                                    planReferense.key.toString(),
                                    financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory } ?.get(0)!!.key,
                                    binding.timeOfNotifications.text.toString(),
                                    dateOfExpence,
                                    binding.periodOfNotification.selectedItem.toString())
                            }
                            NotificationManager.setAutoTransaction(
                                requireContext(),
                                planReferense.key.toString(),
                                financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key,
                                financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget }?.get(0)!!.key,
                                dateOfExpence.get(Calendar.YEAR),
                                dateOfExpence.get(Calendar.MONTH)+1,
                                dateOfExpence,
                                amount = "-${
                                    String.format(
                                        "%.2f",
                                        if (valueDoubleOthers != 0.0) valueDoubleOthers else valueDouble
                                    ).replace(',', '.')
                                }",
                                baseAmount = "-${
                                    String.format("%.2f", valueDouble)
                                        .replace(',', '.')
                                }")
                            findNavController().popBackStack()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}

            })
    }
    private fun convertValue(){
        val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(requireContext())
        if(currencyConvertor!=null){
            when (beginCurrency){
                    newCurrency->{
                        binding.currencyNew.visibility = View.GONE
                        binding.translateValueNew.visibility = View.GONE
                        binding.equalSymbolNewExpence.visibility = View.GONE
                }
                currencyConvertor.baseCode->{
                    textWatcher = object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            if (s != null) {
                                if (s.isEmpty()){
                                    binding.savingsValue.text.clear()
                                    binding.translateValueNew.text.clear()
                                }
                                else if (s === binding.translateValueNew.text && s.isNotEmpty()) {
                                    binding.savingsValue.setText(String.format("%.2f",s.toString().toDouble()/currencyConvertor.conversionRates[newCurrency]!!).replace(',','.'))
                                } else if (s === binding.savingsValue.text && s.isNotEmpty()) {
                                    binding.translateValueNew.setText(String.format("%.2f", s.toString().toDouble()*currencyConvertor.conversionRates[newCurrency]!!).replace(',','.'))
                                }
                            }
                        }
                    }
                    binding.translateValueNew.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            binding.translateValueNew.removeTextChangedListener(textWatcher)
                        } else {
                            binding.translateValueNew.addTextChangedListener(textWatcher)
                        }
                    }

                    binding.savingsValue.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            binding.savingsValue.removeTextChangedListener(textWatcher)
                        } else {
                            binding.savingsValue.addTextChangedListener(textWatcher)
                        }
                    }
                }

                else->{
                    textWatcher = object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            if (s != null) {
                                if (s.isEmpty()){
                                    binding.savingsValue.text.clear()
                                    binding.translateValueNew.text.clear()
                                }
                                else if (s === binding.translateValueNew.text && s.isNotEmpty()) {
                                    val newValueToBase = s.toString().toDouble()/currencyConvertor.conversionRates[newCurrency]!!
                                    binding.savingsValue.setText(String.format("%.2f", newValueToBase*currencyConvertor.conversionRates[beginCurrency]!!).replace(',','.'))
                                } else if (s === binding.savingsValue.text && s.isNotEmpty()) {
                                    val newValueToBase = s.toString().toDouble()/currencyConvertor.conversionRates[beginCurrency]!!
                                    binding.translateValueNew.setText(String.format("%.2f", newValueToBase*currencyConvertor.conversionRates[newCurrency]!!).replace(',','.'))
                                }
                            }
                        }
                    }

                    binding.translateValueNew.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            binding.translateValueNew.removeTextChangedListener(textWatcher)
                        } else {
                            binding.translateValueNew.addTextChangedListener(textWatcher)
                        }
                    }

                    binding.savingsValue.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            binding.savingsValue.removeTextChangedListener(textWatcher)
                        } else {
                            binding.savingsValue.addTextChangedListener(textWatcher)
                        }
                    }
                }
            }
        }
    }

    private fun radioGroupFunction(){
        binding.radioGroupNewExpence.setOnCheckedChangeListener { _, _ ->
            newCurrency = beginCurrency
            binding.spinnerBudget.setSelection(0)
            whatIsChecked()
        }
    }

    private fun whatIsChecked(){
        if (binding.income.isChecked){
            binding.buttonAddIncome.text = resources.getString(R.string.add)
            binding.budgetTitle.text = resources.getString(R.string.budget)
            binding.currencyExpence.setOnClickListener {
                findNavController().navigate(R.id.action_newTransactionFragment_to_currencyDialogFragment)
            }
            binding.calendarViewBudget.visibility = View.VISIBLE
            binding.calendarViewCategory.visibility = View.GONE
            binding.categoryTitle.visibility = View.GONE
            binding.spinnerCategory.visibility = View.GONE
            binding.timeOfNotificationsTitle.visibility = View.INVISIBLE
            binding.timeOfNotifications.visibility = View.INVISIBLE
            binding.periodOfNotificationTitle.visibility = View.GONE
            binding.periodOfNotification.visibility = View.GONE
            binding.budgetTitle.text = resources.getString(R.string.budget)
            binding.spinnerBudgetTo.visibility = View.GONE
            binding.budgetTitleTo.visibility = View.GONE
            checkAllFilledIncome()

        }
        else if(binding.transfer.isChecked){

            binding.spinnerBudgetTo.visibility = View.VISIBLE
            binding.budgetTitleTo.visibility = View.VISIBLE
            binding.buttonAddIncome.text = resources.getString(R.string.trans)
            binding.currencyExpence.setOnClickListener(null)
            binding.currencyNew.setOnClickListener(null)
            binding.calendarViewBudget.visibility = View.VISIBLE
            binding.calendarViewCategory.visibility = View.GONE

            adapterBudget = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, budgetList.filter { budgetExist-> !budgetExist.budgetItem.isDeleted }.map { budget -> budget.budgetItem.name } )
            adapterBudget.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerBudget.adapter = adapterBudget

            binding.spinnerBudgetTo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, budgetList.filter { budgetExist-> !budgetExist.budgetItem.isDeleted && budgetExist.budgetItem.name != binding.spinnerBudget.selectedItem.toString() }.map { budget -> budget.budgetItem.name } )
                .apply {  setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)}
            binding.budgetTitle.text = resources.getString(R.string.budget_transfer_from)
            binding.categoryTitle.visibility = View.GONE
            binding.spinnerCategory.visibility = View.GONE

            binding.timeOfNotificationsTitle.visibility = View.INVISIBLE
            binding.timeOfNotifications.visibility = View.INVISIBLE
            binding.periodOfNotificationTitle.visibility = View.GONE
            binding.periodOfNotification.visibility = View.GONE

            binding.currencyNew.visibility = View.VISIBLE
            binding.translateValueNew.visibility = View.VISIBLE
            binding.equalSymbolNewExpence.visibility = View.VISIBLE

            beginCurrency = budgetList.find { it.budgetItem.name == binding.spinnerBudgetTo.selectedItem.toString() }!!.budgetItem.currency
            newCurrency = budgetList.find { it.budgetItem.name == binding.spinnerBudget.selectedItem.toString() }!!.budgetItem.currency

            binding.currencyNew.text = requireContext().resources.getString(
                requireContext().resources.getIdentifier(
                    newCurrency,
                    "string",
                    requireContext().packageName
                )
            )
            binding.currencyExpence.text = requireContext().resources.getString(
                requireContext().resources.getIdentifier(
                    beginCurrency,
                    "string",
                    requireContext().packageName
                )
            )
            updateCurrency(true)
            checkAllFilledIncome()


        } else if(binding.expence.isChecked) {
            if(categoryList.isEmpty()){
                Snackbar.make(binding.expence, "У вас не выбрано ни одной категории расходов!", Snackbar.LENGTH_LONG).show()
                binding.income.isChecked = true
                binding.calendarViewBudget.visibility = View.VISIBLE
                binding.calendarViewCategory.visibility = View.GONE
                binding.categoryTitle.visibility = View.GONE
                binding.spinnerCategory.visibility = View.GONE
                binding.spinnerBudgetTo.visibility = View.GONE
                binding.budgetTitleTo.visibility = View.GONE
            } else {
                binding.budgetTitle.text = resources.getString(R.string.budget)
                binding.spinnerBudgetTo.visibility = View.GONE
                binding.budgetTitleTo.visibility = View.GONE
                binding.calendarViewBudget.visibility = View.GONE
                binding.calendarViewCategory.visibility = View.VISIBLE
                binding.categoryTitle.visibility = View.VISIBLE
                binding.spinnerCategory.visibility = View.VISIBLE
                binding.currencyNew.visibility = View.VISIBLE
                binding.translateValueNew.visibility = View.VISIBLE
                binding.equalSymbolNewExpence.visibility = View.VISIBLE
                binding.timeOfNotificationsTitle.visibility = View.INVISIBLE
                binding.timeOfNotifications.visibility = View.INVISIBLE
                binding.periodOfNotificationTitle.visibility = View.GONE
                binding.periodOfNotification.visibility = View.GONE

                if (financeViewModel.financeDate.value!!.first>Calendar.getInstance().get(Calendar.MONTH)+1||financeViewModel.financeDate.value!!.second>Calendar.getInstance().get(Calendar.YEAR)){
                    binding.buttonAddIncome.text = resources.getString(R.string.plan)
                    binding.periodOfNotificationTitle.visibility = View.VISIBLE
                    binding.periodOfNotification.visibility = View.VISIBLE

                    val resultList = mutableListOf<String>()
                    when{
                        financeViewModel.financeDate.value!!.second>Calendar.getInstance().get(Calendar.YEAR) -> resultList.addAll(periodList)
                        financeViewModel.financeDate.value!!.first-1 > Calendar.getInstance().get(Calendar.MONTH) -> {
                            for (i in 0 until 6){
                                resultList.add(periodList[i])
                            }
                            if(1==Calendar.getInstance().get(Calendar.DAY_OF_MONTH)){
                                resultList.add(periodList[6])
                            }
                        }
                    }
                    adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resultList)
                    adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.periodOfNotification.adapter = adapterPeriod
                }

                newCurrency =
                    financeViewModel.budgetLiveData.value!!.filter { it.budgetItem.name == binding.spinnerBudget.selectedItem.toString() }[0].budgetItem.currency
                binding.currencyExpence.setOnClickListener(null)
                binding.currencyNew.setOnClickListener(null)
                beginCurrency = baseCurrency
                binding.currencyNew.text = requireContext().resources.getString(
                    requireContext().resources.getIdentifier(
                        newCurrency,
                        "string",
                        requireContext().packageName
                    )
                )
                updateCurrency(true)
                checkAllFilledExpence()
            }
        }
    }

    private fun updateCurrency(expence:Boolean){
        if(beginCurrency == newCurrency){
            newCurrency = financeViewModel.budgetLiveData.value!!.filter { it.budgetItem.name == binding.spinnerBudget.selectedItem.toString()}[0].budgetItem.currency
        }
        if(!expence) {
            beginCurrency = financeViewModel.budgetLiveData.value!!.filter { it.budgetItem.name == binding.spinnerBudget.selectedItem.toString() }[0].budgetItem.currency
            binding.currencyExpence.text = requireContext().resources.getString(requireContext().resources.getIdentifier(financeViewModel.budgetLiveData.value!!.filter { it.budgetItem.name == binding.spinnerBudget.selectedItem.toString()}[0].budgetItem.currency, "string", requireContext().packageName))
        } else {
            binding.currencyExpence.text = requireContext().resources.getString(requireContext().resources.getIdentifier(beginCurrency, "string", requireContext().packageName))
        }
        if(newCurrency!=beginCurrency && newCurrency.isNotEmpty() && !expence){
            binding.currencyExpence.setOnClickListener(null)
            binding.currencyNew.setOnClickListener {
                findNavController().navigate(R.id.action_newTransactionFragment_to_currencyDialogFragment)
            }
        } else if (!expence){
            binding.currencyNew.setOnClickListener(null)
            binding.currencyExpence.setOnClickListener {
                findNavController().navigate(R.id.action_newTransactionFragment_to_currencyDialogFragment)
            }
        }
        else {
            binding.currencyNew.setOnClickListener(null)
            binding.currencyExpence.setOnClickListener(null)
        }

        textWatcher?.let { watcher->
            binding.translateValueNew.removeTextChangedListener(watcher)
            binding.savingsValue.removeTextChangedListener(watcher)
            binding.translateValueNew.text.clear()
            binding.savingsValue.text.clear()
        }
        convertValue()
        binding.savingsValue.clearFocus()
        binding.translateValueNew.clearFocus()
    }

    private fun checkAllFilledIncome(){
        binding.buttonAddIncome.isEnabled = binding.savingsValue.text.isNotEmpty()&&binding.spinnerBudget.selectedItemPosition!=-1
    }

    private fun checkAllFilledExpence(){
        binding.buttonAddIncome.isEnabled = binding.savingsValue.text.isNotEmpty()&&binding.spinnerCategory.selectedItemPosition!=-1&&binding.spinnerBudget.selectedItemPosition!=-1
    }

}

data class HistoryItem(var budgetId:String = "",
                       var placeId: String = "",
                       var isCategory:Boolean?=false,
                       var isGoal:Boolean?=false,
                       var isLoan:Boolean?=false,
                       var isSub:Boolean?=false,
                       var isTransfer:Boolean?=false,
                       var amount:String="",
                       var date:String = "",
                       var baseAmount:String = "",
                       var key:String = "")
