package com.example.mybudget.drawersection.finance

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.mybudget.BudgetNotificationManager
import com.example.mybudget.drawersection.finance.budget.BudgetItemWithKey
import com.example.mybudget.drawersection.finance.budget._BudgetItem
import com.example.mybudget.drawersection.finance.category._CategoryItem
import com.example.mybudget.drawersection.finance.history.HistoryItem
import com.example.mybudget.start_pages.Constants
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

class NewTransactionViewModel:ViewModel() {
    private var table: DatabaseReference = Firebase.database.reference
    private var auth: FirebaseAuth = Firebase.auth

    fun addIncomeBase(valueDouble:Double, dateOfIncome:Calendar, budget:BudgetItemWithKey){
        val reference = table.child("Users").child(auth.currentUser!!.uid)
            .child("Budgets").child("Base budget")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentBudgetItem = snapshot.getValue(_BudgetItem::class.java)
                currentBudgetItem?.amount = "%.2f".format(currentBudgetItem?.amount!!.toDouble()+valueDouble).replace(',','.')
                currentBudgetItem.count += 1
                reference.setValue(currentBudgetItem).addOnCompleteListener {

                    val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                        .child("${dateOfIncome.get(Calendar.YEAR)}/${dateOfIncome.get(Calendar.MONTH)+1}")
                        .push()

                    newHistory.setValue(
                        HistoryItem(budgetId = budget.key, amount = "%.2f".format(valueDouble).replace(',','.'),
                            baseAmount = "%.2f".format(valueDouble).replace(',','.'),
                            date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfIncome.time), key = newHistory.key.toString())
                    )
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addIncomeOthers(key:String, valueDouble:Double, valueDoubleOthers:Double, dateOfIncome:Calendar){
        val reference = table.child("Users").child(auth.currentUser!!.uid)
            .child("Budgets").child("Other budget")
            .child(key)
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentBudgetItem = snapshot.getValue(_BudgetItem::class.java)
                currentBudgetItem?.amount = "%.2f".format(currentBudgetItem?.amount!!.toDouble()+valueDouble).replace(',','.')
                currentBudgetItem.count += 1
                reference.setValue(currentBudgetItem).addOnCompleteListener {
                    val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                        .child("${dateOfIncome.get(Calendar.YEAR)}/${dateOfIncome.get(Calendar.MONTH)+1}")
                        .push()
                    newHistory.setValue(
                        HistoryItem(budgetId = key, amount = "%.2f".format(if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.'),
                            baseAmount = "%.2f".format(if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.'),
                            date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfIncome.time), key = newHistory.key.toString())
                    )
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addTransfer(budgetFrom:BudgetItemWithKey, budgetTo:BudgetItemWithKey, valueDoubleOthers:Double, valueDouble:Double, dateOfExpence:Calendar){
        budgetFrom.budgetItem.count++
        budgetTo.budgetItem.count++
        budgetFrom.budgetItem.amount = "%.2f".format(budgetFrom.budgetItem.amount.toDouble() - valueDoubleOthers).replace(",", ".")
        budgetTo.budgetItem.amount = "%.2f".format(budgetTo.budgetItem.amount.toDouble() + valueDouble).replace(",", ".")
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
    }

    fun addNewPlanCategory(dateOfExpence:Calendar, financeViewModel: FinanceViewModel, valueDouble:Double, valueDoubleOthers: Double, nameCategory:String, nameBudget:String, periodOfNotification:Long, context: Context, time:String, period:String){
        val planReferense =  table.child("Users").child(auth.currentUser!!.uid).child("Plan")
            .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
            .push()
        planReferense.setValue(
            HistoryItem(placeId = financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key, budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key,
                amount = "-${"%.2f".format(if( valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}",
                baseAmount = "-${"%.2f".format(valueDouble).replace(',','.')}",
                date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),  isCategory = true,
                key = planReferense.key.toString())
        )
        if(periodOfNotification!=0L && periodOfNotification!=-1L) {
            BudgetNotificationManager.notification(
                context,
                Constants.CHANNEL_ID_PLAN,
                planReferense.key.toString(),
                financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory } ?.get(0)!!.key,
                time,
                dateOfExpence,
                period)
        }
        BudgetNotificationManager.setAutoTransaction(
            context,
            planReferense.key.toString(),
            financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key,
            dateOfExpence.get(Calendar.YEAR),
            dateOfExpence.get(Calendar.MONTH)+1,
            dateOfExpence,
            Constants.CHANNEL_ID_PLAN)
    }

    fun categoryExpence(reference:DatabaseReference, reference2:DatabaseReference, currentBudgetItem:_BudgetItem, currentCategoryExpence2:_CategoryItem, dateOfExpence:Calendar, financeViewModel: FinanceViewModel, nameBudget: String, nameCategory: String, valueDoubleOthers: Double, valueDouble: Double){
        reference.setValue(currentBudgetItem)
        reference2.setValue(currentCategoryExpence2).addOnCompleteListener {

            val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                .push()
            newHistory.setValue(
                HistoryItem(budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key,placeId = financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key,
                    isCategory = true, amount = "-${"%.2f".format(if( valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}"
                    ,baseAmount = "-${"%.2f".format(valueDouble).replace(',','.')}",
                    date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),
                    key = newHistory.key.toString())
            )
        }
    }

    fun updateNewPlanCategory(reference2: DatabaseReference, currentCategoryExpence2: _CategoryItem, dateOfExpence:Calendar, financeViewModel: FinanceViewModel, valueDouble:Double, valueDoubleOthers: Double, nameCategory:String, nameBudget:String, periodOfNotification:Long, context: Context, time:String, period:String){
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
                        "%.2f".format(
                            if (valueDoubleOthers != 0.0) valueDoubleOthers else valueDouble
                        ).replace(',', '.')
                    }",
                    baseAmount = "-${
                        "%.2f".format(valueDouble)
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
            if(periodOfNotification!=0L && periodOfNotification!=-1L) {
                BudgetNotificationManager.notification(
                    context,
                    Constants.CHANNEL_ID_PLAN,
                    planReferense.key.toString(),
                    financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory } ?.get(0)!!.key,
                    time,
                    dateOfExpence,
                    period)
            }
            BudgetNotificationManager.setAutoTransaction(
                context,
                planReferense.key.toString(),
                financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key,
                dateOfExpence.get(Calendar.YEAR),
                dateOfExpence.get(Calendar.MONTH)+1,
                dateOfExpence,
                Constants.CHANNEL_ID_PLAN)
            }
    }

    fun newTransactionBase(dateOfExpence:Calendar, financeViewModel: FinanceViewModel, valueDouble:Double, valueDoubleOthers: Double, nameCategory:String, nameBudget:String, periodOfNotification:Long, context: Context, time:String, period:String){
        val planReferense =  table.child("Users").child(auth.currentUser!!.uid).child("Plan")
            .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
            .push()
        planReferense.setValue(
            HistoryItem(placeId = financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key, budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key,
                amount = "-${"%.2f".format(if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}",
                baseAmount = "-${"%.2f".format(valueDouble).replace(',','.')}",
                date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),  isCategory = true,
                key = planReferense.key.toString())
        )
        if(periodOfNotification!=0L && periodOfNotification!=-1L) {
            BudgetNotificationManager.notification(
                context,
                Constants.CHANNEL_ID_PLAN,
                planReferense.key.toString(),
                financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory } ?.get(0)!!.key,
                time,
                dateOfExpence,
                period)
        }
        BudgetNotificationManager.setAutoTransaction(
            context,
            planReferense.key.toString(),
            financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key,
            dateOfExpence.get(Calendar.YEAR),
            dateOfExpence.get(Calendar.MONTH)+1,
            dateOfExpence,
            Constants.CHANNEL_ID_PLAN)
    }

    fun newPlanBase(reference2: DatabaseReference, currentCategoryExpence:_CategoryItem, dateOfExpence:Calendar, financeViewModel: FinanceViewModel, valueDouble:Double, valueDoubleOthers: Double, nameCategory:String, nameBudget:String, periodOfNotification:Long, context: Context, time:String, period:String){
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
                        "%.2f".format(
                            if (valueDoubleOthers != 0.0) valueDoubleOthers else valueDouble
                        ).replace(',', '.')
                    }",
                    baseAmount = "-${
                        "%.2f".format(valueDouble)
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
            if(periodOfNotification!=0L && periodOfNotification!=-1L) {
                BudgetNotificationManager.notification(
                    context,
                    Constants.CHANNEL_ID_PLAN,
                    planReferense.key.toString(),
                    financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory } ?.get(0)!!.key,
                    time,
                    dateOfExpence,
                    period)
            }
            BudgetNotificationManager.setAutoTransaction(
                context,
                planReferense.key.toString(),
                financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory }?.get(0)!!.key,
                dateOfExpence.get(Calendar.YEAR),
                dateOfExpence.get(Calendar.MONTH)+1,
                dateOfExpence,
                Constants.CHANNEL_ID_PLAN)
        }
    }

    fun newHistoryBase(reference:DatabaseReference, reference2:DatabaseReference, currentBudgetItem:_BudgetItem, currentCategoryExpence:_CategoryItem, dateOfExpence:Calendar, financeViewModel: FinanceViewModel, nameBudget: String, nameCategory: String, valueDoubleOthers: Double, valueDouble: Double){
        reference.setValue(currentBudgetItem)
        reference2.setValue(currentCategoryExpence).addOnCompleteListener {
            val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                .push()
            newHistory.setValue(
                HistoryItem(budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key, financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key,
                    isCategory = true, amount = "-${"%.2f".format(if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}"
                    , baseAmount = "-${"%.2f".format(valueDouble).replace(',','.')}",
                    date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),
                    key = newHistory.key.toString())
            )
        }
    }

    fun newHistoryBaseTooMuch(reference:DatabaseReference, reference2:DatabaseReference, currentBudgetItem:_BudgetItem, currentCategoryExpence:_CategoryItem, dateOfExpence:Calendar, financeViewModel: FinanceViewModel, nameBudget: String, nameCategory: String, valueDoubleOthers: Double, valueDouble: Double){
        reference.setValue(currentBudgetItem)
        reference2.setValue(currentCategoryExpence).addOnCompleteListener {
            val newHistory = table.child("Users").child(auth.currentUser!!.uid).child("History")
                .child("${dateOfExpence.get(Calendar.YEAR)}/${dateOfExpence.get(Calendar.MONTH)+1}")
                .push()
            newHistory.setValue(
                HistoryItem(budgetId = financeViewModel.budgetLiveData.value?.filter { it.budgetItem.name == nameBudget}?.get(0)!!.key, financeViewModel.categoryBeginLiveData.value?.filter { it.categoryBegin.name == nameCategory}?.get(0)!!.key,
                    isCategory = true, amount = "-${"%.2f".format(if(valueDoubleOthers!=0.0) valueDoubleOthers else valueDouble).replace(',','.')}",
                    baseAmount = "-${"%.2f".format(valueDouble).replace(',','.')}",
                    date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(dateOfExpence.time),
                    key = newHistory.key.toString())
            )
        }
    }
}