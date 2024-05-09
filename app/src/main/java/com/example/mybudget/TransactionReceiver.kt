package com.example.mybudget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mybudget.drawersection.finance.HistoryItem
import com.example.mybudget.drawersection.finance.budget._BudgetItem
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.example.mybudget.drawersection.subs.SubItem
import com.example.mybudget.start_pages.Constants
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class TransactionReceiver : BroadcastReceiver() {

    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference

    override fun onReceive(context: Context, intent: Intent) {
        val categoryId = intent.getStringExtra("categoryId")
        val budgetId = intent.getStringExtra("budgetId")
        val year = intent.getStringExtra("year")
        val month = intent.getStringExtra("month")
        val planId = intent.getStringExtra("planId")
        val type = intent.getStringExtra("type")
        auth = Firebase.auth
        table = Firebase.database.reference
        when(type){
            Constants.CHANNEL_ID_LOAN->{}
            Constants.CHANNEL_ID_SUB->{
                updateSub(context, budgetId!!, planId!!)
            }
            Constants.CHANNEL_ID_PLAN->{
                updateCategory(year!!.toInt(), month!!.toInt(), categoryId!!, planId!!)
                NotificationManager.cancelAlarmManager(context, planId)
                NotificationManager.cancelAutoTransaction(context, planId)
            }
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

    private fun updateSub(context: Context, budgetId: String, planId: String){
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Subs")
            .child(planId)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(SubItem::class.java)?.let {subItem->
                        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(subItem.date)
                        val calendar = Calendar.getInstance().apply {
                            time = date!!
                        }
                        calendar.add(when(subItem.period.split(" ")[1]){
                            "d"->Calendar.DAY_OF_MONTH
                            "w"->Calendar.WEEK_OF_MONTH
                            "m"->Calendar.MONTH
                            else->Calendar.YEAR
                        }, subItem.period.split(" ")[0].toInt())

                        subItem.date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)
                        table.child("Users").child(auth.currentUser!!.uid).child("Subs").child(planId).setValue(subItem).addOnSuccessListener {
                            val budgetReference =
                                when(subItem.budgetId){
                                    "Base budget"-> table.child("Users")
                                        .child(auth.currentUser!!.uid)
                                        .child("Budgets")
                                        .child("Base budget")

                                    else->table.child("Users")
                                        .child(auth.currentUser!!.uid)
                                        .child("Budgets")
                                        .child("Other budget")
                                        .child(subItem.budgetId)
                                }


                            budgetReference.addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    snapshot.getValue(_BudgetItem::class.java)?.let {
                                        it.amount =  "%.2f".format(it.amount.toDouble() - subItem.amount.toDouble()).replace(",", ".")
                                        it.count++
                                        budgetReference.setValue(it).addOnSuccessListener {
                                            val historyItem = table.child("Users").child(auth.currentUser!!.uid).child("History")
                                                .child("${Calendar.getInstance().get(Calendar.YEAR)}/${Calendar.getInstance().get(Calendar.MONTH)+1}").push()
                                            historyItem.setValue(
                                                HistoryItem(
                                                    budgetId = subItem.budgetId,
                                                    placeId = planId,
                                                    isSub = true,
                                                    amount = "-${subItem.amount}",
                                                    date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Calendar.getInstance().time),
                                                    baseAmount = "-${subItem.amount}",
                                                    key = historyItem.key.toString()
                                                )
                                            )
                                            val sharedPreferences = context.getSharedPreferences("NotificationPeriodAndTime", Context.MODE_PRIVATE)
                                            val periodBegin = sharedPreferences.getString(planId, "|")?.split("|")?.get(0)?: context.resources.getStringArray(R.array.periodicity)[0]
                                            val timeBegin = sharedPreferences.getString(planId, "|")?.split("|")?.get(1)?:"12:00"

                                            updateSubNotification(
                                                context = context,
                                                id = planId,
                                                time = timeBegin,
                                                dateOfExpence = Calendar.getInstance().apply{
                                                    set(subItem.date.split(".")[2].toInt(),
                                                    subItem.date.split(".")[1].toInt()-1,
                                                    subItem.date.split(".")[0].toInt())},
                                                periodOfNotification = periodBegin,
                                                budgetId = budgetId,
                                                year = subItem.date.split(".")[2].toInt(),
                                                month = subItem.date.split(".")[1].toInt()
                                            )
                                        }
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("CheckNotification_NR", error.message)
                                    NotificationManager.cancelAlarmManager(context, planId)
                                    NotificationManager.cancelAutoTransaction(context, planId)
                                }
                            })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CheckNotification_NR", error.message)
                    NotificationManager.cancelAlarmManager(context, planId)
                    NotificationManager.cancelAutoTransaction(context, planId)
                }

            })
    }

    private fun updateSubNotification(context: Context, id:String, time:String, dateOfExpence:Calendar, periodOfNotification:String, budgetId: String, year: Int, month: Int){
        NotificationManager.cancelAlarmManager(context, id)
        NotificationManager.cancelAutoTransaction(context, id)

        NotificationManager.notification(
            context = context,
            channelID = Constants.CHANNEL_ID_SUB,
            id = id,
            placeId = id,
            time = time,
            dateOfExpence = dateOfExpence,
            periodOfNotification = periodOfNotification
        )
        NotificationManager.setAutoTransaction(
            context = context,
            id = id,
            placeId = id,
            budgetId = budgetId,
            year = year,
            month = month,
            dateOfExpence = dateOfExpence,
            type = Constants.CHANNEL_ID_SUB
        )
    }
}