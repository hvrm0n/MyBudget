package com.example.mybudget.drawersection.finance

import android.content.Context
import android.util.Log
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.findNavController
import com.example.mybudget.ExchangeRateManager
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.example.mybudget.drawersection.finance.budget._BudgetItem
import com.example.mybudget.drawersection.finance.category.CategoryBeginWithKey
import com.example.mybudget.drawersection.finance.category.CategoryItemWithKey
import com.example.mybudget.drawersection.finance.category._CategoryBegin
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.example.mybudget.drawersection.finance.history.HistoryItem
import com.example.mybudget.drawersection.goals.GoalItem
import com.example.mybudget.drawersection.goals.GoalItemWithKey
import com.example.mybudget.drawersection.loans.LoanItem
import com.example.mybudget.drawersection.loans.LoanItemWithKey
import com.example.mybudget.drawersection.subs.SubItem
import com.example.mybudget.drawersection.subs.SubItemWithKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.abs

class FinanceViewModel(private val table: DatabaseReference, private val auth: FirebaseAuth, private val context: Context):ViewModel() {
    private val category = mutableListOf<CategoryItemWithKey>()
    private val combinedList = mutableListOf<BudgetItemWithKey>()
    private val baseBudget = mutableListOf<BudgetItemWithKey>()
    private val otherBudget = mutableListOf<BudgetItemWithKey>()
    private val categoryBegin = mutableListOf<CategoryBeginWithKey>()
    private val historyList = mutableListOf<HistoryItem>()
    private val dateList = mutableListOf<Pair<Int, Int>>()
    private val planList = mutableListOf<HistoryItem>()
    private val goalList = mutableListOf<GoalItemWithKey>()
    private val subList = mutableListOf<SubItemWithKey>()
    private val loanList = mutableListOf<LoanItemWithKey>()

    private val _budgetLiveData = MutableLiveData<List<BudgetItemWithKey>>()
    val budgetLiveData: LiveData<List<BudgetItemWithKey>> get() = _budgetLiveData

    private val _headerSum = MutableLiveData<Boolean>(true)
    val headerSum: LiveData<Boolean> get() = _headerSum


    private val _categoryLiveData = MutableLiveData<List<CategoryItemWithKey>>()
    val categoryLiveData: LiveData<List<CategoryItemWithKey>> get() = _categoryLiveData


    private val _categoryBeginLiveData = MutableLiveData<List<CategoryBeginWithKey>>()
    val categoryBeginLiveData: LiveData<List<CategoryBeginWithKey>> get() = _categoryBeginLiveData

    private val _categoryDate = MutableLiveData<List<Pair<Int,Int>>>()
    val categoryDate: LiveData<List<Pair<Int,Int>>> get() = _categoryDate


    private val _historyLiveData = MutableLiveData<List<HistoryItem>>()
    val historyLiveData: LiveData<List<HistoryItem>> get() = _historyLiveData


    private val _planLiveData = MutableLiveData<List<HistoryItem>>()
    val planLiveData: LiveData<List<HistoryItem>> get() = _planLiveData


    private val _financeDate = MutableLiveData<Pair<Int,Int>>()
    val financeDate: LiveData<Pair<Int,Int>> get() = _financeDate


    private val _subLiveData = MutableLiveData<List<SubItemWithKey>>()
    val subLiveData: LiveData<List<SubItemWithKey>> get() = _subLiveData


    private val _goalsLiveData = MutableLiveData<List<GoalItemWithKey>>()
    val goalsData: LiveData<List<GoalItemWithKey>> get() = _goalsLiveData


    private val _loansLiveData = MutableLiveData<List<LoanItemWithKey>>()
    val loansLiveData: LiveData<List<LoanItemWithKey>> get() = _loansLiveData

    fun updateBudgetData(newData: List<BudgetItemWithKey>) {
        _budgetLiveData.value = newData
    }

    fun updateCategoryData(newData: List<CategoryItemWithKey>) {
        _categoryLiveData.value = newData
    }

    fun updateHistoryData(newData: List<HistoryItem>) {
        _historyLiveData.value = newData
    }

    fun updatePlanData(newData: List<HistoryItem>) {
        _planLiveData.value = newData
    }

