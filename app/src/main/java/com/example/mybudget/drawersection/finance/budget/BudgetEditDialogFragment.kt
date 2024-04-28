package com.example.mybudget.drawersection.finance.budget

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.HistoryItem
import com.example.mybudget.drawersection.finance.SharedViewModel
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BudgetEditDialogFragment:DialogFragment() {

    private var selection: Triple<String, String, String> = Triple("","", "")
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var currency: TextView
    private lateinit var budgetViewModel:FinanceViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.card_add_budget, null)
        val etName: EditText = dialogView.findViewById(R.id.nameBudgetNew)
        val tvName: TextView = dialogView.findViewById(R.id.titleAddBudget)
        val etAmount: EditText = dialogView.findViewById(R.id.amountNew)
        val tvAmount: TextView = dialogView.findViewById(R.id.amountAddBudget)
        val spinnerType: Spinner = dialogView.findViewById(R.id.typeNewBudget)
        currency = dialogView.findViewById(R.id.currencyNewBudget)

        val checkBox: CheckBox = dialogView.findViewById(R.id.checkBoxBasic)
        auth = Firebase.auth
        table = Firebase.database.reference
        builder.setView(dialogView)

        val adapterType = ArrayAdapter.createFromResource(requireContext(), R.array.budget_types, android.R.layout.simple_spinner_item)
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapterType
        val sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        budgetViewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
        var oldCurrency:String? = null
        sharedViewModel.dataToPass.value = null
        sharedViewModel.dataToPass.observe(this) { data ->
            if (data!=null && data.first.isNotEmpty()){
                currency.text = data.third
                if (requireArguments().getString("basicCurrency")==null){
                    etAmount.setText(changeCurrencyAmount(oldCurrency?:requireArguments().getString("currency")!!,
                        newCurrency = data.first,
                        newAmount = etAmount.text.toString(),
                        context = requireContext(),

                        ))
                }
                selection = data
                sharedViewModel.dataToPass.value = Triple("","","")
                oldCurrency = selection.first
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

            tvName.text = "Название"
            tvAmount.text = "Накопления"
            currency.text = currencySymbol
            checkBox.isChecked = base
            etName.setText(name)
            etAmount.setText(amount)

            currency.setOnClickListener { findNavController().navigate(R.id.action_budgetEditDialogFragment_to_currencyDialogFragment) }

            spinnerType.setSelection(adapterType.getPosition(type))

            builder.setPositiveButton("Сохранить") { dialog, _ ->
                if (etName.text.isNotEmpty() && etAmount.text.isNotEmpty()){
                    if (!base){
                            if(budgetViewModel.budgetLiveData.value?.filter { key!=it.key }?.all { it.budgetItem.name != etName.text.toString()} == false) Toast.makeText(context, "Счет с таким названием уже существует!", Toast.LENGTH_LONG).show()
                            else {
                                if (!checkBox.isChecked) {

                                        table.child("Users").child(auth.currentUser!!.uid)
                                            .child("Budgets").child("Other budget")
                                            .child(key!!)
                                            .setValue(
                                                _BudgetItem(
                                                    etName.text.toString(),
                                                    changeCurrencyAmount(currencyShort!!,
                                                        selection.first.ifEmpty { currencyShort },etAmount.text.toString(), /*key,*/ context),
                                                    spinnerType.selectedItem.toString(),
                                                    transaction?.toInt() ?: 0,
                                                    changeCurrency(currencyShort,
                                                        selection.first.ifEmpty { currencyShort }, key, context, false)))
                                } else {
                                        table.child("Users").child(auth.currentUser!!.uid)
                                            .child("Budgets").child("Base budget")
                                            .addListenerForSingleValueEvent(object :
                                                ValueEventListener {
                                                override fun onDataChange(snapshot: DataSnapshot) {
                                                    val baseOld =
                                                        snapshot.getValue(_BudgetItem::class.java)
                                                    if (baseOld != null) {

                                                        table.child("Users").child(auth.currentUser!!.uid).child("History").addListenerForSingleValueEvent(object : ValueEventListener{
                                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                                for (years in snapshot.children){
                                                                    for (months in years.children){
                                                                        for (historyItem in months.children){
                                                                            historyItem.getValue(HistoryItem::class.java)?.let {
                                                                                when (it.budgetId){
                                                                                    "Base budget" ->{
                                                                                        table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("budgetId").setValue(key)

                                                                                    }
                                                                                    key-> table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                                                        .child(years.key.toString()).child(months.key.toString()).child(it.key).child("budgetId").setValue("Base budget")
                                                                                    else ->{}
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }

                                                                table.child("Users").child(auth.currentUser!!.uid).child("Plan").addListenerForSingleValueEvent(object : ValueEventListener{
                                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                                        for (years in snapshot.children){
                                                                            for (months in years.children){
                                                                                for (historyItem in months.children){
                                                                                    historyItem.getValue(HistoryItem::class.java)?.let {
                                                                                        when (it.budgetId){
                                                                                            "Base budget" ->{
                                                                                                table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                                                                                    .child(years.key.toString()).child(months.key.toString()).child(it.key).child("budgetId").setValue(key)

                                                                                            }
                                                                                            key-> table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                                                                                .child(years.key.toString()).child(months.key.toString()).child(it.key).child("budgetId").setValue("Base budget")
                                                                                            else ->{}
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }


                                                                        table.child("Users")
                                                                            .child(auth.currentUser!!.uid)
                                                                            .child("Budgets").child("Base budget")
                                                                            .setValue(
                                                                                _BudgetItem(
                                                                                    etName.text.toString(),
                                                                                    changeCurrencyAmount(currencyShort!!,
                                                                                        selection.first.ifEmpty { currencyShort },etAmount.text.toString(), /*"Base budget",*/ context),
                                                                                    spinnerType.selectedItem.toString(),
                                                                                    transaction?.toInt() ?: 0,
                                                                                    changeCurrency(currencyShort,
                                                                                        selection.first.ifEmpty { currencyShort }, "Base budget", context, true)))
                                                                            .addOnCompleteListener {
                                                                                table.child("Users")
                                                                                    .child(auth.currentUser!!.uid)
                                                                                    .child("Budgets")
                                                                                    .child("Other budget")
                                                                                    .child(key!!)
                                                                                    .setValue(baseOld)
                                                                                    .addOnCompleteListener {
                                                                                        dialog.dismiss()
                                                                                    }
                                                                            }
                                                                    }
                                                                    override fun onCancelled(error: DatabaseError) {}}
                                                                )
                                                                }
                                                            override fun onCancelled(error: DatabaseError) {}}
                                                        )

                                                    }
                                                }

                                                override fun onCancelled(error: DatabaseError) {}
                                            })
                                }
                            }
                    }
                    else{
                        if(budgetViewModel.budgetLiveData.value?.filter { key!=it.key }?.all { it.budgetItem.name != etName.text.toString()} == false) Toast.makeText(context, "Счет с таким названием уже существует!", Toast.LENGTH_LONG).show()
                        else {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                .child("Base budget")
                                .setValue(
                                    _BudgetItem(
                                        etName.text.toString(),
                                        changeCurrencyAmount(currencyShort!!,
                                            selection.first.ifEmpty { currencyShort },etAmount.text.toString(), /*"Base budget",*/ context),
                                        spinnerType.selectedItem.toString(),
                                        transaction?.toInt() ?: 0,
                                        changeCurrency(currencyShort,
                                            selection.first.ifEmpty { currencyShort }, "Base budget", context, true))
                                ).addOnCompleteListener {
                                    dialog.dismiss()
                                }
                        }
                    }
                } else Toast.makeText(context, "Вы заполнили не все данные", Toast.LENGTH_LONG).show()
            }

            builder.setNegativeButton("Удалить") { dialog, _ ->
                if (base) Toast.makeText(context, "Нельзя удалить основной бюджет!", Toast.LENGTH_LONG).show()
                else if(budgetViewModel.planLiveData.value!!.any { it.budgetId == key})Toast.makeText(context, "У Вас есть запланированные операции с этим бюджетом!", Toast.LENGTH_LONG).show()
                else {
                    AlertDialog.Builder(context)
                        .setTitle("Удаление бюджета")
                        .setMessage("Вы уверены, что хотите удалить бюджет?")
                        .setPositiveButton("Подтвердить") { dialog2, _ ->
                            if (name == etName.text.toString() && etName.text.isNotEmpty() && etAmount.text.isNotEmpty()) {
                                table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                    .child("Other budget").child(key!!).child("deleted").setValue(true)
                                    .addOnCompleteListener {
                                        dialog.dismiss()
                                    }
                            } else when {
                                name != etName.text.toString() -> Toast.makeText(
                                    context,
                                    "Вы собираетесь удалить отредактированный бюджет",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            dialog2.dismiss()
                            dialog.dismiss()
                        }
                        .setNegativeButton("Отмена") { dialog2, _ ->
                            dialog2.dismiss()
                        }.show()
                }
            }

            builder.setNeutralButton("Отмена") { dialog, _ ->
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
            builder.setPositiveButton("Добавить") { dialog, _ ->
                if (etName.text.isNotEmpty() && etAmount.text.isNotEmpty() && financeViewModel.budgetLiveData.value!!.none {it.budgetItem.name == etName.text.toString()}){
                    if (!checkBox.isChecked){
                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").push().setValue(
                        _BudgetItem(etName.text.toString(), etAmount.text.toString(),  spinnerType.selectedItem.toString(), 0, selection.first.ifEmpty { currencyShort!!})).
                    addOnCompleteListener {
                        dialog.dismiss()}
                    }
                    else{
                        lifecycleScope.launch(Dispatchers.IO){
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val baseOld = snapshot.getValue(_BudgetItem::class.java)
                                        if (baseOld!=null){
                                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").setValue(
                                                _BudgetItem(etName.text.toString(), etAmount.text.toString(),  spinnerType.selectedItem.toString(), 0, selection.first.ifEmpty { currencyShort!!}))
                                                .addOnCompleteListener {
                                                    table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").push().setValue(baseOld).addOnCompleteListener {
                                                        dialog.dismiss()}
                                                }
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}

                                })
                        }
                    }
                } else when{
                    !financeViewModel.budgetLiveData.value!!.none {it.budgetItem.name == etName.text.toString()}-> {
                        val deletedBudget = financeViewModel.budgetLiveData.value!!.find { it.budgetItem.name == etName.text.toString()}!!
                        when(deletedBudget.budgetItem.isDeleted){
                            true->{
                                AlertDialog.Builder(context)
                                    .setTitle("Восстановление бюджета")
                                    .setMessage("У Вас уже был бюджет с таким названием.\nХотите его восстановить?")
                                    .setPositiveButton("Да") { dialog2, _ ->
                                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(deletedBudget.key).child("deleted").setValue(false)
                                        dialog2.dismiss()
                                        }
                                    .setNegativeButton("Нет") { dialog2, _ ->
                                        dialog2.dismiss()
                                    }.show()


                            }
                            else-> Toast.makeText(
                                context,
                                "Счет с таким названием уже существует!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    etName.text.isEmpty() || etAmount.text.isEmpty() -> Toast.makeText(context, "Вы ввели не все данные.", Toast.LENGTH_LONG).show()}
            }
            builder.setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog

    }

    private fun changeCurrencyAmount(oldCurrency: String, newCurrency: String,newAmount:String, /*budgetKey: String, */context: Context):String{
        /*if(newAmount==budgetViewModel.budgetLiveData.value?.find { it.key == budgetKey }!!.budgetItem.amount && newCurrency!=newAmount){*/
            val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
            if(currencyConvertor!=null){
                return when (oldCurrency){
                    currencyConvertor.baseCode->{
                        String.format("%.2f", newAmount.toDouble()/*budgetViewModel.budgetLiveData.value?.find {budgetList-> budgetList.key == budgetKey }!!.budgetItem.amount.toDouble()*/*currencyConvertor.conversionRates[newCurrency]!!).replace(',','.')
                    }
                    else->{
                        String.format("%.2f", newAmount.toDouble()/*budgetViewModel.budgetLiveData.value?.find {budgetList-> budgetList.key == budgetKey }!!.budgetItem.amount.toDouble()*/*currencyConvertor.conversionRates[newCurrency]!!/currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.')
                    }
                }
            }
       /* }*/
        return newAmount
    }
    private fun changeCurrency(oldCurrency:String, newCurrency:String, budgetKey:String, context: Context, baseChanged:Boolean):String{
        val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
        if(oldCurrency!=newCurrency){
            if(currencyConvertor!=null){
                when (oldCurrency){
                    currencyConvertor.baseCode->{
                        if(newCurrency == budgetViewModel.budgetLiveData.value?.find { it.key=="Base budget" }!!.budgetItem.currency){
                            table.child("Users").child(auth.currentUser!!.uid).child("History").addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (years in snapshot.children){
                                        for (months in years.children){
                                            for (historyItem in months.children){
                                                historyItem.getValue(HistoryItem::class.java)?.let {
                                                    if (it.budgetId == budgetKey){
                                                        table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("amount").setValue(budgetViewModel.historyLiveData.value?.find {historyList-> historyList.key == it.key }!!.baseAmount)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}}
                            )

                            table.child("Users").child(auth.currentUser!!.uid).child("Plan").addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (years in snapshot.children){
                                        for (months in years.children){
                                            for (historyItem in months.children){
                                                historyItem.getValue(HistoryItem::class.java)?.let {
                                                    if (it.budgetId == budgetKey){
                                                        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("amount").setValue(budgetViewModel.planLiveData.value?.find {planList-> planList.key == it.key }!!.baseAmount)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}}
                            )
                        }
                        else{
                            table.child("Users").child(auth.currentUser!!.uid).child("History").addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (years in snapshot.children){
                                        for (months in years.children){
                                            for (historyItem in months.children){
                                                historyItem.getValue(HistoryItem::class.java)?.let {
                                                    if (it.budgetId == budgetKey){
                                                        table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("amount").setValue(String.format("%.2f", currencyConvertor.conversionRates[newCurrency]!!*it.amount.toDouble()/currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.'))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}}
                            )

                            table.child("Users").child(auth.currentUser!!.uid).child("Plan").addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (years in snapshot.children){
                                        for (months in years.children){
                                            for (historyItem in months.children){
                                                historyItem.getValue(HistoryItem::class.java)?.let {
                                                    if (it.budgetId == budgetKey){
                                                        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("amount").setValue(String.format("%.2f", currencyConvertor.conversionRates[newCurrency]!!*it.amount.toDouble()/currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.')).addOnCompleteListener {
                                                            }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}}
                            )
                        }
                    }

                    else->{
                        if(newCurrency == budgetViewModel.budgetLiveData.value?.find { it.key=="Base budget" }!!.budgetItem.currency){
                            table.child("Users").child(auth.currentUser!!.uid).child("History").addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (years in snapshot.children){
                                        for (months in years.children){
                                            for (historyItem in months.children){
                                                historyItem.getValue(HistoryItem::class.java)?.let {
                                                    if (it.budgetId == budgetKey){

                                                        table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("amount").setValue(budgetViewModel.historyLiveData.value?.find {historyList-> historyList.key == it.key }!!.baseAmount)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}}
                            )

                            table.child("Users").child(auth.currentUser!!.uid).child("Plan").addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (years in snapshot.children){
                                        for (months in years.children){
                                            for (historyItem in months.children){
                                                historyItem.getValue(HistoryItem::class.java)?.let {
                                                    if (it.budgetId == budgetKey){
                                                        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("amount").setValue(budgetViewModel.planLiveData.value?.find {planList-> planList.key == it.key }!!.baseAmount)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}}
                            )

                        }
                        else{
                            table.child("Users").child(auth.currentUser!!.uid).child("History").addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (years in snapshot.children){
                                        for (months in years.children){
                                            for (historyItem in months.children){
                                                historyItem.getValue(HistoryItem::class.java)?.let { history->
                                                    if (history.budgetId == budgetKey){
                                                        table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                            .child(years.key.toString()).child(months.key.toString()).child(history.key).child("amount").setValue(String.format("%.2f", currencyConvertor.conversionRates[newCurrency]!!*history.amount.toDouble()/currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.')).addOnCompleteListener {
                                                                if(history.budgetId=="Base budget"){
                                                                    table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                                        .child(years.key.toString()).child(months.key.toString()).child(history.key).child("baseAmount").setValue(String.format("%.2f", currencyConvertor.conversionRates[newCurrency]!!*history.amount.toDouble()/currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.'))
                                                                }
                                                            }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}}
                            )

                            table.child("Users").child(auth.currentUser!!.uid).child("Plan").addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (years in snapshot.children){
                                        for (months in years.children){
                                            for (historyItem in months.children){
                                                historyItem.getValue(HistoryItem::class.java)?.let { history->
                                                    if (history.budgetId == budgetKey){
                                                        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                                            .child(years.key.toString()).child(months.key.toString()).child(history.key).child("amount").setValue(String.format("%.2f", currencyConvertor.conversionRates[newCurrency]!!*history.amount.toDouble()/currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.')).addOnCompleteListener {
                                                                if(history.budgetId=="Base budget"){
                                                                    table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                                                        .child(years.key.toString()).child(months.key.toString()).child(history.key).child("baseAmount").setValue(String.format("%.2f", currencyConvertor.conversionRates[newCurrency]!!*history.amount.toDouble()/currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.'))
                                                                }
                                                            }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}}
                            )
                        }
                    }
                }
            }
        }

        if(budgetKey == "Base budget" && currencyConvertor!=null && baseChanged){
            val oldCurrency2 = budgetViewModel.budgetLiveData.value?.find { it.key=="Base budget" }!!.budgetItem.currency
            when (oldCurrency2){
                currencyConvertor.baseCode->{
                        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (years in snapshot.children) {
                                        for (months in years.children) {
                                            for (type in months.children) {
                                                for (categoryItem in type.children) {
                                                    categoryItem.getValue(_CategoryItem::class.java)
                                                        ?.let {
                                                            val categoryItemNew = it
                                                            when (categoryItemNew.remainder) {
                                                                "0" -> {
                                                                    categoryItemNew.total =
                                                                        String.format(
                                                                            "%.2f",
                                                                            currencyConvertor.conversionRates[newCurrency]!! * categoryItemNew.total.toDouble()
                                                                        ).replace(',', '.')
                                                                }

                                                                else -> {
                                                                    categoryItemNew.total =
                                                                        String.format(
                                                                            "%.2f",
                                                                            currencyConvertor.conversionRates[newCurrency]!! * categoryItemNew.total.toDouble()
                                                                        ).replace(',', '.')
                                                                    categoryItemNew.remainder =
                                                                        String.format(
                                                                            "%.2f",
                                                                            currencyConvertor.conversionRates[newCurrency]!! * categoryItemNew.remainder.toDouble()
                                                                        ).replace(',', '.')
                                                                }
                                                            }
                                                            table.child("Users")
                                                                .child(auth.currentUser!!.uid)
                                                                .child("Categories")
                                                                .child(years.key.toString())
                                                                .child(months.key.toString())
                                                                .child(type.key.toString())
                                                                .child(categoryItem.key!!)
                                                                .setValue(it)
                                                        }
                                                }
                                            }
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            }
                            )

                    table.child("Users").child(auth.currentUser!!.uid).child("History").addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (years in snapshot.children){
                                for (months in years.children){
                                    for (historyItem in months.children){
                                        historyItem.getValue(HistoryItem::class.java)?.let {
                                                if(it.isCategory==true){
                                                    table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                    .child(years.key.toString()).child(months.key.toString()).child(it.key).child("baseAmount").setValue(
                                                        when(it.budgetId){
                                                            "Base budget" -> it.amount
                                                            else ->
                                                        String.format("%.2f", currencyConvertor.conversionRates[newCurrency]!!*it.baseAmount.toDouble()/*/currencyConvertor.conversionRates[oldCurrency2]!!*/).replace(',','.')})
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}}
                    )

                    table.child("Users").child(auth.currentUser!!.uid).child("Plan").addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (years in snapshot.children){
                                for (months in years.children){
                                    for (historyItem in months.children){
                                        historyItem.getValue(HistoryItem::class.java)?.let {
                                            if(it.isCategory==true) {
                                                table.child("Users").child(auth.currentUser!!.uid)
                                                    .child("Plan")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString()).child(it.key)
                                                    .child("baseAmount").setValue(
                                                        when (it.budgetId) {
                                                            "Base budget" -> it.amount
                                                            else ->
                                                                String.format(
                                                                    "%.2f",
                                                                    currencyConvertor.conversionRates[newCurrency]!! * it.baseAmount.toDouble()/*/currencyConvertor.conversionRates[oldCurrency2]!!*/
                                                                ).replace(',', '.')
                                                        }
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}}
                    )
                }

                else-> {
                    table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (years in snapshot.children) {
                                    for (months in years.children) {
                                        for (type in months.children) {
                                            for (categoryItem in type.children) {
                                                categoryItem.getValue(_CategoryItem::class.java)
                                                    ?.let {
                                                        when (it.remainder) {
                                                            "0" -> {
                                                                it.total = String.format(
                                                                    "%.2f",
                                                                    currencyConvertor.conversionRates[newCurrency]!! * it.total.toDouble() / currencyConvertor.conversionRates[oldCurrency2]!!
                                                                ).replace(',', '.')
                                                            }

                                                            else -> {
                                                                it.total = String.format(
                                                                    "%.2f",
                                                                    currencyConvertor.conversionRates[newCurrency]!! * it.total.toDouble() / currencyConvertor.conversionRates[oldCurrency2]!!
                                                                ).replace(',', '.')
                                                                it.remainder =
                                                                    String.format(
                                                                        "%.2f",
                                                                        currencyConvertor.conversionRates[newCurrency]!! * it.remainder.toDouble() / currencyConvertor.conversionRates[oldCurrency2]!!
                                                                    ).replace(',', '.')
                                                            }
                                                        }
                                                        table.child("Users")
                                                            .child(auth.currentUser!!.uid)
                                                            .child("Categories")
                                                            .child(years.key.toString())
                                                            .child(months.key.toString())
                                                            .child("ExpenseCategories")
                                                            .child(categoryItem.key!!)
                                                            .setValue(it)
                                                    }
                                            }
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        }
                        )

                    table.child("Users").child(auth.currentUser!!.uid).child("History").addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (years in snapshot.children){
                                for (months in years.children){
                                    for (historyItem in months.children){
                                        historyItem.getValue(HistoryItem::class.java)?.let {
                                            if (it.isCategory == true) {
                                                table.child("Users").child(auth.currentUser!!.uid)
                                                    .child("History")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString()).child(it.key)
                                                    .child("baseAmount").setValue(
                                                        when (it.budgetId) {
                                                            "Base budget" -> it.amount
                                                            else -> String.format(
                                                                "%.2f",
                                                                currencyConvertor.conversionRates[newCurrency]!! * it.baseAmount.toDouble() / currencyConvertor.conversionRates[oldCurrency2]!!
                                                            ).replace(',', '.')
                                                        }
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}}
                    )

                    table.child("Users").child(auth.currentUser!!.uid).child("Plan").addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (years in snapshot.children){
                                for (months in years.children){
                                    for (historyItem in months.children){
                                        historyItem.getValue(HistoryItem::class.java)?.let {
                                            if(it.isCategory==true){
                                                table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                                    .child(years.key.toString()).child(months.key.toString()).child(it.key).child("baseAmount").setValue(
                                                        when(it.budgetId){
                                                        "Base budget" -> it.amount
                                                        else ->String.format("%.2f", currencyConvertor.conversionRates[newCurrency]!!*it.baseAmount.toDouble()/currencyConvertor.conversionRates[oldCurrency2]!!).replace(',','.')}
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}}
                    )

                }
            }
        }
        return newCurrency
    }
}

