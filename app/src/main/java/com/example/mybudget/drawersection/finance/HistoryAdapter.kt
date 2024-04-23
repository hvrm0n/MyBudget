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
import com.example.mybudget.NotificationManager
import com.example.mybudget.R
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
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

    fun saveItemAtPosition(position: Int, newValue:String, valueInCurrency: String, beginValue:String,
                           beginValueBase:String, dateBegin:Triple<Int, Int, Int>, dateNew:Triple<Int, Int, Int>,
                            time:String, period:String){

        if(!plan) {
            var save = false
            if (history[position].isCategory == true || history[position].isGoal == true || history[position].isLoan == true) {
                val budgetItem =
                    financeViewModel.budgetLiveData.value!!.find { it.key == history[position].budgetId }

                if (budgetItem==null){
                    Toast.makeText(
                        context,
                        "Такого бюджета больше нет, операцию нельзя изменить.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    budgetItem.budgetItem.amount =
                        "%.2f".format(budgetItem.budgetItem.amount.toDouble() + beginValue.toDouble() - valueInCurrency.toDouble())
                            .replace(",", ".")

                    when (history[position].budgetId) {
                        "Base budget" -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                .child("Base budget").child("amount")
                                .setValue(budgetItem.budgetItem.amount)

                            history[position].amount = "-${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${
                                history[position].amount.filter { amount -> !amount.isDigit() }
                                    .replace("-", "").replace(".", "")
                            }"

                            history[position].baseAmount = "-${"%.2f".format(newValue.toDouble()).replace(",", ".")}"
                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child(
                                    "${history[position].date.split(".")[2]}/${
                                        history[position].date.split(
                                            "."
                                        )[1].toInt()
                                    }"
                                )
                                .child(history[position].key).setValue(history[position])
                            save = true
                        }

                        else -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                .child("Other budget").child(budgetItem.key)
                                .setValue(budgetItem.budgetItem)
                            history[position].amount = "-${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${
                                history[position].amount.filter { amount -> !amount.isDigit() }
                                    .replace("-", "").replace(".", "")
                            }"

                            history[position].baseAmount = "-${"%.2f".format(newValue.toDouble()).replace(",", ".")}"
                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child(
                                    "${history[position].date.split(".")[2]}/${
                                        history[position].date.split(
                                            "."
                                        )[1].toInt()
                                    }"
                                )
                                .child(history[position].key).setValue(history[position])
                            save = true
                        }
                    }
                }
                if (!save) {
                    Toast.makeText(
                        context,
                        "Такого бюджета больше нет, операцию нельзя изменить.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    when {
                        history[position].isCategory == true -> {
                            val category =  financeViewModel.categoryLiveData.value!!.find { it.key == history[position].placeId }
                            if(category==null){
                                Toast.makeText(
                                    context,
                                    "Такой категории больше нет, операцию нельзя изменить.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else {

                                when (category.categoryItem.remainder) {
                                    "0" -> category.categoryItem.total =
                                        "%.2f".format(category.categoryItem.total.toDouble() - beginValueBase.toDouble() + newValue.toDouble())
                                            .replace(",", ".")

                                    else -> category.categoryItem.remainder =
                                        "%.2f".format(category.categoryItem.remainder.toDouble() + beginValueBase.toDouble() - newValue.toDouble())
                                            .replace(",", ".")
                                }
                                table.child("Users").child(auth.currentUser!!.uid)
                                    .child("Categories")
                                    .child(
                                        "${history[position].date.split(".")[2]}/${
                                            history[position].date.split(
                                                "."
                                            )[1].toInt()
                                        }"
                                    )
                                    .child("ExpenseCategories")
                                    .child(history[position].placeId)
                                    .setValue(category.categoryItem)
                            }
                        }

                        history[position].isLoan == true -> {}

                        history[position].isGoal == true -> {}
                    }
                }
            }

            else{
                val budgetItem =
                    financeViewModel.budgetLiveData.value!!.find { it.key == history[position].budgetId }
                if (budgetItem == null){
                    Toast.makeText(
                        context,
                        "Такого бюджета больше нет, операцию нельзя изменить.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    budgetItem.budgetItem.amount =
                        "%.2f".format(budgetItem.budgetItem.amount.toDouble() - beginValue.toDouble() + valueInCurrency.toDouble())
                            .replace(",", ".")
                    when (history[position].budgetId) {
                        "Base budget" -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                .child("Base budget").child("amount")
                                .setValue(budgetItem.budgetItem.amount)
                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child(
                                    "${history[position].date.split(".")[2]}/${
                                        history[position].date.split(
                                            "."
                                        )[1].toInt()
                                    }"
                                )
                                .child(history[position].key).child("amount").setValue(
                                    "${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${
                                        history[position].amount.filter { amount -> !amount.isDigit() }
                                            .replace("-", "").replace(".", "")
                                    }"
                                )
                            save = true
                        }

                        else -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                .child("Other budget").child(budgetItem.key)
                                .setValue(budgetItem.budgetItem)
                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child(
                                    "${history[position].date.split(".")[2]}/${
                                        history[position].date.split(
                                            "."
                                        )[1].toInt()
                                    }"
                                )
                                .child(history[position].key).child("amount").setValue(
                                    "${"%.2f".format(valueInCurrency.toDouble()).replace(",", ".")}${
                                        history[position].amount.filter { amount -> !amount.isDigit() }
                                            .replace("-", "").replace(".", "")
                                    }"
                                )
                            save = true
                        }
                    }
                }

                if(!save){
                    Toast.makeText(context, "Такого бюджета больше нет, операцию нельзя изменить.", Toast.LENGTH_SHORT).show()
                }

            }
        } else{
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
                                Log.e("ChekcValue", category.total)
                                Log.e("ChekcValue", beginValue)
                                Log.e("ChekcValue", newValue)

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

                        NotificationManager.cancelAlarmManager(context, history[position].key)
                        NotificationManager.cancelAutoTransaction(context, history[position].key)
                        NotificationManager.deleteSharedPreference(history[position].key, context)
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.YEAR, dateNew.third)
                        calendar.set(Calendar.MONTH, dateNew.second-1)
                        calendar.set(Calendar.DAY_OF_MONTH, dateNew.first)

                        NotificationManager.setAutoTransaction(context, historyItem.key, historyItem.placeId,
                            historyItem.budgetId, dateNew.third, dateNew.second, calendar, historyItem.amount, historyItem.baseAmount)
                        NotificationManager.notification(context,  historyItem.key, financeViewModel.categoryBeginLiveData.value?.find { it.key == historyItem.placeId }!!.categoryBegin.name, time,calendar,period)
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

                NotificationManager.cancelAlarmManager(context, history[position].key)
                NotificationManager.cancelAutoTransaction(context, history[position].key)
                NotificationManager.deleteSharedPreference(history[position].key, context)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, dateNew.third)
                calendar.set(Calendar.MONTH, dateNew.second-1)
                calendar.set(Calendar.DAY_OF_MONTH, dateNew.first)

                NotificationManager.setAutoTransaction(context, historyItem.key, historyItem.placeId,
                    historyItem.budgetId, dateNew.third, dateNew.second, calendar, historyItem.amount, historyItem.baseAmount)
                NotificationManager.notification(context,  historyItem.key, financeViewModel.categoryBeginLiveData.value?.find { it.key == historyItem.placeId }!!.categoryBegin.name, time,calendar,period)

            }
        }
    }

    fun deleteItemAtPosition(position: Int, value:String){
        if(!plan) {
            var remove = false
            if (history[position].isCategory == true || history[position].isGoal == true || history[position].isLoan == true) {
                val budgetItem =
                    financeViewModel.budgetLiveData.value!!.find { it.key == history[position].budgetId }

                if (budgetItem==null){
                    Toast.makeText(
                        context,
                        "Такого бюджета больше нет, операцию нельзя удалить.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else {
                    budgetItem.budgetItem.amount =
                        "%.2f".format(budgetItem.budgetItem.amount.toDouble() + value.toDouble()).replace(",", ".")
                    budgetItem.budgetItem.count -= 1

                    when (history[position].budgetId) {
                        "Base budget" -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                .child("Base budget").setValue(budgetItem.budgetItem)

                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key).removeValue()
                            remove = true
                        }

                        else -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets")
                                .child("Other budget").child(budgetItem.key).setValue(budgetItem.budgetItem)
                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key).removeValue()
                            remove = true
                        }
                    }
                }

                if(!remove){
                    Toast.makeText(context, "Такого бюджета больше нет, операцию нельзя удалить.", Toast.LENGTH_SHORT).show()
                } else{
                    when{
                        history[position].isCategory==true->{
                            val category =  financeViewModel.categoryLiveData.value!!.find { it.key == history[position].placeId }
                            if(category==null){
                                Toast.makeText(
                                    context,
                                    "Такой категории больше нет, операцию нельзя удалить.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else{
                                when (category.categoryItem.remainder) {
                                    "0"->category.categoryItem.total = "%.2f".format(category.categoryItem.total.toDouble() - abs(history[position].baseAmount.toDouble())).replace(",", ".")
                                    else->category.categoryItem.remainder = "%.2f".format(category.categoryItem.remainder.toDouble() + abs(history[position].baseAmount.toDouble())).replace(",", ".")
                                }
                                table.child("Users").child(auth.currentUser!!.uid).child("Categories")
                                    .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                    .child("ExpenseCategories").child(history[position].placeId).setValue(category.categoryItem)

                            }
                        }

                        history[position].isLoan==true->{

                        }

                        history[position].isGoal==true->{

                        }
                    }
                }
            }
            else{
                val budgetItem =
                    financeViewModel.budgetLiveData.value!!.find { it.key == history[position].budgetId }

                if (budgetItem == null){
                    Toast.makeText(
                        context,
                        "Такого бюджета больше нет, операцию нельзя удалить.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else{
                    budgetItem.budgetItem.amount = "%.2f".format(budgetItem.budgetItem.amount.toDouble() - value.toDouble()).replace(",", ".")
                    budgetItem.budgetItem.count-=1

                    when (history[position].budgetId) {
                        "Base budget" -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Base budget").setValue(budgetItem.budgetItem)
                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key).removeValue()
                            remove = true
                        }

                        else -> {
                            table.child("Users").child(auth.currentUser!!.uid).child("Budgets").child("Other budget").child(budgetItem.key).setValue(budgetItem)
                            table.child("Users").child(auth.currentUser!!.uid).child("History")
                                .child("${history[position].date.split(".")[2]}/${history[position].date.split(".")[1].toInt()}")
                                .child(history[position].key).removeValue()
                            remove = true
                        }
                    }
                }
                if(!remove){
                    Toast.makeText(context, "Такого бюджета больше нет, операцию нельзя удалить.", Toast.LENGTH_SHORT).show()
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
            NotificationManager.cancelAlarmManager(context, history[position].key)
            NotificationManager.cancelAutoTransaction(context, history[position].key)
        }
    }

    fun sortByDate(startDate:Calendar?, endDate:Calendar?, historyNew:List<HistoryItem> = history){
        val startDef = Calendar.getInstance()
        val endDef = Calendar.getInstance()
        startDef.set(Calendar.DAY_OF_MONTH, 1)
        endDef.set(Calendar.DAY_OF_MONTH, endDef.getActualMaximum(Calendar.DAY_OF_MONTH))

        sort(historyNew.sortedByDescending { it.date}.filter { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            .parse(it.date)?.let { calendar ->
                val calendarFromString = Calendar.getInstance()
                calendarFromString.time = calendar

                (calendarFromString.get(Calendar.YEAR) >= (startDate?.get(Calendar.YEAR)
                    ?: Calendar.getInstance().get(Calendar.YEAR)) &&
                        calendarFromString.get(Calendar.MONTH) >= (startDate?.get(
                    Calendar.MONTH
                ) ?: Calendar.getInstance().get(Calendar.MONTH)) &&
                        calendarFromString.get(Calendar.DAY_OF_MONTH) >= ((startDate?.get(Calendar.DAY_OF_MONTH))
                    ?: startDef.get(Calendar.DAY_OF_MONTH))
                        &&
                        calendarFromString.get(Calendar.YEAR) <= (endDate?.get(Calendar.YEAR)
                    ?: Calendar.getInstance().get(Calendar.YEAR)) &&
                        calendarFromString.get(Calendar.MONTH) <= (endDate?.get(Calendar.MONTH)
                    ?: Calendar.getInstance().get(Calendar.MONTH)) &&
                        calendarFromString.get(Calendar.DAY_OF_MONTH) <= ((endDate?.get(Calendar.DAY_OF_MONTH))
                    ?: endDef.get(Calendar.DAY_OF_MONTH)))
            } ?: false}.toList())
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
        private val valueOfTramsaction: TextView = itemView.findViewById(R.id.valueOfTransaction)
        private val imageHistory: ImageView = itemView.findViewById(R.id.historyImage)
        private val date: TextView = itemView.findViewById(R.id.dateOfTransaction)


        private val card: LinearLayout = itemView.findViewById(R.id.cardHistory)

        fun bind(historyItem: HistoryItem, position: Int) {
           val financeViewModel = ViewModelProvider(activity)[FinanceViewModel::class.java]

            valueOfTramsaction.text = historyItem.amount + context.resources.getString( context.resources.getIdentifier( financeViewModel.budgetLiveData.value?.find {  it.key == historyItem.budgetId}?.budgetItem?.currency, "string",  context.packageName))

            when(historyItem.placeId){
                ""->{
                    nameBudgetForPlace.visibility = View.GONE
                    nameBudgetTextView.visibility = View.VISIBLE
                    nameBudgetTextView.text = financeViewModel.budgetLiveData.value!!.filter { it.key == historyItem.budgetId}[0].budgetItem.name
                    placeNameTextView.visibility = View.GONE
                    imageHistory.visibility = View.GONE
                    income.visibility = View.VISIBLE
                    valueOfTramsaction.setTextColor(context.resources.getColor(R.color.dark_green, context.theme))
                }
                else->{
                    income.visibility = View.GONE
                    nameBudgetTextView.visibility = View.GONE
                    nameBudgetForPlace.visibility = View.VISIBLE
                    placeNameTextView.visibility = View.VISIBLE
                    imageHistory.visibility = View.VISIBLE

                    nameBudgetForPlace.text = financeViewModel.budgetLiveData.value!!.find { history[position].budgetId == it.key }!!.budgetItem.name
                    valueOfTramsaction.setTextColor(context.resources.getColor(R.color.dark_orange, context.theme))
                    Log.e("checkCategorHistory", historyItem.key)
                    placeNameTextView.text =
                        if(historyItem.isGoal == true) context.resources.getString(R.string.history_goals, financeViewModel.budgetLiveData.value!!.find { history[position].budgetId == it.key }!!.budgetItem.name)
                        else if(historyItem.isLoan == true) context.resources.getString(R.string.history_loans, financeViewModel.budgetLiveData.value!!.find { history[position].budgetId == it.key }!!.budgetItem.name)
                        else financeViewModel.categoryBeginLiveData.value!!.find { history[position].placeId == it.key }!!.categoryBegin.name
                    imageHistory.setImageDrawable(ContextCompat.getDrawable(context, context.resources.getIdentifier(financeViewModel.categoryBeginLiveData.value!!.find { history[position].placeId == it.key }!!.categoryBegin.path, "drawable", context.packageName)))
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
                val resultList = mutableListOf<String>()
                when{
                    year > Calendar.getInstance().get(Calendar.YEAR) -> resultList.addAll(periodList)
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

                    val resultList = mutableListOf<String>()
                    when{
                        history[position].date.split(".")[2].toInt()>Calendar.getInstance().get(Calendar.YEAR) -> resultList.addAll(periodList)
                        history[position].date.split(".")[1].toInt()-1 > Calendar.getInstance().get(Calendar.MONTH) -> {
                            for (i in 0 until 6){
                                resultList.add(periodList[i])
                            }
                            if(1==Calendar.getInstance().get(Calendar.MONTH)){
                                resultList.add(periodList[6])
                            }
                        }
                        history[position].date.split(".")[0].toInt() > Calendar.getInstance().get(Calendar.DAY_OF_MONTH)->{
                            if ( history[position].date.split(".")[0].toInt() - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)>7){
                                for (i in 0 until 6){
                                    resultList.add(periodList[i])
                                }
                            } else if ( history[position].date.split(".")[0].toInt() - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)==7){
                                for (i in 0 until 5){
                                    resultList.add(periodList[i])
                                }

                            }else if ( history[position].date.split(".")[0].toInt() - Calendar.getInstance().get(Calendar.DAY_OF_MONTH)>=3){
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
                    adapterPeriod = ArrayAdapter(context, android.R.layout.simple_spinner_item, resultList)
                    adapterPeriod.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    periodOfNotificationHistory.adapter = adapterPeriod
                    periodOfNotificationHistory.setSelection(resultList.indexOf(periodBegin.ifEmpty {
                        periodList[0]
                    }))

                    timeOfNotificationsHistory.text = timeBegin.ifEmpty { "12:00" }

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
                }
            }

            val number = history[position].amount.filter {string-> string.isDigit()}
            val numberBegin = number.substring(0, number.length-2)+"."+number.substring(number.length-2)
            value.setText(numberBegin)

            val numberBase = history[position].baseAmount.filter {string-> string.isDigit()}
            Log.e("checkCategoryChange_bv",numberBase)

            val beginValueBase = numberBase.substring(0, numberBase.length-2)+"."+numberBase.substring(numberBase.length-2)

            builder.setPositiveButton("Сохранить"){ dialog, _->
                val newValue = "%.2f".format((abs(history[position].baseAmount.toDouble())/numberBegin.toDouble())*value.text.toString().toDouble()).replace(",", ".")
                saveItemAtPosition(position, newValue, value.text.toString(), numberBegin, beginValueBase ,dateBeginTriple, dateNew, timeOfNotificationsHistory.text.toString(),
                       periodOfNotificationHistory.selectedItem.toString())
                dialog.dismiss()
            }

            builder.setNegativeButton("Удалить") { dialog, _ ->
                AlertDialog.Builder(context)
                    .setTitle("Удаление категории")
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

            val dialog = builder.create()
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.window?.setBackgroundDrawableResource(R.drawable.listview_shadow)
            dialog.show()
        }

    }

}