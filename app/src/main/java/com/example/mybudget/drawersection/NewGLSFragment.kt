package com.example.mybudget.drawersection

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.ExchangeRateResponse
import com.example.mybudget.NotificationManager
import com.example.mybudget.R
import com.example.mybudget.databinding.PageNewGlsBinding
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.HistoryItem
import com.example.mybudget.drawersection.finance.IconsChooserAlertDialog
import com.example.mybudget.drawersection.finance.SharedViewModel
import com.example.mybudget.drawersection.goals.GoalItem
import com.example.mybudget.drawersection.goals.GoalItemWithKey
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
import java.util.Calendar
import java.util.Locale


class NewGLSFragment : Fragment() {
    private lateinit var binding: PageNewGlsBinding
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var periodList: Array<String>
    private lateinit var adapterPeriod: ArrayAdapter<String>
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private var currencyConvertor: ExchangeRateResponse? = null
    private var selection: Triple<String, String, String> = Triple("","", "")

    private var key: String? = null
    private var type: String? = null
    private var dateOfEnd: Calendar = Calendar.getInstance().also { it.set(Calendar.DAY_OF_MONTH, it.get(Calendar.DAY_OF_MONTH)+1) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_new_gls, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = PageNewGlsBinding.bind(view)

        activity?.let {financeViewModel = ViewModelProvider(it)[FinanceViewModel::class.java]}
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

        initPeriod()
        initCalendar()

        when(key){
            null->{
                binding.currencyGLS.text = requireContext().resources.getString(requireContext().resources.getIdentifier(financeViewModel.budgetLiveData.value?.find { it.key == "Base budget" }!!.budgetItem.currency, "string", requireContext().packageName))
                binding.buttonAddGLS.text = requireContext().resources.getString(R.string.add)
                when (type) {
                    "goal"->{
                        initGoal()
                        setOnSelectionChanged()
                        binding.buttonAddGLS.setOnClickListener { saveGoal() }
                    }
                    "loan"->{}
                    "sub"->{}
                }
            }
            else->{
                binding.buttonAddGLS.text = requireContext().resources.getString(R.string.save)
                when (type) {
                    "goal"->{
                        initGoal()
                        setOnSelectionChanged()
                        fillAllGoal(financeViewModel.goalsData.value!!.find { it.key == key }!!)
                        val sharedPreferences = requireContext().getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
                        val periodBegin = sharedPreferences.getString(key, "|")?.split("|")?.get(0)?:periodList[0]
                        val timeBegin = sharedPreferences.getString(key, "|")?.split("|")?.get(1)?:"12:00"
                        val day = dateOfEnd
                        financeViewModel.goalsData.value!!.find { it.key == key }!!.goalItem.date?.split(".")?.let {
                                day.apply {
                                    val resultList = mutableListOf<String>()
                                    when{
                                        it[2].toInt() > Calendar.getInstance().get(Calendar.YEAR) -> resultList.addAll(periodList)
                                        it[1].toInt()-1  > Calendar.getInstance().get(Calendar.MONTH) -> {
                                            for (i in 0 until 6){
                                                resultList.add(periodList[i])
                                            }
                                            if(it[0].toInt()>=Calendar.getInstance().get(Calendar.MONTH)){
                                                resultList.add(periodList[6])
                                            }
                                        }
                                        it[0].toInt() > Calendar.getInstance().get(Calendar.DAY_OF_MONTH)->{
                                            if (it[0].toInt() - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)>7){
                                                for (i in 0 until 6){
                                                    resultList.add(periodList[i])
                                                }
                                            } else if (it[0].toInt() - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)==7){
                                                for (i in 0 until 5){
                                                    resultList.add(periodList[i])
                                                }
                                            }else if (it[0].toInt() - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)>=3){
                                                for (i in 0 until 3){
                                                    resultList.add(periodList[i])
                                                }
                                                resultList.add(periodList[4])
                                            }
                                            else {
                                                resultList.add(periodList[0])
                                                resultList.add(periodList[1])
                                                resultList.add(periodList[4])
                                            }
                                        }
                                    }

                                    adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resultList)
                                    adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                    binding.periodOfNotificationGLS.adapter = adapterPeriod
                                    binding.periodOfNotificationGLS.setSelection(resultList.indexOf(periodBegin.ifEmpty {
                                        periodList[0]
                                    }))
                                    binding.timeOfNotificationsGLS.text = timeBegin.ifEmpty { "12:00" }

                                set(Calendar.YEAR, it[2].toInt())
                                set(Calendar.MONTH, it[1].toInt()-1)
                                set(Calendar.DAY_OF_MONTH, it[0].toInt())
                            }

                            binding.periodOfNotificationGLS.onItemSelectedListener = object :
                                AdapterView.OnItemSelectedListener {
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
                        binding.calendarViewGLS.date = day.timeInMillis
                        dateOfEnd = day
                        binding.buttonAddGLS.setOnClickListener { updateGoal(requireContext()) }
                    }
                    "loan"->{}
                    "sub"->{}
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
        var oldCurrency = financeViewModel.goalsData.value?.find { it.key == key }?.goalItem?.currency
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
    }

    private fun initPeriod(){
        periodList = requireContext().resources.getStringArray(R.array.periodicity)
        adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periodList)
        adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.periodOfNotificationGLS.adapter = adapterPeriod
        binding.periodOfNotificationGLS.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
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

    private fun changeCurrencyAmount(oldCurrency: String, newCurrency: String,newAmount:String, context: Context):String{
        currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
        if(currencyConvertor!=null && oldCurrency!=newCurrency){
            return when (oldCurrency){
                currencyConvertor!!.baseCode->{
                    String.format("%.2f", newAmount.toDouble()* currencyConvertor!!.conversionRates[newCurrency]!!).replace(',','.')
                }
                else->{
                    String.format("%.2f", newAmount.toDouble()* currencyConvertor!!.conversionRates[newCurrency]!!/ currencyConvertor!!.conversionRates[oldCurrency]!!).replace(',','.')
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
            if(dateOfEnd>Calendar.getInstance()){
                val resultList = mutableListOf<String>()
                when{
                    year>Calendar.getInstance().get(Calendar.YEAR) -> resultList.addAll(periodList)
                    month > Calendar.getInstance().get(Calendar.MONTH) -> {
                        for (i in 0 until 6){
                            resultList.add(periodList[i])
                        }
                        if(dayOfMonth>=Calendar.getInstance().get(Calendar.MONTH)){
                            resultList.add(periodList[6])
                        }
                    }
                    dayOfMonth > Calendar.getInstance().get(Calendar.DAY_OF_MONTH)->{
                        if (dayOfMonth - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)>7){
                            for (i in 0 until 6){
                                resultList.add(periodList[i])
                            }
                        } else if (dayOfMonth - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)==7){
                            for (i in 0 until 5){
                                resultList.add(periodList[i])
                            }

                        }else if (dayOfMonth - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)>=3){
                            for (i in 0 until 3){
                                resultList.add(periodList[i])
                            }
                            resultList.add(periodList[4])
                        }
                        else {
                            resultList.add(periodList[0])
                            resultList.add(periodList[1])
                            resultList.add(periodList[4])
                        }
                    }
                }
                adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resultList)
                adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.periodOfNotificationGLS.adapter = adapterPeriod
            }
        }
    }

    private fun setOnSelectionChanged(){
        binding.radioGroupGLS.setOnCheckedChangeListener { _, _ ->
            if (binding.withouthDate.isChecked){
                binding.calendarViewGLS.visibility = View.GONE
                adapterPeriod = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periodList)
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
            checkAllFieldsGoal()
        }
    }

    //FB
    private fun saveGoal(){

        if (financeViewModel.goalsData.value?.filter { !it.goalItem.isDeleted }?.all{it.goalItem.name !=  binding.nameGLSEdit.text.toString()}==false) Snackbar.make(binding.buttonAddGLS, "Такая цель уже существует!", Snackbar.LENGTH_LONG).show()
        else if (financeViewModel.goalsData.value?.filter { it.goalItem.isDeleted }?.all{it.goalItem.name !=  binding.nameGLSEdit.text.toString()}==false){
            AlertDialog.Builder(context)
                .setTitle("Восстановление")
                .setMessage("У Вас уже была такая цель!\nХотите восстановить?")
                .setPositiveButton("Восстановить") { dialog, _ ->
                    table.child("Users")
                        .child(auth.currentUser!!.uid)
                        .child("Goals")
                        .child(financeViewModel.goalsData.value!!.find { it.goalItem.name ==  binding.nameGLSEdit.text.toString()}!!.key)
                        .child("deleted").setValue(false)
                    findNavController().popBackStack()
                    dialog.dismiss()
                }
                .setNegativeButton("Создать новую") { dialog, _ ->
                    makeNewGoal()
                    dialog.dismiss()
                }
                .setNeutralButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
        else makeNewGoal()
    }

    private fun updateGoal(context: Context){
        if (financeViewModel.goalsData.value?.filter { !it.goalItem.isDeleted  && it.key != key}?.all{it.goalItem.name !=  binding.nameGLSEdit.text.toString()}==false) Snackbar.make(binding.buttonAddGLS, "Цель с таким названием уже существует!", Snackbar.LENGTH_LONG).show()
        else{
            val beginItem = financeViewModel.goalsData.value?.find { it.key == key }!!.goalItem
            table.child("Users")
                .child(auth.currentUser!!.uid)
                .child("Goals")
                .child(key!!)
                .setValue(
                    GoalItem(
                        name = binding.nameGLSEdit.text.toString(),
                        target = binding.glsValue.text.toString(),
                        current = changeCurrencyAmount(beginItem.currency,
                            selection.first.ifEmpty { beginItem.currency },
                            beginItem.current,
                            context),
                        currency = selection.first.ifEmpty { financeViewModel.goalsData.value?.find { it.key == key }!!.goalItem.currency },
                        date =  if (binding.withouthDate.isChecked) null else SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfEnd.time),
                        path = binding.imageOfGLM.tag.toString(),
                        isDeleted = false)
                )
            NotificationManager.cancelAlarmManager(context, key!!)
            if (binding.periodOfNotificationGLS.selectedItemPosition!=-1){
                NotificationManager.notification(
                    context = requireContext(),
                    channelID = Constants.CHANNEL_ID_GOAL,
                    id = key!!,
                    time = binding.timeOfNotificationsGLS.text.toString(),
                    dateOfExpence = dateOfEnd,
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

    private fun makeNewGoal(){
        val goalReference = table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Goals")
            .push()
        goalReference.setValue(GoalItem(
                name = binding.nameGLSEdit.text.toString(),
                target = binding.glsValue.text.toString(),
                currency = selection.first.ifEmpty { financeViewModel.budgetLiveData.value?.find { it.key == "Base budget" }!!.budgetItem.currency },
                date = if (binding.withouthDate.isChecked) null else SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfEnd.time),
                path = binding.imageOfGLM.tag.toString(),
                isDeleted = false)
            ).addOnCompleteListener {
                if (binding.periodOfNotificationGLS.selectedItemPosition!=-1){
                    NotificationManager.notification(
                        context = requireContext(),
                        channelID = Constants.CHANNEL_ID_GOAL,
                        id = goalReference.key.toString(),
                        time = binding.timeOfNotificationsGLS.text.toString(),
                        dateOfExpence = dateOfEnd,
                        periodOfNotification = binding.periodOfNotificationGLS.selectedItem.toString()
                    )
                }
                findNavController().popBackStack()
            }
    }



    private fun checkAllFieldsGoal(){
        binding.buttonAddGLS.isEnabled = binding.glsValue.text.isNotEmpty() && binding.nameGLS.text.isNotEmpty() && binding.imageOfGLM.tag!=null
    }

    private fun checkAllFieldsLoan(){
        //buttonAddGLS.isEnabled = editValue.text.isNotEmpty() && nameGLS.text.isNotEmpty() && imageOfGLM.tag!=null
    }

    private fun checkAllFieldsSub(){
        //buttonAddGLS.isEnabled = editValue.text.isNotEmpty() && nameGLS.text.isNotEmpty() && imageOfGLM.tag!=null
    }
}