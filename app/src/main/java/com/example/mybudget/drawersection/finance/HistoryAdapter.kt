package com.example.mybudget.drawersection.finance

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudget.BudgetNotificationManager
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.example.mybudget.drawersection.finance.budget._BudgetItem
import com.example.mybudget.drawersection.finance.category.CategoryItemWithKey
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.example.mybudget.drawersection.goals.GoalItemWithKey
import com.example.mybudget.drawersection.loans.LoanItemWithKey
import com.example.mybudget.drawersection.subs.SubItemWithKey
import com.example.mybudget.start_pages.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class HistoryAdapter(private val context: Context, private var history: List<HistoryItem>, val table: DatabaseReference, val auth: FirebaseAuth, val activity: FragmentActivity):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        private var plan:Boolean = false
        private lateinit var financeViewModel:FinanceViewModel

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.card_history, parent, false)
            financeViewModel = ViewModelProvider(activity)[FinanceViewModel::class.java]
            return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return history.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HistoryAdapter.HistoryViewHolder){
            holder.bind(history[position], position)
        }
    }

    fun checkPlan(isChecked:Boolean) {
        plan = isChecked
    }

    //newValue - новое значение в валюте цели
    //valueInCurrency - значение в валюте бюджета
    //beginValue - в валюте бюджета
    //beginValueBase - baseAmount, в валюте ЦЕЛИ
    private fun editGoalHistory(goalItem: GoalItemWithKey?, budgetItem: BudgetItemWithKey, position: Int, newValue:String, valueInCurrency: String, beginValue:String, beginValueBase:String){

        when {
            goalItem == null -> Toast.makeText(
                context,
                "Такой цели больше нет, операцию нельзя изменить.",
                Toast.LENGTH_SHORT
            ).show()
            (goalItem.goalItem.current.toDouble() + if (history[position].baseAmount.contains('-'))(beginValueBase.toDouble()-newValue.toDouble()) else 0.0 )<-9.0E-6
            -> Toast.makeText(
                context,
                "При изменении Вы уйдете в минус в цели!",
                Toast.LENGTH_SHORT
            ).show()
            else -> {
                val minus = if (history[position].baseAmount.contains("-")) "-" else ""
                budgetItem.budgetItem.amount = when {
                    //с - снимаем с цели и переводим на бюджет
                    history[position].amount.contains("-")-> {
                        goalItem.goalItem.current = "%.2f".format(goalItem.goalItem.current.toDouble() + beginValueBase.toDouble() - newValue.toDouble()).replace(",", ".")
                        "%.2f".format(budgetItem.budgetItem.amount.toDouble() - beginValue.toDouble() + valueInCurrency.toDouble()).replace(",", ".")
                    }
                    //поступление на цель
                    else-> {
                        goalItem.goalItem.current = "%.2f".format(goalItem.goalItem.current.toDouble() - beginValueBase.toDouble() + newValue.toDouble()).replace(",", ".")
                        "%.2f".format(budgetItem.budgetItem.amount.toDouble() + beginValue.toDouble() - valueInCurrency.toDouble()).replace(",", ".")
                    }
                }

                when (history[position].budgetId) {
                    "Base budget" -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Base budget")
                            .child("amount")
                            .setValue(budgetItem.budgetItem.amount)

                        history[position].amount = "$minus${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}"
                        history[position].baseAmount = "$minus${"%.2f".format(newValue.toDouble()).replace(",", ".")}"

                        table.child("Users").child(auth.currentUser!!.uid).child("History")
                            .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                            .child(history[position].key)
                            .setValue(history[position])
                    }
                    else -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItem.key)
                            .setValue(budgetItem.budgetItem)

                        history[position].amount = "$minus${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}"
                        history[position].baseAmount = "$minus${"%.2f".format(newValue.toDouble()).replace(",", ".")}"

                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("History")
                            .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                            .child(history[position].key)
                            .setValue(history[position])
                    }
                }

                if (goalItem.goalItem.current.toDouble()<goalItem.goalItem.target.toDouble())goalItem.goalItem.isReached = false
                else goalItem.goalItem.isReached = true

                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("Goals")
                    .child(history[position].placeId)
                    .setValue(goalItem.goalItem)
            }
        }
    }

    //newValue - новое значение в валюте бюджета с которого переводили
    //valueInCurrency - новое значение в валюте бюджета на который перевели
    //beginValue - значение в валюте бюджета на который перевели
    //beginValueBase - значение в валюте бюджета с которого переводили
    private fun editTransferHistory(budgetItemFrom: BudgetItemWithKey, budgetItemTo: BudgetItemWithKey?, position: Int, newValue:String, valueInCurrency: String, beginValue:String, beginValueBase:String){
        when {
            budgetItemTo == null -> Toast.makeText(
                context,
                "Бюджета, на который переводились средства, больше нет, операцию нельзя изменить.",
                Toast.LENGTH_SHORT
            ).show()

            (budgetItemFrom.budgetItem.amount.toDouble() - newValue.toDouble())<-9.0E-6
            -> Toast.makeText(
                context,
                "При изменении Вы уйдете в минус в бюджете!",
                Toast.LENGTH_SHORT
            ).show()
            else -> {
                budgetItemFrom.budgetItem.amount =  "%.2f".format(budgetItemFrom.budgetItem.amount.toDouble() + beginValueBase.toDouble() - newValue.toDouble()).replace(",", ".")
                budgetItemTo.budgetItem.amount =  "%.2f".format(budgetItemTo.budgetItem.amount.toDouble() - beginValue.toDouble() + valueInCurrency.toDouble()).replace(",", ".")

                when (budgetItemFrom.key) {
                    "Base budget" -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Base budget")
                            .child("amount")
                            .setValue(budgetItemFrom.budgetItem.amount)

                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItemTo.key)
                            .setValue(budgetItemTo.budgetItem)
                    }
                    else -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItemFrom.key)
                            .setValue(budgetItemFrom.budgetItem)

                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Base budget")
                            .child("amount")
                            .setValue(budgetItemTo.budgetItem.amount)
                    }
                }

                history[position].amount = "%.2f".format(valueInCurrency.toDouble()).replace(",", ".")
                history[position].baseAmount = "%.2f".format(newValue.toDouble()).replace(",", ".")

                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("History")
                    .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                    .child(history[position].key)
                    .setValue(history[position])
            }
        }
    }

    private fun editCategoryHistory(categoryItem: CategoryItemWithKey?, budgetItem: BudgetItemWithKey, position: Int, newValue:String, valueInCurrency: String, beginValue:String, beginValueBase:String){
        when (categoryItem) {
            null -> Toast.makeText(
                context,
                "Такой категории больше нет, операцию нельзя изменить.",
                Toast.LENGTH_SHORT
            ).show()

            else -> {

                budgetItem.budgetItem.amount = "%.2f".format(budgetItem.budgetItem.amount.toDouble() + beginValue.toDouble() - valueInCurrency.toDouble()).replace(",", ".")

                when (history[position].budgetId) {
                    "Base budget" -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Base budget")
                            .child("amount")
                            .setValue(budgetItem.budgetItem.amount)

                        history[position].amount = "-${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${history[position].amount.filter { amount -> !amount.isDigit() }.replace("-", "").replace(".", "")}"
                        history[position].baseAmount = "-${"%.2f".format(newValue.toDouble()).replace(",", ".")}"

                        table.child("Users").child(auth.currentUser!!.uid).child("History")
                            .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                            .child(history[position].key)
                            .setValue(history[position])
                    }
                    else -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItem.key)
                            .setValue(budgetItem.budgetItem)

                        history[position].amount = "-${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${
                        history[position].amount.filter { amount -> !amount.isDigit() }.replace("-", "").replace(".", "")}"

                        history[position].baseAmount = "-${"%.2f".format(newValue.toDouble()).replace(",", ".")}"
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("History")
                            .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                            .child(history[position].key)
                            .setValue(history[position])
                    }
                }

                when (categoryItem.categoryItem.remainder) {
                    "0" -> categoryItem.categoryItem.total = "%.2f".format(categoryItem.categoryItem.total.toDouble() - beginValueBase.toDouble() + newValue.toDouble()).replace(",", ".")
                    else -> categoryItem.categoryItem.remainder = "%.2f".format(categoryItem.categoryItem.remainder.toDouble() + beginValueBase.toDouble() - newValue.toDouble()).replace(",", ".")
                    }

                    table.child("Users")
                        .child(auth.currentUser!!.uid)
                        .child("Categories")
                        .child("${history[position].date.split(".")[2]}/${
                                history[position].date.split(".")[1].toInt()}")
                        .child("ExpenseCategories")
                        .child(history[position].placeId)
                        .setValue(categoryItem.categoryItem)
            }
        }
    }

    fun saveItemAtPosition(position: Int, newValue:String, valueInCurrency: String, beginValue:String, beginValueBase:String, dateBegin:Triple<Int, Int, Int>, dateNew:Triple<Int, Int, Int>, time:String, period:String){
        if(!plan) {
            val budgetItem = financeViewModel.budgetLiveData.value!!.find { it.key == history[position].budgetId }
            if (budgetItem==null){
                Toast.makeText(
                    context,
                    "Такого бюджета больше нет, операцию нельзя изменить.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else{
                if (history[position].isCategory == true || history[position].isGoal == true || history[position].isLoan == true || history[position].placeId!="" ||  history[position].isTransfer == true) {
                    when {
                        history[position].isCategory == true -> editCategoryHistory(
                            categoryItem = financeViewModel.categoryLiveData.value!!.find { it.key == history[position].placeId },
                            budgetItem = budgetItem,
                            position = position,
                            newValue = newValue,
                            valueInCurrency = valueInCurrency,
                            beginValue = beginValue,
                            beginValueBase = beginValueBase
                        )

                        history[position].isGoal == true -> editGoalHistory(
                            goalItem = financeViewModel.goalsData.value!!.find { it.key == history[position].placeId },
                            budgetItem = budgetItem,
                            position = position,
                            newValue = newValue,
                            valueInCurrency = valueInCurrency,
                            beginValue = beginValue,
                            beginValueBase = beginValueBase
                        )

                        history[position].isTransfer == true-> editTransferHistory(
                            budgetItemFrom = budgetItem,
                            budgetItemTo = financeViewModel.budgetLiveData.value!!.find { it.key == history[position].placeId },
                            position = position,
                            newValue = newValue,
                            valueInCurrency = valueInCurrency,
                            beginValue = beginValue,
                            beginValueBase = beginValueBase
                        )

                        else ->/*removeCategoryHistory()*/ {}
                    }
                }

                else{
                    budgetItem.budgetItem.amount = "%.2f".format(budgetItem.budgetItem.amount.toDouble() - beginValue.toDouble() + valueInCurrency.toDouble()).replace(",", ".")
                    when (history[position].budgetId) {
                        "Base budget" -> {
                            table.child("Users")
                                .child(auth.currentUser!!.uid)
                                .child("Budgets")
                                .child("Base budget")
                                .child("amount")
                                .setValue(budgetItem.budgetItem.amount)

                            table.child("Users")
                                .child(auth.currentUser!!.uid).child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key).child("amount")
                                .setValue("${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${history[position].amount.filter { amount -> !amount.isDigit() }.replace("-", "").replace(".", "")}")

                            table.child("Users")
                                .child(auth.currentUser!!.uid).child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key).child("baseAmount")
                                .setValue("${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${history[position].amount.filter { amount -> !amount.isDigit() }.replace("-", "").replace(".", "")}")

                        }

                        else -> {
                            table.child("Users")
                                .child(auth.currentUser!!.uid)
                                .child("Budgets")
                                .child("Other budget")
                                .child(budgetItem.key)
                                .setValue(budgetItem.budgetItem)

                            table.child("Users")
                                .child(auth.currentUser!!.uid)
                                .child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key)
                                .child("amount")
                                .setValue("${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${history[position].amount.filter { amount -> !amount.isDigit() }.replace("-", "").replace(".", "")}")

                            table.child("Users")
                                .child(auth.currentUser!!.uid)
                                .child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key)
                                .child("baseAmount")
                                .setValue("${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${history[position].amount.filter { amount -> !amount.isDigit() }.replace("-", "").replace(".", "")}")

                        }
                    }
                }
            }
        }
        else{
            if(history[position].date.split(".")[2].toInt()>Calendar.getInstance().get(Calendar.YEAR)||
                history[position].date.split(".")[1].toInt()>Calendar.getInstance().get(Calendar.MONTH)+1) {
                val referenceCategory =
                    table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                        .child(
                            "${history[position].date.split(".")[2]}/${
                                history[position].date.split(
                                    "."
                                )[1].toInt()
                            }"
                        )
                        .child("ExpenseCategories").child(history[position].placeId)

                referenceCategory.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.getValue(_CategoryItem::class.java)?.let {category->
                            if ((dateNew.third > Calendar.getInstance().get(Calendar.YEAR) ||
                                        dateNew.second > (Calendar.getInstance().get(Calendar.MONTH) + 1)) && category.total!="0.000"
                            ) {

                                category.total = ("%.2f".format(category.total.toDouble() - abs(beginValueBase.toDouble()) + abs(newValue.toDouble()))).replace(",", ".")
                                table.child("Users")
                                    .child(auth.currentUser!!.uid)
                                    .child("Categories")
                                    .child("${dateNew.third}/${dateNew.second}")
                                    .child("ExpenseCategories")
                                    .child(history[position].placeId).setValue(category)
                                if ((dateNew.second != dateBegin.second || dateNew.third != dateBegin.third)||
                                    (dateNew.second==dateBegin.second&&dateNew.third==dateBegin.third &&
                                            category.total=="0.000")) {
                                    referenceCategory.removeValue()
                                }
                            }

                        }


                        val referencePlan =  table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                            .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                            .child(history[position].key)
                        history[position].date =  "${if(dateNew.first.toString().length==2) dateNew.first else "0"+dateNew.first}.${if(dateNew.second.toString().length==2)dateNew.second else "0"+dateNew.second}.${dateNew.third}"
                        history[position].amount = "-${newValue}${history[position].amount.filter {amount-> !amount.isDigit()}.replace("-","").replace(".", "")}"

                        history[position].amount = "-${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${
                            history[position].amount.filter { amount -> !amount.isDigit() }
                                .replace("-", "").replace(".", "")
                        }"
                        history[position].baseAmount = "-${"%.2f".format(newValue.toDouble()).replace(",", ".")}"

                        val historyItem = history[position]

                        table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                            .child("${dateNew.third}/${dateNew.second}").child(history[position].key).setValue(history[position])
                        if(dateNew.second!=dateBegin.second||dateNew.third!=dateBegin.third) {
                            referencePlan.removeValue()
                        }

                        BudgetNotificationManager.cancelAlarmManager(context, history[position].key)
                        BudgetNotificationManager.cancelAutoTransaction(context, history[position].key)
                        BudgetNotificationManager.deleteSharedPreference(history[position].key, context)
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.YEAR, dateNew.third)
                        calendar.set(Calendar.MONTH, dateNew.second-1)
                        calendar.set(Calendar.DAY_OF_MONTH, dateNew.first)

                        BudgetNotificationManager.setAutoTransaction(
                            context = context,
                            id = historyItem.key,
                            placeId = historyItem.placeId,
                            year = dateNew.third,
                            month = dateNew.second,
                            dateOfExpence = calendar,
                            type = Constants.CHANNEL_ID_PLAN
                        )
                        BudgetNotificationManager.notification(context,  Constants.CHANNEL_ID_PLAN, historyItem.key, historyItem.placeId,time,calendar,period)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

            } else{
                val referencePlan =  table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                    .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                    .child(history[position].key)
                history[position].date =  "${if(dateNew.first.toString().length==2) dateNew.first else "0"+dateNew.first}.${if(dateNew.second.toString().length==2)dateNew.second else "0"+dateNew.second}.${dateNew.third}"
                history[position].amount = "-${newValue}${history[position].amount.filter {amount-> !amount.isDigit()}.replace("-","").replace(".", "")}"

                history[position].amount = "-${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${
                    history[position].amount.filter { amount -> !amount.isDigit() }
                        .replace("-", "").replace(".", "")
                }"
                history[position].baseAmount = "-${"%.2f".format(newValue.toDouble()).replace(",", ".")}"

                val historyItem = history[position]

                table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                    .child("${dateNew.third}/${dateNew.second}").child(history[position].key).setValue(history[position])
                if(dateNew.second!=dateBegin.second||dateNew.third!=dateBegin.third) {
                    referencePlan.removeValue()
                }

                BudgetNotificationManager.cancelAlarmManager(context, history[position].key)
                BudgetNotificationManager.cancelAutoTransaction(context, history[position].key)
                BudgetNotificationManager.deleteSharedPreference(history[position].key, context)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, dateNew.third)
                calendar.set(Calendar.MONTH, dateNew.second-1)
                calendar.set(Calendar.DAY_OF_MONTH, dateNew.first)

                BudgetNotificationManager.setAutoTransaction(context, historyItem.key, historyItem.placeId,
                     dateNew.third, dateNew.second, calendar, Constants.CHANNEL_ID_PLAN)
                BudgetNotificationManager.notification(context,  Constants.CHANNEL_ID_PLAN,  historyItem.key, historyItem.placeId, time,calendar,period)

            }
        }
    }

    fun deleteItemAtPosition(position: Int, value:String){
        if(!plan) {
            val budgetItem = financeViewModel.budgetLiveData.value!!.find { it.key == history[position].budgetId }

            if (budgetItem==null){
                Toast.makeText(
                    context,
                    "Такого бюджета больше нет, операцию нельзя удалить.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else{
                if (history[position].isCategory == true || history[position].isGoal == true || history[position].isLoan == true || history[position].isSub == true || history[position].isTransfer == true) {
                    when {
                        history[position].isCategory == true->removeCategoryHistory(
                            budgetItem = budgetItem,
                            categoryItem = financeViewModel.categoryLiveData.value!!.find { it.key == history[position].placeId },
                            value = value,
                            position = position
                        )
                        history[position].isGoal == true->removeGoalHistory(
                            budgetItem = budgetItem,
                            goalItem = financeViewModel.goalsData.value!!.find { it.key == history[position].placeId },
                            value = value,
                            position = position
                        )
                        history[position].isTransfer == true->removeTransferHistory(
                            budgetItemFrom = budgetItem,
                            budgetItemTo = financeViewModel.budgetLiveData.value!!.find { it.key == history[position].placeId },
                            position = position
                        )
                        history[position].isLoan == true-> removeLoanHistory(
                            budgetItem = budgetItem,
                            loanItem = financeViewModel.loansLiveData.value!!.find { it.key == history[position].placeId },
                            value = value,
                            position = position
                        )
                        history[position].isSub == true->removeSubHistory(
                            budgetItem = budgetItem,
                            subItem = financeViewModel.subLiveData.value!!.find { it.key == history[position].placeId },
                            value = value,
                            position = position
                        )
                    }
                }
                //пополнение бюджета
                else{
                    budgetItem.budgetItem.amount = "%.2f".format(budgetItem.budgetItem.amount.toDouble() - value.toDouble()).replace(",", ".")
                    budgetItem.budgetItem.count-=1

                    when (history[position].budgetId) {
                        "Base budget" -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").setValue(budgetItem.budgetItem)
                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key).removeValue()
                        }

                        else -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(budgetItem.key).setValue(budgetItem.budgetItem)
                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key).removeValue()
                        }
                    }
                }
            }
        } else{
            val historyAmount = history[position].baseAmount.toDouble()
            if(history[position].date.split(".")[2].toInt()>Calendar.getInstance().get(Calendar.YEAR)||
                history[position].date.split(".")[1].toInt()>Calendar.getInstance().get(Calendar.MONTH)+1) {
                val referenceCategory =
                    table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                        .child(
                            "${history[position].date.split(".")[2]}/${
                                history[position].date.split(
                                    "."
                                )[1].toInt()
                            }"
                        )
                        .child("ExpenseCategories").child(history[position].placeId)
                referenceCategory.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.getValue(_CategoryItem::class.java)?.let { categoryItem ->
                            categoryItem.total =
                                "%.2f".format(categoryItem.total.toDouble() - abs(historyAmount))
                                    .replace(",", ".")
                            if (categoryItem.total == "0.000") {
                                referenceCategory.removeValue()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            table.child("Users").child(auth.currentUser!!.uid).child("Plan")
                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                .child(history[position].key).removeValue()
            BudgetNotificationManager.cancelAlarmManager(context, history[position].key)
            BudgetNotificationManager.cancelAutoTransaction(context, history[position].key)
        }
    }

    //по категории в минус не уйдем поэтому не проверяем
    private fun removeCategoryHistory(budgetItem:BudgetItemWithKey, categoryItem: CategoryItemWithKey?, value:String, position:Int){
        when (categoryItem) {
            null -> Toast.makeText(
                context,
                "Такой категории больше нет, операцию нельзя удалить.",
                Toast.LENGTH_SHORT
            ).show()

            else -> {
                budgetItem.budgetItem.amount = "%.2f".format(budgetItem.budgetItem.amount.toDouble() + value.toDouble()).replace(",", ".")
                budgetItem.budgetItem.count -= 1

                when (history[position].budgetId) {
                    "Base budget" -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Base budget")
                            .setValue(budgetItem.budgetItem)
                    }

                    else -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItem.key)
                            .setValue(budgetItem.budgetItem)
                    }
                }
                when (categoryItem.categoryItem.remainder) {
                    "0"->categoryItem.categoryItem.total = "%.2f".format(categoryItem.categoryItem.total.toDouble() - abs(history[position].baseAmount.toDouble())).replace(",", ".")
                    else->categoryItem.categoryItem.remainder = "%.2f".format(categoryItem.categoryItem.remainder.toDouble() + abs(history[position].baseAmount.toDouble())).replace(",", ".")
                }
                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("Categories")
                    .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                    .child("ExpenseCategories")
                    .child(history[position].placeId)
                    .setValue(categoryItem.categoryItem)


                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("History")
                    .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                    .child(history[position].key).removeValue()
            }
        }
    }

    //можем уйти в минус по цели, проверяем
    private fun removeGoalHistory(budgetItem:BudgetItemWithKey, goalItem: GoalItemWithKey?, value:String, position:Int){
        when{
            goalItem == null -> Toast.makeText(
                context,
                "Такой цели больше нет, операцию нельзя удалить.",
                Toast.LENGTH_SHORT
            ).show()

            (goalItem.goalItem.current.toDouble() - if (!history[position].baseAmount.contains('-'))history[position].baseAmount.toDouble() else 0.0)<-9.0E-6
            -> Toast.makeText(
                context,
                "При удалении Вы уйдете в минус в цели!",
                Toast.LENGTH_SHORT
            ).show()

            else -> {
                budgetItem.budgetItem.amount = when {
                    //с - снимаем с цели и переводим на бюджет
                    history[position].amount.contains("-")-> "%.2f".format(budgetItem.budgetItem.amount.toDouble() - value.toDouble()).replace(",", ".")
                    //поступление на цель
                    else-> "%.2f".format(budgetItem.budgetItem.amount.toDouble() + value.toDouble()).replace(",", ".")

                }
                budgetItem.budgetItem.count -= 1
                goalItem.goalItem.current = "%.2f".format(goalItem.goalItem.current.toDouble() - history[position].baseAmount.toDouble()).replace(",", ".")

                when (history[position].budgetId) {
                    "Base budget" -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Base budget")
                            .setValue(budgetItem.budgetItem)
                    }

                    else -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItem.key)
                            .setValue(budgetItem.budgetItem)
                    }
                }

                if (goalItem.goalItem.current.toDouble()<goalItem.goalItem.target.toDouble())goalItem.goalItem.isReached = false
                else goalItem.goalItem.isReached = true

                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("Goals")
                    .child(history[position].placeId)
                    .setValue(goalItem.goalItem)

                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("History")
                    .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                    .child(history[position].key).removeValue()
            }
        }
    }
    private fun removeSubHistory(budgetItem:BudgetItemWithKey, subItem: SubItemWithKey?, value:String, position:Int){
        when{
            subItem == null -> Toast.makeText(
                context,
                "Такой подписки больше нет, операцию нельзя удалить.",
                Toast.LENGTH_SHORT
            ).show()

            else -> {
                budgetItem.budgetItem.amount = "%.2f".format(value.toDouble() + budgetItem.budgetItem.amount.toDouble()).replace(",", ".")
                budgetItem.budgetItem.count -= 1

                when (history[position].budgetId) {
                    "Base budget" -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Base budget")
                            .setValue(budgetItem.budgetItem)
                    }

                    else -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItem.key)
                            .setValue(budgetItem.budgetItem)
                    }
                }

                if (history[position].key ==financeViewModel.historyLiveData.value?.filter { it.placeId == subItem.key }?.maxByOrNull { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it.date)?.time ?: 0 }?.key) {
                    val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(subItem.subItem.date)

                    val calendarPrev = Calendar.getInstance().apply {
                        time = date!!
                    }
                    val calendarNext = Calendar.getInstance().apply {
                        time = date!!
                    }
                    val calendarCurrent = Calendar.getInstance().apply {
                        time = date!!
                    }

                    calendarPrev.add(when(subItem.subItem.period.split(" ")[1]){
                        "d"->Calendar.DAY_OF_MONTH
                        "w"->Calendar.WEEK_OF_MONTH
                        "m"->Calendar.MONTH
                        else->Calendar.YEAR
                    }, subItem.subItem.period.split(" ")[0].toInt() * -1)

                    calendarNext.add(when(subItem.subItem.period.split(" ")[1]){
                        "d"->Calendar.DAY_OF_MONTH
                        "w"->Calendar.WEEK_OF_MONTH
                        "m"->Calendar.MONTH
                        else->Calendar.YEAR
                    }, subItem.subItem.period.split(" ")[0].toInt())

                    val sharedPreferences = context.getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
                    val periodBegin = sharedPreferences.getString(subItem.key, "|")?.split("|")?.get(0)?:context.resources.getStringArray(R.array.periodicity)[0]
                    val timeBegin = sharedPreferences.getString(subItem.key, "|")?.split("|")?.get(1)?:"12:00"

                    BudgetNotificationManager.cancelAlarmManager(context, subItem.key)
                    BudgetNotificationManager.cancelAutoTransaction(context, subItem.key)

                    subItem.subItem.date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(
                        when{
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis < calendarPrev.apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis -> {
                                updateNotificationSub(
                                    subItemWithKey = subItem,
                                    calendar = calendarPrev,
                                    timeBegin = timeBegin,
                                    periodBegin = periodBegin
                                )
                                calendarPrev.time
                            }
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis < calendarCurrent.apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis->{
                                updateNotificationSub(
                                    subItemWithKey = subItem,
                                    calendar = calendarCurrent,
                                    timeBegin = timeBegin,
                                    periodBegin = periodBegin
                                )
                                calendarCurrent.time
                            }
                            else ->{
                                updateNotificationSub(
                                    subItemWithKey = subItem,
                                    calendar = calendarNext,
                                    timeBegin = timeBegin,
                                    periodBegin = periodBegin
                                )
                                calendarNext.time
                            }
                        }
                    )

                    table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("Subs")
                    .child(history[position].placeId)
                    .setValue(subItem.subItem)
                }

                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("History")
                    .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                    .child(history[position].key).removeValue()
            }
        }
    }

    private fun removeLoanHistory(budgetItem:BudgetItemWithKey, loanItem: LoanItemWithKey?, value:String, position:Int){
        when{
            loanItem == null -> Toast.makeText(
                context,
                "Такого обязательного платежа больше нет, операцию нельзя удалить.",
                Toast.LENGTH_SHORT
            ).show()

            else -> {
                budgetItem.budgetItem.amount = "%.2f".format(value.toDouble() + budgetItem.budgetItem.amount.toDouble()).replace(",", ".")
                budgetItem.budgetItem.count -= 1

                when (history[position].budgetId) {
                    "Base budget" -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Base budget")
                            .setValue(budgetItem.budgetItem)
                    }

                    else -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItem.key)
                            .setValue(budgetItem.budgetItem)
                    }
                }

                val sharedPreferences = context.getSharedPreferences(
                    "NotificationPeriodAndTime",
                    Context.MODE_PRIVATE)

                val periodBegin =
                    sharedPreferences.getString(loanItem.key, "|")?.split("|")?.get(0)
                        ?: context.resources.getStringArray(R.array.periodicity)[0]
                val timeBegin =
                    sharedPreferences.getString(loanItem.key, "|")?.split("|")?.get(1)
                        ?: "12:00"

                if (history[position].key == financeViewModel.historyLiveData.value?.filter { it.placeId == loanItem.key }?.maxByOrNull { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it.date)?.time ?: 0 }?.key) {

                    val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(loanItem.loanItem.dateNext?:loanItem.loanItem.dateOfEnd)
                    val dateEnd = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(loanItem.loanItem.dateOfEnd)

                    val calendarPrev = Calendar.getInstance().apply {
                        time = date!!
                    }
                    val calendarEnd = Calendar.getInstance().apply {
                        time = dateEnd!!
                    }
                    if(loanItem.loanItem.period!=null) {
                        calendarPrev.add(
                            when (loanItem.loanItem.period!!.split(" ")[1]) {
                                "d" -> Calendar.DAY_OF_MONTH
                                "w" -> Calendar.WEEK_OF_MONTH
                                "m" -> Calendar.MONTH
                                else -> Calendar.YEAR
                            }, loanItem.loanItem.period!!.split(" ")[0].toInt() * -1
                        )

                        BudgetNotificationManager.cancelAlarmManager(context, loanItem.key)

                        loanItem.loanItem.dateNext = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendarPrev.time)
                        if(Calendar.getInstance().apply {
                                   set(Calendar.HOUR_OF_DAY, 0)
                                       set(Calendar.MINUTE, 0)
                                       set(Calendar.SECOND, 0)
                                       set(Calendar.MILLISECOND, 0)
                               }.timeInMillis < calendarPrev.apply {
                                   set(Calendar.HOUR_OF_DAY, 0)
                                       set(Calendar.MINUTE, 0)
                                       set(Calendar.SECOND, 0)
                                       set(Calendar.MILLISECOND, 0)
                               }.timeInMillis
                                   &&
                                   calendarPrev.apply {
                                       set(Calendar.HOUR_OF_DAY, 0)
                                       set(Calendar.MINUTE, 0)
                                       set(Calendar.SECOND, 0)
                                       set(Calendar.MILLISECOND, 0)
                                   }.timeInMillis <= calendarEnd.apply {
                                       set(Calendar.HOUR_OF_DAY, 0)
                                       set(Calendar.MINUTE, 0)
                                       set(Calendar.SECOND, 0)
                                       set(Calendar.MILLISECOND, 0)
                                   }.timeInMillis) {
                                   if (!loanItem.loanItem.isDeleted){
                                            BudgetNotificationManager.notification(
                                                context = context,
                                                channelID = Constants.CHANNEL_ID_SUB,
                                                id = loanItem.key,
                                                placeId = loanItem.key,
                                                time = timeBegin,
                                                dateOfExpence = calendarPrev,
                                                periodOfNotification = periodBegin)
                                   }
                               }
                    } else {
                        if (!loanItem.loanItem.isDeleted &&
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis < calendarEnd.apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis){

                            BudgetNotificationManager.notification(
                                context = context,
                                channelID = Constants.CHANNEL_ID_SUB,
                                id = loanItem.key,
                                placeId = loanItem.key,
                                time = timeBegin,
                                dateOfExpence = calendarPrev,
                                periodOfNotification = periodBegin)
                        }
                    }

                    if (loanItem.loanItem.isFinished) loanItem.loanItem.isFinished = false

                    table.child("Users")
                        .child(auth.currentUser!!.uid)
                        .child("Loans")
                        .child(history[position].placeId)
                        .setValue(loanItem.loanItem)
                }

                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("History")
                    .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                    .child(history[position].key).removeValue()
            }
        }
    }



    private fun updateNotificationSub(subItemWithKey: SubItemWithKey, calendar:Calendar, timeBegin:String, periodBegin:String){
        if (!subItemWithKey.subItem.isCancelled && !subItemWithKey.subItem.isDeleted) {
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
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH)+1,
                dateOfExpence = calendar,
                type = Constants.CHANNEL_ID_SUB
            )
        }
    }

    private fun removeTransferHistory(budgetItemFrom:BudgetItemWithKey, budgetItemTo: BudgetItemWithKey?, position: Int){
        when{
            budgetItemTo == null -> Toast.makeText(
                context,
                "Такого бюджета начисления больше нет, операцию нельзя удалить.",
                Toast.LENGTH_SHORT
            ).show()

            (budgetItemTo.budgetItem.amount.toDouble() - history[position].amount.toDouble())<-9.0E-6
            -> Toast.makeText(
                context,
                "При удалении Вы уйдете в минус в бюджете начисления!",
                Toast.LENGTH_SHORT
            ).show()

            else -> {
                budgetItemFrom.budgetItem.amount =
                    "%.2f".format(budgetItemFrom.budgetItem.amount.toDouble() + history[position].baseAmount.toDouble()).replace(",", ".")

                budgetItemTo.budgetItem.amount =
                    "%.2f".format(budgetItemTo.budgetItem.amount.toDouble() - history[position].amount.toDouble()).replace(",", ".")

                budgetItemFrom.budgetItem.count -= 1
                budgetItemTo.budgetItem.count -= 1

                when (budgetItemFrom.key) {
                    "Base budget" -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Base budget")
                            .setValue(budgetItemFrom.budgetItem)

                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItemTo.key)
                            .setValue(budgetItemTo.budgetItem)
                    }

                    else -> {
                        table.child("Users")
                            .child(auth.currentUser!!.uid)
                            .child("Budgets")
                            .child("Other budget")
                            .child(budgetItemFrom.key)
                            .setValue(budgetItemFrom.budgetItem)

                        when (budgetItemTo.key){
                            "Base budget" -> {
                                table.child("Users")
                                    .child(auth.currentUser!!.uid)
                                    .child("Budgets")
                                    .child("Base budget")
                                    .setValue(budgetItemTo.budgetItem)
                            }
                            else ->{
                                table.child("Users")
                                    .child(auth.currentUser!!.uid)
                                    .child("Budgets")
                                    .child("Other budget")
                                    .child(budgetItemFrom.key)
                                    .setValue(budgetItemTo.budgetItem)
                            }
                        }
                    }
                }
                table.child("Users")
                    .child(auth.currentUser!!.uid)
                    .child("History")
                    .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                    .child(history[position].key).removeValue()
            }
        }
    }

    fun sortByDate(startDate:Calendar?, endDate:Calendar?, historyNew:List<HistoryItem> = history){
        if (historyNew.isNotEmpty()){
            val startDef = Calendar.getInstance()
            val endDef = Calendar.getInstance()
            startDef.apply{
                set(Calendar.HOUR, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            endDef.apply{
                set(Calendar.DAY_OF_MONTH, endDef.getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            sort(
                historyNew.asSequence()
                    .filter { it.date.isNotEmpty() && SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .parse(it.date)?.let { calendar ->
                            val calendarFromString = Calendar.getInstance()
                            calendarFromString.time = calendar
                            calendarFromString.apply {
                                set(Calendar.HOUR, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            (calendarFromString.timeInMillis >= (startDate?.timeInMillis
                                ?: startDef.timeInMillis)
                                    &&
                                    calendarFromString.timeInMillis <= (endDate?.timeInMillis
                                        ?: endDef.timeInMillis))
                        } ?: false}.toList().sortedByDescending {SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it.date)})
        }
        else sort(emptyList())
    }

    private fun sort(newHistory: List<HistoryItem>){
        history = newHistory
        notifyDataSetChanged()
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameBudgetTextView: TextView = itemView.findViewById(R.id.historyNameBudget)
        private val income: TextView = itemView.findViewById(R.id.incomeTextView)
        private val nameBudgetForPlace: TextView = itemView.findViewById(R.id.historyNameBudget2)
        private val placeNameTextView: TextView = itemView.findViewById(R.id.historyNameCategory)
        private val valueOfTransaction: TextView = itemView.findViewById(R.id.valueOfTransaction)
        private val imageHistory: ImageView = itemView.findViewById(R.id.historyImage)
        private val date: TextView = itemView.findViewById(R.id.dateOfTransaction)


        private val card: LinearLayout = itemView.findViewById(R.id.cardHistory)

        fun bind(historyItem: HistoryItem, position: Int) {
           val financeViewModel = ViewModelProvider(activity)[FinanceViewModel::class.java]

            valueOfTransaction.text = historyItem.amount + context.resources.getString( context.resources.getIdentifier( financeViewModel.budgetLiveData.value?.find {  it.key == if(historyItem.isTransfer==true) historyItem.placeId else historyItem.budgetId}?.budgetItem?.currency, "string",  context.packageName))

            when(historyItem.placeId){
                ""->{
                    nameBudgetForPlace.visibility = View.GONE
                    nameBudgetTextView.visibility = View.VISIBLE
                    nameBudgetTextView.text = financeViewModel.budgetLiveData.value!!.filter { it.key == historyItem.budgetId}[0].budgetItem.name
                    placeNameTextView.visibility = View.GONE
                    imageHistory.visibility = View.GONE
                    income.visibility = View.VISIBLE
                    valueOfTransaction.setTextColor(context.resources.getColor(R.color.dark_green, context.theme))
                }
                else->{
                    when {
                        historyItem.isGoal == true && historyItem.amount.contains('-')-> {income.visibility = View.VISIBLE}
                        historyItem.isTransfer == true -> income.visibility = View.VISIBLE
                        else -> income.visibility = View.GONE
                    }
                    nameBudgetTextView.visibility =  when{
                        historyItem.isGoal == true -> {
                            nameBudgetTextView.text = financeViewModel.goalsData.value!!.find { history[position].placeId == it.key }!!.goalItem.name
                            View.VISIBLE
                        }

                        historyItem.isTransfer==true->{
                            nameBudgetTextView.text =  financeViewModel.budgetLiveData.value!!.find { history[position].placeId == it.key }!!.budgetItem.name
                            View.VISIBLE
                        }
                        else -> View.GONE
                    }
                    nameBudgetForPlace.visibility = View.VISIBLE
                    placeNameTextView.visibility = View.VISIBLE
                    imageHistory.visibility = View.VISIBLE
                    setTextColor(historyItem)
                    nameBudgetForPlace.text =  financeViewModel.budgetLiveData.value!!.find { history[position].budgetId == it.key }!!.budgetItem.name
                    placeNameTextView.text =
                        if(historyItem.isLoan == true) {
                            placeNameTextView.visibility = View.VISIBLE
                            financeViewModel.loansLiveData.value!!.find { history[position].placeId == it.key }!!.loanItem.name
                        }
                        else if (historyItem.isCategory == true) {
                            placeNameTextView.visibility = View.VISIBLE
                            financeViewModel.categoryBeginLiveData.value!!.find { history[position].placeId == it.key }!!.categoryBegin.name
                        }
                        else if (historyItem.isSub == true){
                            placeNameTextView.visibility = View.VISIBLE
                            financeViewModel.subLiveData.value!!.find { history[position].placeId == it.key }!!.subItem.name
                        }
                        else {placeNameTextView.visibility = View.GONE
                            ""}
                    imageHistory.visibility = when{
                        historyItem.isTransfer == true -> View.GONE
                        else->{
                            imageHistory.setImageDrawable(ContextCompat.getDrawable(context, context.resources.getIdentifier(when{
                                history[position].isCategory==true->financeViewModel.categoryBeginLiveData.value!!.find { history[position].placeId == it.key }!!.categoryBegin.path
                                history[position].isGoal==true->financeViewModel.goalsData.value!!.find { history[position].placeId == it.key }!!.goalItem.path
                                history[position].isSub==true->financeViewModel.subLiveData.value!!.find { history[position].placeId == it.key }!!.subItem.path
                                history[position].isLoan==true->financeViewModel.loansLiveData.value!!.find { history[position].placeId == it.key }!!.loanItem.path
                                else->"family"
                            }, "drawable", context.packageName)))
                            View.VISIBLE
                        }
                    }
                }
            }

            if(position==0 ||  history[position-1].date != historyItem.date){
                date.visibility = View.VISIBLE
                date.text = historyItem.date
            } else {
                date.visibility = View.GONE
            }

            card.setOnClickListener {
                openEditOrDeleteDialog(position)
            }
        }

        private fun setTextColor(historyItem: HistoryItem){
            when{
                historyItem.isGoal==true -> {
                    if (historyItem.amount.contains('-'))valueOfTransaction.setTextColor(context.resources.getColor(R.color.dark_orange, context.theme))
                    else valueOfTransaction.setTextColor(context.resources.getColor(R.color.dark_green, context.theme))
                }
                historyItem.isTransfer==true -> valueOfTransaction.setTextColor(context.resources.getColor(R.color.dark_green, context.theme))
                else -> valueOfTransaction.setTextColor(context.resources.getColor(R.color.dark_orange, context.theme))
            }

        }

        private fun openEditOrDeleteDialog(position: Int){
            val dialogView = View.inflate(context, R.layout.card_edit_history, null)
            val builder = AlertDialog.Builder(context)
            builder.setView(dialogView)
            builder.setTitle("Редактирование операции")

            val value = dialogView.findViewById<EditText>(R.id.editValueHistory)
            val calendar = dialogView.findViewById<CalendarView>(R.id.datePickerEdit)
            val periodOfNotificationTitleHistory: TextView = dialogView.findViewById(R.id.periodOfNotificationTitleHistory)
            val periodOfNotificationHistory: Spinner = dialogView.findViewById(R.id.periodOfNotificationHistory)
            val timeOfNotificationsTitleHistory: TextView = dialogView.findViewById(R.id.timeOfNotificationsTitleHistory)
            val timeOfNotificationsHistory: TextView = dialogView.findViewById(R.id.timeOfNotificationsHistory)

            val dateBegin = Calendar.getInstance()
            val dateBeginTriple:Triple<Int, Int, Int> = Triple(
            history[position].date.split(".")[0].toInt(),
            history[position].date.split(".")[1].toInt(),
            history[position].date.split(".")[2].toInt()
            )
            var dateNew:Triple<Int, Int, Int> = Triple(
                history[position].date.split(".")[0].toInt(),
                history[position].date.split(".")[1].toInt(),
                history[position].date.split(".")[2].toInt()
            )
            val sharedPreferences = context.getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
            val periodList = context.resources.getStringArray(R.array.periodicity)

            var adapterPeriod = ArrayAdapter(context, android.R.layout.simple_spinner_item, periodList)
            adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            periodOfNotificationHistory.adapter = adapterPeriod

            val periodBegin = sharedPreferences.getString(history[position].key, "|")?.split("|")?.get(0)?:periodList[0]
            val timeBegin = sharedPreferences.getString(history[position].key, "|")?.split("|")?.get(1)?:"12:00"

            periodOfNotificationHistory.onItemSelectedListener = object : OnItemSelectedListener{
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                   if(id!=0L){
                       timeOfNotificationsHistory.visibility = View.VISIBLE
                       timeOfNotificationsTitleHistory.visibility = View.VISIBLE
                   } else {
                       timeOfNotificationsHistory.visibility = View.GONE
                       timeOfNotificationsTitleHistory.visibility = View.GONE
                   }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    timeOfNotificationsHistory.visibility = View.GONE
                    timeOfNotificationsTitleHistory.visibility = View.GONE
                }

            }

            calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
                dateNew = Triple(dayOfMonth, month+1, year)
                val dateEnd = LocalDate.of(year, month+1, dayOfMonth)
                val dateNow = LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                val daysBetween = ChronoUnit.DAYS.between(dateNow, dateEnd)
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

                adapterPeriod = ArrayAdapter(context, android.R.layout.simple_spinner_item, resultList)
                adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                periodOfNotificationHistory.adapter = adapterPeriod
                periodOfNotificationHistory.setSelection(resultList.indexOf(periodBegin.ifEmpty {
                    periodList[0]
                }))
                timeOfNotificationsHistory.text = timeBegin.ifEmpty { "12:00" }

            }

            calendar.visibility = when(plan){
                true -> {
                    Calendar.getInstance().apply{
                        set(Calendar.DAY_OF_MONTH, this.get(Calendar.DAY_OF_MONTH)+1)
                        calendar.minDate = this.timeInMillis
                    }
                   dateBegin.apply {
                        set(Calendar.DAY_OF_MONTH, history[position].date.split(".")[0].toInt())
                        set(Calendar.MONTH, history[position].date.split(".")[1].toInt()-1)
                        set(Calendar.YEAR, history[position].date.split(".")[2].toInt())
                       calendar.date = this.timeInMillis
                   }
                    val dateEnd = LocalDate.of(history[position].date.split(".")[2].toInt(), history[position].date.split(".")[1].toInt(), history[position].date.split(".")[0].toInt())
                    val dateNow = LocalDate.of(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                    val daysBetween = ChronoUnit.DAYS.between(dateNow, dateEnd)
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

                    if (periodBegin.isNotEmpty()){
                        if (resultList.indexOf(periodBegin) == -1)resultList.add(periodBegin)
                        when(periodList.indexOf(periodBegin)){
                            0 -> {
                                timeOfNotificationsTitleHistory.visibility = View.GONE
                                timeOfNotificationsHistory.visibility = View.GONE
                            }
                            else -> {
                                timeOfNotificationsTitleHistory.visibility = View.VISIBLE
                                timeOfNotificationsHistory.visibility = View.VISIBLE
                            }
                        }
                        timeOfNotificationsHistory.text = timeBegin.ifEmpty { "12:00" }
                    }

                    adapterPeriod = ArrayAdapter(context, android.R.layout.simple_spinner_item, resultList)
                    adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    periodOfNotificationHistory.adapter = adapterPeriod
                    periodOfNotificationHistory.setSelection(resultList.indexOf(periodBegin.ifEmpty {
                        periodList[0]
                    }))

                    View.VISIBLE
                }
                else -> View.GONE
            }

            when(plan){
                true->{
                    periodOfNotificationTitleHistory.visibility = View.VISIBLE
                    periodOfNotificationHistory.visibility = View.VISIBLE

                    timeOfNotificationsHistory.setOnClickListener {
                        it as TextView
                        val cal = Calendar.getInstance()
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        val minute = cal.get(Calendar.MINUTE)
                        val timePickerDialog = TimePickerDialog(context, { _, selectedHour, selectedMinute ->
                            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                            it.text = formattedTime }, hour, minute, true)
                        timePickerDialog.show()
                    }
                } else ->{
                    periodOfNotificationTitleHistory.visibility = View.GONE
                    periodOfNotificationHistory.visibility = View.GONE
                    timeOfNotificationsTitleHistory.visibility = View.GONE
                    timeOfNotificationsHistory.visibility = View.GONE

                if (history[position].isSub == true || history[position].isLoan == true){
                    value.isEnabled = false
                    }
                }
            }

            val numberBegin = history[position].amount.filter {string-> string.isDigit() || string=='.'}
            value.setText(numberBegin)

            val beginValueBase = history[position].baseAmount.filter {string-> string.isDigit() || string=='.'}

            when{
                plan && Calendar.getInstance().apply {
                    set(history[position].date.split(".")[2].toInt(), history[position].date.split(".")[1].toInt()-1, history[position].date.split(".")[0].toInt(), 0, 0, 0)
                }.timeInMillis <= Calendar.getInstance().timeInMillis->{
                    calendar.visibility = View.GONE
                    value.isEnabled = false
                    periodOfNotificationTitleHistory.visibility = View.GONE
                    periodOfNotificationHistory.visibility = View.GONE
                    timeOfNotificationsTitleHistory.visibility = View.GONE
                    timeOfNotificationsHistory.visibility = View.GONE

                    builder.setPositiveButton("Списать"){ dialog, _->
                        updateCategory(
                            year = history[position].date.split(".")[2].toInt(),
                            month = history[position].date.split(".")[1].toInt(),
                            categoryId = history[position].placeId,
                            planId = history[position].key
                        )
                        dialog.dismiss()
                    }

                    builder.setNegativeButton("Удалить") { dialog, _ ->
                        AlertDialog.Builder(context)
                            .setTitle("Удаление операции")
                            .setMessage("Вы уверены, что хотите удалить транзакцию?")
                            .setPositiveButton("Подтвердить") { dialog2, _ ->
                                deleteItemAtPosition(position, numberBegin)
                                dialog2.dismiss()
                                dialog.dismiss()
                            }
                            .setNegativeButton("Отмена") { dialog2, _ ->
                                dialog2.dismiss()
                            }.show()
                    }

                    builder.setNeutralButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                }
                else ->{
                    builder.setPositiveButton("Сохранить"){ dialog, _->
                        val newValue = "%.2f".format((abs(history[position].baseAmount.toDouble())/numberBegin.toDouble())*value.text.toString().toDouble()).replace(",", ".")
                        saveItemAtPosition(position, newValue, value.text.toString(), numberBegin, beginValueBase ,dateBeginTriple, dateNew, timeOfNotificationsHistory.text.toString(),
                            periodOfNotificationHistory.selectedItem.toString())
                        dialog.dismiss()
                    }

                    builder.setNegativeButton("Удалить") { dialog, _ ->
                        AlertDialog.Builder(context)
                            .setTitle("Удаление операции")
                            .setMessage("Вы уверены, что хотите удалить транзакцию?\nБудет удалена операция с исходной суммой!")
                            .setPositiveButton("Подтвердить") { dialog2, _ ->
                                deleteItemAtPosition(position, numberBegin)
                                dialog2.dismiss()
                                dialog.dismiss()
                            }
                            .setNegativeButton("Отмена") { dialog2, _ ->
                                dialog2.dismiss()
                            }.show()
                    }

                    builder.setNeutralButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }

                }
            }

            val dialog = builder.create()
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
            dialog.show()
        }
    }

    private fun updateCategory(year: Int, month: Int, categoryId:String, planId: String){
        val categoryReference = table.child("Users").child(auth.currentUser!!.uid)
            .child("Categories").child("$year/$month")
            .child("ExpenseCategories").child(categoryId)

        table.child("Users").child(auth.currentUser!!.uid)
            .child("Plan").child("$year/$month")
            .child(planId).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(HistoryItem::class.java)?.let {
                        categoryReference.runTransaction(object : Transaction.Handler {
                            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                val category = mutableData.getValue(_CategoryItem::class.java)
                                if (category != null) {
                                    when(category.remainder){
                                        "0"->category.total = "%.2f".format(category.total.toDouble()+abs(it.baseAmount.toDouble())).replace(',','.')
                                        else->category.remainder = "%.2f".format(category.remainder.toDouble()-abs(it.baseAmount.toDouble())).replace(',','.')
                                    }
                                    mutableData.value = category
                                }
                                return Transaction.success(mutableData)
                            }

                            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                                if (databaseError != null) {
                                    Log.d("TAG", "Transaction completed. Committed: $committed")
                                } else {
                                    updateBudget(it.budgetId, it.amount, year, month, planId)
                                }
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}

            })
    }

    private fun updateBudget(budgetId:String, amount:String, year: Int, month: Int, planId: String){
        val budgetReference =
            when(budgetId){
                "Base budget"-> table.child("Users").child(auth.currentUser!!.uid)
                    .child("Budgets").child("Base budget")
                else->table.child("Users").child(auth.currentUser!!.uid)
                    .child("Budgets").child("Other budget").child(budgetId)
            }
        budgetReference.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val budget = mutableData.getValue(_BudgetItem::class.java)
                if (budget != null) {
                    budget.amount = "%.2f".format(budget.amount.toDouble() - abs(amount.toDouble())).replace(',', '.')
                    budget.count++
                    mutableData.value = budget
                }
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (databaseError != null) {
                    Log.d("TAG", "Transaction completed. Committed: $committed")
                } else {
                    planToHistory(year, month, planId)
                }
            }
        })
    }

    private fun planToHistory(year: Int, month: Int, planId:String){
        val historyReference = table.child("Users").child(auth.currentUser!!.uid).child("History")
            .child("$year/$month").child(planId)
        val planReference = table.child("Users").child(auth.currentUser!!.uid).child("Plan")
            .child("$year/$month").child(planId)

        planReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(HistoryItem::class.java)?.let {
                    historyReference.setValue(it)
                    planReference.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}