package com.example.mybudget.drawersection.finance.budget

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.example.mybudget.BudgetNotificationManager
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.history.HistoryItem
import com.example.mybudget.drawersection.finance.SelectedBudgetViewModel
import com.example.mybudget.drawersection.finance.SharedViewModel
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.example.mybudget.drawersection.subs.SubItem
import com.example.mybudget.start_pages.Constants
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
import java.util.Calendar

class BudgetEditDialogFragment:DialogFragment() {

    private var selection: Triple<String, String, String> = Triple("","", "")
    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference
    private lateinit var currency: TextView
    private lateinit var financeViewModel:FinanceViewModel
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
        auth = Firebase.auth
        table = Firebase.database.reference
        builder.setView(dialogView)

        val adapterType = ArrayAdapter.createFromResource(requireContext(), R.array.budget_types, android.R.layout.simple_spinner_item)
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapterType
        val sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        financeViewModel = ViewModelProvider(requireActivity())[FinanceViewModel::class.java]
        selectedBudgetViewModel = ViewModelProvider(requireActivity())[SelectedBudgetViewModel::class.java]
        var oldCurrency:String? = null
        sharedViewModel.dataToPass.value = null
        sharedViewModel.dataToPass.observe(this) { data ->
            if (data!=null && data.first.isNotEmpty()){
                currency.text = data.third
                if (requireArguments().getString("basicCurrency")==null){
                    etAmount.setText(changeCurrencyAmount(
                        oldCurrency = oldCurrency?:requireArguments().getString("currency")!!,
                        newCurrency = data.first,
                        newAmount = etAmount.text.toString(),
                        context = requireContext()))
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

                                        table.child("Users").child(auth.currentUser!!.uid)
                                            .child("Budgets").child("Other budget")
                                            .child(key!!)
                                            .setValue(
                                                _BudgetItem(
                                                    etName.text.toString(),
                                                    etAmount.text.toString(),
                                                    spinnerType.selectedItem.toString(),
                                                    transaction?.toInt() ?: 0,
                                                    changeCurrency(currencyShort!!,
                                                        selection.first.ifEmpty { currencyShort }, key, context,
                                                        baseChanged = false,
                                                        checkBox = false,
                                                        selection.first.isNotEmpty(),
                                                        null)))
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
                                                                            historyItem.getValue(
                                                                                HistoryItem::class.java)?.let {
                                                                                    when (it.budgetId){
                                                                                        "Base budget" ->{
                                                                                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                                                                .child(years.key.toString()).child(months.key.toString()).child(it.key).child("budgetId").setValue(key)
                                                                                        }
                                                                                        key-> {
                                                                                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("budgetId").setValue("Base budget")
                                                                                        }
                                                                                        else ->{}
                                                                                    }
                                                                                if (it.isTransfer == true ){
                                                                                    when (it.placeId){
                                                                                        "Base budget" ->{
                                                                                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                                                                .child(years.key.toString()).child(months.key.toString()).child(it.key).child("placeId").setValue(key)
                                                                                        }
                                                                                        key-> {
                                                                                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                                                                .child(years.key.toString()).child(months.key.toString()).child(it.key).child("placeId").setValue("Base budget")
                                                                                        }
                                                                                        else ->{}
                                                                                    }
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
                                                                                    historyItem.getValue(
                                                                                        HistoryItem::class.java)?.let {
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

                                                                        table.child("Users").child(auth.currentUser!!.uid).child("Subs").addListenerForSingleValueEvent(object : ValueEventListener{
                                                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                                                for (sub in snapshot.children){
                                                                                    sub.getValue(SubItem::class.java)?.let {
                                                                                        when (it.budgetId){
                                                                                            "Base budget"->{
                                                                                                table.child("Users")
                                                                                                    .child(auth.currentUser!!.uid)
                                                                                                    .child("Subs")
                                                                                                    .child(sub.key.toString())
                                                                                                    .child("budgetId")
                                                                                                    .setValue(key)
                                                                                            }
                                                                                            key->{table.child("Users")
                                                                                                .child(auth.currentUser!!.uid)
                                                                                                .child("Subs")
                                                                                                .child(sub.key.toString())
                                                                                                .child("budgetId")
                                                                                                .setValue("Base budget")
                                                                                            }
                                                                                            else->{}
                                                                                        }
                                                                                    }
                                                                                }
                                                                                table.child("Users")
                                                                                    .child(auth.currentUser!!.uid)
                                                                                    .child("Budgets").child("Base budget")
                                                                                    .setValue(
                                                                                        _BudgetItem(
                                                                                            etName.text.toString(),
                                                                                            etAmount.text.toString(),
                                                                                            spinnerType.selectedItem.toString(),
                                                                                            transaction?.toInt() ?: 0,
                                                                                            changeCurrency(currencyShort!!,
                                                                                                selection.first.ifEmpty { currencyShort }, "Base budget", context,
                                                                                                baseChanged = true,
                                                                                                checkBox = true,
                                                                                                selection.first.isNotEmpty(),
                                                                                                baseOld.currency)))
                                                                                    .addOnCompleteListener {
                                                                                        table.child("Users")
                                                                                            .child(auth.currentUser!!.uid)
                                                                                            .child("Budgets")
                                                                                            .child("Other budget")
                                                                                            .child(key!!)
                                                                                            .setValue(baseOld)
                                                                                            .addOnCompleteListener {

                                                                                                selectedBudgetViewModel.selectedBudget.value?.map{
                                                                                                    when (it){
                                                                                                        key -> "Base budget"
                                                                                                        "Base budget" -> key
                                                                                                        else -> it
                                                                                                    }
                                                                                                }?.let { newSelection ->
                                                                                                        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("sumOfFinance", newSelection.toSet()).apply()
                                                                                                        selectedBudgetViewModel.updateSelectionData(newSelection)
                                                                                                    }
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
                        if(financeViewModel.budgetLiveData.value?.filter { key!=it.key }?.all { it.budgetItem.name != etName.text.toString()} == false) Toast.makeText(context, "Счет с таким названием уже существует!", Toast.LENGTH_LONG).show()
                        else {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                .child("Base budget")
                                .setValue(
                                    _BudgetItem(
                                        etName.text.toString(),
                                        etAmount.text.toString(),
                                        spinnerType.selectedItem.toString(),
                                        transaction?.toInt() ?: 0,
                                        changeCurrency(currencyShort!!,
                                            selection.first.ifEmpty { currencyShort }, "Base budget", context,
                                            baseChanged = true,
                                            checkBox = false,
                                            selection.first.isNotEmpty(),
                                            null))
                                ).addOnCompleteListener {
                                    dialog.dismiss()
                                }
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
                                table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                    .child("Other budget").child(key!!).child("deleted").setValue(true)
                                    .addOnCompleteListener {
                                        deletePlansAndSubs(budgetKey = key, context = context)
                                        dialog.dismiss()

                                    }
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
                                    .setTitle(resources.getString(R.string.repair_budget))
                                    .setMessage(resources.getString(R.string.repair_budget_ask))
                                    .setPositiveButton(resources.getString(R.string.yes)) { dialog2, _ ->
                                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(deletedBudget.key).child("deleted").setValue(false).addOnCompleteListener {
                                            restoreSubs(deletedBudget.key, contextForRestore)
                                            dialog2.dismiss()
                                            }
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

    private fun deletePlansAndSubs(budgetKey: String, context: Context){
        financeViewModel.subLiveData.value?.filter { it.subItem.budgetId == budgetKey }?.forEach { subItemWithKey ->
            BudgetNotificationManager.cancelAlarmManager(
                context = context,
                id = subItemWithKey.key,
                deletePrefs = false
            )
            BudgetNotificationManager.cancelAutoTransaction(
                context = context,
                id = subItemWithKey.key
            )
        }

        financeViewModel.planLiveData.value?.filter { it.budgetId == budgetKey }?.forEach { planItem ->
            BudgetNotificationManager.cancelAlarmManager(
                context = context,
                id = planItem.key
            )
            BudgetNotificationManager.cancelAutoTransaction(
                context = context,
                id = planItem.key
            )
        }

        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (years in snapshot.children){
                        for (months in years.children){
                            for (historyItem in months.children){
                                historyItem.getValue(HistoryItem::class.java)?.let { history->
                                    if (history.budgetId == budgetKey){
                                        if(months.key!!.toInt()>Calendar.getInstance().get(Calendar.MONTH)+1 && years.key!!.toInt()>=Calendar.getInstance().get(Calendar.YEAR) ||
                                            years.key!!.toInt()>Calendar.getInstance().get(Calendar.YEAR)){

                                            table.child("Users").child(auth.currentUser!!.uid).child("Category").child("Categories")
                                                .child("${years.key}/${months.key}").child("CategoriesExpence").child(history.placeId).addListenerForSingleValueEvent(object : ValueEventListener{
                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                        snapshot.getValue(_CategoryItem::class.java)?.let {

                                                            it.total = "%.2f".format(it.total.toDouble() - history.baseAmount.toDouble()).replace(",", ".")

                                                            table.child("Users").child(auth.currentUser!!.uid).child("Category").child("Categories")
                                                                .child("${years.key}/${months.key}").child("CategoriesExpence").child(history.placeId).setValue(it)

                                                            if (it.total == "0.000") {
                                                                table.child("Users").child(auth.currentUser!!.uid).child("Category").child("Categories")
                                                                    .child("${years.key}/${months.key}").child("CategoriesExpence").child(history.placeId).removeValue()
                                                            }
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {}
                                                })
                                        }

                                        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                                            .child("${years.key}/${months.key}").child(history.key).removeValue()
                                    }
                                }
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {} }
            )
    }

    private fun restoreSubs(budgetKey: String, context: Context){

        val sharedPreferences = context.getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
        var periodBegin:String
        var timeBegin:String
        val calendar = Calendar.getInstance()

        financeViewModel.subLiveData.value?.filter { it.subItem.budgetId == budgetKey }?.forEach { subItemWithKey ->

            periodBegin = sharedPreferences.getString(subItemWithKey.key, "|")?.split("|")?.get(0)?:context.resources.getStringArray(R.array.periodicity)[0]
            timeBegin = sharedPreferences.getString(subItemWithKey.key, "|")?.split("|")?.get(1)?:"12:00"
            calendar.set(subItemWithKey.subItem.date.split(".")[2].toInt(),
                subItemWithKey.subItem.date.split(".")[1].toInt()-1,
                subItemWithKey.subItem.date.split(".")[0].toInt(), 0,0,0)

            BudgetNotificationManager.notification(
                context = context,
                channelID = Constants.CHANNEL_ID_SUB,
                id = subItemWithKey.key,
                placeId = subItemWithKey.key,
                time = timeBegin,
                dateOfExpence = calendar,
                periodOfNotification = periodBegin
            )
            BudgetNotificationManager.setAutoTransaction(
                context = context,
                id = subItemWithKey.key,
                placeId = subItemWithKey.key,
                year = subItemWithKey.subItem.date.split(".")[2].toInt(),
                month = subItemWithKey.subItem.date.split(".")[1].toInt()-1,
                dateOfExpence = calendar,
                type = Constants.CHANNEL_ID_SUB
            )
        }
    }

    private fun changeCurrencyAmount(oldCurrency: String, newCurrency: String,newAmount:String, /*budgetKey: String, */context: Context):String{
            val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
            if(currencyConvertor!=null){
                return when (oldCurrency){
                    currencyConvertor.baseCode->{
                        "%.2f".format( newAmount.toDouble()*currencyConvertor.conversionRates[newCurrency]!!).replace(',','.')
                    }
                    else->{
                        "%.2f".format(newAmount.toDouble()*currencyConvertor.conversionRates[newCurrency]!!/currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.')
                    }
                }
            }
        return newAmount
    }

    private fun changeCurrency(oldCurrency:String, newCurrency:String, budgetKey:String, context: Context, baseChanged: Boolean, checkBox: Boolean, newSelection:Boolean, oldBaseCurrency:String?):String{
        val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
        if((oldCurrency!=newCurrency || checkBox || baseChanged) && currencyConvertor!=null){
            table.child("Users").child(auth.currentUser!!.uid)
                .child("History")
                .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (years in snapshot.children){
                        for (months in years.children){
                            for (historyItem in months.children){
                                historyItem.getValue(HistoryItem::class.java)?.let { history->
                                    if (history.budgetId == budgetKey || history.placeId == budgetKey || baseChanged){
                                        when{
                                            newCurrency == financeViewModel.budgetLiveData.value?.find { it.key=="Base budget" }!!.budgetItem.currency
                                                    && history.isCategory == true  && history.budgetId == budgetKey && newSelection-> {
                                                table.child("Users")
                                                    .child(auth.currentUser!!.uid)
                                                    .child("History")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString())
                                                    .child(history.key)
                                                    .child("amount")
                                                    .setValue(history.baseAmount)
                                                }

                                            history.isTransfer == true && budgetKey == history.placeId && (!checkBox || newSelection)-> {
                                                table.child("Users")
                                                    .child(auth.currentUser!!.uid)
                                                    .child("History")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString())
                                                    .child(history.key)
                                                    .child("amount")
                                                    .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * history.amount.toDouble() / currencyConvertor.conversionRates[oldCurrency]!!)
                                                    .replace(',', '.'))

                                                if (newCurrency == financeViewModel.budgetLiveData.value?.find { it.key == history.budgetId }?.budgetItem?.currency){
                                                    table.child("Users")
                                                        .child(auth.currentUser!!.uid)
                                                        .child("History")
                                                        .child(years.key.toString())
                                                        .child(months.key.toString())
                                                        .child(history.key)
                                                        .child("baseAmount")
                                                        .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * history.amount.toDouble() / currencyConvertor.conversionRates[oldCurrency]!!)
                                                            .replace(',', '.'))
                                                }
                                            }

                                            (history.isLoan == true || history.isGoal == true || history.isSub == true || history.placeId.isEmpty())
                                                    && budgetKey == history.budgetId && (!checkBox || newSelection) -> {
                                                        if( history.isLoan == true && financeViewModel.loansLiveData.value?.find { it.key == history.placeId }?.loanItem?.currency == newCurrency){
                                                            table.child("Users")
                                                                .child(auth.currentUser!!.uid)
                                                                .child("History")
                                                                .child(years.key.toString())
                                                                .child(months.key.toString())
                                                                .child(history.key)
                                                                .child("baseAmount")
                                                                .setValue(financeViewModel.loansLiveData.value?.find { it.key == history.placeId }?.loanItem?.amount)

                                                            table.child("Users")
                                                                .child(auth.currentUser!!.uid)
                                                                .child("History")
                                                                .child(years.key.toString())
                                                                .child(months.key.toString())
                                                                .child(history.key)
                                                                .child("amount")
                                                                .setValue(financeViewModel.loansLiveData.value?.find { it.key == history.placeId }?.loanItem?.amount)
                                                        } else{
                                                            table.child("Users")
                                                                .child(auth.currentUser!!.uid)
                                                                .child("History")
                                                                .child(years.key.toString())
                                                                .child(months.key.toString())
                                                                .child(history.key)
                                                                .child("amount")
                                                                .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * history.amount.toDouble() / currencyConvertor.conversionRates[oldCurrency]!!).replace(',', '.'))

                                                            if(history.isGoal == true && financeViewModel.goalsData.value?.find { it.key == history.placeId }?.goalItem?.currency == newCurrency){
                                                                    table.child("Users")
                                                                        .child(auth.currentUser!!.uid)
                                                                        .child("History")
                                                                        .child(years.key.toString())
                                                                        .child(months.key.toString())
                                                                        .child(history.key)
                                                                        .child("baseAmount")
                                                                        .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * history.amount.toDouble() / currencyConvertor.conversionRates[oldCurrency]!!).replace(',', '.'))
                                                            }
                                                        }
                                                    }

                                            history.isCategory == true  && budgetKey == history.budgetId && (!checkBox || newSelection) ->{
                                                table.child("Users")
                                                    .child(auth.currentUser!!.uid)
                                                    .child("History")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString())
                                                    .child(history.key)
                                                    .child("amount")
                                                    .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * history.amount.toDouble() / currencyConvertor.conversionRates[oldCurrency]!!).replace(',', '.'))

                                            }
                                        }

                                        when{
                                            history.isCategory == true  && baseChanged-> {
                                                table.child("Users")
                                                    .child(auth.currentUser!!.uid)
                                                    .child("History")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString())
                                                    .child(history.key)
                                                    .child("baseAmount")
                                                    .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * history.baseAmount.toDouble() / currencyConvertor.conversionRates[if (checkBox) oldBaseCurrency else oldCurrency]!!).replace(',', '.'))

                                                if (budgetKey == history.budgetId){
                                                    table.child("Users")
                                                        .child(auth.currentUser!!.uid)
                                                        .child("History")
                                                        .child(years.key.toString())
                                                        .child(months.key.toString())
                                                        .child(history.key)
                                                        .child("amount")
                                                        .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * history.baseAmount.toDouble() / currencyConvertor.conversionRates[if (checkBox) oldBaseCurrency else oldCurrency]!!).replace(',', '.'))
                                                }
                                            }

                                            (history.isTransfer == true ||  history.isSub == true || history.placeId.isEmpty()) && budgetKey == history.budgetId && (!checkBox || newSelection)-> {
                                                table.child("Users")
                                                    .child(auth.currentUser!!.uid)
                                                    .child("History")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString())
                                                    .child(history.key)
                                                    .child("baseAmount")
                                                    .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * history.baseAmount.toDouble() / currencyConvertor.conversionRates[oldCurrency]!!).replace(',', '.'))

                                                if (history.isTransfer == true && newCurrency == financeViewModel.budgetLiveData.value?.find { it.key == history.placeId }?.budgetItem?.currency){
                                                    table.child("Users")
                                                        .child(auth.currentUser!!.uid)
                                                        .child("History")
                                                        .child(years.key.toString())
                                                        .child(months.key.toString())
                                                        .child(history.key)
                                                        .child("amount")
                                                        .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * history.baseAmount.toDouble() / currencyConvertor.conversionRates[oldCurrency]!!).replace(',', '.'))
                                                }
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

            table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (years in snapshot.children){
                        for (months in years.children){
                            for (historyItem in months.children){
                                historyItem.getValue(HistoryItem::class.java)?.let { plan->
                                    if (plan.budgetId == budgetKey || baseChanged) {
                                        when {
                                            newCurrency == financeViewModel.budgetLiveData.value?.find { it.key=="Base budget" }!!.budgetItem.currency
                                                    &&  plan.budgetId == budgetKey && newSelection-> {
                                                table.child("Users")
                                                    .child(auth.currentUser!!.uid)
                                                    .child("Plan")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString())
                                                    .child(plan.key).child("amount")
                                                    .setValue(plan.baseAmount)
                                            }
                                            plan.budgetId == budgetKey && newSelection ->{
                                                table.child("Users")
                                                    .child(auth.currentUser!!.uid)
                                                    .child("Plan")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString())
                                                    .child(plan.key).child("amount")
                                                    .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * plan.amount.toDouble() / currencyConvertor.conversionRates[oldCurrency]!!).replace(',', '.')
                                                    )
                                            }
                                        }

                                        if (baseChanged){
                                            table.child("Users")
                                                .child(auth.currentUser!!.uid)
                                                .child("Plan")
                                                .child(years.key.toString())
                                                .child(months.key.toString())
                                                .child(plan.key).child("baseAmount")
                                                .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!!*plan.baseAmount.toDouble()/currencyConvertor.conversionRates[if (checkBox) oldBaseCurrency else oldCurrency]!!).replace(',','.'))

                                            if (budgetKey == plan.budgetId){
                                                table.child("Users")
                                                    .child(auth.currentUser!!.uid)
                                                    .child("History")
                                                    .child(years.key.toString())
                                                    .child(months.key.toString())
                                                    .child(plan.key)
                                                    .child("amount")
                                                    .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!! * plan.baseAmount.toDouble() / currencyConvertor.conversionRates[if (checkBox) oldBaseCurrency else oldCurrency]!!).replace(',', '.'))
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

