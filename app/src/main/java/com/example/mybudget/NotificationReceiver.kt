package com.example.mybudget

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.mybudget.drawersection.finance.category._CategoryBegin
import com.example.mybudget.drawersection.goals.GoalItem
import com.example.mybudget.drawersection.loans.LoanItem
import com.example.mybudget.drawersection.subs.SubItem
import com.example.mybudget.start_pages.Constants
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelID = intent.getStringExtra("channelID")
        val placeId = intent.getStringExtra("placeId")
        val date = Calendar.getInstance()
        date.timeInMillis = intent.getLongExtra("date", 0)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notificationText = when(channelID){
            Constants.CHANNEL_ID_PLAN->context.resources.getStringArray(R.array.notification_category)
            Constants.CHANNEL_ID_GOAL->context.resources.getStringArray(R.array.notification_goal)
            Constants.CHANNEL_ID_SUB->context.resources.getStringArray(R.array.notification_sub)
            else -> context.resources.getStringArray(R.array.notification_loans)
        }
        if (channelID!=null&&placeId!=null){
            getName(channelID, placeId,/* date,*/ notificationText){
                val notification:Notification = NotificationCompat.Builder(context, channelID)
                .setSmallIcon(R.drawable.piggybank_18)
                .setContentTitle(
                    when (channelID){
                        Constants.CHANNEL_ID_PLAN -> "Не забывайте про запланированные траты!"
                        Constants.CHANNEL_ID_GOAL -> "Цели ждут!"
                        Constants.CHANNEL_ID_SUB -> "Не забывайте о подписках!"
                        else ->"Важные выплаты приближаются!"
                    }
                )
                .setContentText(it)
                .build()
                notificationManager.notify(42, notification)
            }
        }
    }

    private fun getName(channelId:String,placeId:String/*, dateOfExpence:Calendar*/, notificationText:Array<String>, callback: (String) -> Unit){
        when(channelId){
            Constants.CHANNEL_ID_PLAN->{
                Firebase.database.reference
                    .child("Users")
                    .child(Firebase.auth.currentUser!!.uid)
                    .child("Categories")
                    .child("Categories base")
                    .child(placeId).addListenerForSingleValueEvent(object :ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.getValue(_CategoryBegin::class.java)?.let {
                                callback(String.format(notificationText[notificationText.indices.random()], it.name))
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}

                    })
            }
            Constants.CHANNEL_ID_GOAL->{
                Firebase.database.reference
                    .child("Users")
                    .child(Firebase.auth.currentUser!!.uid)
                    .child("Goals")
                    .child(placeId).addListenerForSingleValueEvent(object :ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.getValue(GoalItem::class.java)?.let {
                                callback(String.format(notificationText[notificationText.indices.random()], it.name))
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
            Constants.CHANNEL_ID_SUB->{
                Firebase.database.reference
                    .child("Users")
                    .child(Firebase.auth.currentUser!!.uid)
                    .child("Subs")
                    .child(placeId).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.getValue(SubItem::class.java)?.let {
                                callback(String.format(notificationText[notificationText.indices.random()], it.name))
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }

            Constants.CHANNEL_ID_LOAN->{
                Firebase.database.reference
                    .child("Users")
                    .child(Firebase.auth.currentUser!!.uid)
                    .child("Loans")
                    .child(placeId).addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.getValue(LoanItem::class.java)?.let {
                                callback(String.format(notificationText[notificationText.indices.random()], it.name, it.dateNext?:it.dateOfEnd))
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }
    }
}