    fun updateCategoryBeginData(newData: List<CategoryBeginWithKey>) {
        _categoryBeginLiveData.value = newData
    }

    fun updateCategoryDate(newData: List<Pair<Int, Int>>) {
        _categoryDate.value = newData
    }

    fun updateDate(newData: Pair<Int, Int>) {
        _financeDate.value = newData
    }

    fun updateGoalsData(newData: List<GoalItemWithKey>) {
        _goalsLiveData.value = newData
    }

    fun updateSubsData(newData: List<SubItemWithKey>) {
        _subLiveData.value = newData
    }

    fun updateLoansData(newData: List<LoanItemWithKey>) {
        _loansLiveData.value = newData
    }



    //распределение средств по категориям
    fun distributeMoney(selectedBudget: List<BudgetItemWithKey>):Boolean{
        var distribute = false
        if(!categoryLiveData.value.isNullOrEmpty()){
            val highPriorityCategories = categoryLiveData.value!!.filter { it.categoryItem.priority == 2 }
            val mediumPriorityCategories = categoryLiveData.value!!.filter { it.categoryItem.priority == 1 }
            val lowPriorityCategories = categoryLiveData.value!!.filter { it.categoryItem.priority == 0 }

            val totalHighPriorityCategories = highPriorityCategories.size
            val totalMediumPriorityCategories = mediumPriorityCategories.size
            val totalLowPriorityCategories = lowPriorityCategories.size

            var highPriorityCoefficient = 0.0
            var mediumPriorityCoefficient = 0.0
            var lowPriorityCoefficient = 0.0


            if(totalHighPriorityCategories !=0){
                if(totalMediumPriorityCategories!=0 && totalLowPriorityCategories!=0){
                    mediumPriorityCoefficient = totalMediumPriorityCategories/categoryLiveData.value!!.size.toDouble()
                    highPriorityCoefficient = (1.0-mediumPriorityCoefficient) * ((totalMediumPriorityCategories+totalHighPriorityCategories)/categoryLiveData.value!!.size.toDouble())
                    lowPriorityCoefficient = 1.0-highPriorityCoefficient-mediumPriorityCoefficient
                } else if (totalLowPriorityCategories!=0){
                    lowPriorityCoefficient = 0.33*totalLowPriorityCategories/categoryLiveData.value!!.size.toDouble()
                    highPriorityCoefficient = 1.0-lowPriorityCoefficient
                } else if (totalMediumPriorityCategories!=0){
                    mediumPriorityCoefficient = 0.66*totalMediumPriorityCategories/categoryLiveData.value!!.size.toDouble()
                    highPriorityCoefficient = 1.0-mediumPriorityCoefficient
                } else highPriorityCoefficient = 1.0

            } else if (totalMediumPriorityCategories!=0){
                if(totalLowPriorityCategories!=0){
                    lowPriorityCoefficient = 0.66 * totalLowPriorityCategories/categoryLiveData.value!!.size.toDouble()
                    mediumPriorityCoefficient = 1.0 - lowPriorityCoefficient
                }
                else{
                    mediumPriorityCoefficient = 1.0
                }

            }
            else if(totalLowPriorityCategories!=0){
                lowPriorityCoefficient = 1.0
            }

            totalMoney(selectedBudget){ totalMoney->
                val highPriorityBudget:Double = totalMoney * highPriorityCoefficient / totalHighPriorityCategories
                val mediumPriorityBudget:Double = totalMoney * mediumPriorityCoefficient/ totalMediumPriorityCategories
                val lowPriorityBudget:Double = totalMoney * lowPriorityCoefficient / totalLowPriorityCategories
                var totalCheck = 0.0
                for (categoryItem in categoryLiveData.value!!){
                    val expence = if(categoryItem.categoryItem.remainder.toDouble() == 0.0) categoryItem.categoryItem.total.toDouble() else categoryItem.categoryItem.total.toDouble()-categoryItem.categoryItem.remainder.toDouble()
                    categoryItem.categoryItem.total = "%.2f".format(when(categoryItem.categoryItem.priority){
                        0  -> lowPriorityBudget
                        1 -> mediumPriorityBudget
                        else -> highPriorityBudget
                    }).replace(',','.')
                    categoryItem.categoryItem.remainder = "%.2f".format(categoryItem.categoryItem.total.toDouble()-expence).replace(',','.')
                    totalCheck+=categoryItem.categoryItem.total.toDouble()

                    table.child("Users").child(auth.currentUser!!.uid)
                        .child("Categories").child("${Calendar.getInstance().get(Calendar.YEAR)}/${
                            Calendar.getInstance().get(
                                Calendar.MONTH)+1}")
                        .child("ExpenseCategories").child(categoryItem.key).setValue(categoryItem.categoryItem)

                    val sharedPreferences = context.getSharedPreferences("preference_distribute", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isDistributed", true)
                    editor.putString("isDistributedDay", "${Calendar.getInstance().get(Calendar.MONTH)}.${
                        Calendar.getInstance().get(
                            Calendar.YEAR)}")
                    editor.apply()
                    distribute = true
                }
            }
        }
        return distribute
    }

    private fun totalMoney(selectedBudget: List<BudgetItemWithKey>, callback: (Double) -> Unit) {
        var total = 0.0
        val baseCurrency = budgetLiveData.value!!.find {budget-> budget.key == "Base budget" }!!.budgetItem.currency
        selectedBudget.forEach{budgetItem->
            when(budgetItem.budgetItem.currency){
                baseCurrency -> {
                    total += budgetItem.budgetItem.amount.toDouble() - withSub(budgetItem.key)
                }
                else ->{
                    val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
                    if(currencyConvertor!=null){
                        total += when(currencyConvertor.baseCode){
                            baseCurrency -> (budgetItem.budgetItem.amount.toDouble() - withSub(budgetItem.key))/currencyConvertor.conversionRates[budgetItem.budgetItem.currency]!!
                            else->{
                                val newValueToBase = ((budgetItem.budgetItem.amount.toDouble() - withSub(budgetItem.key))/currencyConvertor.conversionRates[budgetItem.budgetItem.currency]!!)
                                newValueToBase*currencyConvertor.conversionRates[baseCurrency]!!
                            }
                        }
                    }
                }
            }
            callback(total - withLoan(baseCurrency, context))
        }
    }

    private fun withSub(budgetItemKey:String) = subLiveData.value?.filter { !it.subItem.isCancelled
            && !it.subItem.isDeleted
            && it.subItem.budgetId == budgetItemKey
            && it.subItem.date.split(".")[1].toInt() == Calendar.getInstance().get(Calendar.MONTH)+1
            && it.subItem.date.split(".")[2].toInt() == Calendar.getInstance().get(Calendar.YEAR)}
        ?.sumOf { it.subItem.amount.toDouble() } ?: 0.0

    private fun withLoan(baseCurrency : String, context: Context) = loansLiveData.value?.filter {
        if(it.loanItem.period!=null) {
            !it.loanItem.isFinished
                    && !it.loanItem.isDeleted
                    && it.loanItem.dateNext!!.split(".")[1].toInt() == Calendar.getInstance().get(Calendar.MONTH)+1
        }
        else{
            !it.loanItem.isFinished
                    && !it.loanItem.isDeleted
                    && it.loanItem.dateOfEnd.split(".")[1].toInt() == Calendar.getInstance().get(Calendar.MONTH)+1
        }
    }?.sumOf {
        when(it.loanItem.currency){
            baseCurrency -> {
                it.loanItem.amount.toDouble()
            }
            else ->{
                val currencyConvertor = ExchangeRateManager.getExchangeRateResponse(context)
                if(currencyConvertor!=null){
                    when(currencyConvertor.baseCode){
                        baseCurrency ->
                            it.loanItem.amount.toDouble()/currencyConvertor.conversionRates[it.loanItem.currency]!!
                        else->{
                            val newValueToBase = ((it.loanItem.amount.toDouble())/currencyConvertor.conversionRates[it.loanItem.currency]!!)
                            newValueToBase*currencyConvertor.conversionRates[baseCurrency]!!
                        }
                    }
                } else {
                    0.0
                }
            }
        }
    }?:0.0

     fun cancelDistribution():Boolean{
        if(!categoryLiveData.value.isNullOrEmpty()){
            for (categoryItem in categoryLiveData.value!!){
                categoryItem.categoryItem.total = "%.2f".format(categoryItem.categoryItem.total.toDouble() - categoryItem.categoryItem.remainder.toDouble()).replace(',','.')
                categoryItem.categoryItem.remainder = "0"

                table.child("Users").child(auth.currentUser!!.uid)
                    .child("Categories").child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}")
                    .child("ExpenseCategories").child(categoryItem.key).setValue(categoryItem.categoryItem)
            }
            val sharedPreferences = context.getSharedPreferences("preference_distribute", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("isDistributed", false)
            editor.putString("isDistributedDay", "")
            editor.apply()
            return true
        }
        return false
    }

    //обновление данных
    fun updateCategoryOnce(month:Int, year:Int, doAfter: (Unit)->Unit) {
        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("$year/${month}").child("ExpenseCategories").addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    category.clear()

                    for (expenseCategory in snapshot.children){
                        expenseCategory.getValue(_CategoryItem::class.java)?.let {
                            category.add(CategoryItemWithKey(expenseCategory.key.toString(), it))
                        }
                    }
                    updateCategoryData(category.sortedByDescending { it.categoryItem.priority })
                    doAfter(Unit)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }


    fun updateBeginCategory(){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories").child("Categories base")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryBegin.clear()
                    for(category in snapshot.children) {
                        category.getValue(_CategoryBegin::class.java)?.let {
                            categoryBegin.add(CategoryBeginWithKey(category.key.toString(), it))
                        }
                    }
                    updateCategoryBeginData(categoryBegin)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    fun updateBudget(){
        table.child("Users").child(auth.currentUser!!.uid).child("Budgets").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                baseBudget.clear()
                otherBudget.clear()
                combinedList.clear()
                for (budget in snapshot.children){
                    budget.getValue(_BudgetItem::class.java)?.let {
                        if(budget.key=="Base budget") {
                            baseBudget.add(BudgetItemWithKey(budget.key.toString(), it))
                        }
                        else {
                            for(other in budget.children){
                                other.getValue(_BudgetItem::class.java)?.let { data ->
                                    otherBudget.add(BudgetItemWithKey(other.key.toString(), data))
                                }
                            }
                        }
                    }
                }

                combinedList.run {
                    add(baseBudget[0])
                    addAll(otherBudget)
                }
                updateBudgetData(combinedList)
                _headerSum.value = !_headerSum.value!!
                //updateHeaderSum()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }

    fun updateCategory(){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("${Calendar.getInstance().get(Calendar.YEAR)}/${
                Calendar.getInstance().get(
                    Calendar.MONTH)+1}").child("ExpenseCategories")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    category.clear()
                    for (expenseCategory in snapshot.children){
                        expenseCategory.getValue(_CategoryItem::class.java)?.let {
                            category.add(CategoryItemWithKey(expenseCategory.key.toString(), it))
                        }
                    }
                    updateCategoryData(category.sortedByDescending { it.categoryItem.priority })
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    fun updateDate(){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                dateList.clear()
                snapshot.children.forEach {years->
                    years.children.forEach { months->
                        if(years.key!=null&&months.key!=null&&years.key.toString().isDigitsOnly()){
                            dateList.add(Pair(months.key.toString().toInt(), years.key.toString().toInt()))
                        }
                    }
                }
                val current = Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR))
                if(dateList.isEmpty() || !dateList.contains(current)){
                    dateList.add(Pair(Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.YEAR)))
                }
                updateCategoryDate(dateList)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun updateHistory(){
        table.child("Users").child(auth.currentUser!!.uid).child("History")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    historyList.clear()
                    for (years in snapshot.children) {
                        for(months in years.children){
                            for(histories in months.children)
                                histories.getValue(HistoryItem::class.java)?.let {
                                    historyList.add(it)
                                }
                        }
                    }
                    updateHistoryData(historyList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    fun updatePlan() {
        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    planList.clear()
                    for (years in snapshot.children) {
                        for (months in years.children) {
                            for (histories in months.children)
                                histories.getValue(HistoryItem::class.java)?.let {
                                    planList.add(it)
                                }
                        }
                    }
                    updatePlanData(planList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("errorLog", error.toException().toString())
                }
            })
    }

    fun updateGoals(){
        table.child("Users").child(auth.currentUser!!.uid).child("Goals").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                goalList.clear()
                for (goal in snapshot.children){
                    goal.getValue(GoalItem::class.java)?.let { gi->
                        goalList.add(GoalItemWithKey(goal.key.toString(), gi))
                    }
                }
                updateGoalsData(
                    goalList.asSequence()
                        .filter { it.goalItem.date!=null && if (it.goalItem.date!=null){
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY,0)
                                set(Calendar.MINUTE,0)
                                set(Calendar.SECOND,0)
                            }.timeInMillis <= Calendar.getInstance().apply {
                                set(
                                    it.goalItem.date!!.split(".")[2].toInt(),
                                    it.goalItem.date!!.split(".")[1].toInt()-1,
                                    it.goalItem.date!!.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                        } else true}
                        .sortedByDescending { it.goalItem.target.toDouble() - it.goalItem.current.toDouble() }
                        .toList()
                            +
                            goalList
                                .asSequence() .filter { it.goalItem.date==null }
                                .sortedByDescending { it.goalItem.target.toDouble() - it.goalItem.current.toDouble() }
                                .toList()

                            + goalList.asSequence()
                        .filter { it.goalItem.date!=null && if (it.goalItem.date!=null){
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY,0)
                                set(Calendar.MINUTE,0)
                                set(Calendar.SECOND,0)
                            }.timeInMillis > Calendar.getInstance().apply {
                                set(
                                    it.goalItem.date!!.split(".")[2].toInt(),
                                    it.goalItem.date!!.split(".")[1].toInt()-1,
                                    it.goalItem.date!!.split(".")[0].toInt(), 0,0,0)
                            }.timeInMillis
                        } else true}
                        .sortedByDescending { it.goalItem.target.toDouble() - it.goalItem.current.toDouble() }
                        .toList()
                )
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }

    fun updateSubs(){
        table.child("Users").child(auth.currentUser!!.uid).child("Subs").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                subList.clear()
                for (sub in snapshot.children){
                    sub.getValue(SubItem::class.java)?.let { si->
                        subList.add(SubItemWithKey(sub.key.toString(), si))
                    }
                }

                updateSubsData(
                    subList.asSequence()
                        .filter { !it.subItem.isCancelled  && !it.subItem.isDeleted  } .toList()
                            + subList.asSequence() .filter { it.subItem.isCancelled }.toList()
                            + subList.filter { it.subItem.isDeleted }.toList())
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }


    fun updateLoans(){
        table.child("Users").child(auth.currentUser!!.uid).child("Loans").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                loanList.clear()
                for (loan in snapshot.children){
                    loan.getValue(LoanItem::class.java)?.let { loanItem->
                        loanList.add(LoanItemWithKey(loan.key.toString(), loanItem))
                    }
                }

                updateLoansData(
                    //активные
                    loanList.asSequence()
                        .filter { !it.loanItem.isFinished  && !it.loanItem.isDeleted && if (it.loanItem.dateNext!=null){
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY,0)
                                set(Calendar.MINUTE,0)
                                set(Calendar.SECOND,0)
                            }.timeInMillis <= Calendar.getInstance().apply {
                                set(
                                    it.loanItem.dateNext!!.split(".")[2].toInt(),
                                    it.loanItem.dateNext!!.split(".")[1].toInt()-1,
                                    it.loanItem.dateNext!!.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                        } else
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY,0)
                                set(Calendar.MINUTE,0)
                                set(Calendar.SECOND,0)
                            }.timeInMillis <= Calendar.getInstance().apply {
                                set(
                                    it.loanItem.dateOfEnd.split(".")[2].toInt(),
                                    it.loanItem.dateOfEnd.split(".")[1].toInt()-1,
                                    it.loanItem.dateOfEnd.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                        } .toList().sortedBy {loan->
                            when (loan.loanItem.period){
                                null->loan.loanItem.dateOfEnd.split('.').let {
                                    ChronoUnit.DAYS.between(
                                        LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                        LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                }
                                else->loan.loanItem.dateNext?.split('.')?.let {
                                    ChronoUnit.DAYS.between(
                                        LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                        LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                }
                            }
                        }
                            +
                            //просроченные
                            loanList.asSequence()
                                .filter { !it.loanItem.isFinished  && !it.loanItem.isDeleted && if (it.loanItem.dateNext!=null){
                                    Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY,0)
                                        set(Calendar.MINUTE,0)
                                        set(Calendar.SECOND,0)
                                    }.timeInMillis > Calendar.getInstance().apply {
                                        set(
                                            it.loanItem.dateNext!!.split(".")[2].toInt(),
                                            it.loanItem.dateNext!!.split(".")[1].toInt()-1,
                                            it.loanItem.dateNext!!.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                                } else
                                    Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY,0)
                                        set(Calendar.MINUTE,0)
                                        set(Calendar.SECOND,0)
                                    }.timeInMillis > Calendar.getInstance().apply {
                                        set(
                                            it.loanItem.dateOfEnd.split(".")[2].toInt(),
                                            it.loanItem.dateOfEnd.split(".")[1].toInt()-1,
                                            it.loanItem.dateOfEnd.split(".")[0].toInt(), 0,0,0)}.timeInMillis
                                } .toList().sortedBy {loan->
                                    when (loan.loanItem.period){
                                        null->loan.loanItem.dateOfEnd.split('.').let {
                                            ChronoUnit.DAYS.between(
                                                LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                                LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                        }
                                        else->loan.loanItem.dateNext?.split('.')?.let {
                                            ChronoUnit.DAYS.between(
                                                LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                                LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                        }
                                    }
                                }
                            //завершенные
                            + loanList.asSequence()
                        .filter { it.loanItem.isFinished }.toList()
                        .sortedBy {loan->
                            when (loan.loanItem.period){
                                null->loan.loanItem.dateOfEnd.split('.').let {
                                    ChronoUnit.DAYS.between(
                                        LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                        LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                }
                                else->loan.loanItem.dateNext?.split('.')?.let {
                                    ChronoUnit.DAYS.between(
                                        LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH)),
                                        LocalDate.of(it[2].toInt(),it[1].toInt(), it[0].toInt()))
                                }
                            }
                        }
                            + loanList.filter { it.loanItem.isDeleted }.toList())
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }

    fun beginCheckUpCategories(){
        val categoryReference = table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").child("ExpenseCategories")

        categoryReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (expenseCategory in snapshot.children){
                    expenseCategory.getValue(_CategoryItem::class.java)?.let {category->
                        if(category.isPlanned){
                            categoryReference.child(expenseCategory.key.toString()).child("planned").setValue(false).addOnCompleteListener {
                                table.child("Users").child(auth.currentUser!!.uid).child("History")
                                    .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").addListenerForSingleValueEvent(
                                        object :ValueEventListener{
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                var sum = 0.0
                                                for (historyItem in snapshot.children){
                                                    historyItem.getValue(HistoryItem::class.java)?.let {
                                                        sum += if (it.placeId == expenseCategory.key) abs(it.baseAmount.toDouble()) else 0.0
                                                    }
                                                }
                                                categoryReference.child(expenseCategory.key.toString()).child("total").setValue(if (sum==0.0) "0" else "%.2f".format(sum).replace(",", "."))
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                            }

                                        }
                                    )
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("errorLog", error.toException().toString())
            }
        })
    }

    //создание категории
    fun addCategory(name:String, spinnerPriority:Int){
        table.child("Users").child(auth.currentUser!!.uid).child("Categories")
            .child("${financeDate.value!!.second}/${financeDate.value!!.first}")
            .child("ExpenseCategories")
            .child( categoryBeginLiveData.value!!.filter { it.categoryBegin.name == name }[0].key).setValue(_CategoryItem("0", "0.00", spinnerPriority,
                isPlanned = (Calendar.getInstance().get(Calendar.YEAR)<financeDate.value!!.second||Calendar.getInstance().get(Calendar.MONTH)+1<financeDate.value!!.first))).addOnSuccessListener {
            }.addOnFailureListener { ex->
                Log.e("SaveError", ex.message.toString())
            }
    }

}