            table.child("Users").child(auth.currentUser!!.uid).child("Subs").addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (sub in snapshot.children){
                        sub.getValue(SubItem::class.java)?.let {
                            if (it.budgetId == budgetKey ){
                                table.child("Users")
                                    .child(auth.currentUser!!.uid)
                                    .child("Subs")
                                    .child(sub.key.toString())
                                    .child("amount")
                                    .setValue("%.2f".format(currencyConvertor.conversionRates[newCurrency]!!*it.amount.toDouble()/currencyConvertor.conversionRates[oldCurrency]!!).replace(',','.'))
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}}
            )
        }

        if(budgetKey == "Base budget" && currencyConvertor!=null && baseChanged){
            when (oldBaseCurrency?:oldCurrency){
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
                                                                    "%.2f".format(
                                                                        currencyConvertor.conversionRates[newCurrency]!! * categoryItemNew.total.toDouble()
                                                                    ).replace(',', '.')
                                                            }

                                                            else -> {
                                                                categoryItemNew.total =
                                                                    "%.2f".format(
                                                                        currencyConvertor.conversionRates[newCurrency]!! * categoryItemNew.total.toDouble()
                                                                    ).replace(',', '.')
                                                                categoryItemNew.remainder =
                                                                    "%.2f".format(
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
                                                                it.total = "%.2f".format(
                                                                    currencyConvertor.conversionRates[newCurrency]!! * it.total.toDouble() / currencyConvertor.conversionRates[oldBaseCurrency?:oldCurrency]!!
                                                                ).replace(',', '.')
                                                            }

                                                            else -> {
                                                                it.total = "%.2f".format(
                                                                    currencyConvertor.conversionRates[newCurrency]!! * it.total.toDouble() / currencyConvertor.conversionRates[oldBaseCurrency?:oldCurrency]!!
                                                                ).replace(',', '.')
                                                                it.remainder =
                                                                    "%.2f".format(
                                                                        currencyConvertor.conversionRates[newCurrency]!! * it.remainder.toDouble() / currencyConvertor.conversionRates[oldBaseCurrency?:oldCurrency]!!
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
                }
            }
        }
        return newCurrency
    }
}