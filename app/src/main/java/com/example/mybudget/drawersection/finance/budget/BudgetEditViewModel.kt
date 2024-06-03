package com.example.mybudget.drawersection.finance.budget

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.example.mybudget.BudgetNotificationManager
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.FinanceViewModel
import com.example.mybudget.drawersection.finance.SelectedBudgetViewModel
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.example.mybudget.drawersection.finance.history.HistoryItem
import com.example.mybudget.drawersection.subs.SubItem
import com.example.mybudget.start_pages.Constants
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.util.Calendar

class BudgetEditViewModel: ViewModel() {
    private val auth = Firebase.auth
    private val table = Firebase.database.reference
    private var _selection: Triple<String, String, String> = Triple("","", "")
    val selection: Triple<String, String, String> get() = _selection
    private var _oldCurrency: String?=null
    val oldCurrency: String? get() = _oldCurrency

    fun updateSelection(newSelection:Triple<String, String, String>){
        _selection = newSelection
        _oldCurrency = newSelection.first
    }

    fun updateNotBaseBudget(key:String, name:String, amount:String, type:String, transaction:Int, currencyShort:String, context: Context, financeViewModel: FinanceViewModel){
        table.child("Users").child(auth.currentUser!!.uid)
            .child("Budgets").child("Other budget")
            .child(key)
            .setValue(_BudgetItem(
                name,
                amount,
                type,
                transaction,
                changeCurrency(currencyShort,
                   selection.first.ifEmpty { currencyShort }, key, context,
                    baseChanged = false,
                    checkBox = false,
                    selection.second.isNotEmpty(),
                    null,
                    financeViewModel)))
    }

