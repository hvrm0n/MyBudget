package com.example.mybudget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mybudget.drawersection.finance.HistoryItem
import com.example.mybudget.drawersection.finance.budget._BudgetItem
import com.example.mybudget.drawersection.finance.category._CategoryItem
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
import kotlin.math.abs

class TransactionReceiver : BroadcastReceiver() {

    private lateinit var auth: FirebaseAuth
    private lateinit var table: DatabaseReference

    override fun onReceive(context: Context, intent: Intent) {
        val categoryId = intent.getStringExtra("categoryId")
        val budgetId = intent.getStringExtra("budgetId")
        val year = intent.getStringExtra("year")
        val month = intent.getStringExtra("month")
        val baseAmount = intent.getStringExtra("baseAmount")
        val amount = intent.getStringExtra("amount")
        val planId = intent.getStringExtra("planId")
        Log.e("CategoryCheck", year.toString())
        Log.e("CategoryCheck", month.toString())
        Log.e("CategoryCheck", categoryId.toString())
        Log.e("CategoryCheck", baseAmount.toString())
        Log.e("CategoryCheck", "EnterTransaction")
        auth = Firebase.auth
        table = Firebase.database.reference

        updateCategory(year!!.toInt(), month!!.toInt(), categoryId!!, baseAmount!!)
        updateBudget(budgetId!!, amount!!)
        planToHistory(year.toInt(), month.toInt(), planId!!)
        NotificationManager.cancelAlarmManager(context, planId)
        NotificationManager.cancelAutoTransaction(context, planId)
    }

    private fun updateCategory(year: Int, month: Int, categoryId:String, baseAmount: String){
        val categoryReference = table.child("Users").child(auth.currentUser!!.uid)
            .child("Categories").child("$year/$month")
            .child("ExpenseCategories").child(categoryId)

        categoryReference.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val category = mutableData.getValue(_CategoryItem::class.java)
                if (category != null) {
                    when(category.remainder){
                        "0"->category.total = String.format("%.2f",category.total.toDouble()+abs(baseAmount.toDouble())).replace(',','.')
                        else->category.remainder = String.format("%.2f",category.remainder.toDouble()-abs(baseAmount.toDouble())).replace(',','.')
                    }
                    mutableData.value = category
                }
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (databaseError != null) {
                    Log.d("TAG", "Transaction failed: ${databaseError.message}")
                } else {
                    Log.d("TAG", "Transaction completed. Committed: $committed")
                }
            }
        })
    }

    private fun updateBudget(budgetId:String, amount:String){
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
                    budget.amount = String.format("%.2f", budget.amount.toDouble() - abs(amount.toDouble())).replace(',', '.')
                    budget.count++
                    mutableData.value = budget
                }
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (databaseError != null) {
                    Log.d("TAG", "Transaction failed: ${databaseError.message}")
                } else {
                    Log.d("TAG", "Transaction completed. Committed: $committed")
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