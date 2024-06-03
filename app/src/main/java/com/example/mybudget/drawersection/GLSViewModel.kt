package com.example.mybudget.drawersection

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.mybudget.BudgetNotificationManager
import com.example.mybudget.drawersection.goals.GoalItem
import com.example.mybudget.drawersection.loans.LoanItem
import com.example.mybudget.drawersection.subs.SubItem
import com.example.mybudget.drawersection.subs.SubItemWithKey
import com.example.mybudget.start_pages.Constants
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import java.util.Calendar

class GLSViewModel:ViewModel() {
    private val table = Firebase.database.reference
    private val auth = Firebase.auth
    fun restoreLoan(key:String){
        table.child("Users")
            .child(Firebase.auth.currentUser!!.uid)
            .child("Loans")
            .child(key)
            .child("deleted").setValue(false)
    }

    fun updateLoan(key: String, loanItem: LoanItem){
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Loans")
            .child(key)
            .setValue(loanItem)
    }

    fun makeLoan(loanItem: LoanItem, context: Context, time:String, beginCalendar: Calendar, periodOfNotification:String){
        val loanReference = table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Loans")
            .push()

        loanReference.setValue(loanItem).addOnCompleteListener {
            BudgetNotificationManager.notification(
                context = context,
                channelID = Constants.CHANNEL_ID_LOAN,
                id = loanReference.key!!,
                placeId =  loanReference.key!!,
                time = time,
                dateOfExpence = beginCalendar,
                periodOfNotification = periodOfNotification
            )
        }
    }

    fun makeNewSub(subItem: SubItem, context: Context, time:String, dateLS:Calendar, periodOfNotification: String){
        val subReference = table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Subs")
            .push()
        subReference.setValue(subItem).addOnCompleteListener {
            BudgetNotificationManager.notification(
                context = context,
                channelID = Constants.CHANNEL_ID_SUB,
                id = subReference.key!!,
                placeId =  subReference.key!!,
                time = time,
                dateOfExpence = dateLS,
                periodOfNotification = periodOfNotification
            )
            BudgetNotificationManager.setAutoTransaction(
                context = context,
                id = subReference.key!!,
                placeId = subReference.key!!,
                year = dateLS.get(Calendar.YEAR),
                month = dateLS.get(Calendar.MONTH)-1,
                dateOfExpence = dateLS,
                type = Constants.CHANNEL_ID_SUB
            )
        }
    }

    fun updateSub(key:String, subItem: SubItem, subItemWithKey:SubItemWithKey, context: Context, time:String, dateLS:Calendar, periodOfNotification: String){
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Subs")
            .child(key)
            .setValue(subItem)
        BudgetNotificationManager.cancelAlarmManager(context, subItemWithKey.key)
        BudgetNotificationManager.cancelAutoTransaction(context, subItemWithKey.key)

        BudgetNotificationManager.notification(
            context = context,
            channelID = Constants.CHANNEL_ID_SUB,
            id = subItemWithKey.key,
            placeId = subItemWithKey.key,
            time = time,
            dateOfExpence = dateLS,
            periodOfNotification = periodOfNotification
        )
        BudgetNotificationManager.setAutoTransaction(
            context = context,
            id = subItemWithKey.key,
            placeId = subItemWithKey.key,
            year = dateLS.get(Calendar.YEAR),
            month = dateLS.get(Calendar.MONTH)-1,
            dateOfExpence = dateLS,
            type = Constants.CHANNEL_ID_SUB
        )
    }

    fun cancelledSub(key: String){
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Subs")
            .child(key)
            .child("cancelled").setValue(false)
    }

    fun restoreSub(key: String){
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Subs")
            .child(key)
            .child("deleted").setValue(false)
    }

    fun makeNewGoal(goalItem: GoalItem, periodOfNotificationPosition: Int, context: Context, time: String, dateOfEnd:Calendar, periodOfNotification: String){
        val goalReference = table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Goals")
            .push()
        goalReference.setValue(
            goalItem
        ).addOnCompleteListener {
            if (periodOfNotificationPosition!=-1){
                BudgetNotificationManager.notification(
                    context = context,
                    channelID = Constants.CHANNEL_ID_GOAL,
                    id = goalReference.key.toString(),
                    placeId = null,
                    time = time,
                    dateOfExpence = dateOfEnd,
                    periodOfNotification = periodOfNotification
                )
            }
        }
    }

    fun updateGoal(key: String, goalItem: GoalItem, context: Context, periodOfNotificationPosition: Int, time: String, dateOfEnd: Calendar, periodOfNotification: String){
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Goals")
            .child(key)
            .setValue(goalItem)
        BudgetNotificationManager.cancelAlarmManager(context, key)
        if (periodOfNotificationPosition!=-1){
            BudgetNotificationManager.notification(
                context = context,
                channelID = Constants.CHANNEL_ID_GOAL,
                placeId = null,
                id = key,
                time = time,
                dateOfExpence = dateOfEnd,
                periodOfNotification = periodOfNotification
            )
        }
    }

    fun saveGoal(key: String){
        table.child("Users")
            .child(auth.currentUser!!.uid)
            .child("Goals")
            .child(key)
            .child("deleted").setValue(false)
    }
}