    fun updateBaseBudget(key:String, name:String, amount:String, type:String, transaction:Int, currencyShort:String, context: Context, financeViewModel: FinanceViewModel, selectedBudgetViewModel: SelectedBudgetViewModel){
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
                                                            name,
                                                            amount,
                                                            type,
                                                            transaction,
                                                            changeCurrency(currencyShort,
                                                                selection.first.ifEmpty { currencyShort }, "Base budget", context,
                                                                baseChanged = true,
                                                                checkBox = true,
                                                                selection.second.isNotEmpty(),
                                                                baseOld.currency,
                                                                financeViewModel)))
                                                    .addOnCompleteListener {
                                                        table.child("Users")
                                                            .child(auth.currentUser!!.uid)
                                                            .child("Budgets")
                                                            .child("Other budget")
                                                            .child(key)
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

    fun updateOldBaseBudget(name:String, amount:String, type:String, transaction:Int, currencyShort:String, context: Context, financeViewModel: FinanceViewModel){
        Log.e("CheckCurrency", currencyShort + " " + _selection.first)

        table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
            .child("Base budget")
            .setValue(
                _BudgetItem(
                    name,
                    amount,
                    type,
                    transaction,
                    changeCurrency(currencyShort,
                        selection.first.ifEmpty { currencyShort }, "Base budget", context,
                        baseChanged = true,
                        checkBox = false,
                        selection.second.isNotEmpty(),
                        null,
                        financeViewModel)))
    }

    fun deleteBudget(key:String, context: Context, financeViewModel: FinanceViewModel){
        table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
            .child("Other budget").child(key).child("deleted").setValue(true)
            .addOnCompleteListener {
                deletePlansAndSubs(budgetKey = key, context = context, financeViewModel)
            }
    }

    private fun deletePlansAndSubs(budgetKey: String, context: Context, financeViewModel: FinanceViewModel){
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
                                        if(months.key!!.toInt()> Calendar.getInstance().get(Calendar.MONTH)+1 && years.key!!.toInt()>= Calendar.getInstance().get(
                                                Calendar.YEAR) ||
                                            years.key!!.toInt()> Calendar.getInstance().get(Calendar.YEAR)){

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

    fun newOtherBudget(name:String, amount:String, type:String, currencyShort:String){
        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").push().setValue(
            _BudgetItem(name, amount,  type, 0, selection.first.ifEmpty { currencyShort}))
    }

    fun newBaseBudget(name:String, amount:String, type:String, currencyShort:String, context: Context, financeViewModel: FinanceViewModel, selectedBudgetViewModel: SelectedBudgetViewModel){

        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val baseOld = snapshot.getValue(_BudgetItem::class.java)
                    if (baseOld!=null){
                        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").setValue(
                            _BudgetItem(name, amount,  type, 0,  changeCurrency(
                                currencyShort,
                                selection.first.ifEmpty { currencyShort }, "Base budget", context,
                                baseChanged = true,
                                checkBox = true,
                                selection.second.isNotEmpty(),
                                baseOld.currency,
                                financeViewModel)))
                            .addOnCompleteListener {
                                val referenceBaseOld =
                                table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").push()
                                referenceBaseOld.setValue(baseOld).addOnCompleteListener {
                                        selectedBudgetViewModel.selectedBudget.value?.map{
                                            when (it){
                                                "Base budget" -> referenceBaseOld.key.toString()
                                                else -> it
                                            }
                                        }?.let { newSelection ->
                                            PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet("sumOfFinance", newSelection.toSet()).apply()
                                            selectedBudgetViewModel.updateSelectionData(newSelection)
                                        }

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
                                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("budgetId").setValue(referenceBaseOld.key)
                                                                    }
                                                                    referenceBaseOld.key-> {
                                                                        table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                                            .child(years.key.toString()).child(months.key.toString()).child(it.key).child("budgetId").setValue("Base budget")
                                                                    }
                                                                    else ->{}
                                                                }
                                                                if (it.isTransfer == true ){
                                                                    when (it.placeId){
                                                                        "Base budget" ->{
                                                                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                                                .child(years.key.toString()).child(months.key.toString()).child(it.key).child("placeId").setValue(referenceBaseOld.key)
                                                                        }
                                                                        referenceBaseOld.key-> {
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
                                                                                    .child(years.key.toString()).child(months.key.toString()).child(it.key).child("budgetId").setValue(referenceBaseOld.key)

                                                                            }
                                                                            referenceBaseOld.key-> table.child("Users").child(auth.currentUser!!.uid).child("Plan")
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
                                                                                    .setValue(referenceBaseOld.key)
                                                                            }
                                                                            referenceBaseOld.key->{table.child("Users")
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
                    }
                }
                override fun onCancelled(error: DatabaseError) {}

            })
    }

    fun restoreBudget(deletedBudget:BudgetItemWithKey, context: Context, financeViewModel: FinanceViewModel){
        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(deletedBudget.key).child("deleted").setValue(false).addOnCompleteListener {
            restoreSubs(deletedBudget.key, context, financeViewModel)
        }
    }

    private fun restoreSubs(budgetKey: String, context: Context, financeViewModel: FinanceViewModel){

        val sharedPreferences = context.getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
        var periodBegin:String
        var timeBegin:String
        val calendar = Calendar.getInstance()

        financeViewModel.subLiveData.value?.filter { it.subItem.budgetId == budgetKey }?.forEach { subItemWithKey ->

            periodBegin = sharedPreferences.getString(subItemWithKey.key, "|")?.split("|")?.get(0)?:context.resources.getStringArray(
                R.array.periodicity)[0]
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

    fun changeCurrencyAmount(oldCurrency: String, newCurrency: String,newAmount:String, context: Context):String{
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

    private fun changeCurrency(oldCurrency:String, newCurrency:String, budgetKey:String, context: Context, baseChanged: Boolean, checkBox: Boolean, newSelection:Boolean, oldBaseCurrency:String?, financeViewModel: FinanceViewModel):String{
        val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
        if((oldCurrency!=newCurrency || checkBox || baseChanged) && currencyConvertor!=null){
            table.child("Users").child(auth.currentUser!!.uid)
                .child("History")
                .addListenerForSingleValueEvent(object : ValueEventListener {
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
                .addListenerForSingleValueEvent(object : ValueEventListener {
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
                                                        .child("Plan")
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

            table.child("Users").child(auth.currentUser!!.uid).child("Subs").addListenerForSingleValueEvent(object :
                ValueEventListener {
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